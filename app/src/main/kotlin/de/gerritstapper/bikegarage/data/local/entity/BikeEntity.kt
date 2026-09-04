package de.gerritstapper.bikegarage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bikes",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("shopId")],
)
data class BikeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String? = null,
    val brand: String,
    val model: String,
    val purchaseDate: String? = null,
    val shopId: Long? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "bike_images",
    foreignKeys = [
        ForeignKey(
            entity = BikeEntity::class,
            parentColumns = ["id"],
            childColumns = ["bikeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bikeId")],
)
data class BikeImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bikeId: Long,
    val storagePath: String,
    val displayOrder: Int,
    val isThumbnail: Boolean,
)
