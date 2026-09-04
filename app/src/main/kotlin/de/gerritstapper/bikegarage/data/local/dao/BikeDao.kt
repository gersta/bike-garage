package de.gerritstapper.bikegarage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.gerritstapper.bikegarage.data.local.entity.BikeEntity
import de.gerritstapper.bikegarage.data.local.entity.BikeImageEntity
import de.gerritstapper.bikegarage.data.local.relationship.BikeWithImagesEntity
import de.gerritstapper.bikegarage.data.local.relationship.BikeWithPartsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BikeDao {
    @Insert
    suspend fun insert(bike: BikeEntity): Long

    @Update
    suspend fun update(bike: BikeEntity)

    @Delete
    suspend fun delete(bike: BikeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<BikeImageEntity>)

    @Query("DELETE FROM bike_images WHERE bikeId = :bikeId")
    suspend fun deleteImagesForBike(bikeId: Long)

    @Query("SELECT storagePath FROM bike_images WHERE bikeId = :bikeId")
    suspend fun imagePathsForBike(bikeId: Long): List<String>

    @Query("SELECT * FROM bikes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BikeEntity>>

    @Transaction
    @Query("SELECT * FROM bikes ORDER BY createdAt DESC")
    fun observeAllWithImages(): Flow<List<BikeWithImagesEntity>>

    @Transaction
    @Query("SELECT * FROM bikes WHERE id = :bikeId")
    fun observeWithImages(bikeId: Long): Flow<BikeWithImagesEntity?>

    @Transaction
    @Query("SELECT * FROM bikes WHERE id = :bikeId")
    suspend fun getWithParts(bikeId: Long): BikeWithPartsEntity?

    @Transaction
    suspend fun replaceImages(bikeId: Long, images: List<BikeImageEntity>) {
        deleteImagesForBike(bikeId)
        if (images.isNotEmpty()) insertImages(images)
    }
}
