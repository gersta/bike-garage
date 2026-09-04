package de.gerritstapper.bikegarage.feature.bike

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartTechnicalFields
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationIcon
import java.io.File
import kotlinx.coroutines.flow.collectLatest

@Composable
fun BikeDetailsScreen(
    selectedShopId: Long?,
    onShopSelectionConsumed: () -> Unit,
    onEdit: () -> Unit,
    onShopClick: (Long) -> Unit,
    onCreateShop: () -> Unit,
    onPartClick: (Long) -> Unit,
    onCreatePart: () -> Unit,
    onAssignExistingPart: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: BikeDetailsViewModel = hiltViewModel(),
    deletionViewModel: BikeDeletionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deletionState by deletionViewModel.uiState.collectAsStateWithLifecycle()
    var showPartChoice by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedShopId) {
        selectedShopId?.let {
            viewModel.selectCreatedShop(it)
            onShopSelectionConsumed()
        }
    }
    LaunchedEffect(deletionViewModel) {
        deletionViewModel.events.collectLatest { event ->
            when (event) {
                BikeDeletionEvent.Deleted -> onDeleted()
            }
        }
    }

    BikeDetailsContent(
        state = state,
        onEdit = onEdit,
        onShopClick = onShopClick,
        onCreateShop = onCreateShop,
        onPartClick = onPartClick,
        onCreatePart = { showPartChoice = true },
        onDelete = deletionViewModel::requestDeletion,
        deleteEnabled = !deletionState.isBusy && !state.isAssigningShop,
    )
    if (showPartChoice) {
        BikePartAddDialog(
            onDismiss = { showPartChoice = false },
            onCreatePart = {
                showPartChoice = false
                onCreatePart()
            },
            onAssignExistingPart = {
                showPartChoice = false
                onAssignExistingPart()
            },
        )
    }
    BikeDeletionDialog(
        state = deletionState,
        onTogglePart = deletionViewModel::togglePartSelection,
        onDismiss = deletionViewModel::dismissConfirmation,
        onConfirm = deletionViewModel::confirmDeletion,
    )
}

@Composable
internal fun BikePartAddDialog(
    onDismiss: () -> Unit,
    onCreatePart: () -> Unit,
    onAssignExistingPart: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bike_part_add_choice_title)) },
        text = { Text(stringResource(R.string.bike_part_add_choice_message)) },
        confirmButton = {
            Button(onClick = onCreatePart) {
                Text(stringResource(R.string.bike_part_add_new))
            }
        },
        dismissButton = {
            TextButton(onClick = onAssignExistingPart) {
                Text(stringResource(R.string.bike_part_add_existing))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BikeDetailsContent(
    state: BikeDetailsUiState,
    onEdit: () -> Unit,
    onShopClick: (Long) -> Unit,
    onCreateShop: () -> Unit,
    onPartClick: (Long) -> Unit,
    onCreatePart: () -> Unit,
    onDelete: () -> Unit,
    deleteEnabled: Boolean = true,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.bike_details)) },
                navigationIcon = { HomeNavigationIcon() },
                actions = {
                    IconButton(
                        onClick = onEdit,
                        enabled = state.details != null && !state.isAssigningShop,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.bike_edit),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = state.details != null && deleteEnabled,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.bike_delete),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.details == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.bike_not_found), textAlign = TextAlign.Center)
            }

            else -> BikeDetailsLoadedContent(
                state = state,
                onShopClick = onShopClick,
                onCreateShop = onCreateShop,
                onPartClick = onPartClick,
                onCreatePart = onCreatePart,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun BikeDetailsLoadedContent(
    state: BikeDetailsUiState,
    onShopClick: (Long) -> Unit,
    onCreateShop: () -> Unit,
    onPartClick: (Long) -> Unit,
    onCreatePart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = checkNotNull(state.details)
    val bike = details.bikeWithImages.bike
    val partsByCategory = details.parts.groupBy { it.part.category }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BikeImageGallery(
            imagePaths = details.bikeWithImages.images
                .sortedBy { it.displayOrder }
                .map { it.storagePath },
        )
        BikeDetailField(R.string.bike_nickname, bike.nickname.orEmpty())
        BikeDetailField(R.string.bike_brand, bike.brand)
        BikeDetailField(R.string.bike_model, bike.model)
        BikeDetailField(R.string.bike_purchase_date, bike.purchaseDate.orEmpty())
        BikeShopField(
            shop = details.shop,
            hasShopSelection = bike.shopId != null,
            onShopClick = onShopClick,
            onCreateShop = onCreateShop,
            enabled = !state.isAssigningShop,
        )
        if (state.shopAssignmentFailed) {
            Text(
                text = stringResource(R.string.bike_shop_assignment_failed),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (partsByCategory.isEmpty()) {
            Text(
                text = stringResource(R.string.bike_details_equipment),
                style = MaterialTheme.typography.titleLarge,
            )
            Button(
                onClick = onCreatePart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.bike_details_add_part))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.bike_details_equipment),
                    style = MaterialTheme.typography.titleLarge,
                )
                IconButton(onClick = onCreatePart) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.bike_details_add_part),
                    )
                }
            }
        }
        if (partsByCategory.isEmpty()) {
            Text(stringResource(R.string.bike_details_no_parts))
        } else {
            PartCategory.entries.forEach { category ->
                partsByCategory[category]?.let { parts ->
                    BikePartCategorySection(
                        category = category,
                        parts = parts,
                        onPartClick = onPartClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun BikeImageGallery(imagePaths: List<String>) {
    Text(
        text = stringResource(R.string.bike_images),
        style = MaterialTheme.typography.titleLarge,
    )
    if (imagePaths.isEmpty()) {
        Text(stringResource(R.string.bike_details_no_images))
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            itemsIndexed(imagePaths, key = { _, path -> path }) { index, storagePath ->
                AsyncImage(
                    model = File(LocalContext.current.filesDir, storagePath),
                    contentDescription = stringResource(
                        R.string.bike_details_image,
                        index + 1,
                        imagePaths.size,
                    ),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(240.dp)
                        .height(180.dp),
                )
            }
        }
    }
}

@Composable
private fun BikeDetailField(labelRes: Int, value: String) {
    val displayedValue = value.ifBlank { stringResource(R.string.bike_details_not_specified) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.titleSmall)
        Text(text = displayedValue, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BikeShopField(
    shop: Shop?,
    hasShopSelection: Boolean,
    onShopClick: (Long) -> Unit,
    onCreateShop: () -> Unit,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (shop != null) {
            val description = stringResource(R.string.bike_details_shop_entry, shop.name)
            TextButton(
                onClick = { onShopClick(shop.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = description },
            ) {
                Text(
                    text = description,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.bike_shop),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    if (hasShopSelection) {
                        R.string.bike_details_shop_not_found
                    } else {
                        R.string.bike_shop_none
                    },
                ),
            )
            Button(onClick = onCreateShop, enabled = enabled) {
                Text(stringResource(R.string.bike_shop_create))
            }
        }
    }
}

@Composable
private fun BikePartCategorySection(
    category: PartCategory,
    parts: List<PartWithRelations>,
    onPartClick: (Long) -> Unit,
) {
    Text(
        text = stringResource(category.titleRes()),
        style = MaterialTheme.typography.titleMedium,
    )
    parts.asTypeGroups(category).forEach { typeGroup ->
        Text(
            text = typeGroup.type
                ?: stringResource(R.string.parts_overview_unknown_type),
            style = MaterialTheme.typography.titleSmall,
        )
        typeGroup.parts.forEach { partWithRelations ->
            val part = partWithRelations.part
            val technicalAttribute = partWithRelations.technicalAttribute()
            val description = stringResource(R.string.bike_details_part_entry, part.name)
            Card(
                onClick = { onPartClick(part.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = description },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(text = part.name, style = MaterialTheme.typography.bodyLarge)
                    technicalAttribute?.let { attribute ->
                        Text(
                            text = "${attribute.label}: ${attribute.value}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

private data class BikePartTypeGroup(
    val type: String?,
    val parts: List<PartWithRelations>,
)

private fun List<PartWithRelations>.asTypeGroups(category: PartCategory): List<BikePartTypeGroup> =
    groupBy { details ->
        PartTechnicalFields.canonicalType(details.part.type)
            ?.takeIf { PartTechnicalFields.isTypeForCategory(it, category) }
    }
        .map { (type, parts) ->
            BikePartTypeGroup(
                type = type,
                parts = parts.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.part.name }),
            )
        }
        .sortedWith(
            compareBy<BikePartTypeGroup> { it.type == null }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.type.orEmpty() },
        )

private fun PartWithRelations.technicalAttribute(): PartTechnicalAttribute? {
    val canonicalType = PartTechnicalFields.canonicalType(part.type)
        ?.takeIf { PartTechnicalFields.isTypeForCategory(it, part.category) }
        ?: return null
    return bikePartSecondaryDetails(canonicalType, attributes)?.technicalAttribute
}

internal data class BikePartSecondaryDetails(
    val type: String,
    val technicalAttribute: PartTechnicalAttribute?,
)

internal data class PartTechnicalAttribute(
    val label: String,
    val value: String,
)

internal fun bikePartSecondaryDetails(
    type: String?,
    attributes: List<PartAttribute>,
): BikePartSecondaryDetails? {
    val canonicalType = PartTechnicalFields.canonicalType(type) ?: return null
    val valuesByKey = attributes.associate { it.key to it.value }
    val technicalAttribute = PartTechnicalFields.forType(canonicalType)
        .firstOrNull { field -> !valuesByKey[field.key].isNullOrBlank() }
        ?.let { field -> PartTechnicalAttribute(field.label, checkNotNull(valuesByKey[field.key])) }

    return BikePartSecondaryDetails(
        type = canonicalType,
        technicalAttribute = technicalAttribute,
    )
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
