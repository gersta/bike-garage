package de.gerritstapper.bikegarage.data.image

import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import java.util.concurrent.Executor
import javax.inject.Inject

object ImageCaptureContracts {
    val takePicture = ActivityResultContracts.TakePicture()
    val selectFromPhotoLibrary = ActivityResultContracts.PickVisualMedia()
    val selectMultipleFromPhotoLibrary = ActivityResultContracts.PickMultipleVisualMedia()
}

class CameraXImageCapture @Inject constructor(
    private val imageStorage: InternalImageStorage,
) {
    fun capture(
        imageCapture: ImageCapture,
        pendingImage: PendingImage,
        executor: Executor,
        onResult: (Result<String>) -> Unit,
    ) {
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            imageStorage.fileForPending(pendingImage),
        ).build()
        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onResult(Result.success(pendingImage.storagePath))
                }

                override fun onError(exception: ImageCaptureException) {
                    onResult(Result.failure(exception))
                }
            },
        )
    }
}
