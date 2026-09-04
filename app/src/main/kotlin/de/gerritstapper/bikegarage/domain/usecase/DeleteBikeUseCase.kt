package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.BikeWithParts
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

sealed interface BikeDeletionInspection {
    data class Ready(
        val bike: Bike,
        val parts: List<Part>,
    ) : BikeDeletionInspection

    data object NotFound : BikeDeletionInspection
}

sealed interface DeleteBikeResult {
    data class Deleted(val bike: Bike) : DeleteBikeResult
    data object NotFound : DeleteBikeResult
    data object Failed : DeleteBikeResult
}

class DeleteBikeUseCase @Inject constructor(
    private val bikeRepository: BikeRepository,
) {
    suspend fun inspect(bikeId: Long): BikeDeletionInspection =
        bikeRepository.bikeWithParts(bikeId)?.toInspection()
            ?: BikeDeletionInspection.NotFound

    suspend fun delete(bikeId: Long, selectedPartIds: Set<Long>): DeleteBikeResult =
        try {
            when (val inspection = inspect(bikeId)) {
                is BikeDeletionInspection.Ready -> {
                    val linkedPartIds = inspection.parts.mapTo(mutableSetOf()) { it.id }
                    bikeRepository.deleteBikeWithSelectedParts(
                        bike = inspection.bike,
                        partIdsToDelete = selectedPartIds.intersect(linkedPartIds),
                    )
                    DeleteBikeResult.Deleted(inspection.bike)
                }

                BikeDeletionInspection.NotFound -> DeleteBikeResult.NotFound
            }
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            DeleteBikeResult.Failed
        }
    }

private fun BikeWithParts.toInspection(): BikeDeletionInspection =
    BikeDeletionInspection.Ready(
        bike = bike,
        parts = parts.sortedWith(
            compareBy<Part> { it.name.lowercase() }.thenBy { it.id },
        ),
    )
