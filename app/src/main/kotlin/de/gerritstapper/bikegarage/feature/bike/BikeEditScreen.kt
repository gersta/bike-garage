package de.gerritstapper.bikegarage.feature.bike

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Bike
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationActions
import de.gerritstapper.bikegarage.ui.navigation.LocalNavigateHome
import kotlinx.coroutines.flow.collectLatest

@Composable
fun BikeEditScreen(
    selectedShopId: Long?,
    onShopSelectionConsumed: () -> Unit,
    onCreateShop: () -> Unit,
    onBikeUpdated: (Bike) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: BikeEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val shops by viewModel.shops.collectAsStateWithLifecycle()
    val goHome = LocalNavigateHome.current
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    var navigateHomeAfterDiscard by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedShopId) {
        selectedShopId?.let {
            viewModel.onShopSelected(it)
            onShopSelectionConsumed()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is BikeEditEvent.Updated -> onBikeUpdated(event.bike)
            }
        }
    }

    fun requestNavigation(navigateHome: Boolean) {
        if (state.isBusy || state.hasPendingImageRemoval) return
        if (state.isDirty) {
            navigateHomeAfterDiscard = navigateHome
            showDiscardConfirmation = true
        } else {
            if (navigateHome) goHome() else onNavigateUp()
        }
    }

    BackHandler {
        requestNavigation(navigateHome = false)
    }

    BikeEditContent(
        state = state,
        shops = shops,
        onNicknameChanged = viewModel::onNicknameChanged,
        onBrandChanged = viewModel::onBrandChanged,
        onModelChanged = viewModel::onModelChanged,
        onPurchaseDateChanged = viewModel::onPurchaseDateChanged,
        onShopSelected = viewModel::onShopSelected,
        onCreateShop = onCreateShop,
        onSelectFromPhotoLibrary = viewModel::importImages,
        onCreatePendingCameraCapture = viewModel::createPendingCameraCapture,
        onCameraCaptureCompleted = viewModel::completeCameraCapture,
        onThumbnailSelected = viewModel::selectThumbnail,
        onImageRemoved = viewModel::removeImage,
        onSave = viewModel::save,
        onNavigateUp = { requestNavigation(navigateHome = false) },
        onNavigateHome = { requestNavigation(navigateHome = true) },
    )

    if (showDiscardConfirmation) {
        BikeDiscardChangesDialog(
            onDiscard = {
                showDiscardConfirmation = false
                viewModel.discardUnsavedImages {
                    if (navigateHomeAfterDiscard) goHome() else onNavigateUp()
                }
            },
            onContinueEditing = { showDiscardConfirmation = false },
        )
    }
}

@Composable
internal fun BikeEditContent(
    state: BikeEditUiState,
    shops: List<Shop>,
    onNicknameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    onPurchaseDateChanged: (String) -> Unit,
    onShopSelected: (Long?) -> Unit,
    onCreateShop: () -> Unit,
    onSelectFromPhotoLibrary: (List<Uri>) -> Unit,
    onCreatePendingCameraCapture: () -> PendingImage?,
    onCameraCaptureCompleted: (PendingImage, Boolean) -> Unit,
    onThumbnailSelected: (String) -> Unit,
    onImageRemoved: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateHome: (() -> Unit)? = null,
    knownBrands: List<String> = BikeBrandCatalog.knownBrands,
) {
    when {
        state.isLoading -> BikeEditStatusContent(
            onNavigateUp = onNavigateUp,
            onNavigateHome = onNavigateHome,
            content = { CircularProgressIndicator() },
        )

        state.bike == null -> BikeEditStatusContent(
            onNavigateUp = onNavigateUp,
            content = { Text(stringResource(R.string.bike_not_found)) },
        )

        else -> BikeFormContent(
            titleRes = R.string.bike_edit,
            state = BikeFormUiState(
                nickname = state.nickname,
                brand = state.brand,
                model = state.model,
                purchaseDate = state.purchaseDate,
                shopId = state.shopId,
                imagePaths = state.imagePaths,
                thumbnailPath = state.thumbnailPath,
                brandError = state.brandError?.messageRes(),
                modelError = state.modelError?.messageRes(),
                imageError = state.imageError?.messageRes(),
                purchaseDateError = state.purchaseDateError?.messageRes(),
                imageImportFailed = state.imageImportFailed,
                imageRemovalError = when {
                    state.obsoleteImageRemovalFailed ->
                        R.string.bike_obsolete_image_removal_failed

                    state.imageRemovalFailed -> R.string.bike_image_removal_failed
                    else -> null
                },
                saveFailed = state.saveFailed,
                isInputEnabled = state.canEdit,
                isSaveEnabled = !state.isBusy,
                isNavigationEnabled = state.canEdit,
            ),
            shops = shops,
            onNicknameChanged = onNicknameChanged,
            onBrandChanged = onBrandChanged,
            onModelChanged = onModelChanged,
            onPurchaseDateChanged = onPurchaseDateChanged,
            onShopSelected = onShopSelected,
            onCreateShop = onCreateShop,
            onSelectFromPhotoLibrary = onSelectFromPhotoLibrary,
            onCreatePendingCameraCapture = onCreatePendingCameraCapture,
            onCameraCaptureCompleted = onCameraCaptureCompleted,
            onThumbnailSelected = onThumbnailSelected,
            onImageRemoved = onImageRemoved,
            onSave = onSave,
            onNavigateUp = onNavigateUp,
            onNavigateHome = onNavigateHome,
            knownBrands = knownBrands,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BikeEditStatusContent(
    onNavigateUp: () -> Unit,
    onNavigateHome: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.bike_edit)) },
                navigationIcon = {
                    HomeNavigationActions(onHomeClick = onNavigateHome) {
                        TextButton(onClick = onNavigateUp) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
