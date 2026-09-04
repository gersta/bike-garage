package de.gerritstapper.bikegarage.domain.repository

import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartImage
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import kotlinx.coroutines.flow.Flow

interface PartRepository {
    fun observeParts(): Flow<List<PartWithRelations>>
    fun observePart(partId: Long): Flow<PartWithRelations?>
    suspend fun savePart(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long
    suspend fun savePartWithImages(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
        images: List<PartImage>,
    ): Long = savePart(part, attributes, bikeIds, shopLinks)
    suspend fun assignPartToBike(partId: Long, bikeId: Long) {
        error("Die Teilzuordnung wird von diesem Repository nicht unterstützt.")
    }
    suspend fun deletePart(part: Part)
}
