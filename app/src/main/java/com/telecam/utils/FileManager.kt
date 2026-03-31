package com.telecam.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for managing media files.
 * Handles file creation, storage, and cleanup.
 */
@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PHOTO_PREFIX = "TeleCam_"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss"
    }

    /**
     * Create a temporary file for camera capture.
     */
    fun createTempFile(isVideo: Boolean = false): File {
        val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
        val extension = if (isVideo) VIDEO_EXTENSION else PHOTO_EXTENSION
        val prefix = if (isVideo) "VID_" else "IMG_"
        
        val storageDir = context.cacheDir
        return File.createTempFile("${prefix}${timestamp}_", extension, storageDir)
    }

    /**
     * Save file to public Pictures directory (for photos).
     */
    fun saveToPictures(sourceFile: File, fileName: String? = null): Uri? {
        val name = fileName ?: sourceFile.name
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, getMimeType(sourceFile))
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TeleCam")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
        }

        return uri
    }

    /**
     * Save video to Movies directory.
     */
    fun saveVideoToMovies(sourceFile: File, fileName: String? = null): Uri? {
        val name = fileName ?: sourceFile.name
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TeleCam")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
        }

        return uri
    }

    /**
     * Get MIME type from file extension.
     */
    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            else -> "application/octet-stream"
        }
    }

    /**
     * Delete temporary files from cache.
     */
    fun cleanupTempFiles() {
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith(PHOTO_PREFIX) || file.name.startsWith("VID_")) {
                file.delete()
            }
        }
    }

    /**
     * Delete a specific file.
     */
    fun deleteFile(path: String): Boolean {
        return File(path).delete()
    }

    /**
     * Check if file exists.
     */
    fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    /**
     * Get file size in bytes.
     */
    fun getFileSize(path: String): Long {
        return File(path).length()
    }

    /**
     * Get file name from path.
     */
    fun getFileName(path: String): String {
        return File(path).name
    }

    /**
     * Convert content Uri to a local File for multipart upload.
     */
    fun uriToFile(uri: Uri, fallbackPrefix: String = "capture_"): File? {
        return try {
            val fileName = resolveDisplayName(uri) ?: "${fallbackPrefix}${System.currentTimeMillis()}"
            val sanitizedName = if (fileName.contains('.')) fileName else "$fileName.bin"
            val destination = File(context.cacheDir, sanitizedName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            destination
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }
}
