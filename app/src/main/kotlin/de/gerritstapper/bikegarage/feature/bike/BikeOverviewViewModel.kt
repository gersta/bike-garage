package de.gerritstapper.bikegarage.feature.bike

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.usecase.GetBikesUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BikeCardViewModel(
    val id: Long,
    val nickname: String?,
    val brand: String,
    val model: String,
    val thumbnailPath: String?,
) {
    companion object {
        fun from(bikeWithImages: BikeWithImages) = BikeCardViewModel(
            id = bikeWithImages.bike.id,
            nickname = bikeWithImages.bike.nickname,
            brand = bikeWithImages.bike.brand,
            model = bikeWithImages.bike.model,
            thumbnailPath = bikeWithImages.images
                .firstOrNull { it.isThumbnail }
                ?.storagePath,
        )
    }
}

@HiltViewModel
class BikeOverviewViewModel @Inject constructor(
    getBikes: GetBikesUseCase,
) : ViewModel() {
    val bikes: StateFlow<List<BikeCardViewModel>> = getBikes()
        .map { bikes -> bikes.map(BikeCardViewModel::from) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}
