package de.gerritstapper.bikegarage.feature.bike

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.model.BikeWithParts
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import de.gerritstapper.bikegarage.domain.usecase.DeleteBikeUseCase
import de.gerritstapper.bikegarage.ui.navigation.BIKE_ID_ARGUMENT
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BikeDeletionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deletionDialogListsLinkedPartsAndConfirmsSelectedPartDeletion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = DeletionScreenBikeRepository(bike, listOf(firstPart, secondPart))
        val viewModel = BikeDeletionViewModel(
            savedStateHandle = SavedStateHandle(mapOf(BIKE_ID_ARGUMENT to bike.id)),
            bikeRepository = repository,
            deleteBike = DeleteBikeUseCase(repository),
        )
        var deletionCompleted = false

        composeRule.setContent {
            BikeGarageTheme {
                BikeDeletionScreen(
                    onDeleted = { deletionCompleted = true },
                    viewModel = viewModel,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.bike == bike
        }
        composeRule.onNodeWithText(context.getString(R.string.delete)).performClick()
        composeRule.onNodeWithText(
            context.getString(R.string.bike_delete_confirmation_title),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(firstPart.name).assertIsDisplayed()
        composeRule.onNodeWithText(secondPart.name).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.bike_delete_part_selection, firstPart.name),
        ).performClick()
        composeRule.runOnIdle {
            assertEquals(setOf(firstPart.id), viewModel.uiState.value.selectedPartIds)
        }

        composeRule.onNodeWithText(
            context.getString(R.string.bike_delete_confirmation_action),
        ).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { deletionCompleted }
        assertEquals(setOf(firstPart.id), repository.deletedPartIds)
    }

    private companion object {
        val bike = Bike(
            id = 1,
            nickname = "Schotterfuchs",
            brand = "Canyon",
            model = "Grizl",
            createdAt = 1,
        )
        val firstPart = Part(
            id = 2,
            category = PartCategory.DRIVETRAIN,
            name = "Kette",
            createdAt = 2,
        )
        val secondPart = Part(
            id = 3,
            category = PartCategory.BRAKES,
            name = "Bremse",
            createdAt = 3,
        )
    }
}

private class DeletionScreenBikeRepository(
    private val bike: Bike,
    initialParts: List<Part>,
) : BikeRepository {
    private val observedBike = MutableStateFlow<BikeWithImages?>(
        BikeWithImages(bike = bike, images = emptyList()),
    )
    private var parts = initialParts
    var deletedPartIds = emptySet<Long>()

    override fun observeBikes(): Flow<List<BikeWithImages>> = emptyFlow()

    override fun observeBike(bikeId: Long): Flow<BikeWithImages?> = observedBike

    override suspend fun bikeWithParts(bikeId: Long): BikeWithParts? =
        observedBike.value?.let { BikeWithParts(bike = bike, parts = parts) }

    override suspend fun saveBike(bike: Bike, images: List<BikeImage>): Long = bike.id

    override suspend fun deleteBike(bike: Bike) = Unit

    override suspend fun deleteBikeWithSelectedParts(
        bike: Bike,
        partIdsToDelete: Set<Long>,
    ) {
        deletedPartIds = partIdsToDelete
        parts = parts.filterNot { it.id in partIdsToDelete }
        observedBike.value = null
    }
}
