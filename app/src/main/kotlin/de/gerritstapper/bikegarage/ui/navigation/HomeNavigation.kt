package de.gerritstapper.bikegarage.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.gerritstapper.bikegarage.R

internal val LocalNavigateHome = compositionLocalOf<() -> Unit> { {} }

@Composable
internal fun HomeNavigationIcon(onClick: (() -> Unit)? = null) {
    IconButton(onClick = onClick ?: LocalNavigateHome.current) {
        Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = stringResource(R.string.navigate_home),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
internal fun HomeNavigationActions(
    onHomeClick: (() -> Unit)? = null,
    navigationAction: @Composable RowScope.() -> Unit,
) {
    Row {
        HomeNavigationIcon(onClick = onHomeClick)
        navigationAction()
    }
}
