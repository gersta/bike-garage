package de.gerritstapper.bikegarage.feature.shop

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import de.gerritstapper.bikegarage.domain.usecase.ShopUpdateFieldError
import de.gerritstapper.bikegarage.domain.usecase.UpdateShopResult
import de.gerritstapper.bikegarage.domain.usecase.UpdateShopUseCase
import de.gerritstapper.bikegarage.ui.navigation.SHOP_ID_ARGUMENT
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ShopEditUiState(
    val shop: Shop? = null,
    val name: String = "",
    val logoPath: String? = null,
    val originalName: String = "",
    val originalLogoPath: String? = null,
    val nameError: ShopUpdateFieldError? = null,
    val logoError: ShopUpdateFieldError? = null,
    val logoImportFailed: Boolean = false,
    val oldLogoRemovalFailed: Boolean = false,
    val oldLogoPathPendingRemoval: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
) {
    val isDirty: Boolean
        get() = shop != null && (name != originalName || logoPath != originalLogoPath)
}

internal sealed interface ShopEditEvent {
    data class Updated(val shop: Shop) : ShopEditEvent
}

@HiltViewModel
class ShopEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shopRepository: ShopRepository,
    private val updateShop: UpdateShopUseCase,
    private val imageStorage: ImageStorage,
) : ViewModel() {
    private val shopId = checkNotNull(savedStateHandle.get<Long>(SHOP_ID_ARGUMENT))

    private val _uiState = MutableStateFlow(ShopEditUiState())
    internal val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ShopEditEvent>()
    internal val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            shopRepository.observeShop(shopId).first()?.shop?.let(::setLoadedShop)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun createPendingLogoCapture(): PendingImage = imageStorage.newCameraCapture()

    fun completeCameraCapture(pendingImage: PendingImage, isSaved: Boolean) {
        viewModelScope.launch {
            if (isSaved) {
                replaceLogo(pendingImage.storagePath)
            } else {
                imageStorage.discard(pendingImage)
            }
        }
    }

    fun importLogo(source: Uri) {
        viewModelScope.launch {
            try {
                replaceLogo(imageStorage.import(source))
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.update { state -> state.copy(logoImportFailed = true) }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        val shop = state.shop ?: return
        if (state.isSaving) return

        _uiState.update {
            it.copy(
                isSaving = true,
                logoImportFailed = false,
                oldLogoRemovalFailed = false,
            )
        }
        viewModelScope.launch {
            when (val result = updateShop(shop, state.name, state.logoPath)) {
                is UpdateShopResult.Updated -> {
                    val oldLogoPath = state.oldLogoPathPendingRemoval
                        ?: shop.logoPath.takeIf { it != result.shop.logoPath }
                    try {
                        if (oldLogoPath != null) imageStorage.delete(oldLogoPath)
                    } catch (exception: Exception) {
                        if (exception is CancellationException) throw exception
                        _uiState.update {
                            it.copy(
                                shop = result.shop,
                                name = result.shop.name,
                                logoPath = result.shop.logoPath,
                                originalName = result.shop.name,
                                originalLogoPath = result.shop.logoPath,
                                oldLogoRemovalFailed = true,
                                oldLogoPathPendingRemoval = oldLogoPath,
                                isSaving = false,
                            )
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            logoPath = null,
                            oldLogoPathPendingRemoval = null,
                            isSaving = false,
                        )
                    }
                    _events.emit(ShopEditEvent.Updated(result.shop))
                }

                is UpdateShopResult.Invalid -> {
                    _uiState.update {
                        it.copy(
                            nameError = result.validation.nameError,
                            logoError = result.validation.logoError,
                            isSaving = false,
                        )
                    }
                }
            }
        }
    }

    fun discardUnsavedLogo() {
        if (_uiState.value.isSaving) return

        val state = _uiState.value
        val logoPath = state.logoPath
            ?.takeIf { it != state.originalLogoPath }
            ?: return
        _uiState.update { it.copy(logoPath = it.originalLogoPath) }
        viewModelScope.launch {
            imageStorage.delete(logoPath)
        }
    }

    private fun setLoadedShop(shop: Shop) {
        _uiState.update {
            it.copy(
                shop = shop,
                name = shop.name,
                logoPath = shop.logoPath,
                originalName = shop.name,
                originalLogoPath = shop.logoPath,
            )
        }
    }

    private suspend fun replaceLogo(logoPath: String) {
        val state = _uiState.value
        val previousPath = state.logoPath
        if (previousPath == logoPath) return

        if (previousPath != null && previousPath != state.originalLogoPath) {
            imageStorage.delete(previousPath)
        }
        _uiState.update {
            it.copy(
                logoPath = logoPath,
                logoError = null,
                logoImportFailed = false,
                oldLogoRemovalFailed = false,
            )
        }
    }
}
