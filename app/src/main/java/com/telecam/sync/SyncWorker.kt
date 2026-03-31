package com.telecam.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.telecam.data.repository.SettingsRepository
import com.telecam.data.repository.UploadQueueRepository
import com.telecam.domain.model.Result
import com.telecam.domain.model.UploadStatus
import com.telecam.utils.NetworkMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker for syncing pending uploads.
 * Runs in background to upload queued files when network is available.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val uploadQueueRepository: UploadQueueRepository,
    private val settingsRepository: SettingsRepository,
    private val networkMonitor: NetworkMonitor,
    private val telegramRepository: com.telecam.data.repository.TelegramRepository
) : CoroutineWorker(context, workerParams) {
    private val tag = "SyncWorker"

    companion object {
        const val WORK_NAME = "sync_upload_worker"
        private const val IMMEDIATE_WORK_NAME = "sync_upload_worker_immediate"
        private const val REPEAT_INTERVAL_MINUTES = 15L
        private const val MAX_ITEMS_PER_SYNC = 10

        /**
         * Schedule periodic sync work.
         */
        fun schedule(context: Context, wifiOnly: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                REPEAT_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * Cancel scheduled sync work.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Request immediate sync.
         */
        fun requestImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if sync is enabled
            val settings = settingsRepository.getSettingsOnce()
            if (!settings.autoUploadEnabled) {
                return@withContext Result.success()
            }

            // Check network
            if (!networkMonitor.isNetworkAvailable()) {
                return@withContext Result.retry()
            }

            // If wifi-only is enabled, check wifi
            if (settings.wifiOnlyUpload && !networkMonitor.isWifiConnected()) {
                return@withContext Result.retry()
            }

            // Get pending items
            val pendingItems = uploadQueueRepository.getPendingItemsOnce()
                .take(MAX_ITEMS_PER_SYNC)

            if (pendingItems.isEmpty()) {
                Log.d(tag, "No pending uploads found")
                return@withContext Result.success()
            }

            // Upload each item
            var allSuccessful = true
            for (item in pendingItems) {
                Log.d(tag, "Uploading queued item id=${item.id}")
                val uploadResult = uploadItem(item, settings.botToken, settings.chatId)
                
                when (uploadResult) {
                    is com.telecam.domain.model.Result.Success -> {
                        Log.d(tag, "Upload success for id=${item.id}")
                        // Item uploaded successfully, already updated in repository
                    }
                    is com.telecam.domain.model.Result.Error -> {
                        allSuccessful = false
                        // Increment retry count
                        uploadQueueRepository.incrementRetryCount(item.id)
                        val newRetryCount = item.retryCount + 1
                        
                        // Check if max retries reached
                        if (newRetryCount >= settings.maxRetries) {
                            uploadQueueRepository.updateStatus(item.id, UploadStatus.FAILED)
                            Log.e(tag, "Upload permanently failed for id=${item.id}")
                        } else {
                            Log.e(tag, "Upload failed for id=${item.id}, retry=$newRetryCount")
                        }
                    }
                    is com.telecam.domain.model.Result.Loading -> {}
                }
            }

            if (allSuccessful) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun uploadItem(
        item: com.telecam.domain.model.UploadQueueItem,
        botToken: String,
        chatId: String
    ): com.telecam.domain.model.Result<String> {
        // Update status to uploading
        uploadQueueRepository.updateStatus(item.id, UploadStatus.UPLOADING)

        val result = telegramRepository.uploadFile(item, botToken, chatId)

        return when (result) {
            is com.telecam.domain.model.Result.Success -> {
                uploadQueueRepository.updateStatus(item.id, UploadStatus.UPLOADED)
                result
            }
            is com.telecam.domain.model.Result.Error -> {
                uploadQueueRepository.updateStatus(item.id, UploadStatus.PENDING)
                uploadQueueRepository.updateErrorMessage(item.id, result.message)
                result
            }
            is com.telecam.domain.model.Result.Loading -> result
        }
    }
}
