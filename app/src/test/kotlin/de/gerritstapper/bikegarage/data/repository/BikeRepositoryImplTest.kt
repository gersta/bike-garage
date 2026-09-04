package de.gerritstapper.bikegarage.data.repository

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.data.local.AppDatabase
import de.gerritstapper.bikegarage.data.local.entity.BikeEntity
import de.gerritstapper.bikegarage.data.local.entity.BikePartCrossRefEntity
import de.gerritstapper.bikegarage.data.local.entity.PartAttributeEntity
import de.gerritstapper.bikegarage.data.local.entity.PartEntity
import de.gerritstapper.bikegarage.data.local.entity.PartShopLinkEntity
import de.gerritstapper.bikegarage.data.local.entity.ShopEntity
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BikeRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: BikeRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = BikeRepositoryImpl(
            database = database,
            bikeDao = database.bikeDao(),
            imageStorage = NoOpImageStorage,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `persists a bike and all image metadata in one save operation`() = runBlocking {
        val bikeId = repository.saveBike(
            bike = Bike(brand = "Canyon", model = "Grizl", createdAt = 1),
            images = listOf(
                BikeImage(storagePath = "images/front.jpg", displayOrder = 0, isThumbnail = true),
                BikeImage(storagePath = "images/side.jpg", displayOrder = 1, isThumbnail = false),
            ),
        )

        val persistedBike = repository.observeBike(bikeId).first()

        assertEquals("Canyon", persistedBike?.bike?.brand)
        assertEquals(
            listOf("images/front.jpg", "images/side.jpg"),
            persistedBike?.images?.map { it.storagePath },
        )
        assertEquals(
            listOf(true, false),
            persistedBike?.images?.map { it.isThumbnail },
        )
    }

    @Test
    fun `deletes selected linked parts and their relations while retaining other bikes and parts`() = runBlocking {
        val shopId = database.shopDao().insert(
            ShopEntity(name = "Radladen", logoPath = "images/shop.jpg", createdAt = 1),
        )
        val bikeToDeleteId = database.bikeDao().insert(
            BikeEntity(brand = "Canyon", model = "Grizl", createdAt = 2),
        )
        val retainedBikeId = database.bikeDao().insert(
            BikeEntity(brand = "Trek", model = "Domane", createdAt = 3),
        )
        val selectedPartId = database.partDao().insert(
            PartEntity(category = "DRIVETRAIN", name = "Kette", createdAt = 4),
        )
        val retainedPartId = database.partDao().insert(
            PartEntity(category = "BRAKES", name = "Bremse", createdAt = 5),
        )
        val unrelatedPartId = database.partDao().insert(
            PartEntity(category = "WHEELS", name = "Felge", createdAt = 6),
        )
        database.partDao().replaceRelations(
            partId = selectedPartId,
            attributes = listOf(
                PartAttributeEntity(partId = selectedPartId, attributeKey = "links", value = "126"),
            ),
            bikeAssignments = listOf(
                BikePartCrossRefEntity(bikeToDeleteId, selectedPartId),
                BikePartCrossRefEntity(retainedBikeId, selectedPartId),
            ),
            shopLinks = listOf(
                PartShopLinkEntity(
                    partId = selectedPartId,
                    shopId = shopId,
                    productUrl = "https://example.test/kette",
                ),
            ),
        )
        database.partDao().replaceRelations(
            partId = retainedPartId,
            attributes = listOf(
                PartAttributeEntity(partId = retainedPartId, attributeKey = "type", value = "disc"),
            ),
            bikeAssignments = listOf(BikePartCrossRefEntity(bikeToDeleteId, retainedPartId)),
            shopLinks = emptyList(),
        )

        repository.deleteBikeWithSelectedParts(
            bike = Bike(id = bikeToDeleteId, brand = "Canyon", model = "Grizl", createdAt = 2),
            partIdsToDelete = setOf(selectedPartId, unrelatedPartId),
        )

        assertNull(database.bikeDao().observeWithImages(bikeToDeleteId).first())
        assertNull(database.partDao().observeWithRelations(selectedPartId).first())
        val retainedPart = database.partDao().observeWithRelations(retainedPartId).first()
        assertEquals(retainedPartId, retainedPart?.part?.id)
        assertEquals(emptyList<Long>(), retainedPart?.bikes?.map { it.id })
        assertEquals(listOf("type"), retainedPart?.attributes?.map { it.attributeKey })
        assertNotNull(database.partDao().observeWithRelations(unrelatedPartId).first())
        val retainedBike = database.bikeDao().getWithParts(retainedBikeId)
        assertEquals(retainedBikeId, retainedBike?.bike?.id)
        assertEquals(emptyList<Long>(), retainedBike?.parts?.map { it.id })
        val shop = database.shopDao().observeWithRelations(shopId).first()
        assertEquals(emptyList<Long>(), shop?.partLinks?.map { it.partId })
    }
}

private object NoOpImageStorage : ImageStorage {
    override fun newCameraCapture(): PendingImage = error("Not used in this test")

    override suspend fun import(source: Uri): String = error("Not used in this test")

    override suspend fun delete(storagePath: String) = Unit

    override suspend fun discard(pendingImage: PendingImage) = Unit
}
