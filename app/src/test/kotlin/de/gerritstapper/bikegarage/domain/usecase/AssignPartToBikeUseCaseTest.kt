package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AssignPartToBikeUseCaseTest {
    @Test
    fun `delegates assignment for persisted bike and part`() = runBlocking {
        val repository = AssigningPartRepository()

        AssignPartToBikeUseCase(repository)(partId = 3, bikeId = 7)

        assertEquals(3L to 7L, repository.assignment)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an unsaved part`() = runBlocking {
        AssignPartToBikeUseCase(AssigningPartRepository())(partId = 0, bikeId = 7)
    }
}

private class AssigningPartRepository : PartRepository {
    var assignment: Pair<Long, Long>? = null

    override fun observeParts(): Flow<List<PartWithRelations>> = emptyFlow()

    override fun observePart(partId: Long): Flow<PartWithRelations?> = emptyFlow()

    override suspend fun savePart(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long = error("Nicht benötigt")

    override suspend fun assignPartToBike(partId: Long, bikeId: Long) {
        assignment = partId to bikeId
    }

    override suspend fun deletePart(part: Part) = error("Nicht benötigt")
}
