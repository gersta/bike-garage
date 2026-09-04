package de.gerritstapper.bikegarage.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PartTechnicalFieldsTest {
    @Test
    fun `provides centralized fields for every supported type`() {
        assertEquals(listOf("Übersetzung"), PartTechnicalFields.forType("Blatt").map { it.label })
        assertEquals(
            listOf("Anzahl Glieder", "Anzahl Gänge"),
            PartTechnicalFields.forType("Kette").map { it.label },
        )
        assertEquals(
            listOf("Anzahl Ritzel", "Übersetzung", "Freilauf"),
            PartTechnicalFields.forType("Kassette").map { it.label },
        )
        assertEquals(listOf("Anzahl Speichen"), PartTechnicalFields.forType("Felge").map { it.label })
        assertEquals(
            listOf("Onepiece", "Durchmesser"),
            PartTechnicalFields.forType("Lenker").map { it.label },
        )
        assertEquals(
            PartTechnicalFields.forType("Blatt"),
            PartTechnicalFields.forType("Kettenblatt"),
        )
    }

    @Test
    fun `does not invent field definitions for unknown types`() {
        assertTrue(PartTechnicalFields.forType("Elektronische Schaltung").isEmpty())
        assertTrue(PartTechnicalFields.forType(null).isEmpty())
    }

    @Test
    fun `offers a centralized type selection for every category`() {
        assertTrue(PartCategory.entries.all { PartTechnicalFields.forCategory(it).isNotEmpty() })
        assertEquals(
            listOf("Kettenblatt", "Kette", "Kassette"),
            PartTechnicalFields.forCategory(PartCategory.DRIVETRAIN).take(3).map { it.name },
        )
    }
}
