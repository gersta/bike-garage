package de.gerritstapper.bikegarage.ui.navigation

import androidx.lifecycle.SavedStateHandle
import de.gerritstapper.bikegarage.domain.model.Shop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShopCreationNavigationTest {
    @Test
    fun `bike form receives and consumes the new shop selection`() {
        val state = SavedStateHandle()
        val shop = Shop(
            id = 8,
            name = "Radhaus",
            logoPath = "images/radhaus.png",
            createdAt = 1,
        )

        state.setShopCreationResult(ShopCreationReturnContext.BIKE_FORM, shop)

        assertEquals(
            ShopCreationResult(selectedShopId = 8),
            state.consumeShopCreationResult(ShopCreationReturnContext.BIKE_FORM),
        )
        assertNull(state.consumeShopCreationResult(ShopCreationReturnContext.BIKE_FORM))
    }

    @Test
    fun `part form has an independent typed return context`() {
        val state = SavedStateHandle()
        val shop = Shop(
            id = 10,
            name = "Teileladen",
            logoPath = "images/teileladen.png",
            createdAt = 1,
        )

        assertEquals(
            ShopCreationReturnContext.PART_FORM,
            ShopCreationReturnContext.fromRouteValue("part-form"),
        )
        assertEquals(
            "shop_creation/bike-form",
            shopCreationRoute(ShopCreationReturnContext.BIKE_FORM),
        )
        state.setShopCreationResult(ShopCreationReturnContext.PART_FORM, shop)
        assertEquals(
            ShopCreationResult(selectedShopId = shop.id),
            state.consumeShopCreationResult(ShopCreationReturnContext.PART_FORM),
        )
    }

    @Test
    fun `bike details has its own shop creation return context`() {
        val state = SavedStateHandle()
        val shop = Shop(
            id = 9,
            name = "Neue Werkstatt",
            logoPath = "images/neue-werkstatt.png",
            createdAt = 1,
        )

        assertEquals(
            ShopCreationReturnContext.BIKE_DETAILS,
            ShopCreationReturnContext.fromRouteValue("bike-details"),
        )
        assertEquals(
            "shop_creation/bike-details",
            shopCreationRoute(ShopCreationReturnContext.BIKE_DETAILS),
        )
        state.setShopCreationResult(ShopCreationReturnContext.BIKE_DETAILS, shop)
        assertEquals(
            ShopCreationResult(selectedShopId = shop.id),
            state.consumeShopCreationResult(ShopCreationReturnContext.BIKE_DETAILS),
        )
    }
}
