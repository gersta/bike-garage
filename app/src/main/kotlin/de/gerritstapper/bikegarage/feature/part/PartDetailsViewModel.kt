package de.gerritstapper.bikegarage.feature.part

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.ui.navigation.PART_ID_ARGUMENT
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal data class PartDetailsUiState(
    val isLoading: Boolean = true,
    val details: PartWithRelations? = null,
)

@HiltViewModel
class PartDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    partRepository: PartRepository,
) : ViewModel() {
    private val partId = checkNotNull(savedStateHandle.get<Long>(PART_ID_ARGUMENT))

    internal val uiState: StateFlow<PartDetailsUiState> = partRepository.observePart(partId)
        .map { details -> PartDetailsUiState(isLoading = false, details = details) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PartDetailsUiState(),
        )
}
