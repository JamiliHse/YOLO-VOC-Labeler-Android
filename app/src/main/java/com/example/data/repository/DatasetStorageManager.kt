package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DatasetStorageManager(private val context: Context) {

    /**
     * Writes text content to a destination Uri selected via SAF CreateDocument
     */
    suspend fun saveAnnotationToFile(
        targetUri: Uri,
        content: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: throw IllegalStateException("Could not open output stream for URI: $targetUri")
        }
    }

    /**
     * Saves a temporary file in cache and returns a shareable content Uri via FileProvider
     */
    suspend fun createShareableFile(
        filename: String,
        content: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val shareDir = File(context.cacheDir, "shared_datasets").apply { mkdirs() }
            val file = File(shareDir, filename)
            FileOutputStream(file).use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }

    /**
     * Saves a cropped or edited Bitmap to cache directory and provides its URI
     */
    suspend fun saveBitmapToCache(
        bitmap: Bitmap,
        filename: String = "image_${System.currentTimeMillis()}.jpg"
    ): Result<Pair<Uri, File>> = withContext(Dispatchers.IO) {
        runCatching {
            val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(imagesDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                out.flush()
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Pair(uri, file)
        }
    }

    /**
     * Reads text from a content URI (e.g. imported XML or TXT)
     */
    suspend fun readTextFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IllegalStateException("Cannot open input stream for $uri")
        }
    }

    /**
     * Creates an Android Share Intent for sending annotations to Drive, Email, etc.
     */
    fun buildShareIntent(fileUri: Uri, mimeType: String, title: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
