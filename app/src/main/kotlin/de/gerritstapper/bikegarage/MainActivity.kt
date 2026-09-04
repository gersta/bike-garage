package de.gerritstapper.bikegarage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import de.gerritstapper.bikegarage.ui.navigation.BikeGarageNavigation
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BikeGarageTheme {
                BikeGarageNavigation()
            }
        }
    }
}
