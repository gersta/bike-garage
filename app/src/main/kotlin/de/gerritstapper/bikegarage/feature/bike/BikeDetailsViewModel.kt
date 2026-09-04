package de.gerritstapper.bikegarage.feature.bike

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.domain.usecase.BikeDetails
import de.gerritstapper.bikegarage.domain.usecase.GetBikeDetailsUseCase
import de.gerritstapper.bikegarage.domain.usecase.SetBikeShopUseCase
import de.gerritstapper.bikegarage.ui.navigation.BIKE_ID_ARGUMENT
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BikeDetailsUiState(
    val isLoading: Boolean = true,
    val details: BikeDetails? = null,
    val isAssigningShop: Boolean = false,
    val shopAssignmentFailed: Boolean = false,
)

@HiltViewModel
class BikeDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getBikeDetails: GetBikeDetailsUseCase,
    private val setBikeShop: SetBikeShopUseCase,
) : ViewModel() {
    private val bikeId = checkNotNull(savedStateHandle.get<Long>(BIKE_ID_ARGUMENT))
    private val shopAssignment = MutableStateFlow(BikeShopAssignmentState())

    val uiState: StateFlow<BikeDetailsUiState> = combine(
        getBikeDetails(bikeId),
        shopAssignment,
    ) { details, assignment ->
        BikeDetailsUiState(
            isLoading = false,
            details = details,
            isAssigningShop = assignment.isAssigning,
            shopAssignmentFailed = assignment.failed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BikeDetailsUiState(),
    )

    fun selectCreatedShop(shopId: Long) {
        val details = uiState.value.details ?: return
        if (uiState.value.isAssigningShop) return

        shopAssignment.value = BikeShopAssignmentState(isAssigning = true)
        viewModelScope.launch {
            try {
                setBikeShop(details.bikeWithImages, shopId)
                shopAssignment.value = BikeShopAssignmentState()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                shopAssignment.update { it.copy(isAssigning = false, failed = true) }
            }
        }
    }
}

private data class BikeShopAssignmentState(
    val isAssigning: Boolean = false,
    val failed: Boolean = false,
)
