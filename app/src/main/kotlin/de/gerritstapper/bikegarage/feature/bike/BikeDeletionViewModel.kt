package de.gerritstapper.bikegarage.feature.bike

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import de.gerritstapper.bikegarage.domain.usecase.BikeDeletionInspection
import de.gerritstapper.bikegarage.domain.usecase.DeleteBikeResult
import de.gerritstapper.bikegarage.domain.usecase.DeleteBikeUseCase
import de.gerritstapper.bikegarage.ui.navigation.BIKE_ID_ARGUMENT
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class BikeDeletionError {
    DELETE_FAILED,
    BIKE_NOT_FOUND,
}

internal data class BikeDeletionUiState(
    val bike: Bike? = null,
    val linkedParts: List<Part> = emptyList(),
    val selectedPartIds: Set<Long> = emptySet(),
    val showConfirmation: Boolean = false,
    val error: BikeDeletionError? = null,
    val isLoading: Boolean = true,
    val isChecking: Boolean = false,
    val isDeleting: Boolean = false,
) {
    val isBusy: Boolean
        get() = isChecking || isDeleting
}

internal sealed interface BikeDeletionEvent {
    data object Deleted : BikeDeletionEvent
}

@HiltViewModel
class BikeDeletionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bikeRepository: BikeRepository,
    private val deleteBike: DeleteBikeUseCase,
) : ViewModel() {
    private val bikeId = checkNotNull(savedStateHandle.get<Long>(BIKE_ID_ARGUMENT))

    private val _uiState = MutableStateFlow(BikeDeletionUiState())
    internal val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BikeDeletionEvent>()
    internal val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            bikeRepository.observeBike(bikeId).collectLatest { bikeWithImages ->
                _uiState.update {
                    it.copy(
                        bike = bikeWithImages?.bike,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun requestDeletion() {
        if (_uiState.value.isBusy) return

        _uiState.update {
            it.copy(
                linkedParts = emptyList(),
                selectedPartIds = emptySet(),
                showConfirmation = false,
                error = null,
                isChecking = true,
            )
        }
        viewModelScope.launch {
            try {
                when (val inspection = deleteBike.inspect(bikeId)) {
                    is BikeDeletionInspection.Ready -> _uiState.update {
                        it.copy(
                            bike = inspection.bike,
                            linkedParts = inspection.parts,
                            showConfirmation = true,
                            isChecking = false,
                        )
                    }

                    BikeDeletionInspection.NotFound -> _uiState.update {
                        it.copy(
                            error = BikeDeletionError.BIKE_NOT_FOUND,
                            isChecking = false,
                        )
                    }
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.update {
                    it.copy(error = BikeDeletionError.DELETE_FAILED, isChecking = false)
                }
            }
        }
    }

    fun togglePartSelection(partId: Long) {
        _uiState.update { state ->
            if (state.isBusy || partId !in state.linkedParts.map { it.id }) {
                state
            } else {
                state.copy(
                    selectedPartIds = state.selectedPartIds.toggle(partId),
                    error = null,
                )
            }
        }
    }

    fun dismissConfirmation() {
        if (!_uiState.value.isDeleting) {
            _uiState.update { it.copy(showConfirmation = false) }
        }
    }

    fun confirmDeletion() {
        val state = _uiState.value
        if (state.isBusy || !state.showConfirmation) return

        _uiState.update {
            it.copy(showConfirmation = false, error = null, isDeleting = true)
        }
        viewModelScope.launch {
            when (val result = deleteBike.delete(bikeId, state.selectedPartIds)) {
                is DeleteBikeResult.Deleted -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.emit(BikeDeletionEvent.Deleted)
                }

                DeleteBikeResult.NotFound -> _uiState.update {
                    it.copy(error = BikeDeletionError.BIKE_NOT_FOUND, isDeleting = false)
                }

                DeleteBikeResult.Failed -> _uiState.update {
                    it.copy(error = BikeDeletionError.DELETE_FAILED, isDeleting = false)
                }
            }
        }
    }
}

private fun Set<Long>.toggle(id: Long): Set<Long> =
    if (id in this) this - id else this + id
