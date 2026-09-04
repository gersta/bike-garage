package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetShopsUseCaseTest {
    @Test
    fun `returns every persisted shop including a bike purchase location`() = runBlocking {
        val purchaseLocation = Shop(
            id = 7,
            name = "Radhaus",
            logoPath = "shops/radhaus.png",
            createdAt = 1,
        )

        val shops = GetShopsUseCase(FakeShopRepository(listOf(purchaseLocation)))().first()

        assertEquals(listOf(purchaseLocation), shops)
    }
}

private class FakeShopRepository(
    private val shops: List<Shop>,
) : ShopRepository {
    override fun observeShops(): Flow<List<Shop>> = flowOf(shops)

    override fun observeShop(shopId: Long): Flow<ShopWithRelations?> = flowOf(null)

    override suspend fun saveShop(shop: Shop): Long = shop.id

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean = false
}
