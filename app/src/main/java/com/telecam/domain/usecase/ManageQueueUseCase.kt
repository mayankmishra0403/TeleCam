package com.telecam.domain.usecase

import com.telecam.data.repository.UploadQueueRepository
import com.telecam.domain.model.UploadQueueItem
import com.telecam.domain.model.UploadStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for managing the upload queue.
 * Encapsulates queue operations following Clean Architecture.
 */
class ManageQueueUseCase @Inject constructor(
    private val uploadQueueRepository: UploadQueueRepository
) {
    /**
     * Get all queue items.
     */
    fun getAllItems(): Flow<List<UploadQueueItem>> {
        return uploadQueueRepository.getAllItems()
    }

    /**
     * Get pending items.
     */
    fun getPendingItems(): Flow<List<UploadQueueItem>> {
        return uploadQueueRepository.getPendingItems()
    }

    /**
     * Get pending count.
     */
    fun getPendingCount(): Flow<Int> {
        return uploadQueueRepository.getPendingCount()
    }

    /**
     * Add new item to queue.
     */
    suspend fun addToQueue(item: UploadQueueItem): Long {
        return uploadQueueRepository.addItem(item)
    }

    /**
     * Update item status.
     */
    suspend fun updateStatus(id: Int, status: UploadStatus) {
        uploadQueueRepository.updateStatus(id, status)
    }

    /**
     * Delete item from queue.
     */
    suspend fun deleteItem(id: Int) {
        uploadQueueRepository.deleteById(id)
    }

    /**
     * Increment retry count.
     */
    suspend fun incrementRetry(id: Int) {
        uploadQueueRepository.incrementRetryCount(id)
    }

    /**
     * Update error message.
     */
    suspend fun updateError(id: Int, message: String?) {
        uploadQueueRepository.updateErrorMessage(id, message)
    }

    /**
     * Get item by ID.
     */
    suspend fun getItem(id: Int): UploadQueueItem? {
        return uploadQueueRepository.getItemById(id)
    }

    /**
     * Cleanup uploaded items.
     */
    suspend fun cleanupUploaded() {
        uploadQueueRepository.deleteUploadedItems()
    }
}
