package de.gerritstapper.bikegarage.feature.garage

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaragePlaceholderScreen(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int = R.string.overview_placeholder,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = { HomeNavigationIcon() },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(messageRes),
                textAlign = TextAlign.Center,
            )
        }
    }
}
