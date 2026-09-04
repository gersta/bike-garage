package de.gerritstapper.bikegarage.feature.shop

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import de.gerritstapper.bikegarage.domain.usecase.UpdateShopUseCase
import de.gerritstapper.bikegarage.ui.navigation.SHOP_ID_ARGUMENT
import de.gerritstapper.bikegarage.ui.navigation.LocalNavigateHome
import de.gerritstapper.bikegarage.ui.navigation.shopDetailsRoute
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ShopEditScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shopEditSavesTheLoadedShopAndReturnsItsDetailsDestination() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val shop = Shop(
            id = 1,
            name = "Radhaus",
            logoPath = "images/radhaus.jpg",
            createdAt = 1,
        )
        val repository = EditScreenShopRepository(shop)
        val viewModel = ShopEditViewModel(
            savedStateHandle = SavedStateHandle(mapOf(SHOP_ID_ARGUMENT to shop.id)),
            shopRepository = repository,
            updateShop = UpdateShopUseCase(repository),
            imageStorage = EditScreenImageStorage(),
        )
        var detailsDestination: String? = null

        composeRule.setContent {
            BikeGarageTheme {
                ShopEditScreen(
                    onShopUpdated = { updatedShop ->
                        detailsDestination = shopDetailsRoute(updatedShop.id)
                    },
                    onNavigateUp = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.shop_edit)).assertIsDisplayed()
        composeRule.onNodeWithText(shop.name).performTextReplacement("Neues Radhaus")
        composeRule.onNodeWithText(context.getString(R.string.save)).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            detailsDestination != null
        }
        assertEquals(
            shop.copy(name = "Neues Radhaus"),
            repository.savedShop,
        )
        assertEquals(shopDetailsRoute(shop.id), detailsDestination)
    }

    @Test
    fun oldLogoRemovalFailureKeepsLogoPickerDisabled() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val shop = Shop(
            id = 1,
            name = "Radhaus",
            logoPath = "images/radhaus.jpg",
            createdAt = 1,
        )

        composeRule.setContent {
            BikeGarageTheme {
                ShopEditContent(
                    state = ShopEditUiState(
                        shop = shop,
                        name = shop.name,
                        logoPath = shop.logoPath,
                        originalName = shop.name,
                        originalLogoPath = shop.logoPath,
                        oldLogoRemovalFailed = true,
                        isLoading = false,
                    ),
                    onNameChanged = {},
                    onSelectFromPhotoLibrary = {},
                    onCreatePendingCameraCapture = {
                        PendingImage("images/camera.jpg", Uri.EMPTY)
                    },
                    onCameraCaptureCompleted = { _, _ -> },
                    onSave = {},
                    onNavigateUp = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.shop_logo_preview),
        ).assertIsNotEnabled()
    }

    @Test
    fun homeNavigationConfirmsBeforeDiscardingEditedShop() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val shop = Shop(
            id = 1,
            name = "Radhaus",
            logoPath = "images/radhaus.jpg",
            createdAt = 1,
        )
        val repository = EditScreenShopRepository(shop)
        val viewModel = ShopEditViewModel(
            savedStateHandle = SavedStateHandle(mapOf(SHOP_ID_ARGUMENT to shop.id)),
            shopRepository = repository,
            updateShop = UpdateShopUseCase(repository),
            imageStorage = EditScreenImageStorage(),
        )
        var navigatedHome = false

        composeRule.setContent {
            BikeGarageTheme {
                CompositionLocalProvider(LocalNavigateHome provides { navigatedHome = true }) {
                    ShopEditScreen(
                        onShopUpdated = {},
                        onNavigateUp = {},
                        viewModel = viewModel,
                    )
                }
            }
        }

        composeRule.onNodeWithText(shop.name).performTextReplacement("Neues Radhaus")
        composeRule.onNodeWithContentDescription(context.getString(R.string.navigate_home)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.shop_discard_changes_title))
            .assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(false, navigatedHome)
        }
        composeRule.onNodeWithText(context.getString(R.string.discard)).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { navigatedHome }
    }
}

private class EditScreenShopRepository(
    private val shop: Shop,
) : ShopRepository {
    var savedShop: Shop? = null

    override fun observeShops(): Flow<List<Shop>> = emptyFlow()

    override fun observeShop(shopId: Long): Flow<ShopWithRelations?> =
        flowOf(ShopWithRelations(shop, emptyList(), emptyList(), emptyList()))

    override suspend fun saveShop(shop: Shop): Long {
        savedShop = shop
        return shop.id
    }

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean = false
}

private class EditScreenImageStorage : ImageStorage {
    override fun newCameraCapture(): PendingImage = error("Nicht benötigt")

    override suspend fun import(source: Uri): String = error("Nicht benötigt")

    override suspend fun delete(storagePath: String) = Unit

    override suspend fun discard(pendingImage: PendingImage) = Unit
}
