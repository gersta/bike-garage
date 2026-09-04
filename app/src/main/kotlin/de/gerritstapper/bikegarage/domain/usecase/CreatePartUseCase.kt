package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartImage
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartTechnicalFields
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import java.net.URI
import javax.inject.Inject

enum class PartCreationFieldError {
    CATEGORY_REQUIRED,
    NAME_REQUIRED,
    SHOP_REQUIRED,
    PRODUCT_URL_REQUIRED,
    PRODUCT_URL_INVALID,
    INVENTORY_COUNT_INVALID,
}

data class PartProductLinkDraft(
    val shopId: Long?,
    val productUrl: String,
)

data class PartProductLinkValidation(
    val shopError: PartCreationFieldError? = null,
    val productUrlError: PartCreationFieldError? = null,
)

data class PartCreationValidation(
    val categoryError: PartCreationFieldError? = null,
    val nameError: PartCreationFieldError? = null,
    val inventoryCountError: PartCreationFieldError? = null,
    val productLinkErrors: Map<Int, PartProductLinkValidation> = emptyMap(),
) {
    val isValid: Boolean
        get() = categoryError == null &&
            nameError == null &&
            inventoryCountError == null &&
            productLinkErrors.values.all {
                it.shopError == null && it.productUrlError == null
            }
}

sealed interface CreatePartResult {
    data class Created(val part: Part) : CreatePartResult
    data class Invalid(val validation: PartCreationValidation) : CreatePartResult
}

class CreatePartUseCase @Inject constructor(
    private val partRepository: PartRepository,
) {
    suspend operator fun invoke(
        category: PartCategory?,
        type: String?,
        name: String,
        manufacturer: String,
        manufacturerNumber: String,
        description: String,
        technicalAttributes: Map<String, String>,
        bikeIds: Set<Long>,
        productLinks: List<PartProductLinkDraft>,
        inventoryCount: Int? = null,
        imagePaths: List<String> = emptyList(),
        thumbnailPath: String? = null,
    ): CreatePartResult {
        val validation = validate(category, name, productLinks, inventoryCount)
        if (!validation.isValid) return CreatePartResult.Invalid(validation)

        val savedCategory = requireNotNull(category)
        val savedType = PartTechnicalFields.canonicalType(type)
            ?.takeIf { PartTechnicalFields.isTypeForCategory(it, savedCategory) }
        val technicalKeys = PartTechnicalFields.forType(savedType).mapTo(mutableSetOf()) { it.key }
        val attributes = technicalAttributes
            .asSequence()
            .filter { (key, value) -> key in technicalKeys && value.isNotBlank() }
            .map { (key, value) -> PartAttribute(key = key, value = value.trim()) }
            .toList()
        val part = Part(
            category = savedCategory,
            type = savedType,
            name = name.trim(),
            manufacturer = manufacturer.trim().takeIf(String::isNotEmpty),
            manufacturerNumber = manufacturerNumber.trim().takeIf(String::isNotEmpty),
            description = description.trim().takeIf(String::isNotEmpty),
            createdAt = System.currentTimeMillis(),
            inventoryCount = inventoryCount,
        )
        val links = productLinks.map { link ->
            PartShopLink(
                shopId = requireNotNull(link.shopId),
                productUrl = link.productUrl.trim(),
            )
        }

        return CreatePartResult.Created(
            part.copy(
                id = partRepository.savePartWithImages(
                    part = part,
                    attributes = attributes,
                    bikeIds = bikeIds,
                    shopLinks = links,
                    images = imagePaths.mapIndexed { index, storagePath ->
                        PartImage(
                            storagePath = storagePath,
                            displayOrder = index,
                            isThumbnail = storagePath == (thumbnailPath ?: imagePaths.firstOrNull()),
                        )
                    },
                ),
            ),
        )
    }

    fun validate(
        category: PartCategory?,
        name: String,
        productLinks: List<PartProductLinkDraft>,
        inventoryCount: Int? = null,
    ): PartCreationValidation = validatePartForm(category, name, productLinks, inventoryCount)
}

internal fun validatePartForm(
    category: PartCategory?,
    name: String,
    productLinks: List<PartProductLinkDraft>,
    inventoryCount: Int? = null,
): PartCreationValidation =
    PartCreationValidation(
        categoryError = PartCreationFieldError.CATEGORY_REQUIRED.takeUnless { category != null },
        nameError = PartCreationFieldError.NAME_REQUIRED.takeUnless { name.isNotBlank() },
        inventoryCountError = PartCreationFieldError.INVENTORY_COUNT_INVALID.takeIf {
            inventoryCount != null && inventoryCount < 0
        },
        productLinkErrors = productLinks.mapIndexedNotNull { index, link ->
            PartProductLinkValidation(
                shopError = PartCreationFieldError.SHOP_REQUIRED.takeUnless {
                    link.shopId != null
                },
                productUrlError = when {
                    link.productUrl.isBlank() -> PartCreationFieldError.PRODUCT_URL_REQUIRED
                    !isProductUrl(link.productUrl) -> PartCreationFieldError.PRODUCT_URL_INVALID
                    else -> null
                },
            ).takeIf { it.shopError != null || it.productUrlError != null }
                ?.let { index to it }
        }.toMap(),
    )

private fun isProductUrl(value: String): Boolean = runCatching {
    URI(value.trim()).let { uri ->
        uri.host != null && (
            uri.scheme.equals("https", ignoreCase = true) ||
                uri.scheme.equals("http", ignoreCase = true)
            )
    }
}.getOrDefault(false)
