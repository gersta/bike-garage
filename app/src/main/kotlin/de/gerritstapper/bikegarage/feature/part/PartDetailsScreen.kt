package de.gerritstapper.bikegarage.feature.part

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartImage
import de.gerritstapper.bikegarage.domain.model.PartTechnicalFields
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationIcon
import java.io.File
import kotlinx.coroutines.flow.collectLatest
import coil3.compose.AsyncImage

@Composable
fun PartDetailsScreen(
    onEdit: () -> Unit,
    onBikeClick: (Long) -> Unit,
    onDeleted: () -> Unit,
    viewModel: PartDetailsViewModel = hiltViewModel(),
    deletionViewModel: PartDeletionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deletionState by deletionViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var browserOpenFailed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(deletionViewModel) {
        deletionViewModel.events.collectLatest { event ->
            when (event) {
                PartDeletionEvent.Deleted -> onDeleted()
            }
        }
    }

    PartDetailsContent(
        state = state,
        onEdit = onEdit,
        onBikeClick = onBikeClick,
        onDelete = deletionViewModel::requestDeletion,
        onProductUrlClick = { productUrl ->
            browserOpenFailed = !openProductUrl(context, productUrl)
        },
        browserOpenFailed = browserOpenFailed,
        deleteEnabled = !deletionState.isBusy,
        deletionError = deletionState.error,
    )
    PartDeletionDialog(
        state = deletionState,
        onDismiss = deletionViewModel::dismissConfirmation,
        onConfirm = deletionViewModel::confirmDeletion,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PartDetailsContent(
    state: PartDetailsUiState,
    onEdit: () -> Unit,
    onBikeClick: (Long) -> Unit,
    onDelete: () -> Unit,
    onProductUrlClick: (String) -> Unit,
    browserOpenFailed: Boolean = false,
    deleteEnabled: Boolean = true,
    deletionError: PartDeletionError? = null,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.part_details)) },
                navigationIcon = { HomeNavigationIcon() },
                actions = {
                    IconButton(onClick = onEdit, enabled = state.details != null) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.part_edit),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = state.details != null && deleteEnabled,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.part_delete),
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
                Text(
                    stringResource(deletionError?.messageRes() ?: R.string.part_not_found),
                    textAlign = TextAlign.Center,
                )
            }

            else -> PartDetailsLoadedContent(
                details = checkNotNull(state.details),
                onBikeClick = onBikeClick,
                onProductUrlClick = onProductUrlClick,
                browserOpenFailed = browserOpenFailed,
                deletionError = deletionError,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun PartDetailsLoadedContent(
    details: PartWithRelations,
    onBikeClick: (Long) -> Unit,
    onProductUrlClick: (String) -> Unit,
    browserOpenFailed: Boolean,
    deletionError: PartDeletionError?,
    modifier: Modifier = Modifier,
) {
    val part = details.part
    val technicalAttributes = technicalAttributesFor(part.type, details.attributes)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PartImageGallery(details.images)
        PartDetailField(R.string.part_category, stringResource(part.category.titleRes()))
        PartDetailField(R.string.part_type, part.type.orEmpty())
        PartDetailField(R.string.part_name, part.name)
        PartDetailField(R.string.part_manufacturer, part.manufacturer.orEmpty())
        PartDetailField(R.string.part_manufacturer_number, part.manufacturerNumber.orEmpty())
        PartDetailField(R.string.part_description, part.description.orEmpty())
        part.inventoryCount?.let { inventoryCount ->
            PartDetailField(R.string.part_inventory_count, inventoryCount.toString())
        }

        Text(
            text = stringResource(R.string.part_details_assigned_bikes),
            style = MaterialTheme.typography.titleLarge,
        )
        if (details.bikes.isEmpty()) {
            Text(stringResource(R.string.part_details_no_assigned_bikes))
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                details.bikes.forEach { bike ->
                    val label = bike.nickname?.takeIf { it.isNotBlank() }
                        ?: "${bike.brand} ${bike.model}"
                    val description = stringResource(R.string.part_details_bike_entry, label)
                    AssistChip(
                        onClick = { onBikeClick(bike.id) },
                        label = { Text(label) },
                        modifier = Modifier
                            .testTag("part-details-bike-${bike.id}")
                            .semantics {
                                contentDescription = description
                            },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.part_technical_attributes),
            style = MaterialTheme.typography.titleLarge,
        )
        if (technicalAttributes.isEmpty()) {
            Text(stringResource(R.string.part_no_technical_attributes))
        } else {
            technicalAttributes.forEach { attribute ->
                PartDetailField(attribute.label, attribute.value)
            }
        }

        Text(
            text = stringResource(R.string.part_product_links),
            style = MaterialTheme.typography.titleLarge,
        )
        if (details.shopLinks.isEmpty()) {
            Text(stringResource(R.string.part_no_product_links))
        } else {
            details.shopLinks.forEach { shopLink ->
                val shopName = shopLink.shop?.name
                    ?: stringResource(R.string.part_unknown_shop)
                val description = stringResource(
                    R.string.part_product_link_entry,
                    shopName,
                    shopLink.link.productUrl,
                )
                Card(
                    onClick = { onProductUrlClick(shopLink.link.productUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = description },
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(shopName, style = MaterialTheme.typography.titleMedium)
                        Text(shopLink.link.productUrl, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (browserOpenFailed) {
            Text(
                text = stringResource(R.string.part_product_link_open_failed),
                color = MaterialTheme.colorScheme.error,
            )
        }

        deletionError?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PartImageGallery(images: List<PartImage>) {
    val filesDir = LocalContext.current.filesDir
    val imageFiles = images
        .asSequence()
        .sortedBy { it.displayOrder }
        .mapNotNull { image -> image.storagePath.takeIf { it.isNotBlank() } }
        .map { storagePath -> File(filesDir, storagePath) }
        .filter { file -> file.isFile }
        .toList()

    Text(
        text = stringResource(R.string.part_images),
        style = MaterialTheme.typography.titleLarge,
    )
    if (imageFiles.isEmpty()) {
        Text(stringResource(R.string.part_details_no_images))
    } else {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            imageFiles.forEachIndexed { index, imageFile ->
                AsyncImage(
                    model = imageFile,
                    contentDescription = stringResource(
                        R.string.part_details_image,
                        index + 1,
                        imageFiles.size,
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
private fun PartDetailField(labelRes: Int, value: String) {
    PartDetailField(
        label = stringResource(labelRes),
        value = value,
    )
}

@Composable
private fun PartDetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = value.ifBlank { stringResource(R.string.part_details_not_specified) },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

internal data class PartTechnicalAttribute(
    val label: String,
    val value: String,
)

internal fun technicalAttributesFor(
    type: String?,
    attributes: List<PartAttribute>,
): List<PartTechnicalAttribute> {
    val definedFields = PartTechnicalFields.forType(type)
    val valuesByKey = attributes.associate { it.key to it.value }
    val definedKeys = definedFields.mapTo(mutableSetOf()) { it.key }

    return buildList {
        definedFields.forEach { field ->
            add(PartTechnicalAttribute(field.label, valuesByKey[field.key].orEmpty()))
        }
        attributes
            .asSequence()
            .filterNot { it.key in definedKeys }
            .sortedBy { it.key.lowercase() }
            .forEach { attribute ->
                add(PartTechnicalAttribute(attribute.key, attribute.value))
            }
    }
}

internal fun productUrlIntent(productUrl: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(productUrl))
        .addCategory(Intent.CATEGORY_BROWSABLE)

internal fun openProductUrl(context: Context, productUrl: String): Boolean {
    if (!isProductUrl(productUrl)) return false
    return try {
        context.startActivity(productUrlIntent(productUrl))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

internal fun isProductUrl(productUrl: String): Boolean {
    val uri = Uri.parse(productUrl)
    return !uri.host.isNullOrBlank() &&
        (uri.scheme.equals("https", ignoreCase = true) ||
            uri.scheme.equals("http", ignoreCase = true))
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
