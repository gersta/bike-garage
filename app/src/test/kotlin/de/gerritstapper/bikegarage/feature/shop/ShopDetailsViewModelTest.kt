package de.gerritstapper.bikegarage.feature.shop

import androidx.lifecycle.SavedStateHandle
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartShopLinkWithShop
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import de.gerritstapper.bikegarage.ui.navigation.SHOP_ID_ARGUMENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
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
class ShopDetailsViewModelTest {
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
    fun `updates purchased bikes and product linked parts when their flows change`() = runTest(dispatcher) {
        val shop = Shop(id = 1, name = "Radhaus", logoPath = "shops/radhaus.jpg", createdAt = 1)
        val purchasedBike = Bike(
            id = 2,
            nickname = "Gravel",
            brand = "Canyon",
            model = "Grizl",
            shopId = shop.id,
            createdAt = 2,
        )
        val repository = DetailsShopRepository(
            ShopWithRelations(
                shop = shop,
                purchasedBikes = listOf(purchasedBike),
                parts = emptyList(),
                partLinks = emptyList(),
            ),
        )
        val linkedPart = PartWithRelations(
            part = Part(
                id = 3,
                category = PartCategory.DRIVETRAIN,
                name = "Kette",
                createdAt = 3,
            ),
            attributes = emptyList(),
            bikes = listOf(purchasedBike),
            shopLinks = listOf(
                PartShopLinkWithShop(
                    link = PartShopLink(
                        id = 4,
                        partId = 3,
                        shopId = shop.id,
                        productUrl = "https://example.test/kette",
                    ),
                    shop = shop,
                ),
            ),
        )
        val unrelatedPart = linkedPart.copy(
            part = linkedPart.part.copy(id = 5, name = "Bremsbelag"),
            shopLinks = emptyList(),
        )
        val partRepository = DetailsPartRepository(listOf(linkedPart, unrelatedPart))
        val viewModel = ShopDetailsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(SHOP_ID_ARGUMENT to shop.id)),
            shopRepository = repository,
            partRepository = partRepository,
        )

        val collector = launch {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(purchasedBike), viewModel.uiState.value.purchasedBikes)
        assertEquals(listOf(linkedPart), viewModel.uiState.value.parts)

        repository.relations.value = repository.relations.value?.copy(purchasedBikes = emptyList())
        partRepository.parts.value = emptyList()
        advanceUntilIdle()

        assertEquals(emptyList<Bike>(), viewModel.uiState.value.purchasedBikes)
        assertEquals(emptyList<PartWithRelations>(), viewModel.uiState.value.parts)
        collector.cancel()
    }
}

private class DetailsShopRepository(
    initialRelations: ShopWithRelations,
) : ShopRepository {
    val relations = MutableStateFlow<ShopWithRelations?>(initialRelations)

    override fun observeShops(): Flow<List<Shop>> = emptyFlow()

    override fun observeShop(shopId: Long): Flow<ShopWithRelations?> = relations

    override suspend fun saveShop(shop: Shop): Long = error("Nicht benötigt")

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean = error("Nicht benötigt")
}

private class DetailsPartRepository(
    initialParts: List<PartWithRelations>,
) : PartRepository {
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
