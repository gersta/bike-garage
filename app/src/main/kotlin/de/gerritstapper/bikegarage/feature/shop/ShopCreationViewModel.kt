package de.gerritstapper.bikegarage.feature.shop

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.usecase.CreateShopResult
import de.gerritstapper.bikegarage.domain.usecase.CreateShopUseCase
import de.gerritstapper.bikegarage.domain.usecase.ShopCreationFieldError
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ShopCreationUiState(
    val name: String = "",
    val logoPath: String? = null,
    val nameError: ShopCreationFieldError? = null,
    val logoError: ShopCreationFieldError? = null,
    val logoImportFailed: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isDirty: Boolean
        get() = name.isNotBlank() || logoPath != null
}

internal sealed interface ShopCreationEvent {
    data class Created(val shop: Shop) : ShopCreationEvent
}

@HiltViewModel
class ShopCreationViewModel @Inject constructor(
    private val createShop: CreateShopUseCase,
    private val imageStorage: ImageStorage,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShopCreationUiState())
    internal val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ShopCreationEvent>()
    internal val events = _events.asSharedFlow()

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
        if (_uiState.value.isSaving) return

        _uiState.update { it.copy(isSaving = true, logoImportFailed = false) }
        viewModelScope.launch {
            when (val result = createShop(_uiState.value.name, _uiState.value.logoPath)) {
                is CreateShopResult.Created -> {
                    _uiState.update { it.copy(logoPath = null, isSaving = false) }
                    _events.emit(ShopCreationEvent.Created(result.shop))
                }

                is CreateShopResult.Invalid -> {
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

        val logoPath = _uiState.value.logoPath ?: return
        _uiState.update { it.copy(logoPath = null) }
        viewModelScope.launch {
            imageStorage.delete(logoPath)
        }
    }

    private suspend fun replaceLogo(logoPath: String) {
        val previousPath = _uiState.value.logoPath
        if (previousPath == logoPath) return

        if (previousPath != null) {
            imageStorage.delete(previousPath)
        }
        _uiState.update {
            it.copy(
                logoPath = logoPath,
                logoError = null,
                logoImportFailed = false,
            )
        }
    }
}
