package de.gerritstapper.bikegarage.data.repository

import de.gerritstapper.bikegarage.data.local.dao.ShopDao
import de.gerritstapper.bikegarage.data.mapper.toDomain
import de.gerritstapper.bikegarage.data.mapper.toEntity
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShopRepositoryImpl @Inject constructor(
    private val shopDao: ShopDao,
) : ShopRepository {
    override fun observeShops(): Flow<List<Shop>> =
        shopDao.observeAll().map { shops -> shops.map { it.toDomain() } }

    override fun observeShop(shopId: Long): Flow<ShopWithRelations?> =
        shopDao.observeWithRelations(shopId).map { it?.toDomain() }

    override suspend fun saveShop(shop: Shop): Long =
        if (shop.id == 0L) shopDao.insert(shop.toEntity()) else {
            shopDao.update(shop.toEntity())
            shop.id
        }

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean =
        shopDao.deleteIfUnlinked(shop.toEntity())
}
