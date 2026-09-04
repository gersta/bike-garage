package de.gerritstapper.bikegarage.feature.bike

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.usecase.BikeDetails
import de.gerritstapper.bikegarage.ui.navigation.bikeEditRoute
import de.gerritstapper.bikegarage.ui.navigation.partCreationRoute
import de.gerritstapper.bikegarage.ui.navigation.partDetailsRoute
import de.gerritstapper.bikegarage.ui.navigation.shopDetailsRoute
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BikeDetailsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bikeDetailsShowsFieldsAndNavigatesToItsStableDestinations() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var selectedRoute: String? = null
        var deletionRequested = false
        val details = BikeDetails(
            bikeWithImages = BikeWithImages(
                bike = bike,
                images = listOf(
                    BikeImage(
                        bikeId = bike.id,
                        storagePath = "images/front.jpg",
                        displayOrder = 0,
                        isThumbnail = true,
                    ),
                    BikeImage(
                        bikeId = bike.id,
                        storagePath = "images/side.jpg",
                        displayOrder = 1,
                        isThumbnail = false,
                    ),
                ),
            ),
            shop = shop,
            parts = listOf(chain, brake),
        )

        composeRule.setContent {
            BikeGarageTheme {
                BikeDetailsContent(
                    state = BikeDetailsUiState(isLoading = false, details = details),
                    onEdit = { selectedRoute = bikeEditRoute(bike.id) },
                    onShopClick = { selectedRoute = shopDetailsRoute(it) },
                    onCreateShop = {},
                    onPartClick = { selectedRoute = partDetailsRoute(it) },
                    onCreatePart = { selectedRoute = partCreationRoute(bike.id) },
                    onDelete = { deletionRequested = true },
                )
            }
        }

        composeRule.onNodeWithText(bike.nickname!!).assertIsDisplayed()
        composeRule.onNodeWithText(bike.brand).assertIsDisplayed()
        composeRule.onNodeWithText(bike.model).assertIsDisplayed()
        composeRule.onNodeWithText(bike.purchaseDate!!).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.bike_details_image, 1, 2),
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.bike_edit),
        ).performClick()
        composeRule.runOnIdle {
            assertEquals(bikeEditRoute(bike.id), selectedRoute)
        }
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.bike_delete),
        ).performClick()
        composeRule.runOnIdle {
            assertEquals(true, deletionRequested)
        }
        composeRule.onNodeWithText(
            context.getString(R.string.bike_details_shop_entry, shop.name),
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.bike_details_shop_entry, shop.name),
        ).performClick()
        composeRule.runOnIdle {
            assertEquals(shopDetailsRoute(shop.id), selectedRoute)
        }
        composeRule.onNodeWithText(
            context.getString(R.string.part_category_drivetrain),
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.part_category_brakes),
        ).performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Kettenblatt").assertCountEquals(1)
        composeRule.onNodeWithText("Übersetzung: 42 Zähne").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.parts_overview_unknown_type),
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.bike_details_part_entry, chain.part.name),
        ).performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(partDetailsRoute(chain.part.id), selectedRoute)
        }
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.bike_details_add_part),
        ).performClick()
        composeRule.runOnIdle {
            assertEquals(partCreationRoute(bike.id), selectedRoute)
        }
    }

    @Test
    fun emptyEquipmentKeepsLargeAddPartButton() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var createRequested = false
        val emptyDetails = BikeDetails(
            bikeWithImages = BikeWithImages(bike = bike, images = emptyList()),
            shop = null,
            parts = emptyList(),
        )

        composeRule.setContent {
            BikeGarageTheme {
                BikeDetailsContent(
                    state = BikeDetailsUiState(isLoading = false, details = emptyDetails),
                    onEdit = {},
                    onShopClick = {},
                    onCreateShop = {},
                    onPartClick = {},
                    onCreatePart = { createRequested = true },
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.bike_details_add_part),
        ).performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(true, createRequested)
        }
    }

    @Test
    fun addPartDialogOffersNewPartOrExistingPartSearch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var selectedAction: String? = null

        composeRule.setContent {
            BikeGarageTheme {
                BikePartAddDialog(
                    onDismiss = {},
                    onCreatePart = { selectedAction = "new" },
                    onAssignExistingPart = { selectedAction = "existing" },
                )
            }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.bike_part_add_new),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.bike_part_add_existing),
        ).performClick()
        composeRule.runOnIdle {
            assertEquals("existing", selectedAction)
        }
    }

    private companion object {
        val bike = Bike(
            id = 4,
            nickname = "Schotterfuchs",
            brand = "Canyon",
            model = "Grizl",
            purchaseDate = "2026-08-21",
            shopId = 8,
            createdAt = 1,
        )
        val shop = Shop(id = 8, name = "Radhaus", logoPath = "shops/radhaus.jpg", createdAt = 1)
        val chain = PartWithRelations(
            part = Part(
                id = 9,
                category = PartCategory.DRIVETRAIN,
                type = "Kettenblatt",
                name = "Kette",
                createdAt = 1,
            ),
            attributes = listOf(PartAttribute(key = "gear_ratio", value = "42 Zähne")),
            bikes = emptyList(),
            shopLinks = emptyList(),
        )
        val brake = PartWithRelations(
            part = Part(
                id = 10,
                category = PartCategory.BRAKES,
                type = "Kette",
                name = "Bremse",
                createdAt = 1,
            ),
            attributes = emptyList(),
            bikes = emptyList(),
            shopLinks = emptyList(),
        )
    }
}
