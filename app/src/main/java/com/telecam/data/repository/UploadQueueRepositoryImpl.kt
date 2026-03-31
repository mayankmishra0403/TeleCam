package com.telecam.data.repository

import com.telecam.data.local.dao.UploadQueueDao
import com.telecam.data.local.entity.UploadQueueEntity
import com.telecam.domain.model.UploadQueueItem
import com.telecam.domain.model.UploadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UploadQueueRepository.
 * Handles all local database operations for upload queue.
 */
@Singleton
class UploadQueueRepositoryImpl @Inject constructor(
    private val uploadQueueDao: UploadQueueDao
) : UploadQueueRepository {

    override fun getAllItems(): Flow<List<UploadQueueItem>> {
        return uploadQueueDao.getAllItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPendingItems(): Flow<List<UploadQueueItem>> {
        return uploadQueueDao.getPendingItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPendingItemsOnce(): List<UploadQueueItem> {
        return uploadQueueDao.getPendingItemsOnce().map { it.toDomain() }
    }

    override fun getPendingCount(): Flow<Int> {
        return uploadQueueDao.getPendingCount()
    }

    override suspend fun addItem(item: UploadQueueItem): Long {
        return uploadQueueDao.insert(UploadQueueEntity.fromDomain(item))
    }

    override suspend fun updateItem(item: UploadQueueItem) {
        uploadQueueDao.update(UploadQueueEntity.fromDomain(item))
    }

    override suspend fun deleteItem(item: UploadQueueItem) {
        uploadQueueDao.delete(UploadQueueEntity.fromDomain(item))
    }

    override suspend fun deleteById(id: Int) {
        uploadQueueDao.deleteById(id)
    }

    override suspend fun updateStatus(id: Int, status: UploadStatus) {
        uploadQueueDao.updateStatus(id, status.name)
    }

    override suspend fun updateErrorMessage(id: Int, message: String?) {
        uploadQueueDao.updateErrorMessage(id, message)
    }

    override suspend fun incrementRetryCount(id: Int) {
        uploadQueueDao.incrementRetryCount(id)
    }

    override suspend fun getItemById(id: Int): UploadQueueItem? {
        return uploadQueueDao.getItemById(id)?.toDomain()
    }

    override suspend fun deleteUploadedItems() {
        uploadQueueDao.deleteUploadedItems()
    }
}
