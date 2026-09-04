package de.gerritstapper.bikegarage.feature.shop

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.data.image.PendingImage
import de.gerritstapper.bikegarage.domain.model.Shop
import de.gerritstapper.bikegarage.domain.usecase.ShopUpdateFieldError
import de.gerritstapper.bikegarage.ui.navigation.HomeNavigationActions
import de.gerritstapper.bikegarage.ui.navigation.LocalNavigateHome
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ShopEditScreen(
    onShopUpdated: (Shop) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: ShopEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val goHome = LocalNavigateHome.current
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    var navigateHomeAfterDiscard by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ShopEditEvent.Updated -> onShopUpdated(event.shop)
            }
        }
    }
    DisposableEffect(viewModel) {
        onDispose(viewModel::discardUnsavedLogo)
    }

    fun requestNavigation(navigateHome: Boolean) {
        if (state.isSaving) return
        if (state.isDirty) {
            navigateHomeAfterDiscard = navigateHome
            showDiscardConfirmation = true
        } else if (navigateHome) {
            goHome()
        } else {
            onNavigateUp()
        }
    }

    BackHandler {
        requestNavigation(navigateHome = false)
    }

    ShopEditContent(
        state = state,
        onNameChanged = viewModel::onNameChanged,
        onSelectFromPhotoLibrary = viewModel::importLogo,
        onCreatePendingCameraCapture = viewModel::createPendingLogoCapture,
        onCameraCaptureCompleted = viewModel::completeCameraCapture,
        onSave = viewModel::save,
        onNavigateUp = { requestNavigation(navigateHome = false) },
        onNavigateHome = { requestNavigation(navigateHome = true) },
    )

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(R.string.shop_discard_changes_title)) },
            text = { Text(stringResource(R.string.shop_discard_changes_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        viewModel.discardUnsavedLogo()
                        if (navigateHomeAfterDiscard) goHome() else onNavigateUp()
                    },
                ) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(stringResource(R.string.continue_editing))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShopEditContent(
    state: ShopEditUiState,
    onNameChanged: (String) -> Unit,
    onSelectFromPhotoLibrary: (android.net.Uri) -> Unit,
    onCreatePendingCameraCapture: () -> PendingImage,
    onCameraCaptureCompleted: (PendingImage, Boolean) -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateHome: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.shop_edit)) },
                navigationIcon = {
                    HomeNavigationActions(onHomeClick = onNavigateHome) {
                        TextButton(onClick = onNavigateUp, enabled = !state.isSaving) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (state.shop == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.shop_not_found))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ShopLogoPicker(
                    logoPath = state.logoPath,
                    enabled = !state.isSaving && !state.oldLogoRemovalFailed,
                    onSelectFromPhotoLibrary = onSelectFromPhotoLibrary,
                    onCreatePendingCameraCapture = onCreatePendingCameraCapture,
                    onCameraCaptureCompleted = onCameraCaptureCompleted,
                )

                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    label = { Text(stringResource(R.string.shop_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isSaving && !state.oldLogoRemovalFailed,
                    isError = state.nameError != null,
                    supportingText = {
                        state.nameError?.let { Text(stringResource(it.messageRes())) }
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                )

                if (state.logoError != null) {
                    Text(
                        text = stringResource(state.logoError.messageRes()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (state.logoImportFailed) {
                    Text(
                        text = stringResource(R.string.shop_logo_import_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (state.oldLogoRemovalFailed) {
                    Text(
                        text = stringResource(R.string.shop_old_logo_removal_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

private fun ShopUpdateFieldError.messageRes(): Int = when (this) {
    ShopUpdateFieldError.NAME_REQUIRED -> R.string.shop_name_required
    ShopUpdateFieldError.LOGO_REQUIRED -> R.string.shop_logo_required
}
