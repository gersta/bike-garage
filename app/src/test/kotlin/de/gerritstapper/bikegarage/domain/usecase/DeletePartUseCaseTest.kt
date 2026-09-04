package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeletePartUseCaseTest {
    @Test
    fun `deletes the requested part`() = runBlocking {
        val repository = DeletionPartRepository(details = details)

        val result = DeletePartUseCase(repository).delete(part.id)

        assertEquals(DeletePartResult.Deleted(part), result)
        assertEquals(part, repository.deletedPart)
        assertNull(repository.details.value)
    }

    @Test
    fun `reports a missing part without requesting deletion`() = runBlocking {
        val repository = DeletionPartRepository(details = null)

        val result = DeletePartUseCase(repository).delete(part.id)

        assertEquals(DeletePartResult.NotFound, result)
        assertNull(repository.deletedPart)
    }

    @Test
    fun `reports a repository failure without reporting deletion`() = runBlocking {
        val repository = DeletionPartRepository(details = details, failDeletion = true)

        val result = DeletePartUseCase(repository).delete(part.id)

        assertEquals(DeletePartResult.Failed, result)
        assertNull(repository.deletedPart)
        assertEquals(details, repository.details.value)
    }

    private companion object {
        val part = Part(
            id = 7,
            category = PartCategory.DRIVETRAIN,
            name = "Kette",
            createdAt = 1,
        )
        val details = PartWithRelations(
            part = part,
            attributes = emptyList(),
            bikes = emptyList(),
            shopLinks = emptyList(),
        )
    }
}

private class DeletionPartRepository(
    details: PartWithRelations?,
    private val failDeletion: Boolean = false,
) : PartRepository {
    val details = MutableStateFlow(details)
    var deletedPart: Part? = null

    override fun observeParts(): Flow<List<PartWithRelations>> = emptyFlow()

    override fun observePart(partId: Long): Flow<PartWithRelations?> = details

    override suspend fun savePart(
        part: Part,
        attributes: List<PartAttribute>,
        bikeIds: Set<Long>,
        shopLinks: List<PartShopLink>,
    ): Long = error("Nicht benötigt")

    override suspend fun deletePart(part: Part) {
        check(!failDeletion) { "Datenbankfehler" }
        deletedPart = part
        details.value = null
    }
}
