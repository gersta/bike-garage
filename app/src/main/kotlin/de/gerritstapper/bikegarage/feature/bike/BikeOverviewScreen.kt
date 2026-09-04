package de.gerritstapper.bikegarage.feature.bike

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationIcon
import java.io.File

@Composable
fun BikeOverviewScreen(
    onBikeClick: (Long) -> Unit,
    onCreateBike: () -> Unit,
    viewModel: BikeOverviewViewModel = hiltViewModel(),
) {
    val bikes by viewModel.bikes.collectAsStateWithLifecycle()
    BikeOverviewContent(
        bikes = bikes,
        onBikeClick = onBikeClick,
        onCreateBike = onCreateBike,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BikeOverviewContent(
    bikes: List<BikeCardViewModel>,
    onBikeClick: (Long) -> Unit,
    onCreateBike: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.bikes_overview)) },
                navigationIcon = { HomeNavigationIcon() },
            )
        },
        floatingActionButton = {
            if (bikes.isNotEmpty()) {
                FloatingActionButton(onClick = onCreateBike) {
                    Text(stringResource(R.string.bike_create_short))
                }
            }
        },
    ) { innerPadding ->
        if (bikes.isEmpty()) {
            EmptyBikeOverview(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onCreateBike = onCreateBike,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(bikes, key = { it.id }) { bike ->
                    BikeCard(
                        bike = bike,
                        onClick = { onBikeClick(bike.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBikeOverview(
    modifier: Modifier,
    onCreateBike: () -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.bikes_empty_state),
            textAlign = TextAlign.Center,
        )
        Button(onClick = onCreateBike) {
            Text(stringResource(R.string.bike_create))
        }
    }
}

@Composable
private fun BikeCard(
    bike: BikeCardViewModel,
    onClick: () -> Unit,
) {
    val cardContentDescription = stringResource(R.string.bike_card, bike.brand, bike.model)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = cardContentDescription },
    ) {
        bike.thumbnailPath?.let { thumbnailPath ->
            AsyncImage(
                model = File(LocalContext.current.filesDir, thumbnailPath),
                contentDescription = stringResource(
                    R.string.bike_thumbnail,
                    bike.brand,
                    bike.model,
                ),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            bike.nickname?.let { nickname ->
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = bike.brand,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = bike.model,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
