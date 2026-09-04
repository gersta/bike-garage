package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

sealed interface PartDeletionInspection {
    data class Ready(val part: Part) : PartDeletionInspection
    data object NotFound : PartDeletionInspection
}

sealed interface DeletePartResult {
    data class Deleted(val part: Part) : DeletePartResult
    data object NotFound : DeletePartResult
    data object Failed : DeletePartResult
}

class DeletePartUseCase @Inject constructor(
    private val partRepository: PartRepository,
) {
    suspend fun inspect(partId: Long): PartDeletionInspection =
        partRepository.observePart(partId).first()?.part
            ?.let(PartDeletionInspection::Ready)
            ?: PartDeletionInspection.NotFound

    suspend fun delete(partId: Long): DeletePartResult =
        try {
            when (val inspection = inspect(partId)) {
                is PartDeletionInspection.Ready -> {
                    partRepository.deletePart(inspection.part)
                    DeletePartResult.Deleted(inspection.part)
                }

                PartDeletionInspection.NotFound -> DeletePartResult.NotFound
            }
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            DeletePartResult.Failed
        }
}
