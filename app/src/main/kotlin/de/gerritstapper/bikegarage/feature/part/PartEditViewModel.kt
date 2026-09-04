package de.gerritstapper.bikegarage.feature.part

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartTechnicalFields
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.domain.usecase.GetBikesUseCase
import de.gerritstapper.bikegarage.domain.usecase.GetShopsUseCase
import de.gerritstapper.bikegarage.domain.usecase.PartCreationFieldError
import de.gerritstapper.bikegarage.domain.usecase.PartProductLinkDraft
import de.gerritstapper.bikegarage.domain.usecase.UpdatePartResult
import de.gerritstapper.bikegarage.domain.usecase.UpdatePartUseCase
import de.gerritstapper.bikegarage.ui.navigation.PART_ID_ARGUMENT
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PartEditUiState(
    val part: Part? = null,
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
    val originalCategory: PartCategory? = null,
    val originalType: String? = null,
    val originalName: String = "",
    val originalManufacturer: String = "",
    val originalManufacturerNumber: String = "",
    val originalDescription: String = "",
    val originalInventoryCount: String = "",
    val originalImagePaths: List<String> = emptyList(),
    val originalThumbnailPath: String? = null,
    val originalTechnicalAttributes: Map<String, String> = emptyMap(),
    val originalBikeIds: Set<Long> = emptySet(),
    val originalProductLinks: List<PartProductLinkUiState> = emptyList(),
    val categoryError: PartCreationFieldError? = null,
    val nameError: PartCreationFieldError? = null,
    val inventoryCountError: Boolean = false,
    val imageImportFailed: Boolean = false,
    val imageRemovalFailed: Boolean = false,
    val obsoleteImageRemovalFailed: Boolean = false,
    val obsoleteImagePathsPendingRemoval: List<String> = emptyList(),
    val saveFailed: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isProcessingImage: Boolean = false,
) {
    val isBusy: Boolean
        get() = isSaving || isProcessingImage

    val hasPendingImageRemoval: Boolean
        get() = obsoleteImagePathsPendingRemoval.isNotEmpty()

    val canEdit: Boolean
        get() = !isBusy && !hasPendingImageRemoval

    val technicalValuesRemovedOnSave: List<String>
        get() {
            val allowedKeys = PartTechnicalFields.forType(type).mapTo(mutableSetOf()) { it.key }
            val labelsByKey = PartTechnicalFields.types
                .flatMap { it.technicalFields }
                .associate { it.key to it.label }
            return technicalAttributes
                .asSequence()
                .filter { (key, value) -> key !in allowedKeys && value.isNotBlank() }
                .map { (key, _) -> labelsByKey[key] ?: key }
                .sorted()
                .toList()
        }

    val isDirty: Boolean
        get() = part != null && (
            category != originalCategory ||
                type != originalType ||
                name != originalName ||
                manufacturer != originalManufacturer ||
                manufacturerNumber != originalManufacturerNumber ||
                description != originalDescription ||
                inventoryCount != originalInventoryCount ||
                imagePaths != originalImagePaths ||
                thumbnailPath != originalThumbnailPath ||
                technicalAttributes.nonBlankValues() != originalTechnicalAttributes.nonBlankValues() ||
                selectedBikeIds != originalBikeIds ||
                productLinks.formValues() != originalProductLinks.formValues()
            )
}

internal sealed interface PartEditEvent {
    data class Updated(val part: Part) : PartEditEvent
}

@HiltViewModel
class PartEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val partRepository: PartRepository,
    private val updatePart: UpdatePartUseCase,
    getBikes: GetBikesUseCase,
    getShops: GetShopsUseCase,
    private val imageStorage: ImageStorage,
) : ViewModel() {
    private val partId = checkNotNull(savedStateHandle.get<Long>(PART_ID_ARGUMENT))
    private var nextProductLinkId = 1L
    private var pendingShopLinkId: Long? = null

    private val _uiState = MutableStateFlow(PartEditUiState())
    internal val uiState = _uiState.asStateFlow()

    val bikes = getBikes()
        .map { bikeWithImages -> bikeWithImages.map { it.bike } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Bike>())
    val shops = getShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Shop>())

    private val _events = MutableSharedFlow<PartEditEvent>()
    internal val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            partRepository.observePart(partId).first()?.let(::setLoadedPart)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onCategorySelected(category: PartCategory?) {
        if (_uiState.value.isSaving) return
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
        if (_uiState.value.isSaving) return
        _uiState.update { state ->
            val selectedType = PartTechnicalFields.canonicalType(type)?.takeIf {
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
        updateForm { it.copy(name = name, nameError = null) }
    }

    fun onManufacturerChanged(manufacturer: String) {
        updateForm { it.copy(manufacturer = manufacturer) }
    }

    fun onManufacturerNumberChanged(manufacturerNumber: String) {
        updateForm { it.copy(manufacturerNumber = manufacturerNumber) }
    }

    fun onDescriptionChanged(description: String) {
        updateForm { it.copy(description = description) }
    }

    fun onInventoryCountChanged(inventoryCount: String) {
        updateForm { it.copy(inventoryCount = inventoryCount, inventoryCountError = false) }
    }

    fun onTechnicalAttributeChanged(key: String, value: String) {
        updateForm { state ->
            if (key !in PartTechnicalFields.forType(state.type).map { it.key }) {
                state
            } else {
                state.copy(technicalAttributes = state.technicalAttributes + (key to value))
            }
        }
    }

    fun toggleBikeSelection(bikeId: Long) {
        updateForm { state ->
            state.copy(
                selectedBikeIds = state.selectedBikeIds.toMutableSet().apply {
                    if (!add(bikeId)) remove(bikeId)
                },
            )
        }
    }

    fun addProductLink() {
        updateForm { state ->
            state.copy(
                productLinks = state.productLinks + PartProductLinkUiState(id = nextProductLinkId++),
            )
        }
    }

    fun removeProductLink(linkId: Long) {
        updateForm { state ->
            state.copy(productLinks = state.productLinks.filterNot { it.id == linkId })
        }
        if (pendingShopLinkId == linkId) pendingShopLinkId = null
    }

    fun onProductLinkShopSelected(linkId: Long, shopId: Long?) {
        updateProductLink(linkId) { it.copy(shopId = shopId, shopError = null) }
    }

    fun onProductUrlChanged(linkId: Long, productUrl: String) {
        updateProductLink(linkId) { it.copy(productUrl = productUrl, productUrlError = null) }
    }

    fun createShopForProductLink(linkId: Long) {
        if (!_uiState.value.isSaving) pendingShopLinkId = linkId
    }

    fun selectCreatedShop(shopId: Long) {
        val linkId = pendingShopLinkId ?: _uiState.value.productLinks
            .firstOrNull { it.shopId == null }
            ?.id
        if (linkId != null) onProductLinkShopSelected(linkId, shopId)
        pendingShopLinkId = null
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
            if (it.canEdit && storagePath in it.imagePaths) it.copy(thumbnailPath = storagePath) else it
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
        val part = state.part ?: return
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
                    val result = updatePart(
                        part = part,
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
                    is UpdatePartResult.Updated -> {
                        val obsoleteImagePaths = (
                            state.obsoleteImagePathsPendingRemoval +
                                (state.originalImagePaths - state.imagePaths)
                            ).distinct()
                        val failedImagePaths = removeObsoleteImages(obsoleteImagePaths)
                        if (failedImagePaths.isNotEmpty()) {
                            _uiState.update {
                                it.withSavedValues(
                                    part = result.part,
                                    pendingImageRemoval = failedImagePaths,
                                    obsoleteImageRemovalFailed = true,
                                    isSaving = false,
                                )
                            }
                            return@launch
                        }
                        _uiState.update {
                            it.withSavedValues(
                                part = result.part,
                                pendingImageRemoval = emptyList(),
                                obsoleteImageRemovalFailed = false,
                                isSaving = false,
                            )
                        }
                        _events.emit(PartEditEvent.Updated(result.part))
                    }

                    is UpdatePartResult.Invalid -> {
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

    private fun setLoadedPart(details: PartWithRelations) {
        val part = details.part
        val type = PartTechnicalFields.canonicalType(part.type)
            ?.takeIf { PartTechnicalFields.isTypeForCategory(it, part.category) }
        val technicalAttributes = fieldsFor(
            type,
            details.attributes.associate { it.key to it.value },
        )
        val productLinks = details.shopLinks.map { link ->
            PartProductLinkUiState(
                id = link.link.id,
                shopId = link.link.shopId,
                productUrl = link.link.productUrl,
            )
        }
        val imagePaths = details.images.sortedBy { it.displayOrder }.map { it.storagePath }
        val thumbnailPath = details.images.firstOrNull { it.isThumbnail }?.storagePath
            ?: imagePaths.firstOrNull()
        nextProductLinkId = (productLinks.maxOfOrNull { it.id } ?: 0L) + 1
        _uiState.update {
            it.copy(
                part = part,
                category = part.category,
                type = type,
                name = part.name,
                manufacturer = part.manufacturer.orEmpty(),
                manufacturerNumber = part.manufacturerNumber.orEmpty(),
                description = part.description.orEmpty(),
                inventoryCount = part.inventoryCount?.toString().orEmpty(),
                imagePaths = imagePaths,
                thumbnailPath = thumbnailPath,
                technicalAttributes = technicalAttributes,
                selectedBikeIds = details.bikes.mapTo(mutableSetOf()) { bike -> bike.id },
                productLinks = productLinks,
                originalCategory = part.category,
                originalType = type,
                originalName = part.name,
                originalManufacturer = part.manufacturer.orEmpty(),
                originalManufacturerNumber = part.manufacturerNumber.orEmpty(),
                originalDescription = part.description.orEmpty(),
                originalInventoryCount = part.inventoryCount?.toString().orEmpty(),
                originalImagePaths = imagePaths,
                originalThumbnailPath = thumbnailPath,
                originalTechnicalAttributes = technicalAttributes,
                originalBikeIds = details.bikes.mapTo(mutableSetOf()) { bike -> bike.id },
                originalProductLinks = productLinks,
            )
        }
    }

    private fun updateForm(transform: (PartEditUiState) -> PartEditUiState) {
        _uiState.update { state ->
            if (!state.canEdit) state else transform(state).copy(saveFailed = false)
        }
    }

    private fun updateProductLink(
        linkId: Long,
        transform: (PartProductLinkUiState) -> PartProductLinkUiState,
    ) {
        updateForm { state ->
            state.copy(
                productLinks = state.productLinks.map { link ->
                    if (link.id == linkId) transform(link) else link
                },
            )
        }
    }

    private fun fieldsFor(type: String?, values: Map<String, String>): Map<String, String> =
        values + PartTechnicalFields.forType(type).associate { field ->
            field.key to values[field.key].orEmpty()
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

    private suspend fun addImage(storagePath: String) {
        if (_uiState.value.isSaving || _uiState.value.hasPendingImageRemoval) {
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

private fun List<PartProductLinkUiState>.formValues(): List<Triple<Long, Long?, String>> =
    map { Triple(it.id, it.shopId, it.productUrl) }

private fun Map<String, String>.nonBlankValues(): Map<String, String> =
    filterValues { it.isNotBlank() }

private fun PartEditUiState.withImageRemoved(storagePath: String): PartEditUiState {
    val remainingImagePaths = imagePaths - storagePath
    return copy(
        imagePaths = remainingImagePaths,
        thumbnailPath = thumbnailPath?.takeIf { it in remainingImagePaths }
            ?: remainingImagePaths.firstOrNull(),
        imageRemovalFailed = false,
    )
}

private fun PartEditUiState.withSavedValues(
    part: Part,
    pendingImageRemoval: List<String>,
    obsoleteImageRemovalFailed: Boolean,
    isSaving: Boolean,
): PartEditUiState = copy(
    part = part,
    originalCategory = category,
    originalType = type,
    originalName = name,
    originalManufacturer = manufacturer,
    originalManufacturerNumber = manufacturerNumber,
    originalDescription = description,
    originalInventoryCount = inventoryCount,
    originalTechnicalAttributes = technicalAttributes,
    originalBikeIds = selectedBikeIds,
    originalProductLinks = productLinks,
    originalImagePaths = imagePaths,
    originalThumbnailPath = thumbnailPath,
    obsoleteImagePathsPendingRemoval = pendingImageRemoval,
    obsoleteImageRemovalFailed = obsoleteImageRemovalFailed,
    isSaving = isSaving,
)
