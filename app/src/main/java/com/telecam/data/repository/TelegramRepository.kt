package com.telecam.data.repository

import com.telecam.domain.model.Result
import com.telecam.domain.model.UploadQueueItem

/**
 * Repository interface for Telegram upload operations.
 */
interface TelegramRepository {

    /**
     * Upload a file to Telegram.
     * @param item The queue item to upload.
     * @param botToken Telegram bot token.
     * @param chatId Target chat ID.
     * @return Result with file ID if successful.
     */
    suspend fun uploadFile(
        item: UploadQueueItem,
        botToken: String,
        chatId: String
    ): Result<String>

    /**
     * Check if Telegram credentials are configured.
     */
    suspend fun isConfigured(): Boolean
}
