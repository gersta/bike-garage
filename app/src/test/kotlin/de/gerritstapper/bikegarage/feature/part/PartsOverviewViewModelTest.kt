package de.gerritstapper.bikegarage.feature.part

import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.domain.usecase.GetPartsUseCase
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PartsOverviewViewModelTest {
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
    fun `groups every part by category and canonical type with all and no bike assignments`() = runTest(dispatcher) {
        val assignedPart = partWithRelations(
            id = 1,
            category = PartCategory.DRIVETRAIN,
            name = "Kette",
            type = " Kette ",
            bikes = listOf(
                Bike(id = 2, nickname = "Gravel", brand = "Canyon", model = "Grizl", createdAt = 1),
                Bike(id = 3, brand = "Trek", model = "Domane", createdAt = 2),
            ),
        )
        val unassignedPart = partWithRelations(
            id = 2,
            category = PartCategory.BRAKES,
            name = "Scheibenbremse",
            type = "Unbekannt",
        )
        val viewModel = PartsOverviewViewModel(
            GetPartsUseCase(OverviewPartRepository(listOf(unassignedPart, assignedPart))),
        )

        val collector = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertEquals(
            listOf(PartCategory.DRIVETRAIN, PartCategory.BRAKES),
            viewModel.uiState.value.categories.map { it.category },
        )
        assertEquals(
            listOf("Gravel (Canyon Grizl)", "Trek Domane"),
            viewModel.uiState.value.categories.first().types.single().parts.single().bikeNames,
        )
        assertEquals("Kette", viewModel.uiState.value.categories.first().types.single().type)
        assertEquals(
            emptyList<String>(),
            viewModel.uiState.value.categories.last().types.single().parts.single().bikeNames,
        )
        assertEquals(null, viewModel.uiState.value.categories.last().types.single().type)

        collector.cancel()
    }

    @Test
    fun `sorts type groups and part names while grouping missing and invalid types as fallback`() {
        val grouped = listOf(
            partWithRelations(1, PartCategory.DRIVETRAIN, "Zulu", type = "Kassette"),
            partWithRelations(2, PartCategory.DRIVETRAIN, "alpha", type = "Blatt"),
            partWithRelations(3, PartCategory.DRIVETRAIN, "Bravo", type = "Kettenblatt"),
            partWithRelations(4, PartCategory.DRIVETRAIN, "Leer", type = " "),
            partWithRelations(5, PartCategory.DRIVETRAIN, "Falsch", type = "Bremssattel"),
        ).asCategoryOverviews()

        val drivetrain = grouped.single()
        assertEquals(
            listOf("Kassette", "Kettenblatt", null),
            drivetrain.types.map { it.type },
        )
        assertEquals(listOf("alpha", "Bravo"), drivetrain.types[1].parts.map { it.name })
        assertEquals(listOf("Falsch", "Leer"), drivetrain.types.last().parts.map { it.name })
    }
}

private class OverviewPartRepository(
    parts: List<PartWithRelations>,
) : PartRepository {
    private val parts = MutableStateFlow(parts)

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

private fun partWithRelations(
    id: Long,
    category: PartCategory,
    name: String,
    type: String? = null,
    bikes: List<Bike> = emptyList(),
): PartWithRelations = PartWithRelations(
    part = Part(id = id, category = category, type = type, name = name, createdAt = id),
    attributes = emptyList(),
    bikes = bikes,
    shopLinks = emptyList(),
)
