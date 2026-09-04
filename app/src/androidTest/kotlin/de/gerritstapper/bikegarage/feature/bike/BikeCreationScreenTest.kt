package de.gerritstapper.bikegarage.feature.bike

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BikeCreationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun completeFormSavesBikeWithSelectedShopAndImage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val shop = Shop(id = 7, name = "Radhaus", logoPath = "images/shop.jpg", createdAt = 1)
        var brand by mutableStateOf("")
        var model by mutableStateOf("")
        var saved = false
        var selectedShopId: Long? = null

        composeRule.setContent {
            BikeGarageTheme {
                BikeCreationContent(
                    state = BikeCreationUiState(
                        brand = brand,
                        model = model,
                        imagePaths = listOf("images/bike.jpg"),
                        thumbnailPath = "images/bike.jpg",
                    ),
                    shops = listOf(shop),
                    onNicknameChanged = {},
                    onBrandChanged = { brand = it },
                    onModelChanged = { model = it },
                    onPurchaseDateChanged = {},
                    onShopSelected = { selectedShopId = it },
                    onCreateShop = {},
                    onSelectFromPhotoLibrary = {},
                    onCreatePendingCameraCapture = {
                        PendingImage("images/camera.jpg", Uri.EMPTY)
                    },
                    onCameraCaptureCompleted = { _, _ -> },
                    onThumbnailSelected = {},
                    onImageRemoved = {},
                    onSave = { saved = true },
                    onNavigateUp = {},
                )
            }
        }

        composeRule.onNodeWithTag("bike-brand").performTextInput("Canyon")
        composeRule.onNodeWithTag("bike-model").performTextInput("Grizl")
        composeRule.onNodeWithText(context.getString(R.string.bike_shop_none)).performClick()
        composeRule.onNodeWithText(shop.name).performClick()
        composeRule.onNodeWithText(context.getString(R.string.save)).performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals("Canyon", brand)
            assertEquals("Grizl", model)
            assertEquals(shop.id, selectedShopId)
            assertTrue(saved)
        }
        composeRule.onNodeWithText(context.getString(R.string.bike_image_thumbnail)).assertIsDisplayed()
    }

    @Test
    fun brandFieldSuggestsMatchingKnownBrandsAndKeepsFreeText() {
        var brand by mutableStateOf("")

        composeRule.setContent {
            BikeGarageTheme {
                BikeCreationContent(
                    state = BikeCreationUiState(
                        brand = brand,
                        imagePaths = listOf("images/bike.jpg"),
                        thumbnailPath = "images/bike.jpg",
                    ),
                    shops = emptyList(),
                    onNicknameChanged = {},
                    onBrandChanged = { brand = it },
                    onModelChanged = {},
                    onPurchaseDateChanged = {},
                    onShopSelected = {},
                    onCreateShop = {},
                    onSelectFromPhotoLibrary = {},
                    onCreatePendingCameraCapture = { null },
                    onCameraCaptureCompleted = { _, _ -> },
                    onThumbnailSelected = {},
                    onImageRemoved = {},
                    onSave = {},
                    onNavigateUp = {},
                    knownBrands = listOf("Canyon", "Trek"),
                )
            }
        }

        composeRule.onNodeWithTag("bike-brand").performTextInput("can")
        composeRule.onNodeWithText("Canyon").performClick()
        composeRule.runOnIdle {
            assertEquals("Canyon", brand)
        }
        composeRule.onNodeWithTag("bike-brand").performTextReplacement("")
        composeRule.onNodeWithText("Canyon").assertIsDisplayed()
        composeRule.onNodeWithText("Trek").assertIsDisplayed()
        composeRule.onNodeWithTag("bike-brand").performTextInput("can")
        composeRule.onNodeWithText("Canyon").assertIsDisplayed()
        composeRule.onNodeWithTag("bike-brand").performTextReplacement("Custom")
        composeRule.runOnIdle {
            assertEquals("Custom", brand)
        }
    }

    @Test
    fun emptyImageListShowsAddPlaceholderAndImageSourceChoice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            BikeGarageTheme {
                BikeCreationContent(
                    state = BikeCreationUiState(),
                    shops = emptyList(),
                    onNicknameChanged = {},
                    onBrandChanged = {},
                    onModelChanged = {},
                    onPurchaseDateChanged = {},
                    onShopSelected = {},
                    onCreateShop = {},
                    onSelectFromPhotoLibrary = {},
                    onCreatePendingCameraCapture = { null },
                    onCameraCaptureCompleted = { _, _ -> },
                    onThumbnailSelected = {},
                    onImageRemoved = {},
                    onSave = {},
                    onNavigateUp = {},
                )
            }
        }

        composeRule.onNodeWithTag("bike-image-placeholder").performScrollTo().performClick()

        composeRule.onNodeWithText(context.getString(R.string.bike_image_camera)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.bike_image_photo_library))
            .assertIsDisplayed()
    }

    @Test
    fun existingImagePreviewIsCenteredInSharedForm() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            BikeGarageTheme {
                BikeCreationContent(
                    state = BikeCreationUiState(
                        imagePaths = listOf("images/bike.jpg"),
                        thumbnailPath = "images/bike.jpg",
                    ),
                    shops = emptyList(),
                    onNicknameChanged = {},
                    onBrandChanged = {},
                    onModelChanged = {},
                    onPurchaseDateChanged = {},
                    onShopSelected = {},
                    onCreateShop = {},
                    onSelectFromPhotoLibrary = {},
                    onCreatePendingCameraCapture = { null },
                    onCameraCaptureCompleted = { _, _ -> },
                    onThumbnailSelected = {},
                    onImageRemoved = {},
                    onSave = {},
                    onNavigateUp = {},
                )
            }
        }

        val imagePreview = composeRule.onNodeWithContentDescription(
            context.getString(R.string.bike_image_preview, 1),
        )
        imagePreview.performScrollTo()

        val previewCenter = imagePreview.fetchSemanticsNode().boundsInRoot.center.x
        val formCenter = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.center.x

        assertEquals(formCenter, previewCenter, 0.5f)
    }
}
