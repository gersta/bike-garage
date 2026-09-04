package de.gerritstapper.bikegarage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "parts", indices = [Index("category")])
data class PartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val type: String? = null,
    val name: String,
    val manufacturer: String? = null,
    val manufacturerNumber: String? = null,
    val description: String? = null,
    val createdAt: Long,
    val inventoryCount: Int? = null,
) {
    init {
        require(inventoryCount == null || inventoryCount >= 0) {
            "Der Inventarbestand darf nicht negativ sein."
        }
    }
}

@Entity(
    tableName = "part_images",
    foreignKeys = [
        ForeignKey(
            entity = PartEntity::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("partId")],
)
data class PartImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partId: Long,
    val storagePath: String,
    val displayOrder: Int,
    val isThumbnail: Boolean,
)

@Entity(
    tableName = "part_attributes",
    foreignKeys = [
        ForeignKey(
            entity = PartEntity::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["partId", "attributeKey"], unique = true)],
)
data class PartAttributeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partId: Long,
    val attributeKey: String,
    val value: String,
)

@Entity(
    tableName = "bike_part_cross_refs",
    primaryKeys = ["bikeId", "partId"],
    foreignKeys = [
        ForeignKey(
            entity = BikeEntity::class,
            parentColumns = ["id"],
            childColumns = ["bikeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PartEntity::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("partId")],
)
data class BikePartCrossRefEntity(
    val bikeId: Long,
    val partId: Long,
)
