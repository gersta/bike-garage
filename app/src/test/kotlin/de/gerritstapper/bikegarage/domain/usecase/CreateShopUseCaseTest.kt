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

class CreateShopUseCaseTest {
    @Test
    fun `requires a name and logo with field-specific validation`() = runBlocking {
        val repository = RecordingShopRepository()
        val useCase = CreateShopUseCase(repository)

        val result = useCase(name = "   ", logoPath = null)

        val invalid = result as CreateShopResult.Invalid
        assertEquals(ShopCreationFieldError.NAME_REQUIRED, invalid.validation.nameError)
        assertEquals(ShopCreationFieldError.LOGO_REQUIRED, invalid.validation.logoError)
        assertFalse(invalid.validation.isValid)
        assertTrue(repository.savedShops.isEmpty())
    }

    @Test
    fun `trims the name and returns the persisted shop`() = runBlocking {
        val repository = RecordingShopRepository()
        val useCase = CreateShopUseCase(repository)

        val result = useCase(name = "  Radhaus  ", logoPath = "images/radhaus.png")

        val created = result as CreateShopResult.Created
        assertEquals(42, created.shop.id)
        assertEquals("Radhaus", created.shop.name)
        assertEquals("images/radhaus.png", created.shop.logoPath)
        assertEquals(listOf(created.shop.copy(id = 0)), repository.savedShops)
    }
}

private class RecordingShopRepository : ShopRepository {
    val savedShops = mutableListOf<Shop>()

    override fun observeShops(): Flow<List<Shop>> = emptyFlow()

    override fun observeShop(shopId: Long): Flow<ShopWithRelations?> = emptyFlow()

    override suspend fun saveShop(shop: Shop): Long {
        savedShops += shop
        return 42
    }

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean = false
}
