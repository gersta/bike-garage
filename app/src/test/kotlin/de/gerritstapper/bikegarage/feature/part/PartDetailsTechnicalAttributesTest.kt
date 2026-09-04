package de.gerritstapper.bikegarage.feature.part

import de.gerritstapper.bikegarage.domain.model.PartAttribute
import org.junit.Assert.assertEquals
import org.junit.Test

class PartDetailsTechnicalAttributesTest {
    @Test
    fun `shows defined fields in their central order and retains additional attributes`() {
        val displayed = technicalAttributesFor(
            type = "Kassette",
            attributes = listOf(
                PartAttribute(key = "freehub", value = "Microspline"),
                PartAttribute(key = "sprocket_count", value = "12"),
                PartAttribute(key = "custom_finish", value = "Schwarz"),
            ),
        )

        assertEquals(
            listOf(
                PartTechnicalAttribute("Anzahl Ritzel", "12"),
                PartTechnicalAttribute("Übersetzung", ""),
                PartTechnicalAttribute("Freilauf", "Microspline"),
                PartTechnicalAttribute("custom_finish", "Schwarz"),
            ),
            displayed,
        )
    }

    @Test
    fun `retains every attribute of an unknown type`() {
        val displayed = technicalAttributesFor(
            type = "Motor",
            attributes = listOf(
                PartAttribute(key = "leistung", value = "250 W"),
                PartAttribute(key = "drehmoment", value = "85 Nm"),
            ),
        )

        assertEquals(
            listOf(
                PartTechnicalAttribute("drehmoment", "85 Nm"),
                PartTechnicalAttribute("leistung", "250 W"),
            ),
            displayed,
        )
    }
}
