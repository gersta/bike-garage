package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetPartsUseCase @Inject constructor(
    private val partRepository: PartRepository,
) {
    operator fun invoke(): Flow<List<PartWithRelations>> = partRepository.observeParts()
}
