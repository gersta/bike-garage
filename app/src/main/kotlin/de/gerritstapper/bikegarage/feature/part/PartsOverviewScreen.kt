package de.gerritstapper.bikegarage.feature.part

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationIcon

@Composable
fun PartsOverviewScreen(
    onCreatePart: () -> Unit,
    onPartClick: (Long) -> Unit,
    viewModel: PartsOverviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PartsOverviewContent(
        state = state,
        onPartClick = onPartClick,
        onCreatePart = onCreatePart,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PartsOverviewContent(
    state: PartsOverviewUiState,
    onPartClick: (Long) -> Unit,
    onCreatePart: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.parts_overview)) },
                navigationIcon = { HomeNavigationIcon() },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreatePart,
            ) { Text(stringResource(R.string.part_create)) }
        },
    ) { innerPadding ->
        if (state.isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.parts_overview_empty_state),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.categories.forEach { group ->
                    item(key = "category-${group.category.name}") {
                        Text(
                            text = stringResource(group.category.titleRes()),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    group.types.forEach { typeGroup ->
                        item(key = "type-${group.category.name}-${typeGroup.type.orEmpty()}") {
                            Text(
                                text = typeGroup.type
                                    ?: stringResource(R.string.parts_overview_unknown_type),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        items(typeGroup.parts, key = { it.id }) { part ->
                            PartOverviewCard(
                                part = part,
                                onClick = { onPartClick(part.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartOverviewCard(
    part: PartOverviewRow,
    onClick: () -> Unit,
) {
    val bikeAssignment = if (part.bikeNames.isEmpty()) {
        stringResource(R.string.parts_overview_part_without_bikes)
    } else {
        stringResource(
            R.string.parts_overview_part_bikes,
            part.bikeNames.joinToString(),
        )
    }
    val description = stringResource(R.string.parts_overview_part_entry, part.name)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parts-overview-row-${part.id}")
            .semantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(part.name, style = MaterialTheme.typography.titleMedium)
            Text(bikeAssignment, style = MaterialTheme.typography.bodyMedium)
        }
    }
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
