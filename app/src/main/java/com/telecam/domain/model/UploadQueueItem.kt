package com.telecam.domain.model

/**
 * Domain model representing an item in the upload queue.
 * This is the core entity for the offline-first upload system.
 */
data class UploadQueueItem(
    val id: Int = 0,
    val filePath: String,
    val status: UploadStatus,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val fileName: String = "",
    val fileSize: Long = 0,
    val mimeType: String = "",
    val errorMessage: String? = null,
    val telegramFileId: String? = null
)

/**
 * Status enum for upload queue items.
 */
enum class UploadStatus {
    PENDING,      // Waiting to be uploaded
    UPLOADING,    // Currently being uploaded
    UPLOADED,     // Successfully uploaded to Telegram
    FAILED        // Upload failed (after max retries)
}
