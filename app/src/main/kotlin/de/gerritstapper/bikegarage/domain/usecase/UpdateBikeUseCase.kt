package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import java.time.LocalDate
import javax.inject.Inject

typealias BikeUpdateFieldError = BikeCreationFieldError
typealias BikeUpdateValidation = BikeCreationValidation

sealed interface UpdateBikeResult {
    data class Updated(val bike: Bike) : UpdateBikeResult
    data class Invalid(val validation: BikeUpdateValidation) : UpdateBikeResult
}

class UpdateBikeUseCase @Inject constructor(
    private val bikeRepository: BikeRepository,
) {
    suspend operator fun invoke(
        bike: Bike,
        nickname: String,
        brand: String,
        model: String,
        purchaseDate: String,
        shopId: Long?,
        imagePaths: List<String>,
        thumbnailPath: String?,
    ): UpdateBikeResult {
        require(bike.id > 0) { "Nur gespeicherte Fahrräder können bearbeitet werden." }

        val persistedImagePaths = imagePaths.filter { it.isNotBlank() }.distinct()
        val validation = validate(brand, model, purchaseDate, persistedImagePaths)
        if (!validation.isValid) {
            return UpdateBikeResult.Invalid(validation)
        }

        val updatedBike = bike.copy(
            nickname = nickname.trim().takeIf { it.isNotEmpty() },
            brand = brand.trim(),
            model = model.trim(),
            purchaseDate = purchaseDate.trim().takeIf(String::isNotEmpty),
            shopId = shopId,
        )
        val selectedThumbnailIndex = persistedImagePaths.indexOf(
            thumbnailPath.takeIf { it in persistedImagePaths } ?: persistedImagePaths.first(),
        )
        val images = persistedImagePaths.mapIndexed { index, storagePath ->
            BikeImage(
                storagePath = storagePath,
                displayOrder = index,
                isThumbnail = index == selectedThumbnailIndex,
            )
        }

        bikeRepository.saveBike(updatedBike, images)
        return UpdateBikeResult.Updated(updatedBike)
    }

    fun validate(
        brand: String,
        model: String,
        purchaseDate: String,
        imagePaths: List<String>,
    ): BikeUpdateValidation =
        BikeCreationValidation(
            brandError = BikeUpdateFieldError.BRAND_REQUIRED.takeUnless { brand.isNotBlank() },
            modelError = BikeUpdateFieldError.MODEL_REQUIRED.takeUnless { model.isNotBlank() },
            imageError = BikeUpdateFieldError.IMAGE_REQUIRED.takeUnless { imagePaths.isNotEmpty() },
            purchaseDateError = BikeUpdateFieldError.PURCHASE_DATE_INVALID.takeUnless {
                purchaseDate.isBlank() || isIsoLocalDate(purchaseDate.trim())
            },
        )

    private fun isIsoLocalDate(value: String): Boolean =
        runCatching { LocalDate.parse(value) }.isSuccess
}
