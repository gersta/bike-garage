package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartImage
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatePartUseCaseTest {
    @Test
    fun `replaces a changed type and only persists attributes valid for it`() = runBlocking {
        val repository = UpdatingPartRepository()
        val existingPart = Part(
            id = 7,
            category = PartCategory.DRIVETRAIN,
            type = "Kette",
            name = "CN-M8100",
            createdAt = 1,
        )

        val result = UpdatePartUseCase(repository)(
            part = existingPart,
            category = PartCategory.DRIVETRAIN,
            type = "Kassette",
            name = " CS-M8100 ",
            manufacturer = " Shimano ",
            manufacturerNumber = "",
            description = " 12-fach ",
            technicalAttributes = mapOf(
                "link_count" to "116",
                "gear_count" to "12",
                "sprocket_count" to " 12 ",
                "freehub" to " Micro Spline ",
            ),
            bikeIds = setOf(3, 9),
            productLinks = listOf(
                PartProductLinkDraft(4, " https://example.test/kassette "),
            ),
            inventoryCount = 2,
            imagePaths = listOf("images/cassette.jpg"),
            thumbnailPath = "images/cassette.jpg",
        )

        assertEquals(
            UpdatePartResult.Updated(
                existingPart.copy(
                    type = "Kassette",
                    name = "CS-M8100",
                    manufacturer = "Shimano",
                    description = "12-fach",
                    inventoryCount = 2,
                ),
            ),
            result,
        )
        assertEquals(2, (result as UpdatePartResult.Updated).part.inventoryCount)
        assertEquals(
            listOf(
                PartAttribute(key = "sprocket_count", value = "12"),
                PartAttribute(key = "freehub", value = "Micro Spline"),
            ),
            repository.savedAttributes,
        )
        assertEquals(setOf(3L, 9L), repository.savedBikeIds)
        assertEquals(
            listOf(PartShopLink(shopId = 4, productUrl = "https://example.test/kassette")),
            repository.savedShopLinks,
        )
        assertEquals(
            listOf(PartImage(storagePath = "images/cassette.jpg", displayOrder = 0, isThumbnail = true)),
            repository.savedImages,
        )
    }
}

private class UpdatingPartRepository : PartRepository {
    var savedAttributes = emptyList<PartAttribute>()
    var savedBikeIds = emptySet<Long>()
    var savedShopLinks = emptyList<PartShopLink>()
    var savedImages = emptyList<PartImage>()

    override fun observeParts(): Flow<List<PartWithRelations>> = emptyFlow()

    override fun observePart(partId: Long): Flow<PartWithRelations?> = emptyFlow()

    override suspend fun savePart(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long {
        savedAttributes = attributes
        savedBikeIds = bikeIds
        savedShopLinks = shopLinks
        return part.id
    }

    override suspend fun savePartWithImages(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
        images: List<PartImage>,
    ): Long {
        savedImages = images
        return savePart(part, attributes, bikeIds, shopLinks)
    }

    override suspend fun deletePart(part: Part) = Unit
}
