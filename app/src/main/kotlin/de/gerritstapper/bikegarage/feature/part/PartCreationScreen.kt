package de.gerritstapper.bikegarage.feature.part

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartTechnicalFields
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.usecase.PartCreationFieldError
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationActions
import de.gerritstapper.bikegarage.ui.navigation.LocalNavigateHome
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PartCreationScreen(
    selectedShopId: Long?,
    onShopSelectionConsumed: () -> Unit,
    onCreateShop: () -> Unit,
    onPartCreated: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: PartCreationViewModel = hiltViewModel(),
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
                is PartCreationEvent.Created -> onPartCreated()
            }
        }
    }

    fun requestNavigation(navigateHome: Boolean) {
        if (state.isBusy) return
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
    PartCreationContent(
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun PartCreationContent(
    state: PartCreationUiState,
    bikes: List<Bike>,
    shops: List<Shop>,
    titleRes: Int = R.string.part_create,
    technicalValuesRemovedOnSave: List<String> = emptyList(),
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
    var categoryMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var typeMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    HomeNavigationActions(onHomeClick = onNavigateHome) {
                        TextButton(onClick = onNavigateUp, enabled = !state.isSaving) {
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
            Text(stringResource(R.string.part_category), style = MaterialTheme.typography.titleMedium)
            PartDropdown(
                selectedLabel = state.category?.let { stringResource(it.titleRes()) }
                    ?: stringResource(R.string.part_category_select),
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it },
                enabled = !state.isBusy,
                testTag = "part-category",
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.part_category_select)) },
                    onClick = {
                        onCategorySelected(null)
                        categoryMenuExpanded = false
                    },
                )
                PartCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(stringResource(category.titleRes())) },
                        onClick = {
                            onCategorySelected(category)
                            categoryMenuExpanded = false
                        },
                    )
                }
            }
            state.categoryError?.let { PartFieldError(it) }

            Text(stringResource(R.string.part_type), style = MaterialTheme.typography.titleMedium)
            PartDropdown(
                selectedLabel = state.type ?: stringResource(R.string.part_type_select),
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = it },
                enabled = !state.isSaving && state.category != null,
                testTag = "part-type",
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.part_type_select)) },
                    onClick = {
                        onTypeSelected(null)
                        typeMenuExpanded = false
                    },
                )
                PartTechnicalFields.forCategory(state.category).forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = {
                            onTypeSelected(type.name)
                            typeMenuExpanded = false
                        },
                    )
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChanged,
                label = { Text(stringResource(R.string.part_name)) },
                modifier = Modifier.fillMaxWidth().testTag("part-name"),
                enabled = !state.isBusy,
                singleLine = true,
                isError = state.nameError != null,
                supportingText = {
                    state.nameError?.let { error -> PartFieldError(error) }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = state.manufacturer,
                onValueChange = onManufacturerChanged,
                label = { Text(stringResource(R.string.part_manufacturer)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = state.manufacturerNumber,
                onValueChange = onManufacturerNumberChanged,
                label = { Text(stringResource(R.string.part_manufacturer_number)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChanged,
                label = { Text(stringResource(R.string.part_description)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                minLines = 3,
            )
            OutlinedTextField(
                value = state.inventoryCount,
                onValueChange = onInventoryCountChanged,
                label = { Text(stringResource(R.string.part_inventory_count)) },
                modifier = Modifier.fillMaxWidth().testTag("part-inventory-count"),
                enabled = !state.isBusy,
                singleLine = true,
                isError = state.inventoryCountError,
                supportingText = {
                    if (state.inventoryCountError) {
                        Text(
                            stringResource(R.string.part_inventory_count_invalid),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
            )

            if (technicalValuesRemovedOnSave.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.part_technical_attributes_removed_on_save,
                        technicalValuesRemovedOnSave.joinToString(),
                    ),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            state.type?.let { type ->
                val technicalFields = PartTechnicalFields.forType(type)
                Text(
                    stringResource(R.string.part_technical_attributes),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (technicalFields.isEmpty()) {
                    Text(stringResource(R.string.part_no_technical_attributes))
                } else {
                    technicalFields.forEach { field ->
                        OutlinedTextField(
                            value = state.technicalAttributes[field.key].orEmpty(),
                            onValueChange = { onTechnicalAttributeChanged(field.key, it) },
                            label = { Text(field.label) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("part-technical-${field.key}"),
                            enabled = !state.isSaving,
                            singleLine = true,
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.part_bike_assignments),
                style = MaterialTheme.typography.titleMedium,
            )
            if (bikes.isEmpty()) {
                Text(stringResource(R.string.part_no_bikes))
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    bikes.forEach { bike ->
                        val label = bikeLabel(bike)
                        FilterChip(
                            selected = bike.id in state.selectedBikeIds,
                            onClick = { onBikeToggled(bike.id) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("part-bike-${bike.id}"),
                            enabled = !state.isBusy,
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.part_images),
                style = MaterialTheme.typography.titleMedium,
            )
            PartImagePicker(
                imagePaths = state.imagePaths,
                thumbnailPath = state.thumbnailPath,
                enabled = !state.isBusy,
                onSelectFromPhotoLibrary = onSelectFromPhotoLibrary,
                onCreatePendingCameraCapture = onCreatePendingCameraCapture,
                onCameraCaptureCompleted = onCameraCaptureCompleted,
                onThumbnailSelected = onThumbnailSelected,
                onImageRemoved = onImageRemoved,
            )
            when {
                state.imageImportFailed -> PartImageError(R.string.part_image_import_failed)
                state.imageRemovalFailed -> PartImageError(R.string.part_image_removal_failed)
            }

            Text(
                stringResource(R.string.part_product_links),
                style = MaterialTheme.typography.titleMedium,
            )
            state.productLinks.forEachIndexed { index, link ->
                ProductLinkFields(
                    index = index,
                    link = link,
                    shops = shops,
                    enabled = !state.isSaving,
                    onShopSelected = { onProductLinkShopSelected(link.id, it) },
                    onProductUrlChanged = { onProductUrlChanged(link.id, it) },
                    onCreateShop = { onCreateShopForProductLink(link.id) },
                    onRemove = { onRemoveProductLink(link.id) },
                )
            }
            Button(
                onClick = onAddProductLink,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            ) {
                Text(stringResource(R.string.part_product_link_add))
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            ) {
                Text(stringResource(R.string.save))
            }
            if (state.saveFailed) PartSaveError()
        }
    }
}

@Composable
private fun PartDropdown(
    selectedLabel: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean,
    testTag: String,
    content: @Composable () -> Unit,
) {
    Box {
        OutlinedButton(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.fillMaxWidth().testTag(testTag),
            enabled = enabled,
        ) {
            Text(selectedLabel)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            content()
        }
    }
}

@Composable
private fun ProductLinkFields(
    index: Int,
    link: PartProductLinkUiState,
    shops: List<Shop>,
    enabled: Boolean,
    onShopSelected: (Long?) -> Unit,
    onProductUrlChanged: (String) -> Unit,
    onCreateShop: () -> Unit,
    onRemove: () -> Unit,
) {
    var shopMenuExpanded by rememberSaveable(link.id) { mutableStateOf(false) }
    val selectedShop = shops.firstOrNull { it.id == link.shopId }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.part_product_link_number, index + 1),
            style = MaterialTheme.typography.titleSmall,
        )
        Box {
            OutlinedButton(
                onClick = { shopMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            ) {
                Text(selectedShop?.name ?: stringResource(R.string.part_shop_select))
            }
            DropdownMenu(
                expanded = shopMenuExpanded,
                onDismissRequest = { shopMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.part_shop_select)) },
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
        link.shopError?.let { error -> PartFieldError(error) }
        Button(onClick = onCreateShop, modifier = Modifier.fillMaxWidth(), enabled = enabled) {
            Text(stringResource(R.string.part_shop_create))
        }
        OutlinedTextField(
            value = link.productUrl,
            onValueChange = onProductUrlChanged,
            label = { Text(stringResource(R.string.part_product_url)) },
            modifier = Modifier.fillMaxWidth().testTag("part-product-url-${link.id}"),
            enabled = enabled,
            singleLine = true,
            isError = link.productUrlError != null,
            supportingText = {
                link.productUrlError?.let { error -> PartFieldError(error) }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Next,
            ),
        )
        TextButton(onClick = onRemove, enabled = enabled) {
            Text(stringResource(R.string.part_product_link_remove))
        }
    }
}

@Composable
private fun PartFieldError(error: PartCreationFieldError) {
    Text(
        stringResource(error.messageRes()),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun PartSaveError() {
    Text(
        stringResource(R.string.part_save_failed),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun PartImageError(messageRes: Int) {
    Text(
        stringResource(messageRes),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun PartCreationFieldError.messageRes(): Int = when (this) {
    PartCreationFieldError.CATEGORY_REQUIRED -> R.string.part_category_required
    PartCreationFieldError.NAME_REQUIRED -> R.string.part_name_required
    PartCreationFieldError.SHOP_REQUIRED -> R.string.part_shop_required
    PartCreationFieldError.PRODUCT_URL_REQUIRED -> R.string.part_product_url_required
    PartCreationFieldError.PRODUCT_URL_INVALID -> R.string.part_product_url_invalid
    PartCreationFieldError.INVENTORY_COUNT_INVALID -> R.string.part_inventory_count_invalid
}

private fun PartCategory.titleRes(): Int = when (this) {
    PartCategory.DRIVETRAIN -> R.string.part_category_drivetrain
    PartCategory.BRAKES -> R.string.part_category_brakes
    PartCategory.WHEELS -> R.string.part_category_wheels
    PartCategory.COCKPIT -> R.string.part_category_cockpit
    PartCategory.SADDLE -> R.string.part_category_saddle
    PartCategory.SUSPENSION -> R.string.part_category_suspension
    PartCategory.ACCESSORIES -> R.string.part_category_accessories
}

private fun bikeLabel(bike: Bike): String =
    bike.nickname?.takeIf { it.isNotBlank() } ?: "${bike.brand} ${bike.model}"
