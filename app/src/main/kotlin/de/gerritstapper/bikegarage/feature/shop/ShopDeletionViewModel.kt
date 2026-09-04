package de.gerritstapper.bikegarage.feature.shop

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import de.gerritstapper.bikegarage.domain.usecase.DeleteShopResult
import de.gerritstapper.bikegarage.domain.usecase.DeleteShopUseCase
import de.gerritstapper.bikegarage.domain.usecase.ShopDeletionBlockers
import de.gerritstapper.bikegarage.domain.usecase.ShopDeletionInspection
import de.gerritstapper.bikegarage.ui.navigation.SHOP_ID_ARGUMENT
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class ShopDeletionError {
    DELETE_FAILED,
    LOGO_REMOVAL_FAILED,
    SHOP_NOT_FOUND,
}

internal data class ShopDeletionUiState(
    val shop: Shop? = null,
    val blockers: ShopDeletionBlockers? = null,
    val showConfirmation: Boolean = false,
    val pendingLogoPath: String? = null,
    val error: ShopDeletionError? = null,
    val isChecking: Boolean = false,
    val isDeleting: Boolean = false,
)

internal sealed interface ShopDeletionEvent {
    data object Deleted : ShopDeletionEvent
}

@HiltViewModel
class ShopDeletionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shopRepository: ShopRepository,
    private val deleteShop: DeleteShopUseCase,
    private val imageStorage: ImageStorage,
) : ViewModel() {
    private val shopId = checkNotNull(savedStateHandle.get<Long>(SHOP_ID_ARGUMENT))

    private val _uiState = MutableStateFlow(ShopDeletionUiState())
    internal val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ShopDeletionEvent>()
    internal val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            shopRepository.observeShop(shopId).collectLatest { relations ->
                _uiState.update { it.copy(shop = relations?.shop) }
            }
        }
    }

    fun requestDeletion() {
        if (_uiState.value.isChecking || _uiState.value.isDeleting) return

        _uiState.update {
            it.copy(
                blockers = null,
                showConfirmation = false,
                error = null,
                isChecking = true,
            )
        }
        viewModelScope.launch {
            when (val inspection = deleteShop.inspect(shopId)) {
                is ShopDeletionInspection.Ready -> _uiState.update {
                    it.copy(shop = inspection.shop, showConfirmation = true, isChecking = false)
                }

                is ShopDeletionInspection.Blocked -> _uiState.update {
                    it.copy(
                        shop = inspection.shop,
                        blockers = inspection.blockers,
                        isChecking = false,
                    )
                }

                ShopDeletionInspection.NotFound -> _uiState.update {
                    it.copy(error = ShopDeletionError.SHOP_NOT_FOUND, isChecking = false)
                }
            }
        }
    }

    fun dismissBlockers() {
        _uiState.update { it.copy(blockers = null) }
    }

    fun dismissConfirmation() {
        if (!_uiState.value.isDeleting) {
            _uiState.update { it.copy(showConfirmation = false) }
        }
    }

    fun confirmDeletion() {
        if (_uiState.value.isDeleting) return

        _uiState.update { it.copy(showConfirmation = false, error = null, isDeleting = true) }
        viewModelScope.launch {
            when (val result = deleteShop.delete(shopId)) {
                is DeleteShopResult.Deleted -> {
                    _uiState.update { it.copy(pendingLogoPath = result.shop.logoPath) }
                    deleteLogo(result.shop.logoPath)
                }

                is DeleteShopResult.Blocked -> _uiState.update {
                    it.copy(blockers = result.blockers, isDeleting = false)
                }

                DeleteShopResult.NotFound -> _uiState.update {
                    it.copy(error = ShopDeletionError.SHOP_NOT_FOUND, isDeleting = false)
                }

                DeleteShopResult.Failed -> _uiState.update {
                    it.copy(error = ShopDeletionError.DELETE_FAILED, isDeleting = false)
                }
            }
        }
    }

    fun retryLogoRemoval() {
        val logoPath = _uiState.value.pendingLogoPath ?: return
        if (_uiState.value.isDeleting) return

        _uiState.update { it.copy(error = null, isDeleting = true) }
        viewModelScope.launch {
            deleteLogo(logoPath)
        }
    }

    private suspend fun deleteLogo(logoPath: String) {
        try {
            imageStorage.delete(logoPath)
            _uiState.update {
                it.copy(
                    pendingLogoPath = null,
                    error = null,
                    isDeleting = false,
                )
            }
            _events.emit(ShopDeletionEvent.Deleted)
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            _uiState.update {
                it.copy(
                    pendingLogoPath = logoPath,
                    error = ShopDeletionError.LOGO_REMOVAL_FAILED,
                    isDeleting = false,
                )
            }
        }
    }
}
