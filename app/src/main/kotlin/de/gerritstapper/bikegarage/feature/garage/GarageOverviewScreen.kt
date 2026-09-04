package de.gerritstapper.bikegarage.feature.garage

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageOverviewScreen(
    onBikesClick: () -> Unit,
    onPartsClick: () -> Unit,
    onToolsClick: () -> Unit,
    onClothesClick: () -> Unit,
    onShopsClick: () -> Unit,
) {
    val destinations = listOf(
        GarageTile(R.string.bikes, R.drawable.ic_bike, onBikesClick),
        GarageTile(R.string.parts, R.drawable.ic_parts, onPartsClick),
        GarageTile(R.string.tools, R.drawable.ic_tools, onToolsClick),
        GarageTile(R.string.clothes, R.drawable.ic_clothes, onClothesClick),
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.garage_overview)) },
                navigationIcon = { HomeNavigationIcon() },
                actions = {
                    IconButton(onClick = onShopsClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_shop),
                            contentDescription = stringResource(R.string.shops),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(destinations, key = { it.labelRes }) { destination ->
                GarageTile(destination)
            }
        }
    }
}

@Composable
private fun GarageTile(destination: GarageTile) {
    val label = stringResource(destination.labelRes)
    Card(
        onClick = destination.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = label },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(destination.iconRes),
                contentDescription = null,
                modifier = Modifier.padding(bottom = 12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class GarageTile(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val onClick: () -> Unit,
)
