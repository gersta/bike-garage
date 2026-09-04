package de.gerritstapper.bikegarage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.gerritstapper.bikegarage.data.local.entity.ShopEntity
import de.gerritstapper.bikegarage.data.local.relationship.ShopWithRelationsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Insert
    suspend fun insert(shop: ShopEntity): Long

    @Update
    suspend fun update(shop: ShopEntity)

    @Delete
    suspend fun delete(shop: ShopEntity): Int

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM bikes WHERE shopId = :shopId
            UNION ALL
            SELECT 1 FROM part_shop_links WHERE shopId = :shopId
        )
        """,
    )
    suspend fun hasReferences(shopId: Long): Boolean

    @Query("SELECT * FROM shops ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ShopEntity>>

    @Transaction
    @Query("SELECT * FROM shops WHERE id = :shopId")
    fun observeWithRelations(shopId: Long): Flow<ShopWithRelationsEntity?>

    @Transaction
    suspend fun deleteIfUnlinked(shop: ShopEntity): Boolean {
        if (hasReferences(shop.id)) return false
        return delete(shop) > 0
    }
}
