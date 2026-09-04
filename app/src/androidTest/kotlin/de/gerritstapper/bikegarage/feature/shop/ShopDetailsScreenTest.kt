package de.gerritstapper.bikegarage.feature.shop

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartShopLinkWithShop
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import de.gerritstapper.bikegarage.domain.usecase.DeleteShopUseCase
import de.gerritstapper.bikegarage.ui.navigation.SHOP_ID_ARGUMENT
import de.gerritstapper.bikegarage.ui.navigation.bikeDetailsRoute
import de.gerritstapper.bikegarage.ui.navigation.partDetailsRoute
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ShopDetailsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shopDeletionListsExistingPurchaseAndProductLinksInsteadOfDeleting() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = DetailsScreenShopRepository(
            ShopWithRelations(
                shop = shop,
                purchasedBikes = listOf(
                    Bike(
                        id = 2,
                        nickname = "Gravel",
                        brand = "Canyon",
                        model = "Grizl",
                        shopId = shop.id,
                        createdAt = 2,
                    ),
                ),
                parts = listOf(
                    Part(
                        id = 3,
                        category = PartCategory.DRIVETRAIN,
                        name = "Kette",
                        createdAt = 3,
                    ),
                ),
                partLinks = listOf(
                    PartShopLink(
                        id = 4,
                        partId = 3,
                        shopId = shop.id,
                        productUrl = "https://example.test/kette",
                    ),
                ),
            ),
            mayDelete = false,
        )
        val viewModel = deletionViewModel(repository)
        val detailsViewModel = detailsViewModel(repository)

        composeRule.setContent {
            BikeGarageTheme {
                ShopDetailsScreen(
                    onEdit = {},
                    onDeleted = {},
                    onBikeClick = {},
                    onPartClick = {},
                    viewModel = detailsViewModel,
                    deletionViewModel = viewModel,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            detailsViewModel.uiState.value.shop == shop
        }
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.delete),
        ).performClick()

        composeRule.onNodeWithText(
            context.getString(R.string.shop_delete_blocked_title),
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("Gravel (Canyon Grizl)").assertCountEquals(2)
        composeRule.onNodeWithText("Kette: https://example.test/kette").assertIsDisplayed()
        assertFalse(repository.deleteWasRequested)
    }

    @Test
    fun shopDeletionConfirmsThenDeletesTheShopAndItsLogo() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = DetailsScreenShopRepository(
            ShopWithRelations(
                shop = shop,
                purchasedBikes = emptyList(),
                parts = emptyList(),
                partLinks = emptyList(),
            ),
            mayDelete = true,
        )
        val imageStorage = DetailsScreenImageStorage()
        val viewModel = deletionViewModel(repository, imageStorage)
        val detailsViewModel = detailsViewModel(repository)
        var deletionCompleted = false

        composeRule.setContent {
            BikeGarageTheme {
                ShopDetailsScreen(
                    onEdit = {},
                    onDeleted = { deletionCompleted = true },
                    onBikeClick = {},
                    onPartClick = {},
                    viewModel = detailsViewModel,
                    deletionViewModel = viewModel,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            detailsViewModel.uiState.value.shop == shop
        }
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.delete),
        ).performClick()
        composeRule.onNodeWithText(
            context.getString(R.string.shop_delete_confirmation_title),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.shop_delete_confirmation_action),
        ).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { deletionCompleted }
        assertTrue(repository.deleteWasRequested)
        assertEquals(shop, repository.deletedShop)
        assertEquals(listOf(shop.logoPath), imageStorage.deletedPaths)
    }

    @Test
    fun shopDetailsListsAssociationsAndNavigatesToStableDetailRoutes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val purchasedBike = Bike(
            id = 2,
            nickname = "Gravel",
            brand = "Canyon",
            model = "Grizl",
            shopId = shop.id,
            createdAt = 2,
        )
        val assignedBike = Bike(
            id = 3,
            nickname = "Renner",
            brand = "Trek",
            model = "Madone",
            createdAt = 3,
        )
        val linkedPart = PartWithRelations(
            part = Part(
                id = 4,
                category = PartCategory.DRIVETRAIN,
                name = "Kette",
                createdAt = 4,
            ),
            attributes = emptyList(),
            bikes = listOf(purchasedBike, assignedBike),
            shopLinks = listOf(
                PartShopLinkWithShop(
                    link = PartShopLink(
                        id = 5,
                        partId = 4,
                        shopId = shop.id,
                        productUrl = "https://example.test/kette",
                    ),
                    shop = shop,
                ),
            ),
        )
        var selectedRoute: String? = null

        composeRule.setContent {
            BikeGarageTheme {
                ShopDetailsContent(
                    state = ShopDetailsUiState(
                        isLoading = false,
                        shop = shop,
                        purchasedBikes = listOf(purchasedBike),
                        parts = listOf(linkedPart),
                    ),
                    onEdit = { selectedRoute = "edit" },
                    onBikeClick = { selectedRoute = bikeDetailsRoute(it) },
                    onPartClick = { selectedRoute = partDetailsRoute(it) },
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText(shop.name).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.shop_edit),
        ).performClick()
        composeRule.runOnIdle {
            assertEquals("edit", selectedRoute)
        }
        composeRule.onNodeWithText(
            context.getString(R.string.shop_details_purchased_bikes),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(
                R.string.shop_details_part_bikes,
                "Gravel (Canyon Grizl), Renner (Trek Madone)",
            ),
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.shop_details_bike_entry, "Gravel (Canyon Grizl)"),
        ).performClick()
        composeRule.runOnIdle {
            assertEquals(bikeDetailsRoute(purchasedBike.id), selectedRoute)
        }
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.shop_details_part_entry, linkedPart.part.name),
        ).performClick()
        composeRule.runOnIdle {
            assertEquals(partDetailsRoute(linkedPart.part.id), selectedRoute)
        }
    }

    @Test
    fun shopDeleteIconIsDisabledWhileDeletionIsInProgress() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            BikeGarageTheme {
                ShopDetailsContent(
                    state = ShopDetailsUiState(isLoading = false, shop = shop),
                    onEdit = {},
                    onBikeClick = {},
                    onPartClick = {},
                    onDelete = {},
                    deleteEnabled = false,
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.delete),
        ).assertIsNotEnabled()
    }

    private fun deletionViewModel(
        repository: DetailsScreenShopRepository,
        imageStorage: DetailsScreenImageStorage = DetailsScreenImageStorage(),
    ) = ShopDeletionViewModel(
        savedStateHandle = SavedStateHandle(mapOf(SHOP_ID_ARGUMENT to shop.id)),
        shopRepository = repository,
        deleteShop = DeleteShopUseCase(repository),
        imageStorage = imageStorage,
    )

    private fun detailsViewModel(repository: DetailsScreenShopRepository) = ShopDetailsViewModel(
        savedStateHandle = SavedStateHandle(mapOf(SHOP_ID_ARGUMENT to shop.id)),
        shopRepository = repository,
        partRepository = DetailsScreenPartRepository(),
    )

    private companion object {
        val shop = Shop(
            id = 1,
            name = "Radhaus",
            logoPath = "images/radhaus.jpg",
            createdAt = 1,
        )
    }
}

private class DetailsScreenShopRepository(
    relations: ShopWithRelations,
    private val mayDelete: Boolean,
) : ShopRepository {
    private val relations = MutableStateFlow<ShopWithRelations?>(relations)
    var deleteWasRequested = false
    var deletedShop: Shop? = null

    override fun observeShops(): Flow<List<Shop>> = emptyFlow()

    override fun observeShop(shopId: Long): Flow<ShopWithRelations?> = relations

    override suspend fun saveShop(shop: Shop): Long = shop.id

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean {
        deleteWasRequested = true
        deletedShop = shop
        if (mayDelete) relations.value = null
        return mayDelete
    }
}

private class DetailsScreenImageStorage : ImageStorage {
    val deletedPaths = mutableListOf<String>()

    override fun newCameraCapture(): PendingImage = error("Nicht benötigt")

    override suspend fun import(source: Uri): String = error("Nicht benötigt")

    override suspend fun delete(storagePath: String) {
        deletedPaths += storagePath
    }

    override suspend fun discard(pendingImage: PendingImage) = Unit
}

private class DetailsScreenPartRepository : PartRepository {
    override fun observeParts(): Flow<List<PartWithRelations>> = flowOf(emptyList())

    override fun observePart(partId: Long): Flow<PartWithRelations?> = emptyFlow()

    override suspend fun savePart(
        part: Part,
        attributes: List<de.gerritstapper.bikegarage.domain.model.PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long = error("Nicht benötigt")

    override suspend fun deletePart(part: Part) = error("Nicht benötigt")
}
