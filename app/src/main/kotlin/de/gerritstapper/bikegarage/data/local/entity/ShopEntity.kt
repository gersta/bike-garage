package de.gerritstapper.bikegarage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val logoPath: String,
    val createdAt: Long,
)

@Entity(
    tableName = "part_shop_links",
    foreignKeys = [
        ForeignKey(
            entity = PartEntity::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("partId"), Index("shopId")],
)
data class PartShopLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partId: Long,
    val shopId: Long,
    val productUrl: String,
)
