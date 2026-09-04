package de.gerritstapper.bikegarage.feature.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.usecase.ShopDeletionBlockers
import de.gerritstapper.bikegarage.domain.usecase.ShopProductReference
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationIcon
import java.io.File
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ShopDetailsScreen(
    onEdit: () -> Unit,
    onBikeClick: (Long) -> Unit,
    onPartClick: (Long) -> Unit,
    onDeleted: () -> Unit,
    viewModel: ShopDetailsViewModel = hiltViewModel(),
    deletionViewModel: ShopDeletionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deletionState by deletionViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(deletionViewModel) {
        deletionViewModel.events.collectLatest { event ->
            when (event) {
                ShopDeletionEvent.Deleted -> onDeleted()
            }
        }
    }

    ShopDetailsContent(
        state = state,
        onEdit = onEdit,
        onBikeClick = onBikeClick,
        onPartClick = onPartClick,
        onDelete = deletionViewModel::requestDeletion,
        deleteEnabled = !deletionState.isChecking && !deletionState.isDeleting,
        deletionError = deletionState.error,
        onRetryLogoRemoval = deletionViewModel::retryLogoRemoval,
        retryLogoRemovalEnabled = !deletionState.isDeleting,
    )
    ShopDeletionDialogs(
        state = deletionState,
        onDismissBlockers = deletionViewModel::dismissBlockers,
        onDismissConfirmation = deletionViewModel::dismissConfirmation,
        onConfirmDeletion = deletionViewModel::confirmDeletion,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShopDetailsContent(
    state: ShopDetailsUiState,
    onEdit: () -> Unit,
    onBikeClick: (Long) -> Unit,
    onPartClick: (Long) -> Unit,
    onDelete: () -> Unit,
    deleteEnabled: Boolean = true,
    deletionError: ShopDeletionError? = null,
    onRetryLogoRemoval: () -> Unit = {},
    retryLogoRemovalEnabled: Boolean = true,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.shop_details)) },
                navigationIcon = { HomeNavigationIcon() },
                actions = {
                    IconButton(
                        onClick = onEdit,
                        enabled = state.shop != null,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.shop_edit),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = state.shop != null && deleteEnabled,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.shop == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    Text(
                        text = stringResource(deletionError?.messageRes() ?: R.string.shop_not_found),
                        textAlign = TextAlign.Center,
                    )
                    if (deletionError == ShopDeletionError.LOGO_REMOVAL_FAILED) {
                        Button(
                            onClick = onRetryLogoRemoval,
                            enabled = retryLogoRemovalEnabled,
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            else -> {
                ShopDetailsLoadedContent(
                    shop = state.shop,
                    purchasedBikes = state.purchasedBikes,
                    parts = state.parts,
                    onBikeClick = onBikeClick,
                    onPartClick = onPartClick,
                    deletionError = deletionError,
                    onRetryLogoRemoval = onRetryLogoRemoval,
                    retryLogoRemovalEnabled = retryLogoRemovalEnabled,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun ShopDetailsLoadedContent(
    shop: Shop,
    purchasedBikes: List<Bike>,
    parts: List<PartWithRelations>,
    onBikeClick: (Long) -> Unit,
    onPartClick: (Long) -> Unit,
    deletionError: ShopDeletionError?,
    onRetryLogoRemoval: () -> Unit,
    retryLogoRemovalEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AsyncImage(
            model = File(LocalContext.current.filesDir, shop.logoPath),
            contentDescription = stringResource(R.string.shop_logo, shop.name),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 280.dp),
        )
        Text(
            text = shop.name,
            style = MaterialTheme.typography.headlineSmall,
        )
        deletionError?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (error == ShopDeletionError.LOGO_REMOVAL_FAILED) {
                Button(
                    onClick = onRetryLogoRemoval,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = retryLogoRemovalEnabled,
                ) {
                    Text(stringResource(R.string.retry))
                }
            }
        }

        Text(
            text = stringResource(R.string.shop_details_purchased_bikes),
            style = MaterialTheme.typography.titleLarge,
        )
        if (purchasedBikes.isEmpty()) {
            Text(stringResource(R.string.shop_details_no_purchased_bikes))
        } else {
            purchasedBikes.forEach { bike ->
                ShopDetailsBikeEntry(bike = bike, onClick = { onBikeClick(bike.id) })
            }
        }

        Text(
            text = stringResource(R.string.shop_details_parts),
            style = MaterialTheme.typography.titleLarge,
        )
        if (parts.isEmpty()) {
            Text(stringResource(R.string.shop_details_no_parts))
        } else {
            parts.forEach { part ->
                ShopDetailsPartEntry(part = part, onClick = { onPartClick(part.part.id) })
            }
        }
    }
}

@Composable
private fun ShopDetailsBikeEntry(
    bike: Bike,
    onClick: () -> Unit,
) {
    val reference = bike.detailsReference()
    val description = stringResource(R.string.shop_details_bike_entry, reference)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
    ) {
        Text(
            text = reference,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ShopDetailsPartEntry(
    part: PartWithRelations,
    onClick: () -> Unit,
) {
    val bikeReferences = part.bikes.map { it.detailsReference() }
    val description = stringResource(R.string.shop_details_part_entry, part.part.name)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = part.part.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (bikeReferences.isEmpty()) {
                    stringResource(R.string.shop_details_part_without_bikes)
                } else {
                    stringResource(
                        R.string.shop_details_part_bikes,
                        bikeReferences.joinToString(),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun Bike.detailsReference(): String {
    val nickname = nickname
    return if (nickname.isNullOrBlank()) {
        stringResource(R.string.shop_delete_bike_reference_without_nickname, brand, model)
    } else {
        stringResource(R.string.shop_delete_bike_reference, nickname, brand, model)
    }
}

@Composable
private fun ShopDeletionDialogs(
    state: ShopDeletionUiState,
    onDismissBlockers: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onConfirmDeletion: () -> Unit,
) {
    state.blockers?.let { blockers ->
        ShopDeletionBlockedDialog(
            blockers = blockers,
            onDismiss = onDismissBlockers,
        )
    }
    if (state.showConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            title = { Text(stringResource(R.string.shop_delete_confirmation_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.shop_delete_confirmation_message,
                        state.shop?.name.orEmpty(),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDeletion,
                    enabled = !state.isDeleting,
                ) {
                    Text(stringResource(R.string.shop_delete_confirmation_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissConfirmation,
                    enabled = !state.isDeleting,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ShopDeletionBlockedDialog(
    blockers: ShopDeletionBlockers,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shop_delete_blocked_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.shop_delete_blocked_message))
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (blockers.purchasedBikes.isNotEmpty()) {
                        Text(stringResource(R.string.shop_delete_purchase_locations))
                        blockers.purchasedBikes.forEach { bike ->
                            Text(text = bike.detailsReference())
                        }
                    }
                    if (blockers.productReferences.isNotEmpty()) {
                        Text(stringResource(R.string.shop_delete_product_links))
                        blockers.productReferences.forEach { reference ->
                            Text(text = reference.deletionReference())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

@Composable
private fun ShopProductReference.deletionReference(): String = stringResource(
    R.string.shop_delete_product_reference,
    partName,
    productUrl,
)

private fun ShopDeletionError.messageRes(): Int = when (this) {
    ShopDeletionError.DELETE_FAILED -> R.string.shop_delete_failed
    ShopDeletionError.LOGO_REMOVAL_FAILED -> R.string.shop_logo_deletion_failed
    ShopDeletionError.SHOP_NOT_FOUND -> R.string.shop_not_found
}
