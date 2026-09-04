package de.gerritstapper.bikegarage.feature.part

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PartEditScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bikeMultiSelectionCanBeChangedInThePartEditForm() {
        val firstBike = Bike(id = 3, brand = "Canyon", model = "Grizl", createdAt = 1)
        val secondBike = Bike(id = 9, brand = "Trek", model = "Domane", createdAt = 2)
        val part = Part(
            id = 7,
            category = PartCategory.DRIVETRAIN,
            type = "Kette",
            name = "CN-M8100",
            createdAt = 3,
        )
        var state by mutableStateOf(
            PartEditUiState(
                part = part,
                category = part.category,
                type = part.type,
                name = part.name,
                selectedBikeIds = setOf(firstBike.id),
                isLoading = false,
            ),
        )

        composeRule.setContent {
            BikeGarageTheme {
                PartEditContent(
                    state = state,
                    bikes = listOf(firstBike, secondBike),
                    shops = emptyList(),
                    onCategorySelected = {},
                    onTypeSelected = {},
                    onNameChanged = {},
                    onManufacturerChanged = {},
                    onManufacturerNumberChanged = {},
                    onDescriptionChanged = {},
                    onTechnicalAttributeChanged = { _, _ -> },
                    onBikeToggled = { bikeId ->
                        state = state.copy(
                            selectedBikeIds = state.selectedBikeIds.toMutableSet().apply {
                                if (!add(bikeId)) remove(bikeId)
                            },
                        )
                    },
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

        composeRule.onNodeWithTag("part-bike-${secondBike.id}")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(setOf(firstBike.id, secondBike.id), state.selectedBikeIds)
        }
    }

    @Test
    fun technicalFieldsRenderFromSchemaWhenThePartHasNoSavedTechnicalValues() {
        val part = Part(
            id = 7,
            category = PartCategory.DRIVETRAIN,
            type = "Kette",
            name = "CN-M8100",
            createdAt = 3,
        )
        var state by mutableStateOf(
            PartEditUiState(
                part = part,
                category = part.category,
                type = part.type,
                name = part.name,
                isLoading = false,
            ),
        )

        composeRule.setContent {
            BikeGarageTheme {
                PartEditContent(
                    state = state,
                    bikes = emptyList(),
                    shops = emptyList(),
                    onCategorySelected = {},
                    onTypeSelected = {},
                    onNameChanged = {},
                    onManufacturerChanged = {},
                    onManufacturerNumberChanged = {},
                    onDescriptionChanged = {},
                    onTechnicalAttributeChanged = { key, value ->
                        state = state.copy(technicalAttributes = state.technicalAttributes + (key to value))
                    },
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

        composeRule.onNodeWithTag("part-technical-link_count")
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput("116")
        composeRule.onNodeWithTag("part-technical-gear_count")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals("116", state.technicalAttributes["link_count"])
        }
    }

    @Test
    fun savingWithObsoleteTechnicalValuesRequiresConfirmation() {
        val part = Part(
            id = 7,
            category = PartCategory.COCKPIT,
            type = "Lenker",
            name = "Aeroad Cockpit",
            createdAt = 3,
        )
        var saves = 0
        val state = PartEditUiState(
            part = part,
            category = part.category,
            type = part.type,
            name = part.name,
            technicalAttributes = mapOf("link_count" to "116"),
            isLoading = false,
        )

        composeRule.setContent {
            BikeGarageTheme {
                PartEditContent(
                    state = state,
                    bikes = emptyList(),
                    shops = emptyList(),
                    onCategorySelected = {},
                    onTypeSelected = {},
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
                    onSave = { saves++ },
                    onNavigateUp = {},
                )
            }
        }

        composeRule.onNodeWithText("Speichern").performScrollTo().performClick()
        composeRule.onNodeWithText("Technische Werte entfernen?").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(0, saves)
        }
        composeRule.onNodeWithText("Trotzdem speichern").performClick()
        composeRule.runOnIdle {
            assertEquals(1, saves)
        }
    }
}
