package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.model.BikeWithParts
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateBikeUseCaseTest {
    @Test
    fun `requires brand model and at least one image without saving`() = runBlocking {
        val repository = RecordingBikeRepository()
        val useCase = CreateBikeUseCase(repository)

        val result = useCase(
            nickname = "",
            brand = " ",
            model = "",
            purchaseDate = "2026-99-99",
            shopId = null,
            imagePaths = emptyList(),
            thumbnailPath = null,
        )

        val invalid = result as CreateBikeResult.Invalid
        assertEquals(BikeCreationFieldError.BRAND_REQUIRED, invalid.validation.brandError)
        assertEquals(BikeCreationFieldError.MODEL_REQUIRED, invalid.validation.modelError)
        assertEquals(BikeCreationFieldError.IMAGE_REQUIRED, invalid.validation.imageError)
        assertEquals(BikeCreationFieldError.PURCHASE_DATE_INVALID, invalid.validation.purchaseDateError)
        assertFalse(invalid.validation.isValid)
        assertTrue(repository.savedBikes.isEmpty())
    }

    @Test
    fun `persists ordered images and the selected thumbnail with the bike`() = runBlocking {
        val repository = RecordingBikeRepository()
        val useCase = CreateBikeUseCase(repository)

        val result = useCase(
            nickname = "  Schotterfuchs  ",
            brand = "  Canyon ",
            model = " Grizl  ",
            purchaseDate = "2026-08-21",
            shopId = 9,
            imagePaths = listOf("images/front.jpg", "images/side.jpg"),
            thumbnailPath = "images/side.jpg",
        )

        val created = result as CreateBikeResult.Created
        assertEquals(42, created.bike.id)
        assertEquals("Schotterfuchs", created.bike.nickname)
        assertEquals("Canyon", created.bike.brand)
        assertEquals("Grizl", created.bike.model)
        assertEquals(9L, created.bike.shopId)
        assertEquals(
            listOf(
                BikeImage(storagePath = "images/front.jpg", displayOrder = 0, isThumbnail = false),
                BikeImage(storagePath = "images/side.jpg", displayOrder = 1, isThumbnail = true),
            ),
            repository.savedImages,
        )
    }
}

private class RecordingBikeRepository : BikeRepository {
    val savedBikes = mutableListOf<Bike>()
    var savedImages = emptyList<BikeImage>()

    override fun observeBikes(): Flow<List<BikeWithImages>> = emptyFlow()

    override fun observeBike(bikeId: Long): Flow<BikeWithImages?> = emptyFlow()

    override suspend fun bikeWithParts(bikeId: Long): BikeWithParts? = null

    override suspend fun saveBike(bike: Bike, images: List<BikeImage>): Long {
        savedBikes += bike
        savedImages = images
        return 42
    }

    override suspend fun deleteBike(bike: Bike) = Unit

    override suspend fun deleteBikeWithSelectedParts(bike: Bike, partIdsToDelete: Set<Long>) = Unit
}
