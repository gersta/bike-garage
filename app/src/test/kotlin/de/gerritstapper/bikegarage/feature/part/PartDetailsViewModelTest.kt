package de.gerritstapper.bikegarage.feature.part

import androidx.lifecycle.SavedStateHandle
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.ui.navigation.PART_ID_ARGUMENT
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
class PartDetailsViewModelTest {
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
    fun `reactively exposes the requested part and its relations`() = runTest(dispatcher) {
        val details = PartWithRelations(
            part = Part(
                id = 7,
                category = PartCategory.DRIVETRAIN,
                type = "Kette",
                name = "CN-M8100",
                createdAt = 1,
            ),
            attributes = listOf(
                PartAttribute(partId = 7, key = "link_count", value = "116"),
            ),
            bikes = emptyList(),
            shopLinks = emptyList(),
        )
        val repository = DetailsPartRepository(details)
        val viewModel = PartDetailsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(PART_ID_ARGUMENT to details.part.id)),
            partRepository = repository,
        )

        val collector = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(details, viewModel.uiState.value.details)

        repository.details.value = null
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.details)
        collector.cancel()
    }
}

private class DetailsPartRepository(
    initialDetails: PartWithRelations?,
) : PartRepository {
    val details = MutableStateFlow(initialDetails)

    override fun observeParts(): Flow<List<PartWithRelations>> = emptyFlow()

    override fun observePart(partId: Long): Flow<PartWithRelations?> = details

    override suspend fun savePart(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long = error("Nicht benötigt")

    override suspend fun deletePart(part: Part) = error("Nicht benötigt")
}
