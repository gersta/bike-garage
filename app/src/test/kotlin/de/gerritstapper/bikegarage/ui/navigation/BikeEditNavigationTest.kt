package de.gerritstapper.bikegarage.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class BikeEditNavigationTest {
    @Test
    fun `home navigation targets the garage overview route`() {
        assertEquals("garage_overview", GARAGE_OVERVIEW_ROUTE)
    }

    @Test
    fun `bike edit routes to the bike details destination after saving`() {
        assertEquals("bike_edit/8", bikeEditRoute(8))
        assertEquals("bike_details/8", bikeDetailsRoute(8))
    }

    @Test
    fun `part creation route keeps the originating bike id`() {
        assertEquals("part_creation?bikeId=8", partCreationRoute(8))
    }

    @Test
    fun `part assignment route keeps the target bike id`() {
        assertEquals("part_assignment/8", partAssignmentRoute(8))
    }

    @Test
    fun `parts overview opens creation without a required bike`() {
        assertEquals("part_creation", partCreationRoute())
    }

    @Test
    fun `part edit routes to its details destination after saving`() {
        assertEquals("part_edit/8", partEditRoute(8))
        assertEquals("part_details/8", partDetailsRoute(8))
    }
}
