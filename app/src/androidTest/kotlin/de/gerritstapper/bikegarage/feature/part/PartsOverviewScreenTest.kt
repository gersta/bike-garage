package de.gerritstapper.bikegarage.feature.part

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.ui.theme.BikeGarageTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PartsOverviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun overviewGroupsPartsAndOpensDetailsFromRows() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val drivetrainPart = PartOverviewRow(
            id = 1,
            name = "Kette",
            bikeNames = listOf("Gravel", "Trek Domane"),
        )
        val unassignedBrake = PartOverviewRow(
            id = 2,
            name = "Scheibenbremse",
            bikeNames = emptyList(),
        )
        val state = PartsOverviewUiState(
            categories = listOf(
                PartCategoryOverview(
                    PartCategory.DRIVETRAIN,
                    listOf(PartTypeOverview("Kette", listOf(drivetrainPart))),
                ),
                PartCategoryOverview(
                    PartCategory.BRAKES,
                    listOf(PartTypeOverview(null, listOf(unassignedBrake))),
                ),
            ),
        )
        var openedPartId: Long? = null

        composeRule.setContent {
            BikeGarageTheme {
                PartsOverviewContent(
                    state = state,
                    onPartClick = { partId -> openedPartId = partId },
                    onCreatePart = {},
                )
            }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.part_category_drivetrain),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.part_category_brakes),
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("Kette").assertCountEquals(2)
        composeRule.onNodeWithText(
            context.getString(R.string.parts_overview_unknown_type),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(
                R.string.parts_overview_part_bikes,
                drivetrainPart.bikeNames.joinToString(),
            ),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.parts_overview_part_without_bikes),
        ).assertIsDisplayed()

        composeRule.onNodeWithTag("parts-overview-row-${unassignedBrake.id}").performClick()
        composeRule.runOnIdle {
            assertEquals(unassignedBrake.id, openedPartId)
        }
    }
}
