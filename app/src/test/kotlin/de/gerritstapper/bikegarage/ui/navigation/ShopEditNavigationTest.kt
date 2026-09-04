package de.gerritstapper.bikegarage.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ShopEditNavigationTest {
    @Test
    fun `shop edit routes back to the shop details destination`() {
        assertEquals("shop_edit/8", shopEditRoute(8))
        assertEquals("shop_details/8", shopDetailsRoute(8))
    }
}
