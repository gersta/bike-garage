package de.gerritstapper.bikegarage.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.gerritstapper.bikegarage.data.local.entity.BikeEntity
import de.gerritstapper.bikegarage.data.local.entity.BikeImageEntity
import de.gerritstapper.bikegarage.data.local.entity.BikePartCrossRefEntity
import de.gerritstapper.bikegarage.data.local.entity.PartAttributeEntity
import de.gerritstapper.bikegarage.data.local.entity.PartEntity
import de.gerritstapper.bikegarage.data.local.entity.PartShopLinkEntity
import de.gerritstapper.bikegarage.data.local.entity.ShopEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `relations return images attributes assignments and shop links`() = runBlocking {
        val shopId = database.shopDao().insert(
            ShopEntity(name = "Radladen", logoPath = "images/shop.jpg", createdAt = 1),
        )
        val bikeId = database.bikeDao().insert(
            BikeEntity(brand = "Canyon", model = "Grizl", shopId = shopId, createdAt = 2),
        )
        database.bikeDao().insertImages(
            listOf(
                BikeImageEntity(bikeId = bikeId, storagePath = "images/first.jpg", displayOrder = 0, isThumbnail = true),
                BikeImageEntity(bikeId = bikeId, storagePath = "images/second.jpg", displayOrder = 1, isThumbnail = false),
            ),
        )
        val partId = database.partDao().insert(
            PartEntity(category = "DRIVETRAIN", name = "Kette", createdAt = 3),
        )
        database.partDao().replaceRelations(
            partId = partId,
            attributes = listOf(PartAttributeEntity(partId = partId, attributeKey = "links", value = "126")),
            bikeAssignments = listOf(BikePartCrossRefEntity(bikeId = bikeId, partId = partId)),
            shopLinks = listOf(PartShopLinkEntity(partId = partId, shopId = shopId, productUrl = "https://example.test/kette")),
        )

        val bike = database.bikeDao().observeAllWithImages().first().single()
        val part = database.partDao().observeAllWithRelations().first().single()
        val shop = database.shopDao().observeWithRelations(shopId).first()

        assertEquals(2, bike.images.size)
        assertEquals(partId, part.part.id)
        assertEquals(listOf("links"), part.attributes.map { it.attributeKey })
        assertEquals(listOf(bikeId), part.bikes.map { it.id })
        assertEquals(listOf(shopId), part.shops.map { it.id })
        assertEquals(listOf(partId), shop?.parts?.map { it.id })
        assertEquals(listOf(bikeId), shop?.purchasedBikes?.map { it.id })
    }

    @Test
    fun `replace relations atomically replaces every relation set`() = runBlocking {
        val firstBike = database.bikeDao().insert(
            BikeEntity(brand = "BMC", model = "Roadmachine", createdAt = 1),
        )
        val secondBike = database.bikeDao().insert(
            BikeEntity(brand = "Trek", model = "Domane", createdAt = 2),
        )
        val partId = database.partDao().insert(
            PartEntity(category = "WHEELS", name = "Felge", createdAt = 3),
        )
        database.partDao().replaceRelations(
            partId,
            listOf(PartAttributeEntity(partId = partId, attributeKey = "spokes", value = "28")),
            listOf(BikePartCrossRefEntity(firstBike, partId)),
            emptyList(),
        )
        database.partDao().replaceRelations(
            partId,
            listOf(PartAttributeEntity(partId = partId, attributeKey = "spokes", value = "32")),
            listOf(BikePartCrossRefEntity(secondBike, partId)),
            emptyList(),
        )

        val part = database.partDao().observeWithRelations(partId).first()

        assertEquals(listOf("32"), part?.attributes?.map { it.value })
        assertEquals(listOf(secondBike), part?.bikes?.map { it.id })
        assertTrue(part?.bikes?.none { it.id == firstBike } == true)
    }

    @Test
    fun `shops include a shop that is only a bike purchase location`() = runBlocking {
        val shopId = database.shopDao().insert(
            ShopEntity(name = "Fahrradhaus", logoPath = "images/fahrradhaus.jpg", createdAt = 1),
        )
        database.bikeDao().insert(
            BikeEntity(brand = "Rose", model = "Backroad", shopId = shopId, createdAt = 2),
        )

        val shops = database.shopDao().observeAll().first()

        assertEquals(listOf(shopId), shops.map { it.id })
    }

    @Test
    fun `guarded shop deletion rejects references and deletes an unlinked shop`() = runBlocking {
        val purchaseLinkedShop = ShopEntity(
            name = "Radladen",
            logoPath = "images/purchase-linked.jpg",
            createdAt = 1,
        )
        val purchaseLinkedShopId = database.shopDao().insert(purchaseLinkedShop)
        database.bikeDao().insert(
            BikeEntity(brand = "Canyon", model = "Grizl", shopId = purchaseLinkedShopId, createdAt = 2),
        )
        val productLinkedShop = ShopEntity(
            name = "Teilemarkt",
            logoPath = "images/product-linked.jpg",
            createdAt = 3,
        )
        val productLinkedShopId = database.shopDao().insert(productLinkedShop)
        val partId = database.partDao().insert(
            PartEntity(category = "DRIVETRAIN", name = "Kette", createdAt = 4),
        )
        database.partDao().replaceRelations(
            partId = partId,
            attributes = emptyList(),
            bikeAssignments = emptyList(),
            shopLinks = listOf(
                PartShopLinkEntity(
                    partId = partId,
                    shopId = productLinkedShopId,
                    productUrl = "https://example.test/kette",
                ),
            ),
        )
        val unlinkedShop = ShopEntity(name = "Freier Shop", logoPath = "images/free.jpg", createdAt = 5)
        val unlinkedShopId = database.shopDao().insert(unlinkedShop)

        val purchaseLinkedDeleted = database.shopDao()
            .deleteIfUnlinked(purchaseLinkedShop.copy(id = purchaseLinkedShopId))
        val productLinkedDeleted = database.shopDao()
            .deleteIfUnlinked(productLinkedShop.copy(id = productLinkedShopId))
        val unlinkedDeleted = database.shopDao().deleteIfUnlinked(unlinkedShop.copy(id = unlinkedShopId))

        assertTrue(!purchaseLinkedDeleted)
        assertTrue(!productLinkedDeleted)
        assertTrue(unlinkedDeleted)
        assertEquals(
            purchaseLinkedShopId,
            database.shopDao().observeWithRelations(purchaseLinkedShopId).first()?.shop?.id,
        )
        assertEquals(
            productLinkedShopId,
            database.shopDao().observeWithRelations(productLinkedShopId).first()?.shop?.id,
        )
        assertEquals(null, database.shopDao().observeWithRelations(unlinkedShopId).first())
    }
}
