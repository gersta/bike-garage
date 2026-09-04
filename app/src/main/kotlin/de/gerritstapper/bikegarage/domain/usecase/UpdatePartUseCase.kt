package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartImage
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartTechnicalFields
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import javax.inject.Inject

sealed interface UpdatePartResult {
    data class Updated(val part: Part) : UpdatePartResult
    data class Invalid(val validation: PartCreationValidation) : UpdatePartResult
}

class UpdatePartUseCase @Inject constructor(
    private val partRepository: PartRepository,
) {
    suspend operator fun invoke(
        part: Part,
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
        imagePaths: List<String>? = null,
        thumbnailPath: String? = null,
    ): UpdatePartResult {
        val validation = validatePartForm(category, name, productLinks, inventoryCount)
        if (!validation.isValid) return UpdatePartResult.Invalid(validation)

        val savedCategory = requireNotNull(category)
        val savedType = PartTechnicalFields.canonicalType(type)
            ?.takeIf { PartTechnicalFields.isTypeForCategory(it, savedCategory) }
        val technicalKeys = PartTechnicalFields.forType(savedType).mapTo(mutableSetOf()) { it.key }
        val attributes = technicalAttributes
            .asSequence()
            .filter { (key, value) -> key in technicalKeys && value.isNotBlank() }
            .map { (key, value) -> PartAttribute(key = key, value = value.trim()) }
            .toList()
        val updatedPart = part.copy(
            category = savedCategory,
            type = savedType,
            name = name.trim(),
            manufacturer = manufacturer.trim().takeIf(String::isNotEmpty),
            manufacturerNumber = manufacturerNumber.trim().takeIf(String::isNotEmpty),
            description = description.trim().takeIf(String::isNotEmpty),
            inventoryCount = inventoryCount,
        )
        val links = productLinks.map { link ->
            PartShopLink(
                shopId = requireNotNull(link.shopId),
                productUrl = link.productUrl.trim(),
            )
        }

        if (imagePaths == null) {
            partRepository.savePart(updatedPart, attributes, bikeIds, links)
        } else {
            partRepository.savePartWithImages(
                part = updatedPart,
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
            )
        }
        return UpdatePartResult.Updated(updatedPart)
    }
}
