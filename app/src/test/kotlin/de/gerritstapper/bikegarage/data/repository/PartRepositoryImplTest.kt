package de.gerritstapper.bikegarage.data.repository

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.data.local.AppDatabase
import de.gerritstapper.bikegarage.data.local.entity.BikeEntity
import de.gerritstapper.bikegarage.data.local.entity.ShopEntity
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartImage
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PartRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: PartRepositoryImpl
    private lateinit var imageStorage: RecordingImageStorage

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        imageStorage = RecordingImageStorage()
        repository = PartRepositoryImpl(database, database.partDao(), imageStorage)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `saves part attributes bike assignments and product links together`() = runBlocking {
        val bikeId = database.bikeDao().insert(
            BikeEntity(brand = "Canyon", model = "Grizl", createdAt = 1),
        )
        val shopId = database.shopDao().insert(
            ShopEntity(name = "Radhaus", logoPath = "images/radhaus.jpg", createdAt = 2),
        )

        val partId = repository.savePart(
            part = Part(
                category = PartCategory.DRIVETRAIN,
                type = "Kette",
                name = "CN-M8100",
                createdAt = 3,
            ),
            attributes = listOf(PartAttribute(key = "link_count", value = "116")),
            bikeIds = setOf(bikeId),
            shopLinks = listOf(
                PartShopLink(shopId = shopId, productUrl = "https://example.test/kette"),
            ),
        )

        val saved = repository.observePart(partId).first()
        assertEquals("CN-M8100", saved?.part?.name)
        assertEquals(listOf("116"), saved?.attributes?.map { it.value })
        assertEquals(listOf(bikeId), saved?.bikes?.map { it.id })
        assertEquals(listOf(shopId), saved?.shopLinks?.map { it.link.shopId })
        assertEquals("Radhaus", saved?.shopLinks?.single()?.shop?.name)
    }

    @Test
    fun `saves nullable inventory count and image metadata`() = runBlocking {
        val partId = repository.savePartWithImages(
            part = Part(
                category = PartCategory.BRAKES,
                name = "Bremsbelag",
                createdAt = 1,
                inventoryCount = 4,
            ),
            attributes = emptyList(),
            bikeIds = emptySet(),
            shopLinks = emptyList(),
            images = listOf(
                PartImage(
                    storagePath = "images/pads-front.jpg",
                    displayOrder = 0,
                    isThumbnail = true,
                ),
                PartImage(
                    storagePath = "images/pads-back.jpg",
                    displayOrder = 1,
                    isThumbnail = false,
                ),
            ),
        )

        val saved = repository.observePart(partId).first()

        assertEquals(4, saved?.part?.inventoryCount)
        assertEquals(
            listOf("images/pads-front.jpg", "images/pads-back.jpg"),
            saved?.images?.map { it.storagePath },
        )
        assertEquals(listOf(true, false), saved?.images?.map { it.isThumbnail })
    }

    @Test
    fun `rejects a negative inventory count`() {
        assertThrows(IllegalArgumentException::class.java) {
            Part(
                category = PartCategory.BRAKES,
                name = "Bremsbelag",
                createdAt = 1,
                inventoryCount = -1,
            )
        }
    }

    @Test
    fun `observes unassigned parts alongside parts assigned to bikes`() = runBlocking {
        val bikeId = database.bikeDao().insert(
            BikeEntity(brand = "Canyon", model = "Grizl", createdAt = 1),
        )
        val assignedPartId = repository.savePart(
            part = Part(
                category = PartCategory.DRIVETRAIN,
                name = "Kette",
                createdAt = 2,
            ),
            attributes = emptyList(),
            bikeIds = setOf(bikeId),
            shopLinks = emptyList(),
        )
        val unassignedPartId = repository.savePart(
            part = Part(
                category = PartCategory.BRAKES,
                name = "Scheibenbremse",
                createdAt = 3,
            ),
            attributes = emptyList(),
            bikeIds = emptySet(),
            shopLinks = emptyList(),
        )

        val parts = repository.observeParts().first()

        assertEquals(setOf(assignedPartId, unassignedPartId), parts.map { it.part.id }.toSet())
        assertEquals(
            emptyList<Long>(),
            parts.single { it.part.id == unassignedPartId }.bikes.map { it.id },
        )
        assertEquals(
            listOf(bikeId),
            parts.single { it.part.id == assignedPartId }.bikes.map { it.id },
        )
    }

    @Test
    fun `rolls back a new part when a relation cannot be persisted`() = runBlocking {
        runCatching {
            repository.savePart(
                part = Part(
                    category = PartCategory.BRAKES,
                    name = "Bremse",
                    createdAt = 1,
                ),
                attributes = listOf(PartAttribute(key = "mount", value = "flat")),
                bikeIds = emptySet(),
                shopLinks = listOf(
                    PartShopLink(shopId = 999, productUrl = "https://example.test/brake"),
                ),
            )
        }.onSuccess {
            throw AssertionError("Eine ungültige Shop-Verknüpfung muss fehlschlagen.")
        }

        assertTrue(repository.observeParts().first().isEmpty())
    }

    @Test
    fun `rolls back an edited part and its relation replacement when saving fails`() = runBlocking {
        val firstBikeId = database.bikeDao().insert(
            BikeEntity(brand = "Canyon", model = "Grizl", createdAt = 1),
        )
        val secondBikeId = database.bikeDao().insert(
            BikeEntity(brand = "Trek", model = "Domane", createdAt = 2),
        )
        val part = Part(
            category = PartCategory.DRIVETRAIN,
            type = "Kette",
            name = "CN-M8100",
            createdAt = 3,
        )
        val partId = repository.savePart(
            part = part,
            attributes = listOf(PartAttribute(key = "link_count", value = "116")),
            bikeIds = setOf(firstBikeId),
            shopLinks = emptyList(),
        )

        runCatching {
            repository.savePart(
                part = part.copy(id = partId, type = "Kassette"),
                attributes = listOf(PartAttribute(key = "sprocket_count", value = "12")),
                bikeIds = setOf(secondBikeId),
                shopLinks = listOf(
                    PartShopLink(shopId = 999, productUrl = "https://example.test/kassette"),
                ),
            )
        }.onSuccess {
            throw AssertionError("Eine ungültige Shop-Verknüpfung muss fehlschlagen.")
        }

        val saved = repository.observePart(partId).first()
        assertEquals("Kette", saved?.part?.type)
        assertEquals(listOf("link_count"), saved?.attributes?.map { it.key })
        assertEquals(listOf(firstBikeId), saved?.bikes?.map { it.id })
    }

    @Test
    fun `assigns an existing part idempotently without replacing its other relations`() = runBlocking {
        val firstBikeId = database.bikeDao().insert(
            BikeEntity(brand = "Canyon", model = "Grizl", createdAt = 1),
        )
        val secondBikeId = database.bikeDao().insert(
            BikeEntity(brand = "Trek", model = "Domane", createdAt = 2),
        )
        val shopId = database.shopDao().insert(
            ShopEntity(name = "Radhaus", logoPath = "images/radhaus.jpg", createdAt = 3),
        )
        val partId = repository.savePart(
            part = Part(
                category = PartCategory.DRIVETRAIN,
                type = "Kette",
                name = "CN-M8100",
                createdAt = 4,
            ),
            attributes = listOf(PartAttribute(key = "link_count", value = "116")),
            bikeIds = setOf(firstBikeId),
            shopLinks = listOf(
                PartShopLink(shopId = shopId, productUrl = "https://example.test/kette"),
            ),
        )

        repository.assignPartToBike(partId, secondBikeId)
        repository.assignPartToBike(partId, secondBikeId)

        val saved = repository.observePart(partId).first()
        assertEquals(
            setOf(firstBikeId, secondBikeId),
            saved?.bikes?.map { it.id }?.toSet(),
        )
        assertEquals(listOf("link_count"), saved?.attributes?.map { it.key })
        assertEquals(listOf("116"), saved?.attributes?.map { it.value })
        assertEquals(listOf(shopId), saved?.shopLinks?.map { it.link.shopId })
        assertEquals(2, rowCount("bike_part_cross_refs"))
    }

    @Test
    fun `deletes a part and every relation while preserving assigned bikes and shops`() = runBlocking {
        val firstBikeId = database.bikeDao().insert(
            BikeEntity(brand = "Canyon", model = "Grizl", createdAt = 1),
        )
        val secondBikeId = database.bikeDao().insert(
            BikeEntity(brand = "Trek", model = "Domane", createdAt = 2),
        )
        val shopId = database.shopDao().insert(
            ShopEntity(name = "Radhaus", logoPath = "images/radhaus.jpg", createdAt = 3),
        )
        val partId = repository.savePart(
            part = Part(
                category = PartCategory.DRIVETRAIN,
                type = "Kette",
                name = "CN-M8100",
                createdAt = 4,
            ),
            attributes = listOf(PartAttribute(key = "link_count", value = "116")),
            bikeIds = setOf(firstBikeId, secondBikeId),
            shopLinks = listOf(
                PartShopLink(shopId = shopId, productUrl = "https://example.test/kette"),
            ),
        )

        repository.deletePart(
            Part(
                id = partId,
                category = PartCategory.DRIVETRAIN,
                name = "CN-M8100",
                createdAt = 4,
            ),
        )

        assertTrue(repository.observeParts().first().isEmpty())
        assertEquals(
            listOf(secondBikeId, firstBikeId),
            database.bikeDao().observeAllWithImages().first().map { it.bike.id },
        )
        assertEquals(
            shopId,
            database.shopDao().observeWithRelations(shopId).first()?.shop?.id,
        )
        assertEquals(0, rowCount("part_attributes"))
        assertEquals(0, rowCount("bike_part_cross_refs"))
        assertEquals(0, rowCount("part_shop_links"))
    }

    @Test
    fun `deletes stored part images after deleting the part`() = runBlocking {
        val part = Part(
            category = PartCategory.ACCESSORIES,
            name = "Licht",
            createdAt = 1,
        )
        val partId = repository.savePartWithImages(
            part = part,
            attributes = emptyList(),
            bikeIds = emptySet(),
            shopLinks = emptyList(),
            images = listOf(
                PartImage(
                    storagePath = "images/light.jpg",
                    displayOrder = 0,
                    isThumbnail = true,
                ),
            ),
        )

        repository.deletePart(part.copy(id = partId))

        assertEquals(listOf("images/light.jpg"), imageStorage.deletedPaths)
        assertEquals(0, rowCount("part_images"))
    }

    private fun rowCount(table: String): Int =
        database.openHelper.writableDatabase
            .query("SELECT COUNT(*) FROM $table")
            .use { cursor ->
                check(cursor.moveToFirst())
                cursor.getInt(0)
            }
}

private class RecordingImageStorage : ImageStorage {
    val deletedPaths = mutableListOf<String>()

    override fun newCameraCapture(): PendingImage = error("Nicht benötigt")

    override suspend fun import(source: Uri): String = error("Nicht benötigt")

    override suspend fun delete(storagePath: String) {
        deletedPaths += storagePath
    }

    override suspend fun discard(pendingImage: PendingImage) = error("Nicht benötigt")
}
