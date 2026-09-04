package de.gerritstapper.bikegarage.feature.bike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BikePurchaseDateTest {
    @Test
    fun `converts ISO dates to UTC picker milliseconds and back`() {
        val millis = "2024-02-29".toPurchaseDatePickerMillisOrNull()

        requireNotNull(millis)
        assertEquals("2024-02-29", millis.toPurchaseDateIso())
    }

    @Test
    fun `invalid stored date has no picker value`() {
        assertNull("2024-02-30".toPurchaseDatePickerMillisOrNull())
    }
}
