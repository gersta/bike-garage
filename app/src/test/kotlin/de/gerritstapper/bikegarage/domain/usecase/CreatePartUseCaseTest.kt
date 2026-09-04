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
import org.junit.Assert.assertFalse
import org.junit.Test

class CreatePartUseCaseTest {
    @Test
    fun `requires category name and complete product links without saving`() = runBlocking {
        val repository = RecordingPartRepository()
        val useCase = CreatePartUseCase(repository)

        val result = useCase(
            category = null,
            type = null,
            name = " ",
            manufacturer = "",
            manufacturerNumber = "",
            description = "",
            technicalAttributes = emptyMap(),
            bikeIds = emptySet(),
            productLinks = listOf(PartProductLinkDraft(shopId = null, productUrl = "not-a-url")),
            inventoryCount = -1,
        ) as CreatePartResult.Invalid

        assertEquals(PartCreationFieldError.CATEGORY_REQUIRED, result.validation.categoryError)
        assertEquals(PartCreationFieldError.NAME_REQUIRED, result.validation.nameError)
        assertEquals(
            PartCreationFieldError.INVENTORY_COUNT_INVALID,
            result.validation.inventoryCountError,
        )
        assertEquals(
            PartCreationFieldError.SHOP_REQUIRED,
            result.validation.productLinkErrors.getValue(0).shopError,
        )
        assertEquals(
            PartCreationFieldError.PRODUCT_URL_INVALID,
            result.validation.productLinkErrors.getValue(0).productUrlError,
        )
        assertFalse(result.validation.isValid)
        assertEquals(emptyList<Part>(), repository.savedParts)
    }

    @Test
    fun `persists normalized optional data and only fields defined for selected type`() = runBlocking {
        val repository = RecordingPartRepository()
        val useCase = CreatePartUseCase(repository)

        val result = useCase(
            category = PartCategory.DRIVETRAIN,
            type = "Blatt",
            name = "  52T  ",
            manufacturer = "  Shimano ",
            manufacturerNumber = " FC-R8100 ",
            description = "  Ersatzteil ",
            technicalAttributes = mapOf(
                "gear_ratio" to " 52 ",
                "link_count" to " 116 ",
                "unused" to "ignored",
            ),
            bikeIds = setOf(3, 7),
            productLinks = listOf(
                PartProductLinkDraft(shopId = 9, productUrl = " https://example.test/chainring "),
            ),
            inventoryCount = 4,
            imagePaths = listOf("images/chainring.jpg"),
            thumbnailPath = "images/chainring.jpg",
        )

        val created = result as CreatePartResult.Created
        assertEquals(42, created.part.id)
        assertEquals("Kettenblatt", created.part.type)
        assertEquals("52T", created.part.name)
        assertEquals("Shimano", created.part.manufacturer)
        assertEquals("FC-R8100", created.part.manufacturerNumber)
        assertEquals("Ersatzteil", created.part.description)
        assertEquals(4, created.part.inventoryCount)
        assertEquals(listOf(PartAttribute(key = "gear_ratio", value = "52")), repository.savedAttributes)
        assertEquals(setOf(3L, 7L), repository.savedBikeIds)
        assertEquals(
            listOf(PartShopLink(shopId = 9, productUrl = "https://example.test/chainring")),
            repository.savedShopLinks,
        )
        assertEquals(
            listOf(
                PartImage(
                    storagePath = "images/chainring.jpg",
                    displayOrder = 0,
                    isThumbnail = true,
                ),
            ),
            repository.savedImages,
        )
    }
}

private class RecordingPartRepository : PartRepository {
    val savedParts = mutableListOf<Part>()
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
        savedParts += part
        savedAttributes = attributes
        savedBikeIds = bikeIds
        savedShopLinks = shopLinks
        return 42
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
