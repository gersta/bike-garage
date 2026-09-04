package de.gerritstapper.bikegarage.feature.shop

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import de.gerritstapper.bikegarage.ui.navigation.SHOP_ID_ARGUMENT
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal data class ShopDetailsUiState(
    val isLoading: Boolean = true,
    val shop: Shop? = null,
    val purchasedBikes: List<Bike> = emptyList(),
    val parts: List<PartWithRelations> = emptyList(),
)

@HiltViewModel
class ShopDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    shopRepository: ShopRepository,
    partRepository: PartRepository,
) : ViewModel() {
    private val shopId = checkNotNull(savedStateHandle.get<Long>(SHOP_ID_ARGUMENT))

    internal val uiState: StateFlow<ShopDetailsUiState> = combine(
        shopRepository.observeShop(shopId),
        partRepository.observeParts(),
    ) { shopRelations, parts ->
        if (shopRelations == null) {
            ShopDetailsUiState(isLoading = false)
        } else {
            ShopDetailsUiState(
                isLoading = false,
                shop = shopRelations.shop,
                purchasedBikes = shopRelations.purchasedBikes,
                parts = parts.filter { part ->
                    part.shopLinks.any { link -> link.link.shopId == shopId }
                },
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShopDetailsUiState(),
    )
}
