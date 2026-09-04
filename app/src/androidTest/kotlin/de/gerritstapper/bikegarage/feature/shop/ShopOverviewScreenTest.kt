package de.gerritstapper.bikegarage.feature.shop

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ShopOverviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shopOverviewShowsAllShopsAndOpensTheSelectedShop() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var selectedShopId: Long? = null
        val bikePurchaseLocation = Shop(
            id = 1,
            name = "Fahrradhaus",
            logoPath = "shops/fahrradhaus.png",
            createdAt = 1,
        )
        val productShop = Shop(
            id = 2,
            name = "Teilemarkt",
            logoPath = "shops/teilemarkt.png",
            createdAt = 2,
        )

        composeRule.setContent {
            BikeGarageTheme {
                ShopOverviewContent(
                    shops = listOf(bikePurchaseLocation, productShop),
                    onShopClick = { selectedShopId = it },
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.shops_overview)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.shop_logo, bikePurchaseLocation.name),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.shop_card, productShop.name),
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(productShop.id, selectedShopId)
        }
    }
}
