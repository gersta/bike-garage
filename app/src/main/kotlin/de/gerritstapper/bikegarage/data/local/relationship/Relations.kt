package de.gerritstapper.bikegarage.data.local.relationship

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import de.gerritstapper.bikegarage.data.local.entity.BikeEntity
import de.gerritstapper.bikegarage.data.local.entity.BikeImageEntity
import de.gerritstapper.bikegarage.data.local.entity.BikePartCrossRefEntity
import de.gerritstapper.bikegarage.data.local.entity.PartAttributeEntity
import de.gerritstapper.bikegarage.data.local.entity.PartEntity
import de.gerritstapper.bikegarage.data.local.entity.PartImageEntity
import de.gerritstapper.bikegarage.data.local.entity.PartShopLinkEntity
import de.gerritstapper.bikegarage.data.local.entity.ShopEntity

data class BikeWithImagesEntity(
    @Embedded val bike: BikeEntity,
    @Relation(parentColumn = "id", entityColumn = "bikeId")
    val images: List<BikeImageEntity>,
)

data class BikeWithPartsEntity(
    @Embedded val bike: BikeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BikePartCrossRefEntity::class,
            parentColumn = "bikeId",
            entityColumn = "partId",
        ),
    )
    val parts: List<PartEntity>,
)

data class PartWithRelationsEntity(
    @Embedded val part: PartEntity,
    @Relation(parentColumn = "id", entityColumn = "partId")
    val images: List<PartImageEntity>,
    @Relation(parentColumn = "id", entityColumn = "partId")
    val attributes: List<PartAttributeEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BikePartCrossRefEntity::class,
            parentColumn = "partId",
            entityColumn = "bikeId",
        ),
    )
    val bikes: List<BikeEntity>,
    @Relation(parentColumn = "id", entityColumn = "partId")
    val shopLinks: List<PartShopLinkEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PartShopLinkEntity::class,
            parentColumn = "partId",
            entityColumn = "shopId",
        ),
    )
    val shops: List<ShopEntity>,
)

data class ShopWithRelationsEntity(
    @Embedded val shop: ShopEntity,
    @Relation(parentColumn = "id", entityColumn = "shopId")
    val purchasedBikes: List<BikeEntity>,
    @Relation(parentColumn = "id", entityColumn = "shopId")
    val partLinks: List<PartShopLinkEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PartShopLinkEntity::class,
            parentColumn = "shopId",
            entityColumn = "partId",
        ),
    )
    val parts: List<PartEntity>,
)
