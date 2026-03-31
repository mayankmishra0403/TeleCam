package com.telecam.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.telecam.domain.model.UploadQueueItem
import com.telecam.domain.model.UploadStatus

/**
 * Room entity for upload queue items.
 * Maps to domain model UploadQueueItem.
 */
@Entity(tableName = "upload_queue")
data class UploadQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val filePath: String,
    val status: String,  // Using String for Room enum compatibility
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val fileName: String = "",
    val fileSize: Long = 0,
    val mimeType: String = "",
    val errorMessage: String? = null,
    val telegramFileId: String? = null
) {
    /**
     * Convert entity to domain model.
     */
    fun toDomain(): UploadQueueItem = UploadQueueItem(
        id = id,
        filePath = filePath,
        status = UploadStatus.valueOf(status),
        retryCount = retryCount,
        createdAt = createdAt,
        fileName = fileName,
        fileSize = fileSize,
        mimeType = mimeType,
        errorMessage = errorMessage,
        telegramFileId = telegramFileId
    )

    companion object {
        /**
         * Create entity from domain model.
         */
        fun fromDomain(item: UploadQueueItem): UploadQueueEntity = UploadQueueEntity(
            id = item.id,
            filePath = item.filePath,
            status = item.status.name,
            retryCount = item.retryCount,
            createdAt = item.createdAt,
            fileName = item.fileName,
            fileSize = item.fileSize,
            mimeType = item.mimeType,
            errorMessage = item.errorMessage,
            telegramFileId = item.telegramFileId
        )
    }
}
