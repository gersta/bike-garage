package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetPartsUseCaseTest {
    @Test
    fun `returns assigned and unassigned parts`() = runBlocking {
        val assigned = partWithRelations(id = 1, name = "Kette")
        val unassigned = partWithRelations(id = 2, name = "Schutzblech")

        val parts = GetPartsUseCase(FakePartRepository(listOf(assigned, unassigned)))().first()

        assertEquals(listOf(assigned, unassigned), parts)
    }
}

private class FakePartRepository(
    private val parts: List<PartWithRelations>,
) : PartRepository {
    override fun observeParts(): Flow<List<PartWithRelations>> = flowOf(parts)

    override fun observePart(partId: Long): Flow<PartWithRelations?> = emptyFlow()

    override suspend fun savePart(
        part: Part,
        attributes: List<de.gerritstapper.bikegarage.domain.model.PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long = error("Nicht benötigt")

    override suspend fun deletePart(part: Part) = error("Nicht benötigt")
}

private fun partWithRelations(id: Long, name: String): PartWithRelations =
    PartWithRelations(
        part = Part(
            id = id,
            category = PartCategory.ACCESSORIES,
            name = name,
            createdAt = id,
        ),
        attributes = emptyList(),
        bikes = emptyList(),
        shopLinks = emptyList(),
    )
