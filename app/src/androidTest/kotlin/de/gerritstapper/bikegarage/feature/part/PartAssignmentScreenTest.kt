package de.gerritstapper.bikegarage.feature.part

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PartAssignmentScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resultRowShowsOnlyNameCategoryAndTypeAndAssignsSelectedPart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var assignedPartId: Long? = null

        composeRule.setContent {
            BikeGarageTheme {
                PartAssignmentContent(
                    state = PartAssignmentUiState(
                        parts = listOf(
                            PartAssignmentRow(
                                id = 4,
                                name = "Kette",
                                category = PartCategory.DRIVETRAIN,
                                manufacturer = "Shimano",
                                manufacturerNumber = "CN-M8100",
                                type = "Kette",
                            ),
                        ),
                    ),
                    onNameChanged = {},
                    onManufacturerChanged = {},
                    onManufacturerNumberChanged = {},
                    onTypeChanged = {},
                    onClearQuery = {},
                    onAssign = { assignedPartId = it },
                    onNavigateUp = {},
                )
            }
        }

        composeRule.onNodeWithTag("part-assignment-row-4").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Kette").assertIsDisplayed()
        composeRule.onNodeWithText(
            "${context.getString(R.string.part_category)}: " +
                context.getString(R.string.part_category_drivetrain),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "${context.getString(R.string.part_type)}: Kette",
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("Shimano").assertCountEquals(0)
        composeRule.onAllNodesWithText("CN-M8100").assertCountEquals(0)
        composeRule.onNodeWithText(
            context.getString(R.string.part_assignment_assign),
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(4L, assignedPartId)
        }
    }
}
