package de.gerritstapper.bikegarage.feature.part

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.model.BikeWithParts
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import de.gerritstapper.bikegarage.domain.usecase.GetBikesUseCase
import de.gerritstapper.bikegarage.domain.usecase.GetShopsUseCase
import de.gerritstapper.bikegarage.domain.usecase.UpdatePartUseCase
import de.gerritstapper.bikegarage.ui.navigation.PART_ID_ARGUMENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PartEditViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initializes selected technical fields and retains obsolete values until save`() =
        runTest(dispatcher) {
            val firstBike = Bike(id = 3, brand = "Canyon", model = "Grizl", createdAt = 1)
            val secondBike = Bike(id = 9, brand = "Trek", model = "Domane", createdAt = 2)
            val part = Part(
                id = 7,
                category = PartCategory.DRIVETRAIN,
                type = "Kette",
                name = "CN-M8100",
                createdAt = 3,
                inventoryCount = 6,
            )
            val repository = EditablePartRepository(
                PartWithRelations(
                    part = part,
                    attributes = listOf(
                        PartAttribute(key = "link_count", value = "116"),
                        PartAttribute(key = "gear_count", value = "12"),
                    ),
                    bikes = listOf(firstBike),
                    shopLinks = emptyList(),
                ),
            )
            val viewModel = PartEditViewModel(
                savedStateHandle = SavedStateHandle(mapOf(PART_ID_ARGUMENT to part.id)),
                partRepository = repository,
                updatePart = UpdatePartUseCase(repository),
                getBikes = GetBikesUseCase(EditBikeRepository(listOf(firstBike, secondBike))),
                getShops = GetShopsUseCase(EditShopRepository),
                imageStorage = EditImageStorage,
            )
            val events = mutableListOf<PartEditEvent>()
            val collector = launch { viewModel.events.toList(events) }

            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("Kette", viewModel.uiState.value.type)
            assertEquals("6", viewModel.uiState.value.inventoryCount)
            assertEquals(setOf(firstBike.id), viewModel.uiState.value.selectedBikeIds)

            viewModel.onTypeSelected("Kassette")
            assertEquals("6", viewModel.uiState.value.inventoryCount)
            assertEquals(
                mapOf(
                    "link_count" to "116",
                    "gear_count" to "12",
                    "sprocket_count" to "",
                    "gear_ratio" to "",
                    "freehub" to "",
                ),
                viewModel.uiState.value.technicalAttributes,
            )
            viewModel.onTypeSelected("Kette")
            assertFalse(viewModel.uiState.value.isDirty)

            viewModel.onCategorySelected(PartCategory.COCKPIT)
            assertNull(viewModel.uiState.value.type)
            assertEquals(
                mapOf(
                    "link_count" to "116",
                    "gear_count" to "12",
                    "sprocket_count" to "",
                    "gear_ratio" to "",
                    "freehub" to "",
                ),
                viewModel.uiState.value.technicalAttributes,
            )

            viewModel.onTypeSelected("Lenker")
            assertEquals(
                mapOf(
                    "link_count" to "116",
                    "gear_count" to "12",
                    "sprocket_count" to "",
                    "gear_ratio" to "",
                    "freehub" to "",
                    "onepiece" to "",
                    "diameter" to "",
                ),
                viewModel.uiState.value.technicalAttributes,
            )
            viewModel.onTechnicalAttributeChanged("onepiece", "Ja")
            viewModel.toggleBikeSelection(secondBike.id)

            assertTrue(viewModel.uiState.value.isDirty)
            assertEquals(
                listOf("Anzahl Glieder", "Anzahl Gänge"),
                viewModel.uiState.value.technicalValuesRemovedOnSave,
            )

            viewModel.save()
            advanceUntilIdle()

            assertEquals("Lenker", repository.savedPart?.type)
            assertEquals(
                listOf(PartAttribute(key = "onepiece", value = "Ja")),
                repository.savedAttributes,
            )
            assertEquals(setOf(firstBike.id, secondBike.id), repository.savedBikeIds)
            assertEquals(listOf(PartEditEvent.Updated(repository.savedPart!!)), events)
            collector.cancel()
        }
}

private class EditablePartRepository(
    private val details: PartWithRelations,
) : PartRepository {
    var savedPart: Part? = null
    var savedAttributes = emptyList<PartAttribute>()
    var savedBikeIds = emptySet<Long>()

    override fun observeParts(): Flow<List<PartWithRelations>> = emptyFlow()

    override fun observePart(partId: Long): Flow<PartWithRelations?> = flowOf(details)

    override suspend fun savePart(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long {
        savedPart = part
        savedAttributes = attributes
        savedBikeIds = bikeIds
        return part.id
    }

    override suspend fun deletePart(part: Part) = Unit
}

private class EditBikeRepository(
    bikes: List<Bike>,
) : BikeRepository {
    private val bikeWithImages = bikes.map { BikeWithImages(it, emptyList<BikeImage>()) }

    override fun observeBikes(): Flow<List<BikeWithImages>> = flowOf(bikeWithImages)

    override fun observeBike(bikeId: Long): Flow<BikeWithImages?> = emptyFlow()

    override suspend fun bikeWithParts(bikeId: Long): BikeWithParts? = null

    override suspend fun saveBike(bike: Bike, images: List<BikeImage>): Long = bike.id

    override suspend fun deleteBike(bike: Bike) = Unit

    override suspend fun deleteBikeWithSelectedParts(bike: Bike, partIdsToDelete: Set<Long>) = Unit
}

private object EditShopRepository : ShopRepository {
    override fun observeShops(): Flow<List<Shop>> = emptyFlow()

    override fun observeShop(shopId: Long): Flow<ShopWithRelations?> = emptyFlow()

    override suspend fun saveShop(shop: Shop): Long = shop.id

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean = false
}

private object EditImageStorage : ImageStorage {
    override fun newCameraCapture(): PendingImage = error("Nicht benötigt")

    override suspend fun import(source: Uri): String = error("Nicht benötigt")

    override suspend fun delete(storagePath: String) = Unit

    override suspend fun discard(pendingImage: PendingImage) = Unit
}
