package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.model.BikeWithParts
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteBikeUseCaseTest {
    @Test
    fun `deletes only the bike when no parts are selected`() = runBlocking {
        val repository = DeletionBikeRepository(bike, listOf(firstPart, secondPart))

        val result = DeleteBikeUseCase(repository).delete(bike.id, emptySet())

        assertEquals(DeleteBikeResult.Deleted(bike), result)
        assertEquals(bike, repository.deletedBike)
        assertEquals(emptySet<Long>(), repository.deletedPartIds)
        assertEquals(listOf(firstPart, secondPart), repository.remainingParts)
    }

    @Test
    fun `deletes one selected part and retains unselected linked parts`() = runBlocking {
        val repository = DeletionBikeRepository(bike, listOf(firstPart, secondPart))

        val result = DeleteBikeUseCase(repository).delete(bike.id, setOf(firstPart.id))

        assertEquals(DeleteBikeResult.Deleted(bike), result)
        assertEquals(bike, repository.deletedBike)
        assertEquals(setOf(firstPart.id), repository.deletedPartIds)
        assertEquals(listOf(secondPart), repository.remainingParts)
    }

    @Test
    fun `deletes all selected linked parts but ignores unlinked selections`() = runBlocking {
        val repository = DeletionBikeRepository(bike, listOf(firstPart, secondPart))

        val result = DeleteBikeUseCase(repository).delete(
            bike.id,
            setOf(firstPart.id, secondPart.id, unlinkedPartId),
        )

        assertEquals(DeleteBikeResult.Deleted(bike), result)
        assertEquals(bike, repository.deletedBike)
        assertEquals(setOf(firstPart.id, secondPart.id), repository.deletedPartIds)
        assertTrue(repository.remainingParts.isEmpty())
    }

    @Test
    fun `reports a deletion failure without reporting success`() = runBlocking {
        val repository = DeletionBikeRepository(
            linkedBike = bike,
            initialParts = listOf(firstPart),
            failDeletion = true,
        )

        val result = DeleteBikeUseCase(repository).delete(bike.id, setOf(firstPart.id))

        assertEquals(DeleteBikeResult.Failed, result)
        assertEquals(null, repository.deletedBike)
        assertTrue(repository.remainingParts.contains(firstPart))
    }

    private companion object {
        val bike = Bike(id = 1, brand = "Canyon", model = "Grizl", createdAt = 1)
        val firstPart = Part(
            id = 2,
            category = PartCategory.DRIVETRAIN,
            name = "Kette",
            createdAt = 2,
        )
        val secondPart = Part(
            id = 3,
            category = PartCategory.BRAKES,
            name = "Bremse",
            createdAt = 3,
        )
        const val unlinkedPartId = 4L
    }
}

private class DeletionBikeRepository(
    private val linkedBike: Bike,
    initialParts: List<Part>,
    private val failDeletion: Boolean = false,
) : BikeRepository {
    var remainingParts = initialParts
    var deletedBike: Bike? = null
    var deletedPartIds = emptySet<Long>()

    override fun observeBikes(): Flow<List<BikeWithImages>> = emptyFlow()

    override fun observeBike(bikeId: Long): Flow<BikeWithImages?> = emptyFlow()

    override suspend fun bikeWithParts(bikeId: Long): BikeWithParts? =
        BikeWithParts(bike = linkedBike, parts = remainingParts)

    override suspend fun saveBike(bike: Bike, images: List<BikeImage>): Long = bike.id

    override suspend fun deleteBike(bike: Bike) = Unit

    override suspend fun deleteBikeWithSelectedParts(
        bike: Bike,
        partIdsToDelete: Set<Long>,
    ) {
        check(!failDeletion) { "Datenbankfehler" }
        deletedBike = bike
        deletedPartIds = partIdsToDelete
        remainingParts = remainingParts.filterNot { it.id in partIdsToDelete }
    }
}
