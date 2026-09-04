package de.gerritstapper.bikegarage.feature.shop

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.data.image.ImageCaptureContracts
import de.gerritstapper.bikegarage.data.image.PendingImage
import java.io.File

@Composable
internal fun ShopLogoPicker(
    logoPath: String?,
    enabled: Boolean,
    onSelectFromPhotoLibrary: (Uri) -> Unit,
    onCreatePendingCameraCapture: () -> PendingImage,
    onCameraCaptureCompleted: (PendingImage, Boolean) -> Unit,
) {
    var showSourceChoice by remember { mutableStateOf(false) }
    var pendingCameraCapture: PendingImage? by remember { mutableStateOf(null) }
    val photoLibraryLauncher = rememberLauncherForActivityResult(
        contract = ImageCaptureContracts.selectFromPhotoLibrary,
        onResult = { uri -> uri?.let(onSelectFromPhotoLibrary) },
    )
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ImageCaptureContracts.takePicture,
        onResult = { isSaved ->
            pendingCameraCapture?.let { onCameraCaptureCompleted(it, isSaved) }
            pendingCameraCapture = null
        },
    )
    val previewDescription = stringResource(R.string.shop_logo_preview)

    Surface(
        onClick = { showSourceChoice = true },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp, max = 280.dp)
            .semantics { contentDescription = previewDescription },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        if (logoPath == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .alpha(if (enabled) 1f else 0.6f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.ic_shop),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = stringResource(R.string.shop_logo_label),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            AsyncImage(
                model = File(LocalContext.current.filesDir, logoPath),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showSourceChoice) {
        AlertDialog(
            onDismissRequest = { showSourceChoice = false },
            title = { Text(stringResource(R.string.shop_logo_label)) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showSourceChoice = false
                            pendingCameraCapture = onCreatePendingCameraCapture()
                            cameraLauncher.launch(requireNotNull(pendingCameraCapture).uri)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.shop_logo_camera))
                    }
                    TextButton(
                        onClick = {
                            showSourceChoice = false
                            photoLibraryLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.shop_logo_photo_library))
                    }
                }
            },
            confirmButton = {},
        )
    }
}
