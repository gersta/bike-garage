package de.gerritstapper.bikegarage.feature.bike

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BikeOverviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyOverviewShowsHintAndOpensBikeCreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var createRequested = false

        composeRule.setContent {
            BikeGarageTheme {
                BikeOverviewContent(
                    bikes = emptyList(),
                    onBikeClick = {},
                    onCreateBike = { createRequested = true },
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.bikes_empty_state)).assertIsDisplayed()
        composeRule.onAllNodesWithText(context.getString(R.string.bike_create_short))
            .assertCountEquals(0)
        composeRule.onNodeWithText(context.getString(R.string.bike_create)).performClick()

        composeRule.runOnIdle {
            assertTrue(createRequested)
        }
    }

    @Test
    fun overviewShowsCardsAndOpensSelectedBikeDetails() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var selectedBikeId: Long? = null
        var createRequested = false
        val bikeWithNickname = BikeCardViewModel(
            id = 1,
            nickname = "Schotterfuchs",
            brand = "Canyon",
            model = "Grizl",
            thumbnailPath = "images/grizl.jpg",
        )
        val bikeWithoutNickname = BikeCardViewModel(
            id = 2,
            nickname = null,
            brand = "Trek",
            model = "Domane",
            thumbnailPath = "images/domane.jpg",
        )

        composeRule.setContent {
            BikeGarageTheme {
                BikeOverviewContent(
                    bikes = listOf(bikeWithNickname, bikeWithoutNickname),
                    onBikeClick = { selectedBikeId = it },
                    onCreateBike = { createRequested = true },
                )
            }
        }

        composeRule.onNodeWithText(bikeWithNickname.nickname!!).assertIsDisplayed()
        composeRule.onNodeWithText(bikeWithNickname.brand).assertIsDisplayed()
        composeRule.onNodeWithText(bikeWithoutNickname.model).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.bike_thumbnail, bikeWithNickname.brand, bikeWithNickname.model),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.bike_card, bikeWithoutNickname.brand, bikeWithoutNickname.model),
        ).performClick()
        composeRule.onAllNodesWithText(context.getString(R.string.bike_create))
            .assertCountEquals(0)
        composeRule.onNodeWithText(context.getString(R.string.bike_create_short)).performClick()

        composeRule.runOnIdle {
            assertEquals(bikeWithoutNickname.id, selectedBikeId)
            assertTrue(createRequested)
        }
    }
}
