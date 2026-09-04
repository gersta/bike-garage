package de.gerritstapper.bikegarage.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.feature.bike.BikeCreationScreen
import de.gerritstapper.bikegarage.feature.bike.BikeDetailsScreen
import de.gerritstapper.bikegarage.feature.bike.BikeEditScreen
import de.gerritstapper.bikegarage.feature.bike.BikeOverviewScreen
import de.gerritstapper.bikegarage.feature.garage.GarageOverviewScreen
import de.gerritstapper.bikegarage.feature.garage.GaragePlaceholderScreen
import de.gerritstapper.bikegarage.feature.part.PartAssignmentScreen
import de.gerritstapper.bikegarage.feature.part.PartCreationScreen
import de.gerritstapper.bikegarage.feature.part.PartDetailsScreen
import de.gerritstapper.bikegarage.feature.part.PartEditScreen
import de.gerritstapper.bikegarage.feature.part.PartsOverviewScreen
import de.gerritstapper.bikegarage.feature.shop.ShopCreationScreen
import de.gerritstapper.bikegarage.feature.shop.ShopDetailsScreen
import de.gerritstapper.bikegarage.feature.shop.ShopEditScreen
import de.gerritstapper.bikegarage.feature.shop.ShopOverviewScreen
import de.gerritstapper.bikegarage.domain.model.Shop

internal const val GARAGE_OVERVIEW_ROUTE = "garage_overview"
internal const val BIKES_OVERVIEW_ROUTE = "bikes_overview"
internal const val BIKE_CREATION_ROUTE = "bike_creation"
internal const val BIKE_ID_ARGUMENT = "bikeId"
internal const val BIKE_EDIT_ROUTE = "bike_edit/{$BIKE_ID_ARGUMENT}"
internal const val BIKE_DETAILS_ROUTE = "bike_details/{$BIKE_ID_ARGUMENT}"
internal const val PARTS_OVERVIEW_ROUTE = "parts_overview"
internal const val PART_ID_ARGUMENT = "partId"
internal const val PART_CREATION_ROUTE = "part_creation?bikeId={$BIKE_ID_ARGUMENT}"
internal const val PART_ASSIGNMENT_ROUTE = "part_assignment/{$BIKE_ID_ARGUMENT}"
internal const val PART_DETAILS_ROUTE = "part_details/{$PART_ID_ARGUMENT}"
internal const val PART_EDIT_ROUTE = "part_edit/{$PART_ID_ARGUMENT}"
internal const val TOOLS_ROUTE = "tools"
internal const val CLOTHES_ROUTE = "clothes"
internal const val SHOPS_OVERVIEW_ROUTE = "shops_overview"
internal const val SHOP_ID_ARGUMENT = "shopId"
internal const val SHOP_DETAILS_ROUTE = "shop_details/{$SHOP_ID_ARGUMENT}"
internal const val SHOP_EDIT_ROUTE = "shop_edit/{$SHOP_ID_ARGUMENT}"
internal const val SHOP_CREATION_CONTEXT_ARGUMENT = "returnContext"
internal const val SHOP_CREATION_ROUTE = "shop_creation/{$SHOP_CREATION_CONTEXT_ARGUMENT}"

internal fun shopDetailsRoute(shopId: Long) = "shop_details/$shopId"
internal fun shopEditRoute(shopId: Long) = "shop_edit/$shopId"
internal fun bikeDetailsRoute(bikeId: Long) = "bike_details/$bikeId"
internal fun bikeEditRoute(bikeId: Long) = "bike_edit/$bikeId"
internal fun partDetailsRoute(partId: Long) = "part_details/$partId"
internal fun partEditRoute(partId: Long) = "part_edit/$partId"
internal fun partCreationRoute(bikeId: Long? = null): String =
    bikeId?.takeIf { it > 0 }?.let { "part_creation?bikeId=$it" } ?: "part_creation"
internal fun partAssignmentRoute(bikeId: Long) = "part_assignment/$bikeId"
internal fun shopCreationRoute(context: ShopCreationReturnContext) =
    "shop_creation/${context.routeValue}"

internal enum class ShopCreationReturnContext(
    internal val routeValue: String,
    internal val resultKey: String,
) {
    SHOP_OVERVIEW("shop-overview", "shop-creation-result-shop-overview"),
    BIKE_FORM("bike-form", "shop-creation-result-bike-form"),
    BIKE_DETAILS("bike-details", "shop-creation-result-bike-details"),
    PART_FORM("part-form", "shop-creation-result-part-form"),
    ;

    companion object {
        fun fromRouteValue(value: String?): ShopCreationReturnContext =
            entries.firstOrNull { it.routeValue == value } ?: SHOP_OVERVIEW
    }
}

internal data class ShopCreationResult(
    val selectedShopId: Long,
)

internal fun SavedStateHandle.setShopCreationResult(
    context: ShopCreationReturnContext,
    shop: Shop,
) {
    check(shop.id > 0) { "Ein neuer Shop muss eine gespeicherte ID haben." }
    set(context.resultKey, shop.id)
}

internal fun SavedStateHandle.consumeShopCreationResult(
    context: ShopCreationReturnContext,
): ShopCreationResult? =
    get<Long>(context.resultKey)?.let { selectedShopId ->
        remove<Long>(context.resultKey)
        ShopCreationResult(selectedShopId)
    }

internal fun NavController.navigateToShopCreation(context: ShopCreationReturnContext) {
    navigate(shopCreationRoute(context))
}

internal fun NavController.returnFromShopCreation(
    context: ShopCreationReturnContext,
    shop: Shop,
) {
    checkNotNull(previousBackStackEntry) {
        "Shop-Erstellung benötigt ein Rückkehrziel."
    }.savedStateHandle.setShopCreationResult(context, shop)
    popBackStack()
}

internal fun NavController.navigateHome() {
    if (!popBackStack(GARAGE_OVERVIEW_ROUTE, inclusive = false)) {
        navigate(GARAGE_OVERVIEW_ROUTE) {
            popUpTo(graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
}

@Composable
fun BikeGarageNavigation() {
    val navController = rememberNavController()
    CompositionLocalProvider(LocalNavigateHome provides navController::navigateHome) {
        NavHost(navController = navController, startDestination = GARAGE_OVERVIEW_ROUTE) {
        composable(GARAGE_OVERVIEW_ROUTE) {
            GarageOverviewScreen(
                onBikesClick = { navController.navigate(BIKES_OVERVIEW_ROUTE) },
                onPartsClick = { navController.navigate(PARTS_OVERVIEW_ROUTE) },
                onToolsClick = { navController.navigate(TOOLS_ROUTE) },
                onClothesClick = { navController.navigate(CLOTHES_ROUTE) },
                onShopsClick = { navController.navigate(SHOPS_OVERVIEW_ROUTE) },
            )
        }
        composable(BIKES_OVERVIEW_ROUTE) {
            BikeOverviewScreen(
                onBikeClick = { bikeId -> navController.navigate(bikeDetailsRoute(bikeId)) },
                onCreateBike = { navController.navigate(BIKE_CREATION_ROUTE) },
            )
        }
        composable(BIKE_CREATION_ROUTE) { backStackEntry ->
            val selectedShopId by backStackEntry.savedStateHandle
                .getStateFlow<Long?>(ShopCreationReturnContext.BIKE_FORM.resultKey, null)
                .collectAsStateWithLifecycle()
            BikeCreationScreen(
                selectedShopId = selectedShopId,
                onShopSelectionConsumed = {
                    backStackEntry.savedStateHandle.consumeShopCreationResult(
                        ShopCreationReturnContext.BIKE_FORM,
                    )
                },
                onCreateShop = {
                    navController.navigateToShopCreation(ShopCreationReturnContext.BIKE_FORM)
                },
                onBikeCreated = { bikeId ->
                    navController.navigate(bikeDetailsRoute(bikeId)) {
                        popUpTo(BIKE_CREATION_ROUTE) { inclusive = true }
                    }
                },
                onNavigateUp = navController::navigateUp,
            )
        }
        composable(
            route = BIKE_EDIT_ROUTE,
            arguments = listOf(navArgument(BIKE_ID_ARGUMENT) { type = NavType.LongType }),
        ) { backStackEntry ->
            val selectedShopId by backStackEntry.savedStateHandle
                .getStateFlow<Long?>(ShopCreationReturnContext.BIKE_FORM.resultKey, null)
                .collectAsStateWithLifecycle()
            BikeEditScreen(
                selectedShopId = selectedShopId,
                onShopSelectionConsumed = {
                    backStackEntry.savedStateHandle.consumeShopCreationResult(
                        ShopCreationReturnContext.BIKE_FORM,
                    )
                },
                onCreateShop = {
                    navController.navigateToShopCreation(ShopCreationReturnContext.BIKE_FORM)
                },
                onBikeUpdated = { bike ->
                    navController.navigate(bikeDetailsRoute(bike.id)) {
                        popUpTo(BIKE_EDIT_ROUTE) { inclusive = true }
                    }
                },
                onNavigateUp = navController::navigateUp,
            )
        }
        composable(PARTS_OVERVIEW_ROUTE) {
            PartsOverviewScreen(
                onCreatePart = { navController.navigate(partCreationRoute()) },
                onPartClick = { partId -> navController.navigate(partDetailsRoute(partId)) },
            )
        }
        composable(
            route = BIKE_DETAILS_ROUTE,
            arguments = listOf(navArgument(BIKE_ID_ARGUMENT) { type = NavType.LongType }),
        ) { backStackEntry ->
            val bikeId = checkNotNull(backStackEntry.arguments?.getLong(BIKE_ID_ARGUMENT))
            val selectedShopId by backStackEntry.savedStateHandle
                .getStateFlow<Long?>(ShopCreationReturnContext.BIKE_DETAILS.resultKey, null)
                .collectAsStateWithLifecycle()
            BikeDetailsScreen(
                selectedShopId = selectedShopId,
                onShopSelectionConsumed = {
                    backStackEntry.savedStateHandle.consumeShopCreationResult(
                        ShopCreationReturnContext.BIKE_DETAILS,
                    )
                },
                onEdit = { navController.navigate(bikeEditRoute(bikeId)) },
                onShopClick = { shopId -> navController.navigate(shopDetailsRoute(shopId)) },
                onCreateShop = {
                    navController.navigateToShopCreation(ShopCreationReturnContext.BIKE_DETAILS)
                },
                onPartClick = { partId -> navController.navigate(partDetailsRoute(partId)) },
                onCreatePart = { navController.navigate(partCreationRoute(bikeId)) },
                onAssignExistingPart = {
                    navController.navigate(partAssignmentRoute(bikeId))
                },
                onDeleted = {
                    navController.popBackStack(BIKES_OVERVIEW_ROUTE, inclusive = false)
                },
            )
        }
        composable(
            route = PART_CREATION_ROUTE,
            arguments = listOf(
                navArgument(BIKE_ID_ARGUMENT) {
                    type = NavType.LongType
                    defaultValue = 0L
                },
            ),
        ) { backStackEntry ->
            val selectedShopId by backStackEntry.savedStateHandle
                .getStateFlow<Long?>(ShopCreationReturnContext.PART_FORM.resultKey, null)
                .collectAsStateWithLifecycle()
            PartCreationScreen(
                selectedShopId = selectedShopId,
                onShopSelectionConsumed = {
                    backStackEntry.savedStateHandle.consumeShopCreationResult(
                        ShopCreationReturnContext.PART_FORM,
                    )
                },
                onCreateShop = {
                    navController.navigateToShopCreation(ShopCreationReturnContext.PART_FORM)
                },
                onPartCreated = navController::navigateUp,
                onNavigateUp = navController::navigateUp,
            )
        }
        composable(
            route = PART_ASSIGNMENT_ROUTE,
            arguments = listOf(navArgument(BIKE_ID_ARGUMENT) { type = NavType.LongType }),
        ) {
            PartAssignmentScreen(
                onAssigned = { navController.popBackStack() },
                onNavigateUp = navController::navigateUp,
            )
        }
        composable(
            route = PART_DETAILS_ROUTE,
            arguments = listOf(navArgument(PART_ID_ARGUMENT) { type = NavType.LongType }),
        ) { backStackEntry ->
            val partId = checkNotNull(backStackEntry.arguments?.getLong(PART_ID_ARGUMENT))
            PartDetailsScreen(
                onEdit = { navController.navigate(partEditRoute(partId)) },
                onBikeClick = { bikeId -> navController.navigate(bikeDetailsRoute(bikeId)) },
                onDeleted = { navController.popBackStack() },
            )
        }
        composable(
            route = PART_EDIT_ROUTE,
            arguments = listOf(navArgument(PART_ID_ARGUMENT) { type = NavType.LongType }),
        ) { backStackEntry ->
            val selectedShopId by backStackEntry.savedStateHandle
                .getStateFlow<Long?>(ShopCreationReturnContext.PART_FORM.resultKey, null)
                .collectAsStateWithLifecycle()
            PartEditScreen(
                selectedShopId = selectedShopId,
                onShopSelectionConsumed = {
                    backStackEntry.savedStateHandle.consumeShopCreationResult(
                        ShopCreationReturnContext.PART_FORM,
                    )
                },
                onCreateShop = {
                    navController.navigateToShopCreation(ShopCreationReturnContext.PART_FORM)
                },
                onPartUpdated = { navController.popBackStack() },
                onNavigateUp = navController::navigateUp,
            )
        }
        composable(TOOLS_ROUTE) {
            GaragePlaceholderScreen(
                titleRes = R.string.tools,
                messageRes = R.string.tools_empty_state,
            )
        }
        composable(CLOTHES_ROUTE) {
            GaragePlaceholderScreen(
                titleRes = R.string.clothes,
                messageRes = R.string.clothes_empty_state,
            )
        }
        composable(SHOPS_OVERVIEW_ROUTE) {
            ShopOverviewScreen(
                onShopClick = { shopId -> navController.navigate(shopDetailsRoute(shopId)) },
                onCreateShop = {
                    navController.navigateToShopCreation(ShopCreationReturnContext.SHOP_OVERVIEW)
                },
            )
        }
        composable(
            route = SHOP_CREATION_ROUTE,
            arguments = listOf(
                navArgument(SHOP_CREATION_CONTEXT_ARGUMENT) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val context = ShopCreationReturnContext.fromRouteValue(
                backStackEntry.arguments?.getString(SHOP_CREATION_CONTEXT_ARGUMENT),
            )
            ShopCreationScreen(
                onShopCreated = { shop ->
                    navController.returnFromShopCreation(context, shop)
                },
                onNavigateUp = navController::navigateUp,
            )
        }
        composable(
            route = SHOP_DETAILS_ROUTE,
            arguments = listOf(navArgument(SHOP_ID_ARGUMENT) { type = NavType.LongType }),
        ) { backStackEntry ->
            val shopId = checkNotNull(backStackEntry.arguments?.getLong(SHOP_ID_ARGUMENT))
            ShopDetailsScreen(
                onEdit = { navController.navigate(shopEditRoute(shopId)) },
                onBikeClick = { bikeId -> navController.navigate(bikeDetailsRoute(bikeId)) },
                onPartClick = { partId -> navController.navigate(partDetailsRoute(partId)) },
                onDeleted = {
                    navController.popBackStack(SHOPS_OVERVIEW_ROUTE, inclusive = false)
                },
            )
        }
        composable(
            route = SHOP_EDIT_ROUTE,
            arguments = listOf(navArgument(SHOP_ID_ARGUMENT) { type = NavType.LongType }),
        ) {
            ShopEditScreen(
                onShopUpdated = { shop ->
                    navController.navigate(shopDetailsRoute(shop.id)) {
                        popUpTo(SHOP_DETAILS_ROUTE) { inclusive = true }
                    }
                },
                onNavigateUp = navController::navigateUp,
            )
            }
        }
    }
}
