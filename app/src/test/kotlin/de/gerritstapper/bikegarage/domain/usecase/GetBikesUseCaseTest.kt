package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.model.BikeWithParts
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetBikesUseCaseTest {
    @Test
    fun `returns bikes ordered by creation time descending`() = runBlocking {
        val oldest = bikeWithImages(id = 1, createdAt = 1)
        val newest = bikeWithImages(id = 2, createdAt = 3)
        val middle = bikeWithImages(id = 3, createdAt = 2)

        val bikes = GetBikesUseCase(FakeBikeRepository(listOf(oldest, newest, middle)))().first()

        assertEquals(listOf(newest, middle, oldest), bikes)
    }
}

private fun bikeWithImages(id: Long, createdAt: Long) = BikeWithImages(
    bike = Bike(id = id, brand = "Marke $id", model = "Modell $id", createdAt = createdAt),
    images = listOf(
        BikeImage(
            bikeId = id,
            storagePath = "images/$id.jpg",
            displayOrder = 0,
            isThumbnail = true,
        ),
    ),
)

private class FakeBikeRepository(
    private val bikes: List<BikeWithImages>,
) : BikeRepository {
    override fun observeBikes(): Flow<List<BikeWithImages>> = flowOf(bikes)

    override fun observeBike(bikeId: Long): Flow<BikeWithImages?> = flowOf(null)

    override suspend fun bikeWithParts(bikeId: Long): BikeWithParts? = null

    override suspend fun saveBike(bike: Bike, images: List<BikeImage>): Long = bike.id

    override suspend fun deleteBike(bike: Bike) = Unit

    override suspend fun deleteBikeWithSelectedParts(bike: Bike, partIdsToDelete: Set<Long>) = Unit
}
