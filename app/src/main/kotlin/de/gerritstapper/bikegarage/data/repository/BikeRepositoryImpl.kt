package de.gerritstapper.bikegarage.data.repository

import androidx.room.withTransaction
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.local.AppDatabase
import de.gerritstapper.bikegarage.data.local.dao.BikeDao
import de.gerritstapper.bikegarage.data.mapper.toDomain
import de.gerritstapper.bikegarage.data.mapper.toEntity
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.model.BikeWithParts
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BikeRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val bikeDao: BikeDao,
    private val imageStorage: ImageStorage,
) : BikeRepository {
    override fun observeBikes(): Flow<List<BikeWithImages>> =
        bikeDao.observeAllWithImages().map { bikes -> bikes.map { it.toDomain() } }

    override fun observeBike(bikeId: Long): Flow<BikeWithImages?> =
        bikeDao.observeWithImages(bikeId).map { it?.toDomain() }

    override suspend fun bikeWithParts(bikeId: Long): BikeWithParts? =
        bikeDao.getWithParts(bikeId)?.toDomain()

    override suspend fun saveBike(bike: Bike, images: List<BikeImage>): Long =
        database.withTransaction {
            val bikeId = if (bike.id == 0L) {
                bikeDao.insert(bike.toEntity())
            } else {
                bikeDao.update(bike.toEntity())
                bike.id
            }
            bikeDao.replaceImages(bikeId, images.map { it.toEntity(bikeId) })
            bikeId
        }

    override suspend fun deleteBike(bike: Bike) {
        val imagePaths = bikeDao.imagePathsForBike(bike.id)
        database.withTransaction {
            bikeDao.delete(bike.toEntity())
        }
        for (imagePath in imagePaths) {
            imageStorage.delete(imagePath)
        }
    }

    override suspend fun deleteBikeWithSelectedParts(
        bike: Bike,
        partIdsToDelete: Set<Long>,
    ) {
        val imagePaths = database.withTransaction {
            val bikeImagePaths = bikeDao.imagePathsForBike(bike.id)
            val partImagePaths = if (partIdsToDelete.isEmpty()) {
                emptyList()
            } else {
                database.partDao().imagePathsForPartsAssignedToBike(bike.id, partIdsToDelete)
            }
            if (partIdsToDelete.isNotEmpty()) {
                database.partDao().deletePartsAssignedToBike(bike.id, partIdsToDelete)
            }
            bikeDao.delete(bike.toEntity())
            bikeImagePaths + partImagePaths
        }
        imagePaths.forEach { imagePath ->
            try {
                imageStorage.delete(imagePath)
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
            }
        }
    }
}
