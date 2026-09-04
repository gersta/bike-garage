package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import java.time.LocalDate
import javax.inject.Inject

enum class BikeCreationFieldError {
    BRAND_REQUIRED,
    MODEL_REQUIRED,
    IMAGE_REQUIRED,
    PURCHASE_DATE_INVALID,
}

data class BikeCreationValidation(
    val brandError: BikeCreationFieldError? = null,
    val modelError: BikeCreationFieldError? = null,
    val imageError: BikeCreationFieldError? = null,
    val purchaseDateError: BikeCreationFieldError? = null,
) {
    val isValid: Boolean
        get() = brandError == null &&
            modelError == null &&
            imageError == null &&
            purchaseDateError == null
}

sealed interface CreateBikeResult {
    data class Created(val bike: Bike) : CreateBikeResult
    data class Invalid(val validation: BikeCreationValidation) : CreateBikeResult
}

class CreateBikeUseCase @Inject constructor(
    private val bikeRepository: BikeRepository,
) {
    suspend operator fun invoke(
        nickname: String,
        brand: String,
        model: String,
        purchaseDate: String,
        shopId: Long?,
        imagePaths: List<String>,
        thumbnailPath: String?,
    ): CreateBikeResult {
        val persistedImagePaths = imagePaths.filter { it.isNotBlank() }.distinct()
        val validation = validate(brand, model, purchaseDate, persistedImagePaths)
        if (!validation.isValid) {
            return CreateBikeResult.Invalid(validation)
        }

        val bike = Bike(
            nickname = nickname.trim().takeIf { it.isNotEmpty() },
            brand = brand.trim(),
            model = model.trim(),
            purchaseDate = purchaseDate.trim().takeIf(String::isNotEmpty),
            shopId = shopId,
            createdAt = System.currentTimeMillis(),
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

        return CreateBikeResult.Created(
            bike.copy(id = bikeRepository.saveBike(bike, images)),
        )
    }

    fun validate(
        brand: String,
        model: String,
        purchaseDate: String,
        imagePaths: List<String>,
    ): BikeCreationValidation =
        BikeCreationValidation(
            brandError = BikeCreationFieldError.BRAND_REQUIRED.takeUnless { brand.isNotBlank() },
            modelError = BikeCreationFieldError.MODEL_REQUIRED.takeUnless { model.isNotBlank() },
            imageError = BikeCreationFieldError.IMAGE_REQUIRED.takeUnless { imagePaths.isNotEmpty() },
            purchaseDateError = BikeCreationFieldError.PURCHASE_DATE_INVALID.takeUnless {
                purchaseDate.isBlank() || isIsoLocalDate(purchaseDate.trim())
            },
        )

    private fun isIsoLocalDate(value: String): Boolean =
        runCatching { LocalDate.parse(value) }.isSuccess
}
