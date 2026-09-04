package de.gerritstapper.bikegarage.domain.usecase

import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.model.ShopWithRelations
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class ShopProductReference(
    val partName: String,
    val productUrl: String,
)

data class ShopDeletionBlockers(
    val purchasedBikes: List<Bike>,
    val productReferences: List<ShopProductReference>,
) {
    val isEmpty: Boolean
        get() = purchasedBikes.isEmpty() && productReferences.isEmpty()
}

sealed interface ShopDeletionInspection {
    data class Ready(val shop: Shop) : ShopDeletionInspection
    data class Blocked(
        val shop: Shop,
        val blockers: ShopDeletionBlockers,
    ) : ShopDeletionInspection

    data object NotFound : ShopDeletionInspection
}

sealed interface DeleteShopResult {
    data class Deleted(val shop: Shop) : DeleteShopResult
    data class Blocked(val blockers: ShopDeletionBlockers) : DeleteShopResult
    data object NotFound : DeleteShopResult
    data object Failed : DeleteShopResult
}

class DeleteShopUseCase @Inject constructor(
    private val shopRepository: ShopRepository,
) {
    suspend fun inspect(shopId: Long): ShopDeletionInspection =
        shopRepository.observeShop(shopId).first()?.let(::toInspection)
            ?: ShopDeletionInspection.NotFound

    suspend fun delete(shopId: Long): DeleteShopResult = when (val inspection = inspect(shopId)) {
        is ShopDeletionInspection.Blocked -> DeleteShopResult.Blocked(inspection.blockers)
        ShopDeletionInspection.NotFound -> DeleteShopResult.NotFound
        is ShopDeletionInspection.Ready -> {
            if (shopRepository.deleteShopIfUnlinked(inspection.shop)) {
                DeleteShopResult.Deleted(inspection.shop)
            } else {
                when (val refreshedInspection = inspect(shopId)) {
                    is ShopDeletionInspection.Blocked ->
                        DeleteShopResult.Blocked(refreshedInspection.blockers)
                    ShopDeletionInspection.NotFound -> DeleteShopResult.NotFound
                    is ShopDeletionInspection.Ready -> DeleteShopResult.Failed
                }
            }
        }
    }

    private fun toInspection(relations: ShopWithRelations): ShopDeletionInspection {
        val partsById = relations.parts.associateBy { it.id }
        val blockers = ShopDeletionBlockers(
            purchasedBikes = relations.purchasedBikes,
            productReferences = relations.partLinks.map { link ->
                ShopProductReference(
                    partName = partsById[link.partId]?.name ?: "Teil #${link.partId}",
                    productUrl = link.productUrl,
                )
            },
        )
        return if (blockers.isEmpty) {
            ShopDeletionInspection.Ready(relations.shop)
        } else {
            ShopDeletionInspection.Blocked(relations.shop, blockers)
        }
    }
}
