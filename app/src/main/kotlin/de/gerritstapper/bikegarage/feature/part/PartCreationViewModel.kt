package de.gerritstapper.bikegarage.feature.part

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartTechnicalFields
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.usecase.CreatePartResult
import de.gerritstapper.bikegarage.domain.usecase.CreatePartUseCase
import de.gerritstapper.bikegarage.domain.usecase.GetBikesUseCase
import de.gerritstapper.bikegarage.domain.usecase.GetShopsUseCase
import de.gerritstapper.bikegarage.domain.usecase.PartCreationFieldError
import de.gerritstapper.bikegarage.domain.usecase.PartProductLinkDraft
import de.gerritstapper.bikegarage.ui.navigation.BIKE_ID_ARGUMENT
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PartProductLinkUiState(
    val id: Long,
    val shopId: Long? = null,
    val productUrl: String = "",
    val shopError: PartCreationFieldError? = null,
    val productUrlError: PartCreationFieldError? = null,
)

internal data class PartCreationUiState(
    val category: PartCategory? = null,
    val type: String? = null,
    val name: String = "",
    val manufacturer: String = "",
    val manufacturerNumber: String = "",
    val description: String = "",
    val inventoryCount: String = "",
    val imagePaths: List<String> = emptyList(),
    val thumbnailPath: String? = null,
    val technicalAttributes: Map<String, String> = emptyMap(),
    val selectedBikeIds: Set<Long> = emptySet(),
    val productLinks: List<PartProductLinkUiState> = emptyList(),
    val categoryError: PartCreationFieldError? = null,
    val nameError: PartCreationFieldError? = null,
    val inventoryCountError: Boolean = false,
    val imageImportFailed: Boolean = false,
    val imageRemovalFailed: Boolean = false,
    val saveFailed: Boolean = false,
    val isSaving: Boolean = false,
    val isProcessingImage: Boolean = false,
) {
    val isBusy: Boolean
        get() = isSaving || isProcessingImage

    val isDirty: Boolean
        get() = category != null ||
            type != null ||
            name.isNotBlank() ||
            manufacturer.isNotBlank() ||
            manufacturerNumber.isNotBlank() ||
            description.isNotBlank() ||
            inventoryCount.isNotBlank() ||
            imagePaths.isNotEmpty() ||
            technicalAttributes.values.any { it.isNotBlank() } ||
            selectedBikeIds.isNotEmpty() ||
            productLinks.isNotEmpty()
}

internal sealed interface PartCreationEvent {
    data class Created(val partId: Long) : PartCreationEvent
}

@HiltViewModel
class PartCreationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createPart: CreatePartUseCase,
    getBikes: GetBikesUseCase,
    getShops: GetShopsUseCase,
    private val imageStorage: ImageStorage,
) : ViewModel() {
    private var nextProductLinkId = 1L
    private var pendingShopLinkId: Long? = null
    private val initialBikeId = savedStateHandle.get<Long>(BIKE_ID_ARGUMENT).orEmpty()

    private val _uiState = MutableStateFlow(
        PartCreationUiState(
            selectedBikeIds = setOf(initialBikeId).filterTo(mutableSetOf()) { it > 0 },
        ),
    )
    internal val uiState = _uiState.asStateFlow()

    val bikes = getBikes()
        .map { bikeWithImages -> bikeWithImages.map { it.bike } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Bike>())
    val shops = getShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Shop>())

    private val _events = MutableSharedFlow<PartCreationEvent>()
    internal val events = _events.asSharedFlow()

    fun onCategorySelected(category: PartCategory?) {
        _uiState.update { state ->
            val retainedType = state.type?.takeIf {
                category != null && PartTechnicalFields.isTypeForCategory(it, category)
            }
            state.copy(
                category = category,
                type = retainedType,
                technicalAttributes = fieldsFor(retainedType, state.technicalAttributes),
                categoryError = null,
                saveFailed = false,
            )
        }
    }

    fun onTypeSelected(type: String?) {
        _uiState.update { state ->
            val selectedType = type?.takeIf {
                state.category != null && PartTechnicalFields.isTypeForCategory(it, state.category)
            }
            state.copy(
                type = selectedType,
                technicalAttributes = fieldsFor(selectedType, state.technicalAttributes),
                saveFailed = false,
            )
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, nameError = null, saveFailed = false) }
    }

    fun onManufacturerChanged(manufacturer: String) {
        _uiState.update { it.copy(manufacturer = manufacturer, saveFailed = false) }
    }

    fun onManufacturerNumberChanged(manufacturerNumber: String) {
        _uiState.update { it.copy(manufacturerNumber = manufacturerNumber, saveFailed = false) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description, saveFailed = false) }
    }

    fun onInventoryCountChanged(inventoryCount: String) {
        _uiState.update {
            it.copy(
                inventoryCount = inventoryCount,
                inventoryCountError = false,
                saveFailed = false,
            )
        }
    }

    fun onTechnicalAttributeChanged(key: String, value: String) {
        _uiState.update { state ->
            if (key !in state.technicalAttributes) state else state.copy(
                technicalAttributes = state.technicalAttributes + (key to value),
                saveFailed = false,
            )
        }
    }

    fun toggleBikeSelection(bikeId: Long) {
        _uiState.update { state ->
            state.copy(
                selectedBikeIds = state.selectedBikeIds.toMutableSet().apply {
                    if (!add(bikeId)) remove(bikeId)
                },
                saveFailed = false,
            )
        }
    }

    fun addProductLink() {
        _uiState.update { state ->
            state.copy(
                productLinks = state.productLinks + PartProductLinkUiState(id = nextProductLinkId++),
                saveFailed = false,
            )
        }
    }

    fun removeProductLink(linkId: Long) {
        _uiState.update { state ->
            state.copy(
                productLinks = state.productLinks.filterNot { it.id == linkId },
                saveFailed = false,
            )
        }
        if (pendingShopLinkId == linkId) pendingShopLinkId = null
    }

    fun onProductLinkShopSelected(linkId: Long, shopId: Long?) {
        updateProductLink(linkId) {
            it.copy(shopId = shopId, shopError = null)
        }
    }

    fun onProductUrlChanged(linkId: Long, productUrl: String) {
        updateProductLink(linkId) {
            it.copy(productUrl = productUrl, productUrlError = null)
        }
    }

    fun createShopForProductLink(linkId: Long) {
        pendingShopLinkId = linkId
    }

    fun selectCreatedShop(shopId: Long) {
        val linkId = pendingShopLinkId ?: _uiState.value.productLinks
            .firstOrNull { it.shopId == null }
            ?.id
        if (linkId != null) onProductLinkShopSelected(linkId, shopId)
        pendingShopLinkId = null
    }

    fun createPendingCameraCapture(): PendingImage? =
        if (_uiState.value.isBusy) {
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
                _uiState.update { state -> state.withImageRemoved(storagePath) }
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
        val inventoryCount = state.inventoryCount.takeIf(String::isNotBlank)?.toIntOrNull()
        if (state.inventoryCount.isNotBlank() && inventoryCount == null) {
            _uiState.update { it.copy(inventoryCountError = true) }
            return
        }

        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                when (
                    val result = createPart(
                        category = state.category,
                        type = state.type,
                        name = state.name,
                        manufacturer = state.manufacturer,
                        manufacturerNumber = state.manufacturerNumber,
                        description = state.description,
                        technicalAttributes = state.technicalAttributes,
                        bikeIds = state.selectedBikeIds,
                        productLinks = state.productLinks.map {
                            PartProductLinkDraft(it.shopId, it.productUrl)
                        },
                        inventoryCount = inventoryCount,
                        imagePaths = state.imagePaths,
                        thumbnailPath = state.thumbnailPath,
                    )
                ) {
                    is CreatePartResult.Created -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _events.emit(PartCreationEvent.Created(result.part.id))
                    }

                    is CreatePartResult.Invalid -> {
                        _uiState.update { current ->
                            current.copy(
                                categoryError = result.validation.categoryError,
                                nameError = result.validation.nameError,
                                inventoryCountError =
                                    result.validation.inventoryCountError != null,
                                productLinks = current.productLinks.mapIndexed { index, link ->
                                    result.validation.productLinkErrors[index]?.let { errors ->
                                        link.copy(
                                            shopError = errors.shopError,
                                            productUrlError = errors.productUrlError,
                                        )
                                    } ?: link.copy(shopError = null, productUrlError = null)
                                },
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

    private fun updateProductLink(
        linkId: Long,
        transform: (PartProductLinkUiState) -> PartProductLinkUiState,
    ) {
        _uiState.update { state ->
            state.copy(
                productLinks = state.productLinks.map { link ->
                    if (link.id == linkId) transform(link) else link
                },
                saveFailed = false,
            )
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
                _uiState.value = PartCreationUiState(selectedBikeIds = setOf(initialBikeId)
                    .filterTo(mutableSetOf()) { it > 0 })
                onFinished()
            }
        }

    private suspend fun addImage(storagePath: String) {
            if (_uiState.value.isSaving) {
                imageStorage.delete(storagePath)
                return
            }
            _uiState.update {
                it.copy(
                    imagePaths = it.imagePaths + storagePath,
                    thumbnailPath = it.thumbnailPath ?: storagePath,
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

    private fun fieldsFor(type: String?, values: Map<String, String>): Map<String, String> =
        PartTechnicalFields.forType(type).associate { field ->
            field.key to values[field.key].orEmpty()
        }
}

private fun Long?.orEmpty(): Long = this ?: 0L

private fun PartCreationUiState.withImageRemoved(storagePath: String): PartCreationUiState {
    val remainingImagePaths = imagePaths - storagePath
    return copy(
        imagePaths = remainingImagePaths,
        thumbnailPath = thumbnailPath?.takeIf { it in remainingImagePaths }
            ?: remainingImagePaths.firstOrNull(),
        imageRemovalFailed = false,
    )
}
