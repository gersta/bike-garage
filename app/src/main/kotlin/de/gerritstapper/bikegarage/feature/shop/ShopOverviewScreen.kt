package de.gerritstapper.bikegarage.feature.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationIcon
import java.io.File

@Composable
fun ShopOverviewScreen(
    onShopClick: (Long) -> Unit,
    onCreateShop: () -> Unit,
    viewModel: ShopOverviewViewModel = hiltViewModel(),
) {
    val shops by viewModel.shops.collectAsStateWithLifecycle()
    ShopOverviewContent(
        shops = shops,
        onShopClick = onShopClick,
        onCreateShop = onCreateShop,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShopOverviewContent(
    shops: List<Shop>,
    onShopClick: (Long) -> Unit,
    onCreateShop: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.shops_overview)) },
                navigationIcon = { HomeNavigationIcon() },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateShop) {
                Text(stringResource(R.string.shop_create_short))
            }
        },
    ) { innerPadding ->
        if (shops.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.shops_empty_state),
                    textAlign = TextAlign.Center,
                )
            }
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
                items(shops, key = { it.id }) { shop ->
                    ShopCard(shop = shop, onClick = { onShopClick(shop.id) })
                }
            }
        }
    }
}

@Composable
private fun ShopCard(
    shop: Shop,
    onClick: () -> Unit,
) {
    val cardContentDescription = stringResource(R.string.shop_card, shop.name)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = cardContentDescription },
    ) {
        AsyncImage(
            model = File(LocalContext.current.filesDir, shop.logoPath),
            contentDescription = stringResource(R.string.shop_logo, shop.name),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp),
        )
        Text(
            text = shop.name,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
