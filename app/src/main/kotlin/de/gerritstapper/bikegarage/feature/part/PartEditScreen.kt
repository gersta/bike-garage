package de.gerritstapper.bikegarage.feature.part

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationActions
import de.gerritstapper.bikegarage.ui.navigation.LocalNavigateHome
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PartEditScreen(
    selectedShopId: Long?,
    onShopSelectionConsumed: () -> Unit,
    onCreateShop: () -> Unit,
    onPartUpdated: (Part) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: PartEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bikes by viewModel.bikes.collectAsStateWithLifecycle()
    val shops by viewModel.shops.collectAsStateWithLifecycle()
    val goHome = LocalNavigateHome.current
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    var navigateHomeAfterDiscard by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedShopId) {
        selectedShopId?.let {
            viewModel.selectCreatedShop(it)
            onShopSelectionConsumed()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PartEditEvent.Updated -> onPartUpdated(event.part)
            }
        }
    }

    fun requestNavigation(navigateHome: Boolean) {
        if (state.isBusy || state.hasPendingImageRemoval) return
        if (state.isDirty) {
            navigateHomeAfterDiscard = navigateHome
            showDiscardConfirmation = true
        } else if (navigateHome) {
            goHome()
        } else {
            onNavigateUp()
        }
    }

    BackHandler { requestNavigation(navigateHome = false) }
    PartEditContent(
        state = state,
        bikes = bikes,
        shops = shops,
        onCategorySelected = viewModel::onCategorySelected,
        onTypeSelected = viewModel::onTypeSelected,
        onNameChanged = viewModel::onNameChanged,
        onManufacturerChanged = viewModel::onManufacturerChanged,
        onManufacturerNumberChanged = viewModel::onManufacturerNumberChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onInventoryCountChanged = viewModel::onInventoryCountChanged,
        onTechnicalAttributeChanged = viewModel::onTechnicalAttributeChanged,
        onBikeToggled = viewModel::toggleBikeSelection,
        onAddProductLink = viewModel::addProductLink,
        onRemoveProductLink = viewModel::removeProductLink,
        onProductLinkShopSelected = viewModel::onProductLinkShopSelected,
        onProductUrlChanged = viewModel::onProductUrlChanged,
        onCreateShopForProductLink = {
            viewModel.createShopForProductLink(it)
            onCreateShop()
        },
        onSelectFromPhotoLibrary = viewModel::importImages,
        onCreatePendingCameraCapture = viewModel::createPendingCameraCapture,
        onCameraCaptureCompleted = viewModel::completeCameraCapture,
        onThumbnailSelected = viewModel::selectThumbnail,
        onImageRemoved = viewModel::removeImage,
        onSave = viewModel::save,
        onNavigateUp = { requestNavigation(navigateHome = false) },
        onNavigateHome = { requestNavigation(navigateHome = true) },
    )

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(R.string.part_discard_changes_title)) },
            text = { Text(stringResource(R.string.part_discard_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirmation = false
                    viewModel.discardUnsavedImages {
                        if (navigateHomeAfterDiscard) goHome() else onNavigateUp()
                    }
                }) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(stringResource(R.string.continue_editing))
                }
            },
        )
    }
}

@Composable
internal fun PartEditContent(
    state: PartEditUiState,
    bikes: List<Bike>,
    shops: List<Shop>,
    onCategorySelected: (PartCategory?) -> Unit,
    onTypeSelected: (String?) -> Unit,
    onNameChanged: (String) -> Unit,
    onManufacturerChanged: (String) -> Unit,
    onManufacturerNumberChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onInventoryCountChanged: (String) -> Unit = {},
    onTechnicalAttributeChanged: (String, String) -> Unit,
    onBikeToggled: (Long) -> Unit,
    onAddProductLink: () -> Unit,
    onRemoveProductLink: (Long) -> Unit,
    onProductLinkShopSelected: (Long, Long?) -> Unit,
    onProductUrlChanged: (Long, String) -> Unit,
    onCreateShopForProductLink: (Long) -> Unit,
    onSelectFromPhotoLibrary: (List<Uri>) -> Unit = {},
    onCreatePendingCameraCapture: () -> PendingImage? = { null },
    onCameraCaptureCompleted: (PendingImage, Boolean) -> Unit = { _, _ -> },
    onThumbnailSelected: (String) -> Unit = {},
    onImageRemoved: (String) -> Unit = {},
    onSave: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateHome: (() -> Unit)? = null,
) {
    var showTechnicalValuesRemovalConfirmation by rememberSaveable { mutableStateOf(false) }

    when {
        state.isLoading -> PartEditStatusContent(
            onNavigateUp = onNavigateUp,
            content = { CircularProgressIndicator() },
        )

        state.part == null -> PartEditStatusContent(
            onNavigateUp = onNavigateUp,
            content = { Text(stringResource(R.string.part_not_found)) },
        )

        else -> PartCreationContent(
            state = state.asFormState(),
            bikes = bikes,
            shops = shops,
            titleRes = R.string.part_edit,
            technicalValuesRemovedOnSave = state.technicalValuesRemovedOnSave,
            onCategorySelected = onCategorySelected,
            onTypeSelected = onTypeSelected,
            onNameChanged = onNameChanged,
            onManufacturerChanged = onManufacturerChanged,
            onManufacturerNumberChanged = onManufacturerNumberChanged,
            onDescriptionChanged = onDescriptionChanged,
            onInventoryCountChanged = onInventoryCountChanged,
            onTechnicalAttributeChanged = onTechnicalAttributeChanged,
            onBikeToggled = onBikeToggled,
            onAddProductLink = onAddProductLink,
            onRemoveProductLink = onRemoveProductLink,
            onProductLinkShopSelected = onProductLinkShopSelected,
            onProductUrlChanged = onProductUrlChanged,
            onCreateShopForProductLink = onCreateShopForProductLink,
            onSelectFromPhotoLibrary = onSelectFromPhotoLibrary,
            onCreatePendingCameraCapture = onCreatePendingCameraCapture,
            onCameraCaptureCompleted = onCameraCaptureCompleted,
            onThumbnailSelected = onThumbnailSelected,
            onImageRemoved = onImageRemoved,
            onSave = {
                if (state.technicalValuesRemovedOnSave.isEmpty()) {
                    onSave()
                } else {
                    showTechnicalValuesRemovalConfirmation = true
                }
            },
            onNavigateUp = onNavigateUp,
            onNavigateHome = onNavigateHome,
        )
    }

    if (showTechnicalValuesRemovalConfirmation) {
        AlertDialog(
            onDismissRequest = { showTechnicalValuesRemovalConfirmation = false },
            title = { Text(stringResource(R.string.part_technical_attributes_removal_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.part_technical_attributes_removal_message,
                        state.technicalValuesRemovedOnSave.joinToString(),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showTechnicalValuesRemovalConfirmation = false
                    onSave()
                }) {
                    Text(stringResource(R.string.part_technical_attributes_removal_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTechnicalValuesRemovalConfirmation = false }) {
                    Text(stringResource(R.string.continue_editing))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartEditStatusContent(
    onNavigateUp: () -> Unit,
    onNavigateHome: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.part_edit)) },
                navigationIcon = {
                    HomeNavigationActions(onHomeClick = onNavigateHome) {
                        TextButton(onClick = onNavigateUp) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

private fun PartEditUiState.asFormState() = PartCreationUiState(
    category = category,
    type = type,
    name = name,
    manufacturer = manufacturer,
    manufacturerNumber = manufacturerNumber,
    description = description,
    inventoryCount = inventoryCount,
    imagePaths = imagePaths,
    thumbnailPath = thumbnailPath,
    technicalAttributes = technicalAttributes,
    selectedBikeIds = selectedBikeIds,
    productLinks = productLinks,
    categoryError = categoryError,
    nameError = nameError,
    inventoryCountError = inventoryCountError,
    imageImportFailed = imageImportFailed,
    imageRemovalFailed = imageRemovalFailed || obsoleteImageRemovalFailed,
    saveFailed = saveFailed,
    isProcessingImage = isProcessingImage,
    isSaving = isSaving,
)
