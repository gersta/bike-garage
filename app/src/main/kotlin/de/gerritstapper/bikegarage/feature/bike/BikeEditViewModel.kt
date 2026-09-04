package de.gerritstapper.bikegarage.feature.bike

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import de.gerritstapper.bikegarage.domain.usecase.BikeUpdateFieldError
import de.gerritstapper.bikegarage.domain.usecase.GetShopsUseCase
import de.gerritstapper.bikegarage.domain.usecase.UpdateBikeResult
import de.gerritstapper.bikegarage.domain.usecase.UpdateBikeUseCase
import de.gerritstapper.bikegarage.ui.navigation.BIKE_ID_ARGUMENT
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class BikeEditUiState(
    val bike: Bike? = null,
    val nickname: String = "",
    val brand: String = "",
    val model: String = "",
    val purchaseDate: String = "",
    val shopId: Long? = null,
    val imagePaths: List<String> = emptyList(),
    val thumbnailPath: String? = null,
    val originalNickname: String = "",
    val originalBrand: String = "",
    val originalModel: String = "",
    val originalPurchaseDate: String = "",
    val originalShopId: Long? = null,
    val originalImagePaths: List<String> = emptyList(),
    val originalThumbnailPath: String? = null,
    val brandError: BikeUpdateFieldError? = null,
    val modelError: BikeUpdateFieldError? = null,
    val imageError: BikeUpdateFieldError? = null,
    val purchaseDateError: BikeUpdateFieldError? = null,
    val imageImportFailed: Boolean = false,
    val imageRemovalFailed: Boolean = false,
    val obsoleteImageRemovalFailed: Boolean = false,
    val obsoleteImagePathsPendingRemoval: List<String> = emptyList(),
    val saveFailed: Boolean = false,
    val isLoading: Boolean = true,
    val isProcessingImage: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isBusy: Boolean
        get() = isProcessingImage || isSaving

    val hasPendingImageRemoval: Boolean
        get() = obsoleteImagePathsPendingRemoval.isNotEmpty()

    val canEdit: Boolean
        get() = !isBusy && !hasPendingImageRemoval

    val isDirty: Boolean
        get() = bike != null && (
            nickname != originalNickname ||
                brand != originalBrand ||
                model != originalModel ||
                purchaseDate != originalPurchaseDate ||
                shopId != originalShopId ||
                imagePaths != originalImagePaths ||
                thumbnailPath != originalThumbnailPath
            )
}

internal sealed interface BikeEditEvent {
    data class Updated(val bike: Bike) : BikeEditEvent
}

@HiltViewModel
class BikeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bikeRepository: BikeRepository,
    private val updateBike: UpdateBikeUseCase,
    getShops: GetShopsUseCase,
    private val imageStorage: ImageStorage,
) : ViewModel() {
    private val bikeId = checkNotNull(savedStateHandle.get<Long>(BIKE_ID_ARGUMENT))

    private val _uiState = MutableStateFlow(BikeEditUiState())
    internal val uiState = _uiState.asStateFlow()

    val shops = getShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<BikeEditEvent>()
    internal val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            bikeRepository.observeBike(bikeId).first()?.let(::setLoadedBike)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

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
        if (!_uiState.value.canEdit) {
            null
        } else {
            try {
                imageStorage.newCameraCapture()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.update { it.copy(imageImportFailed = true) }
                null
            }
        }

    fun completeCameraCapture(pendingImage: PendingImage, isSaved: Boolean) {
        if (!isSaved || !_uiState.value.canEdit) {
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
        if (sources.isEmpty() || !_uiState.value.canEdit) return

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
            if (it.canEdit && storagePath in it.imagePaths) {
                it.copy(thumbnailPath = storagePath)
            } else {
                it
            }
        }
    }

    fun removeImage(storagePath: String) {
        val state = _uiState.value
        if (!state.canEdit || storagePath !in state.imagePaths) return

        if (storagePath in state.originalImagePaths) {
            _uiState.update { it.withImageRemoved(storagePath) }
            return
        }

        _uiState.update { it.copy(isProcessingImage = true, imageRemovalFailed = false) }
        viewModelScope.launch {
            try {
                imageStorage.delete(storagePath)
                _uiState.update { it.withImageRemoved(storagePath) }
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
        val bike = state.bike ?: return
        if (state.isBusy) return

        _uiState.update {
            it.copy(
                isSaving = true,
                imageImportFailed = false,
                imageRemovalFailed = false,
                saveFailed = false,
            )
        }
        viewModelScope.launch {
            try {
                when (
                    val result = updateBike(
                        bike = bike,
                        nickname = state.nickname,
                        brand = state.brand,
                        model = state.model,
                        purchaseDate = state.purchaseDate,
                        shopId = state.shopId,
                        imagePaths = state.imagePaths,
                        thumbnailPath = state.thumbnailPath,
                    )
                ) {
                    is UpdateBikeResult.Updated -> {
                        val obsoleteImagePaths = (
                            state.obsoleteImagePathsPendingRemoval +
                                (state.originalImagePaths - state.imagePaths)
                            ).distinct()
                        val failedImagePaths = removeObsoleteImages(obsoleteImagePaths)
                        if (failedImagePaths.isNotEmpty()) {
                            _uiState.update {
                                it.withSavedValues(
                                    bike = result.bike,
                                    pendingImageRemoval = failedImagePaths,
                                    obsoleteImageRemovalFailed = true,
                                    isSaving = false,
                                )
                            }
                            return@launch
                        }
                        _uiState.update {
                            it.withSavedValues(
                                bike = result.bike,
                                pendingImageRemoval = emptyList(),
                                obsoleteImageRemovalFailed = false,
                                isSaving = false,
                            )
                        }
                        _events.emit(BikeEditEvent.Updated(result.bike))
                    }

                    is UpdateBikeResult.Invalid -> {
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
        val state = _uiState.value
        if (state.isBusy || state.hasPendingImageRemoval) return

        val newImagePaths = state.imagePaths - state.originalImagePaths.toSet()
        _uiState.update { it.copy(isProcessingImage = true) }
        viewModelScope.launch {
            newImagePaths.forEach { storagePath ->
                try {
                    imageStorage.delete(storagePath)
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                }
            }
            _uiState.update {
                it.copy(
                    imagePaths = it.originalImagePaths,
                    thumbnailPath = it.originalThumbnailPath,
                    isProcessingImage = false,
                )
            }
            onFinished()
        }
    }

    private fun setLoadedBike(bikeWithImages: BikeWithImages) {
        val bike = bikeWithImages.bike
        val imagePaths = bikeWithImages.images
            .sortedBy { it.displayOrder }
            .map { it.storagePath }
        val thumbnailPath = bikeWithImages.images
            .firstOrNull { it.isThumbnail }
            ?.storagePath
            ?: imagePaths.firstOrNull()
        _uiState.update {
            it.copy(
                bike = bike,
                nickname = bike.nickname.orEmpty(),
                brand = bike.brand,
                model = bike.model,
                purchaseDate = bike.purchaseDate.orEmpty(),
                shopId = bike.shopId,
                imagePaths = imagePaths,
                thumbnailPath = thumbnailPath,
                originalNickname = bike.nickname.orEmpty(),
                originalBrand = bike.brand,
                originalModel = bike.model,
                originalPurchaseDate = bike.purchaseDate.orEmpty(),
                originalShopId = bike.shopId,
                originalImagePaths = imagePaths,
                originalThumbnailPath = thumbnailPath,
            )
        }
    }

    private suspend fun addImage(storagePath: String) {
        if (_uiState.value.isSaving || _uiState.value.hasPendingImageRemoval) {
            imageStorage.delete(storagePath)
            return
        }
        _uiState.update {
            it.copy(
                imagePaths = it.imagePaths + storagePath,
                thumbnailPath = it.thumbnailPath ?: storagePath,
                imageError = null,
                imageImportFailed = false,
            )
        }
    }

    private suspend fun removeObsoleteImages(imagePaths: List<String>): List<String> {
        imagePaths.forEachIndexed { index, storagePath ->
            try {
                imageStorage.delete(storagePath)
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                return imagePaths.drop(index)
            }
        }
        return emptyList()
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

private fun BikeEditUiState.withImageRemoved(storagePath: String): BikeEditUiState {
    val remainingImagePaths = imagePaths - storagePath
    return copy(
        imagePaths = remainingImagePaths,
        thumbnailPath = thumbnailPath
            ?.takeIf { it in remainingImagePaths }
            ?: remainingImagePaths.firstOrNull(),
        imageError = null,
        imageRemovalFailed = false,
    )
}

private fun BikeEditUiState.withSavedValues(
    bike: Bike,
    pendingImageRemoval: List<String>,
    obsoleteImageRemovalFailed: Boolean,
    isSaving: Boolean,
): BikeEditUiState = copy(
    bike = bike,
    originalNickname = nickname,
    originalBrand = brand,
    originalModel = model,
    originalPurchaseDate = purchaseDate,
    originalShopId = shopId,
    originalImagePaths = imagePaths,
    originalThumbnailPath = thumbnailPath,
    obsoleteImagePathsPendingRemoval = pendingImageRemoval,
    obsoleteImageRemovalFailed = obsoleteImageRemovalFailed,
    isSaving = isSaving,
)
