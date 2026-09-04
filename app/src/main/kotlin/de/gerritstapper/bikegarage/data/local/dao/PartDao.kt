package de.gerritstapper.bikegarage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.gerritstapper.bikegarage.data.local.entity.BikePartCrossRefEntity
import de.gerritstapper.bikegarage.data.local.entity.PartAttributeEntity
import de.gerritstapper.bikegarage.data.local.entity.PartEntity
import de.gerritstapper.bikegarage.data.local.entity.PartImageEntity
import de.gerritstapper.bikegarage.data.local.entity.PartShopLinkEntity
import de.gerritstapper.bikegarage.data.local.relationship.PartWithRelationsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {
    @Insert
    suspend fun insert(part: PartEntity): Long

    @Update
    suspend fun update(part: PartEntity)

    @Delete
    suspend fun delete(part: PartEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttributes(attributes: List<PartAttributeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<PartImageEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBikeAssignments(assignments: List<BikePartCrossRefEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopLinks(links: List<PartShopLinkEntity>)

    @Query("DELETE FROM part_attributes WHERE partId = :partId")
    suspend fun deleteAttributes(partId: Long)

    @Query("DELETE FROM part_images WHERE partId = :partId")
    suspend fun deleteImagesForPart(partId: Long)

    @Query("SELECT storagePath FROM part_images WHERE partId = :partId")
    suspend fun imagePathsForPart(partId: Long): List<String>

    @Query(
        """
        SELECT part_images.storagePath
        FROM part_images
        INNER JOIN bike_part_cross_refs
            ON bike_part_cross_refs.partId = part_images.partId
        WHERE bike_part_cross_refs.bikeId = :bikeId
          AND part_images.partId IN (:partIds)
        """,
    )
    suspend fun imagePathsForPartsAssignedToBike(bikeId: Long, partIds: Set<Long>): List<String>

    @Query("DELETE FROM bike_part_cross_refs WHERE partId = :partId")
    suspend fun deleteBikeAssignments(partId: Long)

    @Query("DELETE FROM part_shop_links WHERE partId = :partId")
    suspend fun deleteShopLinks(partId: Long)

    @Query(
        """
        DELETE FROM parts
        WHERE id IN (:partIds)
          AND id IN (
              SELECT partId
              FROM bike_part_cross_refs
              WHERE bikeId = :bikeId
          )
        """,
    )
    suspend fun deletePartsAssignedToBike(bikeId: Long, partIds: Set<Long>)

    @Transaction
    @Query("SELECT * FROM parts ORDER BY category, name COLLATE NOCASE")
    fun observeAllWithRelations(): Flow<List<PartWithRelationsEntity>>

    @Transaction
    @Query("SELECT * FROM parts WHERE id = :partId")
    fun observeWithRelations(partId: Long): Flow<PartWithRelationsEntity?>

    @Transaction
    suspend fun replaceRelations(
        partId: Long,
        attributes: List<PartAttributeEntity>,
        bikeAssignments: List<BikePartCrossRefEntity>,
        shopLinks: List<PartShopLinkEntity>,
    ) {
        deleteAttributes(partId)
        deleteBikeAssignments(partId)
        deleteShopLinks(partId)
        if (attributes.isNotEmpty()) insertAttributes(attributes)
        if (bikeAssignments.isNotEmpty()) insertBikeAssignments(bikeAssignments)
        if (shopLinks.isNotEmpty()) insertShopLinks(shopLinks)
    }

    @Transaction
    suspend fun replaceImages(partId: Long, images: List<PartImageEntity>) {
        deleteImagesForPart(partId)
        if (images.isNotEmpty()) insertImages(images)
    }
}
