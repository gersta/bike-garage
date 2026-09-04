package de.gerritstapper.bikegarage.feature.bike

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.test.platform.app.InstrumentationRegistry
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BikeEditScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadedFormDisplaysBikeValuesAndLetsUserChangeThumbnailAndImages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bike = Bike(
            id = 7,
            nickname = "Schotterfuchs",
            brand = "Canyon",
            model = "Grizl",
            purchaseDate = "2026-08-21",
            shopId = 3,
            createdAt = 1,
        )
        var selectedThumbnail: String? = null
        var removedImage: String? = null

        composeRule.setContent {
            BikeGarageTheme {
                BikeEditContent(
                    state = BikeEditUiState(
                        bike = bike,
                        nickname = bike.nickname.orEmpty(),
                        brand = bike.brand,
                        model = bike.model,
                        purchaseDate = bike.purchaseDate.orEmpty(),
                        shopId = bike.shopId,
                        imagePaths = listOf("images/front.jpg", "images/side.jpg"),
                        thumbnailPath = "images/front.jpg",
                        isLoading = false,
                    ),
                    shops = listOf(Shop(3, "Radhaus", "images/shop.jpg", 1)),
                    onNicknameChanged = {},
                    onBrandChanged = {},
                    onModelChanged = {},
                    onPurchaseDateChanged = {},
                    onShopSelected = {},
                    onCreateShop = {},
                    onSelectFromPhotoLibrary = {},
                    onCreatePendingCameraCapture = {
                        PendingImage("images/camera.jpg", Uri.EMPTY)
                    },
                    onCameraCaptureCompleted = { _, _ -> },
                    onThumbnailSelected = { selectedThumbnail = it },
                    onImageRemoved = { removedImage = it },
                    onSave = {},
                    onNavigateUp = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.bike_edit)).assertIsDisplayed()
        composeRule.onNodeWithTag("bike-brand").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText,
                AnnotatedString("Canyon"),
            ),
        )
        composeRule.onNodeWithTag("bike-model").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText,
                AnnotatedString("Grizl"),
            ),
        )
        composeRule.onNodeWithText(context.getString(R.string.bike_image_make_thumbnail))
            .performScrollTo()
            .performClick()
        composeRule.onAllNodesWithText(context.getString(R.string.bike_image_remove))[1]
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals("images/side.jpg", selectedThumbnail)
            assertEquals("images/side.jpg", removedImage)
        }
        composeRule.onNodeWithTag("bike-image-add").performScrollTo().performClick()
        composeRule.onNodeWithText(context.getString(R.string.bike_image_camera)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.bike_image_photo_library)).assertIsDisplayed()
    }
}
