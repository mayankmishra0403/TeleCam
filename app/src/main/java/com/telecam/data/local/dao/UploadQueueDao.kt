package com.telecam.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.telecam.data.local.entity.UploadQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for upload queue operations.
 */
@Dao
interface UploadQueueDao {

    /**
     * Insert a new item into the queue.
     * @return The row ID of the inserted item.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: UploadQueueEntity): Long

    /**
     * Update an existing queue item.
     */
    @Update
    suspend fun update(item: UploadQueueEntity)

    /**
     * Delete a queue item.
     */
    @Delete
    suspend fun delete(item: UploadQueueEntity)

    /**
     * Get all items in the queue.
     */
    @Query("SELECT * FROM upload_queue ORDER BY createdAt ASC")
    fun getAllItems(): Flow<List<UploadQueueEntity>>

    /**
     * Get all pending items (not yet uploaded).
     */
    @Query("SELECT * FROM upload_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingItems(): Flow<List<UploadQueueEntity>>

    /**
     * Get all pending items as a one-time list.
     */
    @Query("SELECT * FROM upload_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingItemsOnce(): List<UploadQueueEntity>

    /**
     * Get items with specific status.
     */
    @Query("SELECT * FROM upload_queue WHERE status = :status ORDER BY createdAt ASC")
    fun getItemsByStatus(status: String): Flow<List<UploadQueueEntity>>

    /**
     * Get a single item by ID.
     */
    @Query("SELECT * FROM upload_queue WHERE id = :id")
    suspend fun getItemById(id: Int): UploadQueueEntity?

    /**
     * Get count of pending items.
     */
    @Query("SELECT COUNT(*) FROM upload_queue WHERE status = 'PENDING' OR status = 'UPLOADING'")
    fun getPendingCount(): Flow<Int>

    /**
     * Delete all uploaded items (cleanup).
     */
    @Query("DELETE FROM upload_queue WHERE status = 'UPLOADED'")
    suspend fun deleteUploadedItems()

    /**
     * Delete item by ID.
     */
    @Query("DELETE FROM upload_queue WHERE id = :id")
    suspend fun deleteById(id: Int)

    /**
     * Update item status.
     */
    @Query("UPDATE upload_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    /**
     * Increment retry count.
     */
    @Query("UPDATE upload_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Int)

    /**
     * Update error message.
     */
    @Query("UPDATE upload_queue SET errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateErrorMessage(id: Int, errorMessage: String?)
}
