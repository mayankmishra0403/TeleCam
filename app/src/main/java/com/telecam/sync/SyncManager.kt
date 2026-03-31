package com.telecam.sync

import android.content.Context
import com.telecam.data.repository.SettingsRepository
import com.telecam.data.repository.UploadQueueRepository
import com.telecam.utils.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class to coordinate sync operations.
 * Observes network changes and triggers uploads accordingly.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uploadQueueRepository: UploadQueueRepository,
    private val settingsRepository: SettingsRepository,
    private val networkMonitor: NetworkMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Start observing network and queue changes.
     * Automatically triggers sync when conditions are met.
     */
    fun startObserving() {
        // Observe network changes
        scope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { isConnected ->
                if (isConnected) {
                    checkAndSync()
                }
            }
        }

        // Also observe pending items count
        scope.launch {
            uploadQueueRepository.getPendingCount().collectLatest { count ->
                if (count > 0) {
                    checkAndSync()
                }
            }
        }
    }

    /**
     * Check conditions and request sync if appropriate.
     */
    private suspend fun checkAndSync() {
        val settings = settingsRepository.getSettingsOnce()
        
        if (!settings.autoUploadEnabled) return

        val isNetworkAvailable = networkMonitor.isNetworkAvailable()
        val isWifiConnected = networkMonitor.isWifiConnected()

        val shouldSync = if (settings.wifiOnlyUpload) {
            isWifiConnected
        } else {
            isNetworkAvailable
        }

        if (shouldSync) {
            SyncWorker.requestImmediateSync(context)
        }
    }

    /**
     * Schedule periodic sync based on settings.
     */
    suspend fun schedulePeriodicSync() {
        val settings = settingsRepository.getSettingsOnce()
        if (settings.autoUploadEnabled) {
            SyncWorker.schedule(context, settings.wifiOnlyUpload)
        }
    }

    /**
     * Cancel all scheduled syncs.
     */
    fun cancelSync() {
        SyncWorker.cancel(context)
    }

    /**
     * Request immediate sync.
     */
    fun requestImmediateSync() {
        SyncWorker.requestImmediateSync(context)
    }
}
