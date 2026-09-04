package de.gerritstapper.bikegarage.feature.part

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductUrlIntentTest {
    @Test
    fun `creates a browsable view intent for product urls`() {
        val productUrl = "https://example.test/products/chain"

        val intent = productUrlIntent(productUrl)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Uri.parse(productUrl), intent.data)
        assertTrue(Intent.CATEGORY_BROWSABLE in intent.categories.orEmpty())
    }

    @Test
    fun `accepts only web product urls`() {
        assertTrue(isProductUrl("https://example.test/products/chain"))
        assertTrue(isProductUrl("http://example.test/products/chain"))
        assertTrue(!isProductUrl("mailto:shop@example.test"))
        assertTrue(!isProductUrl("example.test/products/chain"))
    }
}
