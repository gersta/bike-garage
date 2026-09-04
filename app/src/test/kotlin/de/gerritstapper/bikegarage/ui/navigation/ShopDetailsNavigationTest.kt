package de.gerritstapper.bikegarage.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ShopDetailsNavigationTest {
    @Test
    fun `shop detail associations use stable bike and part detail routes`() {
        assertEquals("bike_details/8", bikeDetailsRoute(8))
        assertEquals("part_details/13", partDetailsRoute(13))
    }
}
