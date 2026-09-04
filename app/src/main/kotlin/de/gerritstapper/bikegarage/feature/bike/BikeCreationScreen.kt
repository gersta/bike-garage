package de.gerritstapper.bikegarage.feature.bike

import android.net.Uri
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.data.image.ImageCaptureContracts
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.usecase.BikeCreationFieldError
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationActions
import de.gerritstapper.bikegarage.ui.navigation.LocalNavigateHome
import java.io.File
import kotlinx.coroutines.flow.collectLatest

internal data class BikeFormUiState(
    val nickname: String,
    val brand: String,
    val model: String,
    val purchaseDate: String,
    val shopId: Long?,
    val imagePaths: List<String>,
    val thumbnailPath: String?,
    @StringRes val brandError: Int? = null,
    @StringRes val modelError: Int? = null,
    @StringRes val imageError: Int? = null,
    @StringRes val purchaseDateError: Int? = null,
    val imageImportFailed: Boolean = false,
    @StringRes val imageRemovalError: Int? = null,
    val saveFailed: Boolean = false,
    val isInputEnabled: Boolean = true,
    val isSaveEnabled: Boolean = true,
    val isNavigationEnabled: Boolean = true,
)

@Composable
fun BikeCreationScreen(
    selectedShopId: Long?,
    onShopSelectionConsumed: () -> Unit,
    onCreateShop: () -> Unit,
    onBikeCreated: (Long) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: BikeCreationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val shops by viewModel.shops.collectAsStateWithLifecycle()
    val goHome = LocalNavigateHome.current
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    var navigateHomeAfterDiscard by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedShopId) {
        selectedShopId?.let {
            viewModel.onShopSelected(it)
            onShopSelectionConsumed()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is BikeCreationEvent.Created -> onBikeCreated(event.bikeId)
            }
        }
    }

    fun requestNavigation(navigateHome: Boolean) {
        if (state.isBusy) return
        if (state.isDirty) {
            navigateHomeAfterDiscard = navigateHome
            showDiscardConfirmation = true
        } else {
            if (navigateHome) {
                goHome()
            } else {
                onNavigateUp()
            }
        }
    }

    BackHandler {
        requestNavigation(navigateHome = false)
    }

    BikeCreationContent(
        state = state,
        shops = shops,
        onNicknameChanged = viewModel::onNicknameChanged,
        onBrandChanged = viewModel::onBrandChanged,
        onModelChanged = viewModel::onModelChanged,
        onPurchaseDateChanged = viewModel::onPurchaseDateChanged,
        onShopSelected = viewModel::onShopSelected,
        onCreateShop = onCreateShop,
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
        BikeDiscardChangesDialog(
            onDiscard = {
                showDiscardConfirmation = false
                viewModel.discardUnsavedImages {
                    if (navigateHomeAfterDiscard) {
                        goHome()
                    } else {
                        onNavigateUp()
                    }
                }
            },
            onContinueEditing = { showDiscardConfirmation = false },
        )
    }

}

@Composable
internal fun BikeDiscardChangesDialog(
    onDiscard: () -> Unit,
    onContinueEditing: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onContinueEditing,
        title = { Text(stringResource(R.string.bike_discard_changes_title)) },
        text = { Text(stringResource(R.string.bike_discard_changes_message)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.discard))
            }
        },
        dismissButton = {
            TextButton(onClick = onContinueEditing) {
                Text(stringResource(R.string.continue_editing))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BikeCreationContent(
    state: BikeCreationUiState,
    shops: List<Shop>,
    onNicknameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    onPurchaseDateChanged: (String) -> Unit,
    onShopSelected: (Long?) -> Unit,
    onCreateShop: () -> Unit,
    onSelectFromPhotoLibrary: (List<Uri>) -> Unit,
    onCreatePendingCameraCapture: () -> PendingImage?,
    onCameraCaptureCompleted: (PendingImage, Boolean) -> Unit,
    onThumbnailSelected: (String) -> Unit,
    onImageRemoved: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateHome: (() -> Unit)? = null,
    knownBrands: List<String> = BikeBrandCatalog.knownBrands,
) = BikeFormContent(
    titleRes = R.string.bike_create,
    state = BikeFormUiState(
        nickname = state.nickname,
        brand = state.brand,
        model = state.model,
        purchaseDate = state.purchaseDate,
        shopId = state.shopId,
        imagePaths = state.imagePaths,
        thumbnailPath = state.thumbnailPath,
        brandError = state.brandError?.messageRes(),
        modelError = state.modelError?.messageRes(),
        imageError = state.imageError?.messageRes(),
        purchaseDateError = state.purchaseDateError?.messageRes(),
        imageImportFailed = state.imageImportFailed,
        imageRemovalError = R.string.bike_image_removal_failed.takeIf { state.imageRemovalFailed },
        saveFailed = state.saveFailed,
        isInputEnabled = !state.isBusy,
        isSaveEnabled = !state.isBusy,
        isNavigationEnabled = !state.isBusy,
    ),
    shops = shops,
    onNicknameChanged = onNicknameChanged,
    onBrandChanged = onBrandChanged,
    onModelChanged = onModelChanged,
    onPurchaseDateChanged = onPurchaseDateChanged,
    onShopSelected = onShopSelected,
    onCreateShop = onCreateShop,
    onSelectFromPhotoLibrary = onSelectFromPhotoLibrary,
    onCreatePendingCameraCapture = onCreatePendingCameraCapture,
    onCameraCaptureCompleted = onCameraCaptureCompleted,
    onThumbnailSelected = onThumbnailSelected,
    onImageRemoved = onImageRemoved,
    onSave = onSave,
    onNavigateUp = onNavigateUp,
    onNavigateHome = onNavigateHome,
    knownBrands = knownBrands,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BikeFormContent(
    @StringRes titleRes: Int,
    state: BikeFormUiState,
    shops: List<Shop>,
    onNicknameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    onPurchaseDateChanged: (String) -> Unit,
    onShopSelected: (Long?) -> Unit,
    onCreateShop: () -> Unit,
    onSelectFromPhotoLibrary: (List<Uri>) -> Unit,
    onCreatePendingCameraCapture: () -> PendingImage?,
    onCameraCaptureCompleted: (PendingImage, Boolean) -> Unit,
    onThumbnailSelected: (String) -> Unit,
    onImageRemoved: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateHome: (() -> Unit)? = null,
    knownBrands: List<String> = BikeBrandCatalog.knownBrands,
) {
    var pendingCameraCapture: PendingImage? by remember { mutableStateOf(null) }
    var shopMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var brandMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showPurchaseDatePicker by rememberSaveable { mutableStateOf(false) }
    var showImageSourceChoice by rememberSaveable { mutableStateOf(false) }
    val photoLibraryLauncher = rememberLauncherForActivityResult(
        contract = ImageCaptureContracts.selectMultipleFromPhotoLibrary,
        onResult = onSelectFromPhotoLibrary,
    )
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ImageCaptureContracts.takePicture,
        onResult = { isSaved ->
            pendingCameraCapture?.let { onCameraCaptureCompleted(it, isSaved) }
            pendingCameraCapture = null
        },
    )
    val selectedShopName = shops.firstOrNull { it.id == state.shopId }?.name

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    HomeNavigationActions(onHomeClick = onNavigateHome) {
                        TextButton(onClick = onNavigateUp, enabled = state.isNavigationEnabled) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BikeBrandField(
                value = state.brand,
                knownBrands = knownBrands,
                expanded = brandMenuExpanded,
                isEnabled = state.isInputEnabled,
                isError = state.brandError != null,
                errorRes = state.brandError,
                onValueChange = onBrandChanged,
                onExpandedChange = { brandMenuExpanded = it },
            )
            OutlinedTextField(
                value = state.model,
                onValueChange = onModelChanged,
                label = { Text(stringResource(R.string.bike_model)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bike-model"),
                enabled = state.isInputEnabled,
                singleLine = true,
                isError = state.modelError != null,
                supportingText = {
                    state.modelError?.let { Text(stringResource(it)) }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = state.nickname,
                onValueChange = onNicknameChanged,
                label = { Text(stringResource(R.string.bike_nickname)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isInputEnabled,
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
            )
            PurchaseDateField(
                value = state.purchaseDate,
                isEnabled = state.isInputEnabled,
                errorRes = state.purchaseDateError,
                onSelectDate = { showPurchaseDatePicker = true },
                onClearDate = { onPurchaseDateChanged("") },
            )

            Text(
                text = stringResource(R.string.bike_shop),
                style = MaterialTheme.typography.titleMedium,
            )
            Box {
                OutlinedButton(
                    onClick = { shopMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isInputEnabled,
                ) {
                    Text(selectedShopName ?: stringResource(R.string.bike_shop_none))
                }
                DropdownMenu(
                    expanded = shopMenuExpanded,
                    onDismissRequest = { shopMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.bike_shop_none)) },
                        onClick = {
                            onShopSelected(null)
                            shopMenuExpanded = false
                        },
                    )
                    shops.forEach { shop ->
                        DropdownMenuItem(
                            text = { Text(shop.name) },
                            onClick = {
                                onShopSelected(shop.id)
                                shopMenuExpanded = false
                            },
                        )
                    }
                }

            }
            Button(
                onClick = onCreateShop,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isInputEnabled,
            ) {
                Text(stringResource(R.string.bike_shop_create))
            }

            Text(
                text = stringResource(R.string.bike_images),
                style = MaterialTheme.typography.titleMedium,
            )
            BikeImagesSection(
                imagePaths = state.imagePaths,
                thumbnailPath = state.thumbnailPath,
                enabled = state.isInputEnabled,
                onAddImage = { showImageSourceChoice = true },
                onThumbnailSelected = onThumbnailSelected,
                onImageRemoved = onImageRemoved,
            )
            when {
                state.imageError != null -> FieldError(state.imageError)
                state.imageImportFailed -> FieldError(R.string.bike_image_import_failed)
                state.imageRemovalError != null -> FieldError(state.imageRemovalError)
            }
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isSaveEnabled,
            ) {
                Text(stringResource(R.string.save))
            }
            if (state.saveFailed) {
                FieldError(R.string.bike_save_failed)
            }
        }
    }
    if (showImageSourceChoice) {
        BikeImageSourceChoiceDialog(
            onDismiss = { showImageSourceChoice = false },
            onTakePhoto = {
                showImageSourceChoice = false
                onCreatePendingCameraCapture()?.let { pendingImage ->
                    pendingCameraCapture = pendingImage
                    cameraLauncher.launch(pendingImage.uri)
                }
            },
            onChooseFromPhotoLibrary = {
                showImageSourceChoice = false
                photoLibraryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
    if (showPurchaseDatePicker) {
        PurchaseDatePickerDialog(
            initialDate = state.purchaseDate,
            onDateSelected = onPurchaseDateChanged,
            onDismiss = { showPurchaseDatePicker = false },
        )
    }
}

@Composable
private fun BikeImagesSection(
    imagePaths: List<String>,
    thumbnailPath: String?,
    enabled: Boolean,
    onAddImage: () -> Unit,
    onThumbnailSelected: (String) -> Unit,
    onImageRemoved: (String) -> Unit,
) {
    if (imagePaths.isEmpty()) {
        OutlinedButton(
            onClick = onAddImage,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("bike-image-placeholder"),
            enabled = enabled,
        ) {
            Text(stringResource(R.string.bike_images))
        }
    } else {
        imagePaths.forEachIndexed { index, storagePath ->
            BikeImageRow(
                storagePath = storagePath,
                index = index,
                isThumbnail = storagePath == thumbnailPath,
                enabled = enabled,
                onThumbnailSelected = { onThumbnailSelected(storagePath) },
                onImageRemoved = { onImageRemoved(storagePath) },
            )
        }
        OutlinedButton(
            onClick = onAddImage,
            modifier = Modifier.testTag("bike-image-add"),
            enabled = enabled,
        ) {
            Text("+")
        }
    }
}

@Composable
private fun BikeImageSourceChoiceDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromPhotoLibrary: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bike_images)) },
        text = {
            Column {
                TextButton(onClick = onTakePhoto) {
                    Text(stringResource(R.string.bike_image_camera))
                }
                TextButton(onClick = onChooseFromPhotoLibrary) {
                    Text(stringResource(R.string.bike_image_photo_library))
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun PurchaseDatePickerDialog(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toPurchaseDatePickerMillisOrNull(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                        onDateSelected(selectedDateMillis.toPurchaseDateIso())
                    }
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BikeBrandField(
    value: String,
    knownBrands: List<String>,
    expanded: Boolean,
    isEnabled: Boolean,
    isError: Boolean,
    @StringRes errorRes: Int?,
    onValueChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
) {
    val matchingBrands = knownBrands.filter { it.contains(value, ignoreCase = true) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                onExpandedChange(true)
            },
            label = { Text(stringResource(R.string.bike_brand)) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryEditable,
                    isEnabled,
                )
                .testTag("bike-brand"),
            enabled = isEnabled,
            singleLine = true,
            isError = isError,
            supportingText = {
                errorRes?.let { Text(stringResource(it)) }
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Next,
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded && matchingBrands.isNotEmpty(),
            onDismissRequest = { onExpandedChange(false) },
        ) {
            matchingBrands.forEach { brand ->
                DropdownMenuItem(
                    text = { Text(brand) },
                    onClick = {
                        onValueChange(brand)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.testTag("bike-brand-option-$brand"),
                )
            }
        }
    }
}

@Composable
private fun PurchaseDateField(
    value: String,
    isEnabled: Boolean,
    @StringRes errorRes: Int?,
    onSelectDate: () -> Unit,
    onClearDate: () -> Unit,
) {
    OutlinedButton(
        onClick = onSelectDate,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bike-purchase-date"),
        enabled = isEnabled,
    ) {
        Text(
            text = if (value.isBlank()) {
                stringResource(R.string.bike_purchase_date_select)
            } else {
                stringResource(R.string.bike_purchase_date_value, value)
            },
        )
    }
    if (value.isNotBlank()) {
        TextButton(
            onClick = onClearDate,
            enabled = isEnabled,
            modifier = Modifier.testTag("bike-purchase-date-clear"),
        ) {
            Text(stringResource(R.string.bike_purchase_date_clear))
        }
    }
    errorRes?.let { FieldError(it) }
}

@Composable
private fun BikeImageRow(
    storagePath: String,
    index: Int,
    isThumbnail: Boolean,
    enabled: Boolean,
    onThumbnailSelected: () -> Unit,
    onImageRemoved: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = File(LocalContext.current.filesDir, storagePath),
            contentDescription = stringResource(R.string.bike_image_preview, index + 1),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(88.dp)
                .height(88.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isThumbnail) {
                Text(
                    text = stringResource(R.string.bike_image_thumbnail),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                TextButton(onClick = onThumbnailSelected, enabled = enabled) {
                    Text(stringResource(R.string.bike_image_make_thumbnail))
                }
            }
            TextButton(onClick = onImageRemoved, enabled = enabled) {
                Text(stringResource(R.string.bike_image_remove))
            }
        }
    }
}

@Composable
private fun FieldError(@StringRes messageRes: Int) {
    Text(
        text = stringResource(messageRes),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

internal fun BikeCreationFieldError.messageRes(): Int = when (this) {
    BikeCreationFieldError.BRAND_REQUIRED -> R.string.bike_brand_required
    BikeCreationFieldError.MODEL_REQUIRED -> R.string.bike_model_required
    BikeCreationFieldError.IMAGE_REQUIRED -> R.string.bike_image_required
    BikeCreationFieldError.PURCHASE_DATE_INVALID -> R.string.bike_purchase_date_invalid
}
