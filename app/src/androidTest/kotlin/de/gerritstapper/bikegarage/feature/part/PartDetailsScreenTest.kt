package de.gerritstapper.bikegarage.feature.part

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartImage
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartShopLinkWithShop
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.domain.usecase.DeletePartUseCase
import de.gerritstapper.bikegarage.ui.navigation.PART_ID_ARGUMENT
import de.gerritstapper.bikegarage.ui.navigation.bikeDetailsRoute
import de.gerritstapper.bikegarage.ui.navigation.partEditRoute
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PartDetailsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun partDetailsShowsTechnicalFieldsProductLinkAndActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var selectedUrl: String? = null
        var selectedRoute: String? = null
        var deletionRequested = false
        val bike = Bike(id = 3, brand = "Canyon", model = "Grizl", createdAt = 1)
        val part = Part(
            id = 7,
            category = PartCategory.DRIVETRAIN,
            type = "Kette",
            name = "CN-M8100",
            manufacturer = "Shimano",
            manufacturerNumber = "ICNM8100126Q",
            description = "12-fach-Kette",
            createdAt = 1,
        )
        val productUrl = "https://example.test/chain"
        val shop = Shop(id = 2, name = "Radhaus", logoPath = "shops/radhaus.jpg", createdAt = 1)

        composeRule.setContent {
            BikeGarageTheme {
                PartDetailsContent(
                    state = PartDetailsUiState(
                        isLoading = false,
                        details = PartWithRelations(
                            part = part,
                            attributes = listOf(
                                PartAttribute(partId = part.id, key = "link_count", value = "116"),
                                PartAttribute(partId = part.id, key = "gear_count", value = "12"),
                            ),
                            bikes = listOf(bike),
                            shopLinks = listOf(
                                PartShopLinkWithShop(
                                    link = PartShopLink(
                                        partId = part.id,
                                        shopId = shop.id,
                                        productUrl = productUrl,
                                    ),
                                    shop = shop,
                                ),
                            ),
                        ),
                    ),
                    onEdit = { selectedRoute = partEditRoute(part.id) },
                    onBikeClick = { selectedRoute = bikeDetailsRoute(it) },
                    onDelete = { deletionRequested = true },
                    onProductUrlClick = { selectedUrl = it },
                )
            }
        }

        composeRule.onNodeWithText(part.name).assertIsDisplayed()
        composeRule.onAllNodesWithText(
            context.getString(R.string.part_inventory_count),
        ).assertCountEquals(0)
        composeRule.onNodeWithText(
            context.getString(R.string.part_details_no_images),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(part.manufacturer!!).assertIsDisplayed()
        composeRule.onNodeWithText(part.manufacturerNumber!!).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.part_technical_attributes),
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Anzahl Glieder").assertIsDisplayed()
        composeRule.onNodeWithText("116").assertIsDisplayed()
        composeRule.onNodeWithText("Anzahl Gänge").assertIsDisplayed()
        composeRule.onNodeWithText("12").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.part_product_link_entry, shop.name, productUrl),
        ).performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(productUrl, selectedUrl)
        }
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.part_edit),
        ).performClick()
        composeRule.runOnIdle {
            assertEquals(partEditRoute(part.id), selectedRoute)
        }
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.part_details_bike_entry, "Canyon Grizl"),
        ).performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(bikeDetailsRoute(bike.id), selectedRoute)
        }
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.part_delete),
        ).performClick()
        composeRule.runOnIdle {
            assertTrue(deletionRequested)
        }
    }

    @Test
    fun partDetailsShowsInventoryAndAvailableImagesOnly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imagePaths = listOf(
            "images/part-details-first.jpg",
            "images/part-details-second.jpg",
        )
        val imageFiles = imagePaths.map { path ->
            File(context.filesDir, path).apply {
                parentFile?.mkdirs()
                writeBytes(byteArrayOf())
            }
        }

        try {
            composeRule.setContent {
                BikeGarageTheme {
                    PartDetailsContent(
                        state = PartDetailsUiState(
                            isLoading = false,
                            details = PartWithRelations(
                                part = Part(
                                    id = 7,
                                    category = PartCategory.DRIVETRAIN,
                                    name = "CN-M8100",
                                    createdAt = 1,
                                    inventoryCount = 3,
                                ),
                                attributes = emptyList(),
                                bikes = emptyList(),
                                shopLinks = emptyList(),
                                images = listOf(
                                    PartImage(
                                        partId = 7,
                                        storagePath = imagePaths[1],
                                        displayOrder = 1,
                                        isThumbnail = false,
                                    ),
                                    PartImage(
                                        partId = 7,
                                        storagePath = "",
                                        displayOrder = 2,
                                        isThumbnail = false,
                                    ),
                                    PartImage(
                                        partId = 7,
                                        storagePath = "images/missing.jpg",
                                        displayOrder = 3,
                                        isThumbnail = false,
                                    ),
                                    PartImage(
                                        partId = 7,
                                        storagePath = imagePaths[0],
                                        displayOrder = 0,
                                        isThumbnail = true,
                                    ),
                                ),
                            ),
                        ),
                        onEdit = {},
                        onBikeClick = {},
                        onDelete = {},
                        onProductUrlClick = {},
                    )
                }
            }

            composeRule.onNodeWithText(context.getString(R.string.part_inventory_count))
                .assertIsDisplayed()
            composeRule.onNodeWithText("3").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithContentDescription(
                context.getString(R.string.part_details_image, 1, 2),
            ).assertIsDisplayed()
            composeRule.onNodeWithContentDescription(
                context.getString(R.string.part_details_image, 2, 2),
            ).assertIsDisplayed()
        } finally {
            imageFiles.forEach { it.delete() }
        }
    }

    @Test
    fun partDeletionConfirmsThenDeletesThePartAndKeepsBikes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bike = Bike(
            id = 3,
            brand = "Canyon",
            model = "Grizl",
            createdAt = 1,
        )
        val part = Part(
            id = 7,
            category = PartCategory.DRIVETRAIN,
            name = "CN-M8100",
            createdAt = 2,
        )
        val repository = DeletionDetailsPartRepository(
            PartWithRelations(
                part = part,
                attributes = listOf(PartAttribute(partId = part.id, key = "link_count", value = "116")),
                bikes = listOf(bike),
                shopLinks = emptyList(),
            ),
        )
        val detailsViewModel = PartDetailsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(PART_ID_ARGUMENT to part.id)),
            partRepository = repository,
        )
        val deletionViewModel = PartDeletionViewModel(
            savedStateHandle = SavedStateHandle(mapOf(PART_ID_ARGUMENT to part.id)),
            deletePart = DeletePartUseCase(repository),
        )
        var deletionCompleted = false

        composeRule.setContent {
            BikeGarageTheme {
                PartDetailsScreen(
                    onEdit = {},
                    onBikeClick = {},
                    onDeleted = { deletionCompleted = true },
                    viewModel = detailsViewModel,
                    deletionViewModel = deletionViewModel,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            detailsViewModel.uiState.value.details?.part == part
        }
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.part_delete),
        ).performClick()
        composeRule.onNodeWithText(
            context.getString(R.string.part_delete_confirmation_title),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.part_delete_confirmation_message, part.name),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.part_delete_confirmation_action),
        ).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { deletionCompleted }
        assertEquals(part, repository.deletedPart)
        assertEquals(listOf(bike), repository.bikesAtDeletion)
    }
}

private class DeletionDetailsPartRepository(
    initialDetails: PartWithRelations,
) : PartRepository {
    val details = MutableStateFlow<PartWithRelations?>(initialDetails)
    var deletedPart: Part? = null
    var bikesAtDeletion = emptyList<Bike>()

    override fun observeParts(): Flow<List<PartWithRelations>> = emptyFlow()

    override fun observePart(partId: Long): Flow<PartWithRelations?> = details

    override suspend fun savePart(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long = error("Nicht benötigt")

    override suspend fun deletePart(part: Part) {
        deletedPart = part
        bikesAtDeletion = details.value?.bikes.orEmpty()
        details.value = null
    }
}
