package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class BikeDetails(
    val bikeWithImages: BikeWithImages,
    val shop: Shop?,
    val parts: List<PartWithRelations>,
)

class GetBikeDetailsUseCase @Inject constructor(
    private val bikeRepository: BikeRepository,
    private val partRepository: PartRepository,
    private val shopRepository: ShopRepository,
) {
    operator fun invoke(bikeId: Long): Flow<BikeDetails?> = combine(
        bikeRepository.observeBike(bikeId),
        partRepository.observeParts(),
        shopRepository.observeShops(),
    ) { bikeWithImages, parts, shops ->
        bikeWithImages?.let { bike ->
            BikeDetails(
                bikeWithImages = bike,
                shop = shops.firstOrNull { it.id == bike.bike.shopId },
                parts = parts
                    .asSequence()
                    .filter { part -> part.bikes.any { it.id == bikeId } }
                    .sortedWith(
                        compareBy<PartWithRelations> { it.part.category.ordinal }
                            .thenBy { it.part.name.lowercase() }
                            .thenBy { it.part.id },
                    )
                    .toList(),
            )
        }
    }
}
