package de.gerritstapper.bikegarage.feature.part

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PartCreationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun categoryThenTypeShowsItsCentralTechnicalFieldsAndKeepsSeededBike() {
        val bike = Bike(id = 7, brand = "Canyon", model = "Grizl", createdAt = 1)
        var state by mutableStateOf(
            PartCreationUiState(selectedBikeIds = setOf(bike.id)),
        )

        composeRule.setContent {
            BikeGarageTheme {
                PartCreationContent(
                    state = state,
                    bikes = listOf(bike),
                    shops = emptyList(),
                    onCategorySelected = { category ->
                        state = state.copy(category = category, type = null, technicalAttributes = emptyMap())
                    },
                    onTypeSelected = { type ->
                        state = state.copy(
                            type = type,
                            technicalAttributes = when (type) {
                                "Kette" -> mapOf("link_count" to "", "gear_count" to "")
                                "Kassette" -> mapOf(
                                    "sprocket_count" to "",
                                    "gear_ratio" to "",
                                    "freehub" to "",
                                )
                                else -> emptyMap()
                            },
                        )
                    },
                    onNameChanged = {},
                    onManufacturerChanged = {},
                    onManufacturerNumberChanged = {},
                    onDescriptionChanged = {},
                    onTechnicalAttributeChanged = { _, _ -> },
                    onBikeToggled = {},
                    onAddProductLink = {},
                    onRemoveProductLink = {},
                    onProductLinkShopSelected = { _, _ -> },
                    onProductUrlChanged = { _, _ -> },
                    onCreateShopForProductLink = {},
                    onSave = {},
                    onNavigateUp = {},
                )
            }
        }

        composeRule.onNodeWithTag("part-bike-${bike.id}").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("part-category").performScrollTo().performClick()
        composeRule.onNodeWithText("Antrieb").performClick()
        composeRule.onNodeWithTag("part-type").performScrollTo().performClick()
        composeRule.onNodeWithText("Kette").performClick()
        composeRule.onNodeWithTag("part-technical-link_count").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("part-technical-gear_count").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("part-type").performScrollTo().performClick()
        composeRule.onNodeWithText("Kassette").performClick()
        composeRule.onAllNodesWithTag("part-technical-link_count").assertCountEquals(0)
        composeRule.onNodeWithTag("part-technical-sprocket_count")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(PartCategory.DRIVETRAIN, state.category)
            assertEquals(setOf(bike.id), state.selectedBikeIds)
        }
    }

    @Test
    fun productLinkCanSelectExistingShopAndSave() {
        val shop = Shop(id = 9, name = "Radhaus", logoPath = "images/radhaus.jpg", createdAt = 1)
        var addedLink by mutableStateOf(false)
        var selectedShopId: Long? = null
        var url by mutableStateOf("")
        var saved = false
        val link = PartProductLinkUiState(id = 1)

        composeRule.setContent {
            BikeGarageTheme {
                PartCreationContent(
                    state = PartCreationUiState(
                        category = PartCategory.DRIVETRAIN,
                        type = "Kette",
                        name = "CN-M8100",
                        technicalAttributes = mapOf("link_count" to "", "gear_count" to ""),
                        productLinks = if (addedLink) {
                            listOf(link.copy(shopId = selectedShopId, productUrl = url))
                        } else {
                            emptyList()
                        },
                    ),
                    bikes = emptyList(),
                    shops = listOf(shop),
                    onCategorySelected = {},
                    onTypeSelected = {},
                    onNameChanged = {},
                    onManufacturerChanged = {},
                    onManufacturerNumberChanged = {},
                    onDescriptionChanged = {},
                    onTechnicalAttributeChanged = { _, _ -> },
                    onBikeToggled = {},
                    onAddProductLink = { addedLink = true },
                    onRemoveProductLink = {},
                    onProductLinkShopSelected = { _, shopId -> selectedShopId = shopId },
                    onProductUrlChanged = { _, productUrl -> url = productUrl },
                    onCreateShopForProductLink = {},
                    onSave = { saved = true },
                    onNavigateUp = {},
                )
            }
        }

        composeRule.onNodeWithText("Produktlink hinzufügen").performScrollTo().performClick()
        composeRule.onNodeWithText("Shop auswählen").performScrollTo().performClick()
        composeRule.onNodeWithText(shop.name).performClick()
        composeRule.onNodeWithTag("part-product-url-1")
            .performScrollTo()
            .performTextInput("https://example.test/kette")
        composeRule.onNodeWithText("Speichern").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(shop.id, selectedShopId)
            assertEquals("https://example.test/kette", url)
            assertEquals(true, saved)
        }
    }
}
