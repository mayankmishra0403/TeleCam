package com.telecam.data.repository

import com.telecam.domain.model.UploadQueueItem
import com.telecam.domain.model.UploadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for upload queue operations.
 * Defines contract for data access layer.
 */
interface UploadQueueRepository {

    /**
     * Get all queue items as Flow.
     */
    fun getAllItems(): Flow<List<UploadQueueItem>>

    /**
     * Get pending items as Flow.
     */
    fun getPendingItems(): Flow<List<UploadQueueItem>>

    /**
     * Get pending items once (for sync).
     */
    suspend fun getPendingItemsOnce(): List<UploadQueueItem>

    /**
     * Get count of pending items.
     */
    fun getPendingCount(): Flow<Int>

    /**
     * Add item to queue.
     * @return ID of inserted item.
     */
    suspend fun addItem(item: UploadQueueItem): Long

    /**
     * Update queue item.
     */
    suspend fun updateItem(item: UploadQueueItem)

    /**
     * Delete queue item.
     */
    suspend fun deleteItem(item: UploadQueueItem)

    /**
     * Delete item by ID.
     */
    suspend fun deleteById(id: Int)

    /**
     * Update item status.
     */
    suspend fun updateStatus(id: Int, status: UploadStatus)

    /**
     * Update error message for an item.
     */
    suspend fun updateErrorMessage(id: Int, message: String?)

    /**
     * Increment retry count for an item.
     */
    suspend fun incrementRetryCount(id: Int)

    /**
     * Get item by ID.
     */
    suspend fun getItemById(id: Int): UploadQueueItem?

    /**
     * Delete all uploaded items (cleanup).
     */
    suspend fun deleteUploadedItems()
}
