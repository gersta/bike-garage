package de.gerritstapper.bikegarage.feature.shop

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import org.junit.Rule
import org.junit.Test

class ShopLogoPickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyLogoShowsPlaceholderThatOpensSourceChoice() {
        setContent(logoPath = null)

        openSourceChoice()
    }

    @Test
    fun existingLogoPreviewOpensSourceChoice() {
        setContent(logoPath = "images/shop-logo.jpg")

        openSourceChoice()
    }

    private fun setContent(logoPath: String?) {
        composeRule.setContent {
            BikeGarageTheme {
                ShopLogoPicker(
                    logoPath = logoPath,
                    enabled = true,
                    onSelectFromPhotoLibrary = {},
                    onCreatePendingCameraCapture = {
                        PendingImage("images/camera.jpg", Uri.EMPTY)
                    },
                    onCameraCaptureCompleted = { _, _ -> },
                )
            }
        }
    }

    private fun openSourceChoice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.shop_logo_preview),
        ).assertIsDisplayed().performClick()

        composeRule.onNodeWithText(
            context.getString(R.string.shop_logo_camera),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.shop_logo_photo_library),
        ).assertIsDisplayed()
    }
}
