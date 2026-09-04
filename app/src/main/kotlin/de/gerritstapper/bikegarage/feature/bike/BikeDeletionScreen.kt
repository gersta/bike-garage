package de.gerritstapper.bikegarage.feature.bike

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationIcon
import kotlinx.coroutines.flow.collectLatest

@Composable
fun BikeDeletionScreen(
    onDeleted: () -> Unit,
    viewModel: BikeDeletionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                BikeDeletionEvent.Deleted -> onDeleted()
            }
        }
    }

    BikeDeletionContent(
        state = state,
        onDelete = viewModel::requestDeletion,
    )
    BikeDeletionDialog(
        state = state,
        onTogglePart = viewModel::togglePartSelection,
        onDismiss = viewModel::dismissConfirmation,
        onConfirm = viewModel::confirmDeletion,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BikeDeletionContent(
    state: BikeDeletionUiState,
    onDelete: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.bike_details)) },
                navigationIcon = { HomeNavigationIcon() },
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

            state.bike == null -> BikeDeletionStatus(
                message = stringResource(
                    state.error?.messageRes() ?: R.string.bike_not_found,
                ),
                modifier = Modifier.padding(innerPadding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = stringResource(R.string.overview_placeholder),
                    textAlign = TextAlign.Center,
                )
                state.error?.let { error ->
                    Text(
                        text = stringResource(error.messageRes()),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
                Button(
                    onClick = onDelete,
                    enabled = !state.isBusy,
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
private fun BikeDeletionStatus(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun BikeDeletionDialog(
    state: BikeDeletionUiState,
    onTogglePart: (Long) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!state.showConfirmation) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bike_delete_confirmation_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        R.string.bike_delete_confirmation_message,
                        state.bike?.deletionReference().orEmpty(),
                    ),
                )
                if (state.linkedParts.isEmpty()) {
                    Text(stringResource(R.string.bike_delete_no_linked_parts))
                } else {
                    Text(
                        text = stringResource(R.string.bike_delete_linked_parts),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(stringResource(R.string.bike_delete_part_selection_hint))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        state.linkedParts.forEach { part ->
                            BikePartSelection(
                                part = part,
                                selected = part.id in state.selectedPartIds,
                                onToggle = { onTogglePart(part.id) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !state.isDeleting,
            ) {
                Text(stringResource(R.string.bike_delete_confirmation_action))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isDeleting,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun BikePartSelection(
    part: Part,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val selectionDescription = stringResource(R.string.bike_delete_part_selection, part.name)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .semantics { contentDescription = selectionDescription }
            .padding(vertical = 4.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
            )
            Text(
                text = part.name,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun Bike.deletionReference(): String =
    nickname?.takeIf { it.isNotBlank() }
        ?.let { stringResource(R.string.bike_delete_reference, it, brand, model) }
        ?: stringResource(R.string.bike_delete_reference_without_nickname, brand, model)

private fun BikeDeletionError.messageRes(): Int = when (this) {
    BikeDeletionError.DELETE_FAILED -> R.string.bike_delete_failed
    BikeDeletionError.BIKE_NOT_FOUND -> R.string.bike_not_found
}
