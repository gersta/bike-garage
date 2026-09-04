package de.gerritstapper.bikegarage.feature.part

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartTechnicalFields
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.usecase.GetPartsUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal data class PartOverviewRow(
    val id: Long,
    val name: String,
    val bikeNames: List<String>,
)

internal data class PartCategoryOverview(
    val category: PartCategory,
    val types: List<PartTypeOverview>,
)

internal data class PartTypeOverview(
    val type: String?,
    val parts: List<PartOverviewRow>,
)

internal data class PartsOverviewUiState(
    val categories: List<PartCategoryOverview> = emptyList(),
) {
    val isEmpty: Boolean
        get() = categories.isEmpty()
}

@HiltViewModel
class PartsOverviewViewModel @Inject constructor(
    getParts: GetPartsUseCase,
) : ViewModel() {
    internal val uiState: StateFlow<PartsOverviewUiState> = getParts()
        .map { parts -> PartsOverviewUiState(parts.asCategoryOverviews()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PartsOverviewUiState(),
        )

}

internal fun List<PartWithRelations>.asCategoryOverviews(): List<PartCategoryOverview> =
    PartCategory.entries.mapNotNull { category ->
        filter { it.part.category == category }
            .takeIf { it.isNotEmpty() }
            ?.groupBy { details ->
                PartTechnicalFields.canonicalType(details.part.type)
                    ?.takeIf { PartTechnicalFields.isTypeForCategory(it, category) }
            }
            ?.map { (type, details) ->
                PartTypeOverview(
                    type = type,
                    parts = details
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.part.name })
                        .map { it.toOverviewRow() },
                )
            }
            ?.sortedWith(
                compareBy<PartTypeOverview> { it.type == null }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.type.orEmpty() },
            )
            ?.let { types -> PartCategoryOverview(category, types) }
    }

private fun PartWithRelations.toOverviewRow() = PartOverviewRow(
    id = part.id,
    name = part.name,
    bikeNames = bikes
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.overviewLabel() })
        .map { it.overviewLabel() },
)

private fun Bike.overviewLabel(): String =
    nickname?.takeIf { it.isNotBlank() }?.let { "$it ($brand $model)" } ?: "$brand $model"
