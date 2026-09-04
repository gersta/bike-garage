package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import javax.inject.Inject

enum class ShopUpdateFieldError {
    NAME_REQUIRED,
    LOGO_REQUIRED,
}

data class ShopUpdateValidation(
    val nameError: ShopUpdateFieldError? = null,
    val logoError: ShopUpdateFieldError? = null,
) {
    val isValid: Boolean
        get() = nameError == null && logoError == null
}

sealed interface UpdateShopResult {
    data class Updated(val shop: Shop) : UpdateShopResult
    data class Invalid(val validation: ShopUpdateValidation) : UpdateShopResult
}

class UpdateShopUseCase @Inject constructor(
    private val shopRepository: ShopRepository,
) {
    suspend operator fun invoke(
        shop: Shop,
        name: String,
        logoPath: String?,
    ): UpdateShopResult {
        val validation = validate(name, logoPath)
        if (!validation.isValid) {
            return UpdateShopResult.Invalid(validation)
        }

        val updatedShop = shop.copy(
            name = name.trim(),
            logoPath = requireNotNull(logoPath),
        )
        shopRepository.saveShop(updatedShop)
        return UpdateShopResult.Updated(updatedShop)
    }

    fun validate(name: String, logoPath: String?): ShopUpdateValidation =
        ShopUpdateValidation(
            nameError = ShopUpdateFieldError.NAME_REQUIRED.takeUnless { name.isNotBlank() },
            logoError = ShopUpdateFieldError.LOGO_REQUIRED.takeUnless { !logoPath.isNullOrBlank() },
        )
}
