package de.gerritstapper.bikegarage.feature.part

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.data.image.ImageCaptureContracts
import de.gerritstapper.bikegarage.data.image.PendingImage
import java.io.File

@Composable
internal fun PartImagePicker(
    imagePaths: List<String>,
    thumbnailPath: String?,
    enabled: Boolean,
    onSelectFromPhotoLibrary: (List<Uri>) -> Unit,
    onCreatePendingCameraCapture: () -> PendingImage?,
    onCameraCaptureCompleted: (PendingImage, Boolean) -> Unit,
    onThumbnailSelected: (String) -> Unit,
    onImageRemoved: (String) -> Unit,
) {
    var pendingCameraCapture: PendingImage? by remember { mutableStateOf(null) }
    var showImageSourceChoice by rememberSaveable { mutableStateOf(false) }
    val photoLibraryLauncher = rememberLauncherForActivityResult(
        contract = ImageCaptureContracts.selectMultipleFromPhotoLibrary,
        onResult = onSelectFromPhotoLibrary,
    )
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ImageCaptureContracts.takePicture,
        onResult = { isSaved ->
            pendingCameraCapture?.let { onCameraCaptureCompleted(it, isSaved) }
            pendingCameraCapture = null
        },
    )

    if (imagePaths.isEmpty()) {
        OutlinedButton(
            onClick = { showImageSourceChoice = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("part-image-placeholder"),
            enabled = enabled,
        ) {
            Text(stringResource(R.string.part_images))
        }
    } else {
        imagePaths.forEachIndexed { index, storagePath ->
            PartImageRow(
                storagePath = storagePath,
                index = index,
                isThumbnail = storagePath == thumbnailPath,
                enabled = enabled,
                onThumbnailSelected = { onThumbnailSelected(storagePath) },
                onImageRemoved = { onImageRemoved(storagePath) },
            )
        }
        OutlinedButton(
            onClick = { showImageSourceChoice = true },
            modifier = Modifier.testTag("part-image-add"),
            enabled = enabled,
        ) {
            Text("+")
        }
    }

    if (showImageSourceChoice) {
        AlertDialog(
            onDismissRequest = { showImageSourceChoice = false },
            title = { Text(stringResource(R.string.part_images)) },
            text = {
                Column {
                    TextButton(onClick = {
                        showImageSourceChoice = false
                        onCreatePendingCameraCapture()?.let { pendingImage ->
                            pendingCameraCapture = pendingImage
                            cameraLauncher.launch(pendingImage.uri)
                        }
                    }) {
                        Text(stringResource(R.string.part_image_camera))
                    }
                    TextButton(onClick = {
                        showImageSourceChoice = false
                        photoLibraryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }) {
                        Text(stringResource(R.string.part_image_photo_library))
                    }
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun PartImageRow(
    storagePath: String,
    index: Int,
    isThumbnail: Boolean,
    enabled: Boolean,
    onThumbnailSelected: () -> Unit,
    onImageRemoved: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = File(LocalContext.current.filesDir, storagePath),
            contentDescription = stringResource(R.string.part_image_preview, index + 1),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(88.dp)
                .height(88.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isThumbnail) {
                Text(
                    text = stringResource(R.string.part_image_thumbnail),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                TextButton(onClick = onThumbnailSelected, enabled = enabled) {
                    Text(stringResource(R.string.part_image_make_thumbnail))
                }
            }
            TextButton(onClick = onImageRemoved, enabled = enabled) {
                Text(stringResource(R.string.part_image_remove))
            }
        }
    }
}
