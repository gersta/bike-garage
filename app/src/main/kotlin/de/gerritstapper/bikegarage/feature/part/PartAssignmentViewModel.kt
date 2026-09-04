package de.gerritstapper.bikegarage.feature.part

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.usecase.AssignPartToBikeUseCase
import de.gerritstapper.bikegarage.domain.usecase.GetPartsUseCase
import de.gerritstapper.bikegarage.ui.navigation.BIKE_ID_ARGUMENT
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PartAssignmentSearchQuery(
    val name: String = "",
    val manufacturer: String = "",
    val manufacturerNumber: String = "",
    val type: String = "",
) {
    val isEmpty: Boolean
        get() = name.isBlank() &&
            manufacturer.isBlank() &&
            manufacturerNumber.isBlank() &&
            type.isBlank()
}

internal data class PartAssignmentRow(
    val id: Long,
    val name: String,
    val category: PartCategory,
    val manufacturer: String?,
    val manufacturerNumber: String?,
    val type: String?,
)

internal data class PartAssignmentUiState(
    val query: PartAssignmentSearchQuery = PartAssignmentSearchQuery(),
    val parts: List<PartAssignmentRow> = emptyList(),
    val assigningPartId: Long? = null,
    val failedPartId: Long? = null,
)

internal sealed interface PartAssignmentEvent {
    data object Assigned : PartAssignmentEvent
}

@HiltViewModel
class PartAssignmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getParts: GetPartsUseCase,
    private val assignPartToBike: AssignPartToBikeUseCase,
) : ViewModel() {
    private val bikeId = checkNotNull(savedStateHandle.get<Long>(BIKE_ID_ARGUMENT))
    private val query = MutableStateFlow(PartAssignmentSearchQuery())
    private val assignment = MutableStateFlow(PartAssignmentOperation())
    private val _events = MutableSharedFlow<PartAssignmentEvent>()
    internal val events = _events.asSharedFlow()

    internal val uiState: StateFlow<PartAssignmentUiState> = combine(
        getParts(),
        query,
        assignment,
    ) { parts, searchQuery, operation ->
        PartAssignmentUiState(
            query = searchQuery,
            parts = parts.assignableTo(bikeId, searchQuery),
            assigningPartId = operation.assigningPartId,
            failedPartId = operation.failedPartId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PartAssignmentUiState(),
    )

    fun onNameChanged(value: String) = updateQuery { it.copy(name = value) }

    fun onManufacturerChanged(value: String) = updateQuery { it.copy(manufacturer = value) }

    fun onManufacturerNumberChanged(value: String) =
        updateQuery { it.copy(manufacturerNumber = value) }

    fun onTypeChanged(value: String) = updateQuery { it.copy(type = value) }

    fun clearQuery() {
        query.value = PartAssignmentSearchQuery()
    }

    fun assignPart(partId: Long) {
        if (assignment.value.assigningPartId != null || uiState.value.parts.none { it.id == partId }) {
            return
        }

        assignment.value = PartAssignmentOperation(assigningPartId = partId)
        viewModelScope.launch {
            try {
                assignPartToBike(partId, bikeId)
                assignment.value = PartAssignmentOperation()
                _events.emit(PartAssignmentEvent.Assigned)
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                assignment.value = PartAssignmentOperation(failedPartId = partId)
            }
        }
    }

    private fun updateQuery(transform: (PartAssignmentSearchQuery) -> PartAssignmentSearchQuery) {
        query.update(transform)
        assignment.update { it.copy(failedPartId = null) }
    }
}

private data class PartAssignmentOperation(
    val assigningPartId: Long? = null,
    val failedPartId: Long? = null,
)

internal fun List<PartWithRelations>.assignableTo(
    bikeId: Long,
    query: PartAssignmentSearchQuery,
): List<PartAssignmentRow> =
    asSequence()
        .filter { part -> part.bikes.none { it.id == bikeId } }
        .filter { it.part.matches(query) }
        .map { part ->
            PartAssignmentRow(
                id = part.part.id,
                name = part.part.name,
                category = part.part.category,
                manufacturer = part.part.manufacturer,
                manufacturerNumber = part.part.manufacturerNumber,
                type = part.part.type,
            )
        }
        .sortedWith(
            compareBy<PartAssignmentRow> { it.name.lowercase() }
                .thenBy { it.id },
        )
        .toList()

private fun Part.matches(query: PartAssignmentSearchQuery): Boolean =
    name.contains(query.name.trim(), ignoreCase = true) &&
        manufacturer.orEmpty().contains(query.manufacturer.trim(), ignoreCase = true) &&
        manufacturerNumber.orEmpty().contains(query.manufacturerNumber.trim(), ignoreCase = true) &&
        type.orEmpty().contains(query.type.trim(), ignoreCase = true)
