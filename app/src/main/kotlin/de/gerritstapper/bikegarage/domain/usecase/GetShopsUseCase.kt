package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetShopsUseCase @Inject constructor(
    private val shopRepository: ShopRepository,
) {
    operator fun invoke(): Flow<List<Shop>> = shopRepository.observeShops()
}
