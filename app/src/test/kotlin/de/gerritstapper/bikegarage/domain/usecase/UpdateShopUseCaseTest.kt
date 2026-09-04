package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateShopUseCaseTest {
    @Test
    fun `requires a name and logo with field-specific validation`() = runBlocking {
        val repository = RecordingUpdateShopRepository()
        val useCase = UpdateShopUseCase(repository)

        val result = useCase(existingShop, name = "  ", logoPath = null)

        val invalid = result as UpdateShopResult.Invalid
        assertEquals(ShopUpdateFieldError.NAME_REQUIRED, invalid.validation.nameError)
        assertEquals(ShopUpdateFieldError.LOGO_REQUIRED, invalid.validation.logoError)
        assertFalse(invalid.validation.isValid)
        assertTrue(repository.savedShops.isEmpty())
    }

    @Test
    fun `trims fields and preserves the persisted shop identity`() = runBlocking {
        val repository = RecordingUpdateShopRepository()
        val useCase = UpdateShopUseCase(repository)

        val result = useCase(
            shop = existingShop,
            name = "  Neues Radhaus  ",
            logoPath = "images/new-logo.png",
        )

        val updated = result as UpdateShopResult.Updated
        assertEquals(
            existingShop.copy(name = "Neues Radhaus", logoPath = "images/new-logo.png"),
            updated.shop,
        )
        assertEquals(listOf(updated.shop), repository.savedShops)
    }

    private companion object {
        val existingShop = Shop(
            id = 7,
            name = "Radhaus",
            logoPath = "images/old-logo.jpg",
            createdAt = 123,
        )
    }
}

private class RecordingUpdateShopRepository : ShopRepository {
    val savedShops = mutableListOf<Shop>()

    override fun observeShops(): Flow<List<Shop>> = emptyFlow()

    override fun observeShop(shopId: Long): Flow<ShopWithRelations?> = emptyFlow()

    override suspend fun saveShop(shop: Shop): Long {
        savedShops += shop
        return shop.id
    }

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean = false
}
