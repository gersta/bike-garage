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
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateBikeUseCaseTest {
    @Test
    fun `validates mandatory fields without saving`() = runBlocking {
        val repository = UpdateRecordingBikeRepository()
        val useCase = UpdateBikeUseCase(repository)

        val result = useCase(
            bike = existingBike,
            nickname = "",
            brand = "",
            model = " ",
            purchaseDate = "2026-99-99",
            shopId = null,
            imagePaths = emptyList(),
            thumbnailPath = null,
        )

        val invalid = result as UpdateBikeResult.Invalid
        assertEquals(BikeUpdateFieldError.BRAND_REQUIRED, invalid.validation.brandError)
        assertEquals(BikeUpdateFieldError.MODEL_REQUIRED, invalid.validation.modelError)
        assertEquals(BikeUpdateFieldError.IMAGE_REQUIRED, invalid.validation.imageError)
        assertEquals(BikeUpdateFieldError.PURCHASE_DATE_INVALID, invalid.validation.purchaseDateError)
        assertTrue(repository.savedBikes.isEmpty())
    }

    @Test
    fun `updates bike values while preserving identity and selected thumbnail`() = runBlocking {
        val repository = UpdateRecordingBikeRepository()
        val useCase = UpdateBikeUseCase(repository)

        val result = useCase(
            bike = existingBike,
            nickname = "  Schotterfuchs ",
            brand = "  Canyon ",
            model = " Grizl ",
            purchaseDate = "2026-08-21",
            shopId = 9,
            imagePaths = listOf("images/side.jpg", "images/new.jpg"),
            thumbnailPath = "images/new.jpg",
        )

        val updated = (result as UpdateBikeResult.Updated).bike
        assertEquals(
            existingBike.copy(
                nickname = "Schotterfuchs",
                brand = "Canyon",
                model = "Grizl",
                purchaseDate = "2026-08-21",
                shopId = 9,
            ),
            updated,
        )
        assertEquals(listOf(updated), repository.savedBikes)
        assertEquals(
            listOf(
                BikeImage(storagePath = "images/side.jpg", displayOrder = 0, isThumbnail = false),
                BikeImage(storagePath = "images/new.jpg", displayOrder = 1, isThumbnail = true),
            ),
            repository.savedImages,
        )
    }

    private companion object {
        val existingBike = Bike(
            id = 7,
            nickname = "Alt",
            brand = "Rose",
            model = "Backroad",
            purchaseDate = null,
            shopId = 3,
            createdAt = 123,
        )
    }
}

private class UpdateRecordingBikeRepository : BikeRepository {
    val savedBikes = mutableListOf<Bike>()
    var savedImages = emptyList<BikeImage>()

    override fun observeBikes(): Flow<List<BikeWithImages>> = emptyFlow()

    override fun observeBike(bikeId: Long): Flow<BikeWithImages?> = emptyFlow()

    override suspend fun bikeWithParts(bikeId: Long): BikeWithParts? = null

    override suspend fun saveBike(bike: Bike, images: List<BikeImage>): Long {
        savedBikes += bike
        savedImages = images
        return bike.id
    }

    override suspend fun deleteBike(bike: Bike) = Unit

    override suspend fun deleteBikeWithSelectedParts(bike: Bike, partIdsToDelete: Set<Long>) = Unit
}
