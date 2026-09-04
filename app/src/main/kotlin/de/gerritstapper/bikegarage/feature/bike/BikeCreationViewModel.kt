package de.gerritstapper.bikegarage.feature.bike

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.usecase.BikeCreationFieldError
import de.gerritstapper.bikegarage.domain.usecase.CreateBikeResult
import de.gerritstapper.bikegarage.domain.usecase.CreateBikeUseCase
import de.gerritstapper.bikegarage.domain.usecase.GetShopsUseCase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class BikeCreationUiState(
    val nickname: String = "",
    val brand: String = "",
    val model: String = "",
    val purchaseDate: String = "",
    val shopId: Long? = null,
    val imagePaths: List<String> = emptyList(),
    val thumbnailPath: String? = null,
    val brandError: BikeCreationFieldError? = null,
    val modelError: BikeCreationFieldError? = null,
    val imageError: BikeCreationFieldError? = null,
    val purchaseDateError: BikeCreationFieldError? = null,
    val imageImportFailed: Boolean = false,
    val imageRemovalFailed: Boolean = false,
    val saveFailed: Boolean = false,
    val isProcessingImage: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isBusy: Boolean
        get() = isProcessingImage || isSaving

    val isDirty: Boolean
        get() = nickname.isNotBlank() ||
            brand.isNotBlank() ||
            model.isNotBlank() ||
            purchaseDate.isNotBlank() ||
            shopId != null ||
            imagePaths.isNotEmpty()
}

internal sealed interface BikeCreationEvent {
    data class Created(val bikeId: Long) : BikeCreationEvent
}

@HiltViewModel
class BikeCreationViewModel @Inject constructor(
    private val createBike: CreateBikeUseCase,
    getShops: GetShopsUseCase,
    private val imageStorage: ImageStorage,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BikeCreationUiState())
    internal val uiState = _uiState.asStateFlow()

    val shops = getShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<BikeCreationEvent>()
    internal val events = _events.asSharedFlow()

    fun onNicknameChanged(nickname: String) {
        _uiState.update { it.copy(nickname = nickname, saveFailed = false) }
    }

    fun onBrandChanged(brand: String) {
        _uiState.update { it.copy(brand = brand, brandError = null, saveFailed = false) }
    }

    fun onModelChanged(model: String) {
        _uiState.update { it.copy(model = model, modelError = null, saveFailed = false) }
    }

    fun onPurchaseDateChanged(purchaseDate: String) {
        _uiState.update {
            it.copy(
                purchaseDate = purchaseDate,
                purchaseDateError = null,
                saveFailed = false,
            )
        }
    }

    fun onShopSelected(shopId: Long?) {
        _uiState.update { it.copy(shopId = shopId, saveFailed = false) }
    }

    fun createPendingCameraCapture(): PendingImage? =
        try {
            imageStorage.newCameraCapture()
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            _uiState.update { it.copy(imageImportFailed = true) }
            null
        }

    fun completeCameraCapture(pendingImage: PendingImage, isSaved: Boolean) {
        if (!isSaved || _uiState.value.isBusy) {
            discardImage(pendingImage.storagePath)
            return
        }
        _uiState.update { it.copy(isProcessingImage = true) }
        viewModelScope.launch {
            addImage(pendingImage.storagePath)
            _uiState.update { it.copy(isProcessingImage = false) }
        }
    }

    fun importImages(sources: List<Uri>) {
        if (sources.isEmpty() || _uiState.value.isBusy) return

        _uiState.update { it.copy(isProcessingImage = true, imageImportFailed = false) }
        viewModelScope.launch {
            try {
                sources.forEach { source -> addImage(imageStorage.import(source)) }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.update { it.copy(imageImportFailed = true) }
            } finally {
                _uiState.update { it.copy(isProcessingImage = false) }
            }
        }
    }

    fun selectThumbnail(storagePath: String) {
        _uiState.update {
            if (storagePath in it.imagePaths) it.copy(thumbnailPath = storagePath) else it
        }
    }

    fun removeImage(storagePath: String) {
        if (_uiState.value.isBusy || storagePath !in _uiState.value.imagePaths) return

        _uiState.update { it.copy(isProcessingImage = true, imageRemovalFailed = false) }
        viewModelScope.launch {
            try {
                imageStorage.delete(storagePath)
                _uiState.update { state ->
                    val remainingImages = state.imagePaths - storagePath
                    state.copy(
                        imagePaths = remainingImages,
                        thumbnailPath = state.thumbnailPath
                            ?.takeIf { it in remainingImages }
                            ?: remainingImages.firstOrNull(),
                        imageError = null,
                    )
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.update { it.copy(imageRemovalFailed = true) }
            } finally {
                _uiState.update { it.copy(isProcessingImage = false) }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isBusy) return

        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                when (
                    val result = createBike(
                        nickname = state.nickname,
                        brand = state.brand,
                        model = state.model,
                        purchaseDate = state.purchaseDate,
                        shopId = state.shopId,
                        imagePaths = state.imagePaths,
                        thumbnailPath = state.thumbnailPath,
                    )
                ) {
                    is CreateBikeResult.Created -> {
                        _uiState.update {
                            it.copy(
                                imagePaths = emptyList(),
                                thumbnailPath = null,
                                isSaving = false,
                            )
                        }
                        _events.emit(BikeCreationEvent.Created(result.bike.id))
                    }

                    is CreateBikeResult.Invalid -> {
                        _uiState.update {
                            it.copy(
                                brandError = result.validation.brandError,
                                modelError = result.validation.modelError,
                                imageError = result.validation.imageError,
                                purchaseDateError = result.validation.purchaseDateError,
                                isSaving = false,
                            )
                        }
                    }
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    fun discardUnsavedImages(onFinished: () -> Unit) {
        val imagePaths = _uiState.value.imagePaths
        _uiState.update { it.copy(isProcessingImage = true) }
        viewModelScope.launch {
            imagePaths.forEach { storagePath ->
                try {
                    imageStorage.delete(storagePath)
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                }
            }
            _uiState.value = BikeCreationUiState()
            onFinished()
        }
    }

    private suspend fun addImage(storagePath: String) {
        if (_uiState.value.isSaving) {
            imageStorage.delete(storagePath)
            return
        }
        _uiState.update { state ->
            state.copy(
                imagePaths = state.imagePaths + storagePath,
                thumbnailPath = state.thumbnailPath ?: storagePath,
                imageError = null,
                imageImportFailed = false,
            )
        }
    }

    private fun discardImage(storagePath: String) {
        viewModelScope.launch {
            try {
                imageStorage.delete(storagePath)
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
            }
        }
    }
}
