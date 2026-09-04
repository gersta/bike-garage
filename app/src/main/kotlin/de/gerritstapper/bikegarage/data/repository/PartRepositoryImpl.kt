package de.gerritstapper.bikegarage.data.repository

import androidx.room.withTransaction
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.local.AppDatabase
import de.gerritstapper.bikegarage.data.local.dao.PartDao
import de.gerritstapper.bikegarage.data.local.entity.BikePartCrossRefEntity
import de.gerritstapper.bikegarage.data.mapper.toDomain
import de.gerritstapper.bikegarage.data.mapper.toEntity
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartImage
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PartRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val partDao: PartDao,
    private val imageStorage: ImageStorage,
) : PartRepository {
    override fun observeParts(): Flow<List<PartWithRelations>> =
        partDao.observeAllWithRelations().map { parts -> parts.map { it.toDomain() } }

    override fun observePart(partId: Long): Flow<PartWithRelations?> =
        partDao.observeWithRelations(partId).map { it?.toDomain() }

    override suspend fun savePart(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long = savePart(part, attributes, bikeIds, shopLinks, images = null)

    override suspend fun savePartWithImages(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
        images: List<PartImage>,
    ): Long = savePart(part, attributes, bikeIds, shopLinks, images)

    private suspend fun savePart(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
        images: List<PartImage>?,
    ): Long = database.withTransaction {
        val partId = if (part.id == 0L) {
            partDao.insert(part.toEntity())
        } else {
            partDao.update(part.toEntity())
            part.id
        }
        partDao.replaceRelations(
            partId = partId,
            attributes = attributes.map { it.copy(id = 0).toEntity(partId) },
            bikeAssignments = bikeIds.map { BikePartCrossRefEntity(it, partId) },
            shopLinks = shopLinks.map { it.copy(id = 0).toEntity(partId) },
        )
        images?.let { partDao.replaceImages(partId, it.map { image -> image.toEntity(partId) }) }
        partId
    }

    override suspend fun assignPartToBike(partId: Long, bikeId: Long) {
        database.withTransaction {
            partDao.insertBikeAssignments(listOf(BikePartCrossRefEntity(bikeId, partId)))
        }
    }

    override suspend fun deletePart(part: Part) {
        val imagePaths = partDao.imagePathsForPart(part.id)
        database.withTransaction {
            partDao.deleteAttributes(part.id)
            partDao.deleteBikeAssignments(part.id)
            partDao.deleteShopLinks(part.id)
            partDao.delete(part.toEntity())
        }
        for (imagePath in imagePaths) {
            imageStorage.delete(imagePath)
        }
    }
}
