package de.gerritstapper.bikegarage.feature.part

import androidx.lifecycle.SavedStateHandle
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.domain.usecase.AssignPartToBikeUseCase
import de.gerritstapper.bikegarage.domain.usecase.GetPartsUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PartAssignmentViewModelTest {
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
    fun `filters all search fields and hides parts already assigned to the bike`() = runTest(dispatcher) {
        val repository = MutableAssigningPartRepository(
            listOf(
                part(
                    id = 1,
                    name = "Kette",
                    manufacturer = "Shimano",
                    number = "CN-M8100",
                    type = "Kette",
                ),
                part(id = 2, name = "Bremse", manufacturer = "Magura", number = "MT5"),
                part(id = 3, name = "Kette", manufacturer = "Shimano", number = "CN-HG")
                    .copy(bikes = listOf(targetBike)),
            ),
        )
        val viewModel = createViewModel(repository)
        val collector = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertEquals(listOf(2L, 1L), viewModel.uiState.value.parts.map { it.id })
        assertEquals(
            PartCategory.BRAKES,
            viewModel.uiState.value.parts.first { it.id == 2L }.category,
        )
        viewModel.onManufacturerNumberChanged("m8100")
        advanceUntilIdle()
        assertEquals(listOf(1L), viewModel.uiState.value.parts.map { it.id })
        viewModel.onTypeChanged("Kette")
        advanceUntilIdle()
        assertEquals(listOf(1L), viewModel.uiState.value.parts.map { it.id })
        viewModel.onNameChanged("unbekannt")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.parts.isEmpty())
        collector.cancel()
    }

    @Test
    fun `shows retryable failure and emits success after retry`() = runTest(dispatcher) {
        val repository = MutableAssigningPartRepository(listOf(part(id = 2, name = "Bremse")))
        repository.failNextAssignment = true
        val viewModel = createViewModel(repository)
        val stateCollector = launch { viewModel.uiState.collect { } }
        val events = mutableListOf<PartAssignmentEvent>()
        val eventCollector = launch { viewModel.events.collect { events += it } }
        advanceUntilIdle()

        viewModel.assignPart(2)
        advanceUntilIdle()
        assertEquals(2L, viewModel.uiState.value.failedPartId)

        viewModel.assignPart(2)
        advanceUntilIdle()
        assertEquals(listOf(2L to targetBike.id, 2L to targetBike.id), repository.assignments)
        assertEquals(listOf(PartAssignmentEvent.Assigned), events)
        stateCollector.cancel()
        eventCollector.cancel()
    }

    private fun createViewModel(repository: MutableAssigningPartRepository) = PartAssignmentViewModel(
        savedStateHandle = SavedStateHandle(mapOf(BIKE_ID_ARGUMENT to targetBike.id)),
        getParts = GetPartsUseCase(repository),
        assignPartToBike = AssignPartToBikeUseCase(repository),
    )

    private companion object {
        val targetBike = Bike(id = 7, brand = "Canyon", model = "Grizl", createdAt = 1)

        fun part(
            id: Long,
            name: String,
            manufacturer: String = "Hersteller",
            number: String = "ABC",
            type: String = "Bremse",
        ) = PartWithRelations(
            part = Part(
                id = id,
                category = PartCategory.BRAKES,
                type = type,
                name = name,
                manufacturer = manufacturer,
                manufacturerNumber = number,
                createdAt = 1,
            ),
            attributes = emptyList(),
            bikes = emptyList(),
            shopLinks = emptyList(),
        )
    }
}

private class MutableAssigningPartRepository(
    initialParts: List<PartWithRelations>,
) : PartRepository {
    private val parts = MutableStateFlow(initialParts)
    val assignments = mutableListOf<Pair<Long, Long>>()
    var failNextAssignment = false

    override fun observeParts(): Flow<List<PartWithRelations>> = parts

    override fun observePart(partId: Long): Flow<PartWithRelations?> = emptyFlow()

    override suspend fun savePart(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long = error("Nicht benötigt")

    override suspend fun assignPartToBike(partId: Long, bikeId: Long) {
        assignments += partId to bikeId
        if (failNextAssignment) {
            failNextAssignment = false
            error("Datenbankfehler")
        }
    }

    override suspend fun deletePart(part: Part) = error("Nicht benötigt")
}
