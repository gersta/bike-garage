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
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import de.gerritstapper.bikegarage.domain.usecase.CreatePartUseCase
import de.gerritstapper.bikegarage.domain.usecase.GetBikesUseCase
import de.gerritstapper.bikegarage.domain.usecase.GetShopsUseCase
import de.gerritstapper.bikegarage.ui.navigation.BIKE_ID_ARGUMENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PartCreationViewModelTest {
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
    fun `seeds source bike and replaces technical fields when part type changes`() = runTest(dispatcher) {
        val repository = MutablePartRepository()
        val viewModel = PartCreationViewModel(
            savedStateHandle = SavedStateHandle(mapOf(BIKE_ID_ARGUMENT to 7L)),
            createPart = CreatePartUseCase(repository),
            getBikes = GetBikesUseCase(FakeBikeRepository),
            getShops = GetShopsUseCase(FakeShopRepository),
            imageStorage = CreationImageStorage,
        )

        viewModel.onCategorySelected(PartCategory.DRIVETRAIN)
        viewModel.onTypeSelected("Kette")
        viewModel.onTechnicalAttributeChanged("link_count", "116")
        viewModel.onInventoryCountChanged("3")
        viewModel.onTypeSelected("Kassette")

        assertEquals(setOf(7L), viewModel.uiState.value.selectedBikeIds)
        assertEquals(
            mapOf("sprocket_count" to "", "gear_ratio" to "", "freehub" to ""),
            viewModel.uiState.value.technicalAttributes,
        )
        assertEquals("3", viewModel.uiState.value.inventoryCount)
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun `assigns a shop created from the intended product link and saves`() = runTest(dispatcher) {
        val repository = MutablePartRepository()
        val viewModel = PartCreationViewModel(
            savedStateHandle = SavedStateHandle(),
            createPart = CreatePartUseCase(repository),
            getBikes = GetBikesUseCase(FakeBikeRepository),
            getShops = GetShopsUseCase(FakeShopRepository),
            imageStorage = CreationImageStorage,
        )
        val events = mutableListOf<PartCreationEvent>()
        val collector = launch { viewModel.events.collect { events += it } }
        advanceUntilIdle()

        viewModel.onCategorySelected(PartCategory.DRIVETRAIN)
        viewModel.onTypeSelected("Kette")
        viewModel.onNameChanged("CN-M8100")
        viewModel.onInventoryCountChanged("4")
        viewModel.addProductLink()
        val linkId = viewModel.uiState.value.productLinks.single().id
        viewModel.createShopForProductLink(linkId)
        viewModel.selectCreatedShop(9)
        viewModel.onProductUrlChanged(linkId, "https://example.test/kette")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(9L, repository.savedLinks.single().shopId)
        assertEquals(4, repository.savedPart?.inventoryCount)
        assertEquals(listOf(PartCreationEvent.Created(42)), events)
        assertFalse(viewModel.uiState.value.isSaving)
        collector.cancel()
    }
}

private class MutablePartRepository : PartRepository {
    var saveCalls = 0
    var savedLinks = emptyList<PartShopLink>()
    var savedPart: Part? = null

    override fun observeParts(): Flow<List<PartWithRelations>> = emptyFlow()

    override fun observePart(partId: Long): Flow<PartWithRelations?> = emptyFlow()

    override suspend fun savePart(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long {
        saveCalls++
        savedPart = part
        savedLinks = shopLinks
        return 42
    }

    override suspend fun deletePart(part: Part) = Unit
}

private object FakeBikeRepository : BikeRepository {
    override fun observeBikes(): Flow<List<BikeWithImages>> = MutableStateFlow(
        listOf(
            BikeWithImages(
                bike = Bike(id = 7, brand = "Canyon", model = "Grizl", createdAt = 1),
                images = emptyList<BikeImage>(),
            ),
        ),
    )

    override fun observeBike(bikeId: Long): Flow<BikeWithImages?> = emptyFlow()

    override suspend fun bikeWithParts(bikeId: Long): BikeWithParts? = null

    override suspend fun saveBike(bike: Bike, images: List<BikeImage>): Long = error("Nicht benötigt")

    override suspend fun deleteBike(bike: Bike) = Unit

    override suspend fun deleteBikeWithSelectedParts(bike: Bike, partIdsToDelete: Set<Long>) = Unit
}

private object FakeShopRepository : ShopRepository {
    override fun observeShops(): Flow<List<Shop>> = MutableStateFlow(
        listOf(Shop(id = 9, name = "Radhaus", logoPath = "images/radhaus.jpg", createdAt = 1)),
    )

    override fun observeShop(shopId: Long) = emptyFlow<de.gerritstapper.bikegarage.domain.model.ShopWithRelations?>()

    override suspend fun saveShop(shop: Shop): Long = error("Nicht benötigt")

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean = error("Nicht benötigt")
}

private object CreationImageStorage : ImageStorage {
    override fun newCameraCapture(): PendingImage = error("Nicht benötigt")

    override suspend fun import(source: Uri): String = error("Nicht benötigt")

    override suspend fun delete(storagePath: String) = Unit

    override suspend fun discard(pendingImage: PendingImage) = Unit
}
