package de.gerritstapper.bikegarage.domain.model

data class Bike(
    val id: Long = 0,
    val nickname: String? = null,
    val brand: String,
    val model: String,
    /** ISO-8601 local date, for example 2026-08-21. */
    val purchaseDate: String? = null,
    val shopId: Long? = null,
    val createdAt: Long,
)

data class BikeImage(
    val id: Long = 0,
    val bikeId: Long = 0,
    val storagePath: String,
    val displayOrder: Int,
    val isThumbnail: Boolean,
)

enum class PartCategory {
    DRIVETRAIN,
    BRAKES,
    WHEELS,
    COCKPIT,
    SADDLE,
    SUSPENSION,
    ACCESSORIES,
}

data class Part(
    val id: Long = 0,
    val category: PartCategory,
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

data class PartImage(
    val id: Long = 0,
    val partId: Long = 0,
    val storagePath: String,
    val displayOrder: Int,
    val isThumbnail: Boolean,
)

data class PartAttribute(
    val id: Long = 0,
    val partId: Long = 0,
    val key: String,
    val value: String,
)

data class BikePartCrossRef(
    val bikeId: Long,
    val partId: Long,
)

data class Shop(
    val id: Long = 0,
    val name: String,
    val logoPath: String,
    val createdAt: Long,
)

data class PartShopLink(
    val id: Long = 0,
    val partId: Long = 0,
    val shopId: Long,
    val productUrl: String,
)

data class BikeWithImages(
    val bike: Bike,
    val images: List<BikeImage>,
)

data class BikeWithParts(
    val bike: Bike,
    val parts: List<Part>,
)

data class PartShopLinkWithShop(
    val link: PartShopLink,
    val shop: Shop?,
)

data class PartWithRelations(
    val part: Part,
    val attributes: List<PartAttribute>,
    val bikes: List<Bike>,
    val shopLinks: List<PartShopLinkWithShop>,
    val images: List<PartImage> = emptyList(),
)

data class ShopWithRelations(
    val shop: Shop,
    val purchasedBikes: List<Bike>,
    val parts: List<Part>,
    val partLinks: List<PartShopLink>,
)
