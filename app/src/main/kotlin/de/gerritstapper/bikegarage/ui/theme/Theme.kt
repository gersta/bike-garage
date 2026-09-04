package de.gerritstapper.bikegarage.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BikeGarageColorScheme = lightColorScheme()

@Composable
fun BikeGarageTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BikeGarageColorScheme,
        content = content,
    )
}
