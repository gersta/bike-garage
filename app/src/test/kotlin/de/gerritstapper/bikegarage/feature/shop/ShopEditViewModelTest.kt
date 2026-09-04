package de.gerritstapper.bikegarage.feature.shop

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import de.gerritstapper.bikegarage.domain.usecase.UpdateShopUseCase
import de.gerritstapper.bikegarage.ui.navigation.SHOP_ID_ARGUMENT
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ShopEditViewModelTest {
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
    fun `persists replacement before deleting the previous logo and opens updated shop`() = runTest(dispatcher) {
        val operations = CopyOnWriteArrayList<String>()
        val repository = EditShopRepository(existingShop, operations)
        val imageStorage = RecordingImageStorage(operations)
        val viewModel = ShopEditViewModel(
            savedStateHandle = SavedStateHandle(mapOf(SHOP_ID_ARGUMENT to existingShop.id)),
            shopRepository = repository,
            updateShop = UpdateShopUseCase(repository),
            imageStorage = imageStorage,
        )
        val events = mutableListOf<ShopEditEvent>()
        val eventCollector = launch { viewModel.events.toList(events) }

        advanceUntilIdle()
        viewModel.completeCameraCapture(
            pendingImage = PendingImage("images/new-logo.jpg", Uri.parse("content://test/new")),
            isSaved = true,
        )
        advanceUntilIdle()

        assertEquals(emptyList<String>(), operations)
        assertEquals("images/new-logo.jpg", viewModel.uiState.value.logoPath)
        assertFalse(viewModel.uiState.value.isLoading)
        viewModel.save()
        advanceUntilIdle()

        val updatedShop = existingShop.copy(logoPath = "images/new-logo.jpg")
        assertEquals(listOf("save", "delete:images/old-logo.jpg"), operations)
        assertEquals(listOf(ShopEditEvent.Updated(updatedShop)), events)
        assertEquals(listOf(updatedShop), repository.savedShops)
        eventCollector.cancel()
    }

    private companion object {
        val existingShop = Shop(
            id = 7,
            name = "Radhaus",
            logoPath = "images/old-logo.jpg",
            createdAt = 123,
        )
    }
}

private class EditShopRepository(
    private val shop: Shop,
    private val operations: MutableList<String>,
) : ShopRepository {
    val savedShops = mutableListOf<Shop>()

    override fun observeShops(): Flow<List<Shop>> = emptyFlow()

    override fun observeShop(shopId: Long): Flow<ShopWithRelations?> =
        flowOf(ShopWithRelations(shop = shop, purchasedBikes = emptyList(), parts = emptyList(), partLinks = emptyList()))

    override suspend fun saveShop(shop: Shop): Long {
        operations += "save"
        savedShops += shop
        return shop.id
    }

    override suspend fun deleteShopIfUnlinked(shop: Shop): Boolean = false
}

private class RecordingImageStorage(
    private val operations: MutableList<String>,
) : ImageStorage {
    override fun newCameraCapture(): PendingImage = error("Nicht benötigt")

    override suspend fun import(source: Uri): String = error("Nicht benötigt")

    override suspend fun delete(storagePath: String) {
        operations += "delete:$storagePath"
    }

    override suspend fun discard(pendingImage: PendingImage) = Unit
}
