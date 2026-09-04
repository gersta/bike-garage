package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.repository.PartRepository
import javax.inject.Inject

class AssignPartToBikeUseCase @Inject constructor(
    private val partRepository: PartRepository,
) {
    suspend operator fun invoke(partId: Long, bikeId: Long) {
        require(partId > 0) { "Ein gespeichertes Teil muss ausgewählt werden." }
        require(bikeId > 0) { "Ein gespeichertes Fahrrad muss ausgewählt werden." }
        partRepository.assignPartToBike(partId, bikeId)
    }
}
