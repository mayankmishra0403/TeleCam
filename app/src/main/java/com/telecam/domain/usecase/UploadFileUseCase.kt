package com.telecam.domain.usecase

import android.util.Log
import com.telecam.data.repository.SettingsRepository
import com.telecam.data.repository.TelegramRepository
import com.telecam.data.repository.UploadQueueRepository
import com.telecam.domain.model.Result
import com.telecam.domain.model.UploadQueueItem
import com.telecam.domain.model.UploadStatus
import com.telecam.utils.NetworkMonitor
import javax.inject.Inject

/**
 * Use case for uploading files.
 * Handles the upload logic with offline-first approach.
 */
class UploadFileUseCase @Inject constructor(
    private val uploadQueueRepository: UploadQueueRepository,
    private val telegramRepository: TelegramRepository,
    private val settingsRepository: SettingsRepository,
    private val networkMonitor: NetworkMonitor
) {
    private val tag = "UploadFileUseCase"

    /**
     * Process captured media: either upload immediately or queue for later.
     * @return Result indicating success or queue addition.
     */
    suspend fun processMediaCapture(item: UploadQueueItem): Result<UploadResult> {
        val settings = settingsRepository.getSettingsOnce()

        val queueId = uploadQueueRepository.addItem(item).toInt()
        val queuedItem = item.copy(id = queueId)
        Log.d(tag, "Added to queue: id=$queueId path=${queuedItem.filePath}")

        if (!settings.autoUploadEnabled) {
            return Result.Success(UploadResult.QUEUED(queueId))
        }

        // Check network availability
        val isNetworkAvailable = networkMonitor.isNetworkAvailable()
        val isWifiConnected = networkMonitor.isWifiConnected()

        // If wifi-only is enabled, require wifi
        val shouldUploadNow = if (settings.wifiOnlyUpload) {
            isWifiConnected
        } else {
            isNetworkAvailable
        }

        if (shouldUploadNow) {
            // Upload immediately
            return uploadNow(queuedItem, settings.botToken, settings.chatId)
        } else {
            return Result.Success(UploadResult.QUEUED(queueId))
        }
    }

    /**
     * Upload a single file to Telegram.
     */
    suspend fun uploadNow(
        item: UploadQueueItem,
        botToken: String,
        chatId: String
    ): Result<UploadResult> {
        if (botToken.isBlank() || chatId.isBlank()) {
            return Result.Error(
                Exception("Telegram is not configured"),
                "Bot token/chat ID missing"
            )
        }

        // Update status to uploading
        uploadQueueRepository.updateStatus(item.id, UploadStatus.UPLOADING)
        Log.d(tag, "Uploading file: ${item.filePath}")

        val firstAttempt = telegramRepository.uploadFile(item, botToken, chatId)

        when (firstAttempt) {
            is Result.Success -> {
                uploadQueueRepository.updateStatus(item.id, UploadStatus.UPLOADED)
                uploadQueueRepository.updateErrorMessage(item.id, null)
                Log.d(tag, "Upload success: ${item.filePath}")
                return Result.Success(UploadResult.UPLOADED(firstAttempt.data))
            }
            is Result.Error -> {
                uploadQueueRepository.incrementRetryCount(item.id)
                Log.e(tag, "Upload failed (attempt 1): ${firstAttempt.message}")
            }
            is Result.Loading -> return firstAttempt
        }

        val secondAttempt = telegramRepository.uploadFile(item, botToken, chatId)

        return when (secondAttempt) {
            is Result.Success -> {
                uploadQueueRepository.updateStatus(item.id, UploadStatus.UPLOADED)
                uploadQueueRepository.updateErrorMessage(item.id, null)
                Log.d(tag, "Upload success: ${item.filePath}")
                Result.Success(UploadResult.UPLOADED(secondAttempt.data))
            }
            is Result.Error -> {
                uploadQueueRepository.incrementRetryCount(item.id)
                uploadQueueRepository.updateStatus(item.id, UploadStatus.PENDING)
                uploadQueueRepository.updateErrorMessage(item.id, secondAttempt.message)
                Log.e(tag, "Upload failed (attempt 2): ${secondAttempt.message}")
                Result.Error(secondAttempt.exception, secondAttempt.message)
            }
            is Result.Loading -> secondAttempt
        }
    }
}

/**
 * Sealed class representing upload results.
 */
sealed class UploadResult {
    data class UPLOADED(val telegramFileId: String) : UploadResult()
    data class QUEUED(val queueId: Int) : UploadResult()
    data class FAILED(val error: String) : UploadResult()
}
