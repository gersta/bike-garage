package de.gerritstapper.bikegarage.feature.bike

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.model.BikeWithParts
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import de.gerritstapper.bikegarage.domain.usecase.GetShopsUseCase
import de.gerritstapper.bikegarage.domain.usecase.UpdateBikeUseCase
import de.gerritstapper.bikegarage.ui.navigation.BIKE_ID_ARGUMENT
import java.util.concurrent.CopyOnWriteArrayList
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BikeEditViewModelTest {
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
    fun `persists bike changes before removing obsolete images and opens details`() = runTest(dispatcher) {
        val operations = CopyOnWriteArrayList<String>()
        val bikeRepository = EditBikeRepository(existingBike, operations)
        val viewModel = BikeEditViewModel(
            savedStateHandle = SavedStateHandle(mapOf(BIKE_ID_ARGUMENT to existingBike.id)),
            bikeRepository = bikeRepository,
            updateBike = UpdateBikeUseCase(bikeRepository),
            getShops = GetShopsUseCase(EmptyShopRepository),
            imageStorage = EditRecordingImageStorage(operations),
        )
        val events = mutableListOf<BikeEditEvent>()
        val eventCollector = launch { viewModel.events.toList(events) }

        advanceUntilIdle()
        viewModel.onBrandChanged("Canyon")
        viewModel.removeImage("images/remove.jpg")
        viewModel.completeCameraCapture(
            pendingImage = PendingImage("images/new.jpg", Uri.parse("content://test/new")),
            isSaved = true,
        )
        advanceUntilIdle()
        viewModel.selectThumbnail("images/new.jpg")
        assertTrue(viewModel.uiState.value.isDirty)
        assertEquals(emptyList<String>(), operations)
        viewModel.save()
        advanceUntilIdle()

        val updatedBike = existingBike.copy(brand = "Canyon")
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("save", "delete:images/remove.jpg"), operations)
        assertEquals(listOf(updatedBike), bikeRepository.savedBikes)
        assertEquals(
            listOf(
                BikeImage(storagePath = "images/keep.jpg", displayOrder = 0, isThumbnail = false),
                BikeImage(storagePath = "images/new.jpg", displayOrder = 1, isThumbnail = true),
            ),
            bikeRepository.savedImages,
        )
        assertEquals(listOf(BikeEditEvent.Updated(updatedBike)), events)
        eventCollector.cancel()
    }

    private companion object {
        val existingBike = Bike(
            id = 7,
            brand = "Rose",
            model = "Backroad",
            createdAt = 123,
        )
    }
}

private class EditBikeRepository(
    private val bike: Bike,
    private val operations: MutableList<String>,
) : BikeRepository {
    val savedBikes = mutableListOf<Bike>()
    var savedImages = emptyList<BikeImage>()

    override fun observeBikes(): Flow<List<BikeWithImages>> = emptyFlow()

    override fun observeBike(bikeId: Long): Flow<BikeWithImages?> = flowOf(
        BikeWithImages(
            bike = bike,
            images = listOf(
                BikeImage(
                    id = 1,
                    bikeId = bike.id,
                    storagePath = "images/keep.jpg",
                    displayOrder = 0,
                    isThumbnail = true,
                ),
                BikeImage(
                    id = 2,
                    bikeId = bike.id,
                    storagePath = "images/remove.jpg",
                    displayOrder = 1,
                    isThumbnail = false,
                ),
            ),
        ),
    )

    override suspend fun bikeWithParts(bikeId: Long): BikeWithParts? = null

    override suspend fun saveBike(bike: Bike, images: List<BikeImage>): Long {
        operations += "save"
        savedBikes += bike
        savedImages = images
        return bike.id
    }

    override suspend fun deleteBike(bike: Bike) = Unit

    override suspend fun deleteBikeWithSelectedParts(bike: Bike, partIdsToDelete: Set<Long>) = Unit
}

private object EmptyShopRepository : ShopRepository {
    override fun observeShops(): Flow<List<Shop>> = emptyFlow()

    override fun observeShop(shopId: Long): Flow<ShopWithRelations?> = emptyFlow()

    override suspend fun saveShop(shop: Shop): Long = shop.id

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean = false
}

private class EditRecordingImageStorage(
    private val operations: MutableList<String>,
) : ImageStorage {
    override fun newCameraCapture(): PendingImage = error("Nicht benötigt")

    override suspend fun import(source: Uri): String = error("Nicht benötigt")

    override suspend fun delete(storagePath: String) {
        operations += "delete:$storagePath"
    }

    override suspend fun discard(pendingImage: PendingImage) = Unit
}
