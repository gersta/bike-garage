package de.gerritstapper.bikegarage.domain.repository

import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.model.BikeWithParts
import kotlinx.coroutines.flow.Flow

interface BikeRepository {
    fun observeBikes(): Flow<List<BikeWithImages>>
    fun observeBike(bikeId: Long): Flow<BikeWithImages?>
    suspend fun bikeWithParts(bikeId: Long): BikeWithParts?
    suspend fun saveBike(bike: Bike, images: List<BikeImage>): Long
    suspend fun deleteBike(bike: Bike)
    suspend fun deleteBikeWithSelectedParts(bike: Bike, partIdsToDelete: Set<Long>)
}
