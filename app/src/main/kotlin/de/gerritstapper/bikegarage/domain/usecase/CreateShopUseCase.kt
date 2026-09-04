package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import javax.inject.Inject

enum class ShopCreationFieldError {
    NAME_REQUIRED,
    LOGO_REQUIRED,
}

data class ShopCreationValidation(
    val nameError: ShopCreationFieldError? = null,
    val logoError: ShopCreationFieldError? = null,
) {
    val isValid: Boolean
        get() = nameError == null && logoError == null
}

sealed interface CreateShopResult {
    data class Created(val shop: Shop) : CreateShopResult
    data class Invalid(val validation: ShopCreationValidation) : CreateShopResult
}

class CreateShopUseCase @Inject constructor(
    private val shopRepository: ShopRepository,
) {
    suspend operator fun invoke(name: String, logoPath: String?): CreateShopResult {
        val validation = validate(name, logoPath)
        if (!validation.isValid) {
            return CreateShopResult.Invalid(validation)
        }

        val shop = Shop(
            name = name.trim(),
            logoPath = requireNotNull(logoPath),
            createdAt = System.currentTimeMillis(),
        )
        return CreateShopResult.Created(shop.copy(id = shopRepository.saveShop(shop)))
    }

    fun validate(name: String, logoPath: String?): ShopCreationValidation =
        ShopCreationValidation(
            nameError = ShopCreationFieldError.NAME_REQUIRED.takeUnless { name.isNotBlank() },
            logoError = ShopCreationFieldError.LOGO_REQUIRED.takeUnless { !logoPath.isNullOrBlank() },
        )
}
