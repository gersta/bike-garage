package de.gerritstapper.bikegarage.feature.garage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import de.gerritstapper.bikegarage.MainActivity
import de.gerritstapper.bikegarage.R
import org.junit.Rule
import org.junit.Test

class GarageOverviewNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun garageOverviewShowsAllTilesAndNavigatesToTheirDestinations() {
        val activity = composeRule.activity
        val garageOverview = activity.getString(R.string.garage_overview)
        val bikes = activity.getString(R.string.bikes)
        val parts = activity.getString(R.string.parts)
        val tools = activity.getString(R.string.tools)
        val clothes = activity.getString(R.string.clothes)
        val shops = activity.getString(R.string.shops)
        val navigateHome = activity.getString(R.string.navigate_home)

        composeRule.onNodeWithText(garageOverview).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(bikes).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(parts).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(tools).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(clothes).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(navigateHome).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(navigateHome).performClick()
        composeRule.onNodeWithText(garageOverview).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(bikes).performClick()
        composeRule.onNodeWithText(activity.getString(R.string.bikes_overview)).assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText(garageOverview).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(parts).performClick()
        composeRule.onNodeWithText(activity.getString(R.string.parts_overview)).assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText(garageOverview).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(tools).performClick()
        composeRule.onNodeWithText(activity.getString(R.string.tools_empty_state)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(navigateHome).performClick()
        composeRule.onNodeWithText(garageOverview).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(clothes).performClick()
        composeRule.onNodeWithText(activity.getString(R.string.clothes_empty_state)).assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText(garageOverview).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(shops).performClick()
        composeRule.onNodeWithText(activity.getString(R.string.shops_overview)).assertIsDisplayed()
    }
}
