package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetBikesUseCase @Inject constructor(
    private val bikeRepository: BikeRepository,
) {
    operator fun invoke(): Flow<List<BikeWithImages>> =
        bikeRepository.observeBikes().map { bikes ->
            bikes.sortedByDescending { it.bike.createdAt }
        }
}
