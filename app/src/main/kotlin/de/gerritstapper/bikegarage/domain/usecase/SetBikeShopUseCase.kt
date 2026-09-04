package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import javax.inject.Inject

class SetBikeShopUseCase @Inject constructor(
    private val bikeRepository: BikeRepository,
) {
    suspend operator fun invoke(bikeWithImages: BikeWithImages, shopId: Long) {
        bikeRepository.saveBike(
            bike = bikeWithImages.bike.copy(shopId = shopId),
            images = bikeWithImages.images,
        )
    }
}
