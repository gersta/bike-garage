package de.gerritstapper.bikegarage.feature.bike

import androidx.lifecycle.SavedStateHandle
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
import de.gerritstapper.bikegarage.domain.usecase.GetBikeDetailsUseCase
import de.gerritstapper.bikegarage.domain.usecase.SetBikeShopUseCase
import de.gerritstapper.bikegarage.ui.navigation.BIKE_ID_ARGUMENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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
class BikeDetailsViewModelTest {
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
    fun `reactively shows only associated parts and the selected shop`() = runTest(dispatcher) {
        val repository = DetailsBikeRepository(BikeWithImages(bike, listOf(image)))
        val partRepository = DetailsPartRepository(listOf(assignedPart, unrelatedPart))
        val shopRepository = DetailsShopRepository(listOf(shop))
        val viewModel = BikeDetailsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(BIKE_ID_ARGUMENT to bike.id)),
            getBikeDetails = GetBikeDetailsUseCase(repository, partRepository, shopRepository),
            setBikeShop = SetBikeShopUseCase(repository),
        )

        val collector = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(shop, viewModel.uiState.value.details?.shop)
        assertEquals(listOf(assignedPart), viewModel.uiState.value.details?.parts)

        partRepository.parts.value = listOf(unrelatedPart)
        shopRepository.shops.value = emptyList()
        advanceUntilIdle()

        assertEquals(emptyList<PartWithRelations>(), viewModel.uiState.value.details?.parts)
        assertEquals(null, viewModel.uiState.value.details?.shop)
        collector.cancel()
    }

    @Test
    fun `assigns a shop returned from shop creation without changing images`() = runTest(dispatcher) {
        val repository = DetailsBikeRepository(BikeWithImages(bike, listOf(image)))
        val shopRepository = DetailsShopRepository(listOf(shop))
        val viewModel = BikeDetailsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(BIKE_ID_ARGUMENT to bike.id)),
            getBikeDetails = GetBikeDetailsUseCase(
                repository,
                DetailsPartRepository(emptyList()),
                shopRepository,
            ),
            setBikeShop = SetBikeShopUseCase(repository),
        )
        val newShop = shop.copy(id = 11, name = "Neue Werkstatt")
        shopRepository.shops.value = listOf(shop, newShop)
        val collector = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.selectCreatedShop(newShop.id)
        advanceUntilIdle()

        assertEquals(newShop.id, repository.savedBike?.bike?.shopId)
        assertEquals(listOf(image), repository.savedBike?.images)
        assertEquals(newShop, viewModel.uiState.value.details?.shop)
        collector.cancel()
    }

    private companion object {
        val bike = Bike(
            id = 7,
            nickname = "Schotterfuchs",
            brand = "Canyon",
            model = "Grizl",
            shopId = 3,
            createdAt = 1,
        )
        val image = BikeImage(
            bikeId = bike.id,
            storagePath = "images/grizl.jpg",
            displayOrder = 0,
            isThumbnail = true,
        )
        val shop = Shop(id = 3, name = "Radhaus", logoPath = "shops/radhaus.jpg", createdAt = 1)
        val assignedPart = PartWithRelations(
            part = Part(
                id = 9,
                category = PartCategory.DRIVETRAIN,
                name = "Kette",
                createdAt = 1,
            ),
            attributes = emptyList(),
            bikes = listOf(bike),
            shopLinks = emptyList(),
        )
        val unrelatedPart = assignedPart.copy(
            part = assignedPart.part.copy(id = 10, name = "Bremsbelag"),
            bikes = emptyList(),
        )
    }
}

private class DetailsBikeRepository(initialBike: BikeWithImages?) : BikeRepository {
    private val bike = MutableStateFlow(initialBike)
    var savedBike: BikeWithImages? = null

    override fun observeBikes(): Flow<List<BikeWithImages>> = emptyFlow()

    override fun observeBike(bikeId: Long): Flow<BikeWithImages?> = bike

    override suspend fun bikeWithParts(bikeId: Long): BikeWithParts? = null

    override suspend fun saveBike(bike: Bike, images: List<BikeImage>): Long {
        savedBike = BikeWithImages(bike, images)
        this.bike.value = savedBike
        return bike.id
    }

    override suspend fun deleteBike(bike: Bike) = Unit

    override suspend fun deleteBikeWithSelectedParts(
        bike: Bike,
        partIdsToDelete: Set<Long>,
    ) = Unit
}

private class DetailsPartRepository(initialParts: List<PartWithRelations>) : PartRepository {
    val parts = MutableStateFlow(initialParts)

    override fun observeParts(): Flow<List<PartWithRelations>> = parts

    override fun observePart(partId: Long): Flow<PartWithRelations?> = emptyFlow()

    override suspend fun savePart(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long = error("Nicht benötigt")

    override suspend fun deletePart(part: Part) = error("Nicht benötigt")
}

private class DetailsShopRepository(initialShops: List<Shop>) : ShopRepository {
    val shops = MutableStateFlow(initialShops)

    override fun observeShops(): Flow<List<Shop>> = shops

    override fun observeShop(shopId: Long): Flow<ShopWithRelations?> = emptyFlow()

    override suspend fun saveShop(shop: Shop): Long = error("Nicht benötigt")

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean = error("Nicht benötigt")
}
