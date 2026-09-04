package de.gerritstapper.bikegarage.feature.bike

import de.gerritstapper.bikegarage.domain.model.PartAttribute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BikePartSecondaryDetailsTest {
    @Test
    fun `shows canonical type and first populated technical field in definition order`() {
        val details = bikePartSecondaryDetails(
            type = "Kassette",
            attributes = listOf(
                PartAttribute(key = "freehub", value = "Microspline"),
                PartAttribute(key = "sprocket_count", value = "  "),
                PartAttribute(key = "gear_ratio", value = "10-51"),
            ),
        )

        assertEquals(
            BikePartSecondaryDetails(
                type = "Kassette",
                technicalAttribute = PartTechnicalAttribute("Übersetzung", "10-51"),
            ),
            details,
        )
    }

    @Test
    fun `omits secondary rows without a valid populated technical field`() {
        assertNull(bikePartSecondaryDetails(type = null, attributes = emptyList()))
        assertNull(bikePartSecondaryDetails(type = "Motor", attributes = emptyList()))
        assertEquals(
            BikePartSecondaryDetails(type = "Bremsscheibe", technicalAttribute = null),
            bikePartSecondaryDetails(type = "Bremsscheibe", attributes = emptyList()),
        )
        assertEquals(
            BikePartSecondaryDetails(type = "Kettenblatt", technicalAttribute = null),
            bikePartSecondaryDetails(
                type = "Kettenblatt",
                attributes = listOf(PartAttribute(key = "gear_ratio", value = " ")),
            ),
        )
    }
}
