package com.telecam.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.telecam.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast receiver to reschedule sync work after device boot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scope.launch {
                val settings = settingsRepository.getSettingsOnce()
                if (settings.autoUploadEnabled) {
                    SyncWorker.schedule(context, settings.wifiOnlyUpload)
                }
            }
        }
    }
}
