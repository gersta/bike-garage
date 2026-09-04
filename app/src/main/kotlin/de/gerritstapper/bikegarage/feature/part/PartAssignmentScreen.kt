package de.gerritstapper.bikegarage.feature.part

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationActions
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PartAssignmentScreen(
    onAssigned: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: PartAssignmentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                PartAssignmentEvent.Assigned -> onAssigned()
            }
        }
    }
    BackHandler(enabled = state.assigningPartId == null, onBack = onNavigateUp)
    PartAssignmentContent(
        state = state,
        onNameChanged = viewModel::onNameChanged,
        onManufacturerChanged = viewModel::onManufacturerChanged,
        onManufacturerNumberChanged = viewModel::onManufacturerNumberChanged,
        onTypeChanged = viewModel::onTypeChanged,
        onClearQuery = viewModel::clearQuery,
        onAssign = viewModel::assignPart,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PartAssignmentContent(
    state: PartAssignmentUiState,
    onNameChanged: (String) -> Unit,
    onManufacturerChanged: (String) -> Unit,
    onManufacturerNumberChanged: (String) -> Unit,
    onTypeChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onAssign: (Long) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val isAssigning = state.assigningPartId != null
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.part_assignment_title)) },
                navigationIcon = {
                    HomeNavigationActions {
                        TextButton(onClick = onNavigateUp, enabled = !isAssigning) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("part-assignment-list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.part_assignment_hint),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedTextField(
                        value = state.query.name,
                        onValueChange = onNameChanged,
                        modifier = Modifier.fillMaxWidth().testTag("part-assignment-name"),
                        label = { Text(stringResource(R.string.part_name)) },
                        singleLine = true,
                        enabled = !isAssigning,
                    )
                    OutlinedTextField(
                        value = state.query.manufacturer,
                        onValueChange = onManufacturerChanged,
                        modifier = Modifier.fillMaxWidth().testTag("part-assignment-manufacturer"),
                        label = { Text(stringResource(R.string.part_manufacturer)) },
                        singleLine = true,
                        enabled = !isAssigning,
                    )
                    OutlinedTextField(
                        value = state.query.manufacturerNumber,
                        onValueChange = onManufacturerNumberChanged,
                        modifier = Modifier.fillMaxWidth().testTag("part-assignment-number"),
                        label = { Text(stringResource(R.string.part_manufacturer_number)) },
                        singleLine = true,
                        enabled = !isAssigning,
                    )
                    OutlinedTextField(
                        value = state.query.type,
                        onValueChange = onTypeChanged,
                        modifier = Modifier.fillMaxWidth().testTag("part-assignment-type"),
                        label = { Text(stringResource(R.string.part_type)) },
                        singleLine = true,
                        enabled = !isAssigning,
                    )
                    if (!state.query.isEmpty) {
                        TextButton(
                            onClick = onClearQuery,
                            modifier = Modifier.testTag("part-assignment-clear"),
                            enabled = !isAssigning,
                        ) {
                            Text(stringResource(R.string.part_assignment_clear_search))
                        }
                    }
                    if (state.failedPartId != null) {
                        Text(
                            text = stringResource(R.string.part_assignment_failed),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (state.parts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.part_assignment_no_results),
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            } else {
                items(state.parts, key = { it.id }) { part ->
                    PartAssignmentListItem(
                        part = part,
                        isAssigning = isAssigning,
                        failed = part.id == state.failedPartId,
                        onAssign = { onAssign(part.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PartAssignmentListItem(
    part: PartAssignmentRow,
    isAssigning: Boolean,
    failed: Boolean,
    onAssign: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag("part-assignment-row-${part.id}"),
        headlineContent = {
            Text(part.name, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                PartAssignmentDetail(
                    R.string.part_category,
                    stringResource(part.category.titleRes()),
                )
                PartAssignmentDetail(R.string.part_type, part.type.orEmpty())
            }
        },
        trailingContent = {
            TextButton(
                onClick = onAssign,
                enabled = !isAssigning,
            ) {
                Text(
                    stringResource(
                        if (failed) R.string.retry else R.string.part_assignment_assign,
                    ),
                )
            }
        },
    )
}

@Composable
private fun PartAssignmentDetail(labelRes: Int, value: String) {
    Text(
        text = "${stringResource(labelRes)}: $value",
        style = MaterialTheme.typography.bodyMedium,
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
