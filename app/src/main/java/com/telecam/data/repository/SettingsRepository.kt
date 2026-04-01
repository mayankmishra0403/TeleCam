package com.telecam.data.repository

import com.telecam.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app settings.
 */
interface SettingsRepository {

    /**
     * Get current settings as Flow.
     */
    fun getSettings(): Flow<AppSettings>

    /**
     * Get current settings once.
     */
    suspend fun getSettingsOnce(): AppSettings

    /**
     * Update auto upload setting.
     */
    suspend fun setAutoUploadEnabled(enabled: Boolean)

    /**
     * Update wifi only setting.
     */
    suspend fun setWifiOnlyUpload(enabled: Boolean)

    /**
     * Update bot token.
     */
    suspend fun setBotToken(token: String)

    /**
     * Update chat ID.
     */
    suspend fun setChatId(chatId: String)

    /**
     * Update max retries.
     */
    suspend fun setMaxRetries(count: Int)

    /**
     * Update camera facing preference.
     */
    suspend fun setCameraFacing(facing: String)

    /**
     * Save pending one-tap auth token.
     */
    suspend fun setPendingAuthToken(token: String)

    /**
     * Clear pending one-tap auth token.
     */
    suspend fun clearPendingAuthToken()

    /**
     * Save Telegram account details from one-tap onboarding.
     */
    suspend fun setTelegramUserDetails(userId: String, username: String?)

    /**
     * Mark onboarding completion state.
     */
    suspend fun setOnboardingCompleted(completed: Boolean)
}
