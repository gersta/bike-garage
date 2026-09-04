package de.gerritstapper.bikegarage.domain.repository

import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations
import kotlinx.coroutines.flow.Flow

interface ShopRepository {
    fun observeShops(): Flow<List<Shop>>
    fun observeShop(shopId: Long): Flow<ShopWithRelations?>
    suspend fun saveShop(shop: Shop): Long
    suspend fun deleteShopIfUnlinked(shop: Shop): Boolean
}
