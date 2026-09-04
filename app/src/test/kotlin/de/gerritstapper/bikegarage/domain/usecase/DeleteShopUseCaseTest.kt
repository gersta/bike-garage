package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteShopUseCaseTest {
    @Test
    fun `blocks deletion and returns every purchase location and product reference`() = runBlocking {
        val repository = DeletionShopRepository(
            ShopWithRelations(
                shop = shop,
                purchasedBikes = listOf(
                    Bike(
                        id = 2,
                        nickname = "Gravel",
                        brand = "Canyon",
                        model = "Grizl",
                        shopId = shop.id,
                        createdAt = 2,
                    ),
                ),
                parts = listOf(
                    Part(
                        id = 3,
                        category = PartCategory.DRIVETRAIN,
                        name = "Kette",
                        createdAt = 3,
                    ),
                ),
                partLinks = listOf(
                    PartShopLink(
                        id = 4,
                        partId = 3,
                        shopId = shop.id,
                        productUrl = "https://example.test/kette",
                    ),
                ),
            ),
            mayDelete = false,
        )

        val result = DeleteShopUseCase(repository).delete(shop.id)

        val blocked = result as DeleteShopResult.Blocked
        assertEquals(listOf("Gravel"), blocked.blockers.purchasedBikes.map { it.nickname })
        assertEquals(
            listOf(ShopProductReference("Kette", "https://example.test/kette")),
            blocked.blockers.productReferences,
        )
        assertFalse(repository.deleteWasRequested)
    }

    @Test
    fun `deletes a shop without links through the guarded repository operation`() = runBlocking {
        val repository = DeletionShopRepository(
            ShopWithRelations(
                shop = shop,
                purchasedBikes = emptyList(),
                parts = emptyList(),
                partLinks = emptyList(),
            ),
            mayDelete = true,
        )

        val result = DeleteShopUseCase(repository).delete(shop.id)

        assertEquals(DeleteShopResult.Deleted(shop), result)
        assertTrue(repository.deleteWasRequested)
        assertEquals(shop, repository.deletedShop)
    }

    private companion object {
        val shop = Shop(
            id = 1,
            name = "Radhaus",
            logoPath = "images/radhaus.jpg",
            createdAt = 1,
        )
    }
}

private class DeletionShopRepository(
    private val relations: ShopWithRelations,
    private val mayDelete: Boolean,
) : ShopRepository {
    var deleteWasRequested = false
    var deletedShop: Shop? = null

    override fun observeShops(): Flow<List<Shop>> = emptyFlow()

    override fun observeShop(shopId: Long): Flow<ShopWithRelations?> = flowOf(relations)

    override suspend fun saveShop(shop: Shop): Long = shop.id

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean {
        deleteWasRequested = true
        deletedShop = shop
        return mayDelete
    }
}
