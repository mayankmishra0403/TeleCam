package com.telecam.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telecam.domain.model.UploadQueueItem
import com.telecam.domain.usecase.ManageQueueUseCase
import com.telecam.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for queue screen.
 */
@HiltViewModel
class QueueViewModel @Inject constructor(
    private val manageQueueUseCase: ManageQueueUseCase,
    private val syncManager: SyncManager
) : ViewModel() {

    val queueItems: StateFlow<List<UploadQueueItem>> = manageQueueUseCase.getAllItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingCount: StateFlow<Int> = manageQueueUseCase.getPendingCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /**
     * Delete queue item.
     */
    fun deleteItem(id: Int) {
        viewModelScope.launch {
            manageQueueUseCase.deleteItem(id)
        }
    }

    /**
     * Retry upload for failed item.
     */
    fun retryUpload(id: Int) {
        viewModelScope.launch {
            manageQueueUseCase.updateStatus(id, com.telecam.domain.model.UploadStatus.PENDING)
            syncManager.requestImmediateSync()
        }
    }

    /**
     * Clear all uploaded items.
     */
    fun cleanupUploaded() {
        viewModelScope.launch {
            manageQueueUseCase.cleanupUploaded()
        }
    }
}
