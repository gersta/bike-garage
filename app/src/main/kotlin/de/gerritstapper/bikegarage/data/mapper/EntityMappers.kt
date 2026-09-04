package de.gerritstapper.bikegarage.data.mapper

import de.gerritstapper.bikegarage.data.local.entity.BikeEntity
import de.gerritstapper.bikegarage.data.local.entity.BikeImageEntity
import de.gerritstapper.bikegarage.data.local.entity.PartAttributeEntity
import de.gerritstapper.bikegarage.data.local.entity.PartEntity
import de.gerritstapper.bikegarage.data.local.entity.PartImageEntity
import de.gerritstapper.bikegarage.data.local.entity.PartShopLinkEntity
import de.gerritstapper.bikegarage.data.local.entity.ShopEntity
import de.gerritstapper.bikegarage.data.local.relationship.BikeWithImagesEntity
import de.gerritstapper.bikegarage.data.local.relationship.BikeWithPartsEntity
import de.gerritstapper.bikegarage.data.local.relationship.PartWithRelationsEntity
import de.gerritstapper.bikegarage.data.local.relationship.ShopWithRelationsEntity
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.BikeImage
import de.gerritstapper.bikegarage.domain.model.BikeWithImages
import de.gerritstapper.bikegarage.domain.model.BikeWithParts
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.model.PartAttribute
import de.gerritstapper.bikegarage.domain.model.PartCategory
import de.gerritstapper.bikegarage.domain.model.PartImage
import de.gerritstapper.bikegarage.domain.model.PartShopLink
import de.gerritstapper.bikegarage.domain.model.PartShopLinkWithShop
import de.gerritstapper.bikegarage.domain.model.PartWithRelations
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations

fun BikeEntity.toDomain() = Bike(id, nickname, brand, model, purchaseDate, shopId, createdAt)

fun Bike.toEntity() = BikeEntity(id, nickname, brand, model, purchaseDate, shopId, createdAt)

fun BikeImageEntity.toDomain() = BikeImage(id, bikeId, storagePath, displayOrder, isThumbnail)

fun BikeImage.toEntity(bikeId: Long) =
    BikeImageEntity(id, bikeId, storagePath, displayOrder, isThumbnail)

fun PartEntity.toDomain() = Part(
    id = id,
    category = PartCategory.valueOf(category),
    type = type,
    name = name,
    manufacturer = manufacturer,
    manufacturerNumber = manufacturerNumber,
    description = description,
    createdAt = createdAt,
    inventoryCount = inventoryCount,
)

fun Part.toEntity() = PartEntity(
    id = id,
    category = category.name,
    type = type,
    name = name,
    manufacturer = manufacturer,
    manufacturerNumber = manufacturerNumber,
    description = description,
    createdAt = createdAt,
    inventoryCount = inventoryCount,
)

fun PartImageEntity.toDomain() = PartImage(id, partId, storagePath, displayOrder, isThumbnail)

fun PartImage.toEntity(partId: Long) =
    PartImageEntity(id, partId, storagePath, displayOrder, isThumbnail)

fun PartAttributeEntity.toDomain() = PartAttribute(id, partId, attributeKey, value)

fun PartAttribute.toEntity(partId: Long) = PartAttributeEntity(id, partId, key, value)

fun PartShopLinkEntity.toDomain() = PartShopLink(id, partId, shopId, productUrl)

fun PartShopLink.toEntity(partId: Long) = PartShopLinkEntity(id, partId, shopId, productUrl)

fun ShopEntity.toDomain() = Shop(id, name, logoPath, createdAt)

fun Shop.toEntity() = ShopEntity(id, name, logoPath, createdAt)

fun BikeWithImagesEntity.toDomain() = BikeWithImages(bike.toDomain(), images.map { it.toDomain() })

fun BikeWithPartsEntity.toDomain() = BikeWithParts(bike.toDomain(), parts.map { it.toDomain() })

fun PartWithRelationsEntity.toDomain(): PartWithRelations {
    val shopsById = shops.associateBy { it.id }
    return PartWithRelations(
        part = part.toDomain(),
        images = images.map { it.toDomain() },
        attributes = attributes.map { it.toDomain() },
        bikes = bikes.map { it.toDomain() },
        shopLinks = shopLinks.map { link ->
            PartShopLinkWithShop(link.toDomain(), shopsById[link.shopId]?.toDomain())
        },
    )
}

fun ShopWithRelationsEntity.toDomain() = ShopWithRelations(
    shop = shop.toDomain(),
    purchasedBikes = purchasedBikes.map { it.toDomain() },
    parts = parts.map { it.toDomain() },
    partLinks = partLinks.map { it.toDomain() },
)
