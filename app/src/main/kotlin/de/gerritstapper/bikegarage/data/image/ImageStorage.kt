package de.gerritstapper.bikegarage.data.image

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PendingImage(
    val storagePath: String,
    val uri: Uri,
)

interface ImageStorage {
    fun newCameraCapture(): PendingImage
    suspend fun import(source: Uri): String
    suspend fun delete(storagePath: String)
    suspend fun discard(pendingImage: PendingImage)
}

@Singleton
class InternalImageStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ImageStorage {
    private val rootDirectory: File
        get() = File(context.filesDir, IMAGE_DIRECTORY)

    override fun newCameraCapture(): PendingImage {
        val storagePath = uniquePath(JPEG_EXTENSION)
        val file = fileFor(storagePath)
        file.parentFile?.mkdirs()
        return PendingImage(
            storagePath = storagePath,
            uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            ),
        )
    }

    fun fileForPending(pendingImage: PendingImage): File = fileFor(pendingImage.storagePath)

    override suspend fun import(source: Uri): String = withContext(Dispatchers.IO) {
        val extension = sourceExtension(source)
        val storagePath = uniquePath(extension)
        val target = fileFor(storagePath)
        target.parentFile?.mkdirs()
        requireNotNull(context.contentResolver.openInputStream(source)) {
            "Bild kann nicht gelesen werden."
        }.use { input ->
            target.outputStream().use(input::copyTo)
        }
        storagePath
    }

    override suspend fun delete(storagePath: String) = withContext(Dispatchers.IO) {
        val file = fileFor(storagePath)
        check(!file.exists() || file.delete()) { "Bild konnte nicht gelöscht werden." }
        Unit
    }

    override suspend fun discard(pendingImage: PendingImage) {
        delete(pendingImage.storagePath)
    }

    private fun sourceExtension(source: Uri): String =
        MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(context.contentResolver.getType(source))
            ?.lowercase()
            ?.takeIf { it.all(Char::isLetterOrDigit) }
            ?: JPEG_EXTENSION

    private fun uniquePath(extension: String): String =
        "$IMAGE_DIRECTORY/${UUID.randomUUID()}.$extension"

    private fun fileFor(storagePath: String): File {
        require(!File(storagePath).isAbsolute) { "Bildpfade müssen relativ sein." }
        val root = rootDirectory.canonicalFile
        val target = File(context.filesDir, storagePath).canonicalFile
        require(target.path.startsWith("${root.path}${File.separator}")) {
            "Bildpfad liegt außerhalb des App-Speichers."
        }
        return target
    }

    private companion object {
        const val IMAGE_DIRECTORY = "images"
        const val JPEG_EXTENSION = "jpg"
    }
}
