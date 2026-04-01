package com.telecam.domain.usecase

import com.telecam.data.repository.SettingsRepository
import com.telecam.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for managing app settings.
 */
class SettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val tokenPattern = Regex("(\\d{6,}:[A-Za-z0-9_-]{20,})")

    /**
     * Get settings as Flow.
     */
    fun getSettings(): Flow<AppSettings> {
        return settingsRepository.getSettings()
    }

    /**
     * Get settings once.
     */
    suspend fun getSettingsOnce(): AppSettings {
        return settingsRepository.getSettingsOnce()
    }

    /**
     * Toggle auto upload.
     */
    suspend fun setAutoUpload(enabled: Boolean) {
        settingsRepository.setAutoUploadEnabled(enabled)
    }

    /**
     * Toggle wifi only.
     */
    suspend fun setWifiOnly(enabled: Boolean) {
        settingsRepository.setWifiOnlyUpload(enabled)
    }

    /**
     * Update Telegram credentials.
     */
    suspend fun setTelegramCredentials(botToken: String, chatId: String) {
        val normalizedToken = normalizeBotToken(botToken)
        val normalizedChatId = normalizeChatId(chatId)
        settingsRepository.setBotToken(normalizedToken)
        settingsRepository.setChatId(normalizedChatId)
    }

    /**
     * Check if app is configured for uploads.
     */
    suspend fun isConfigured(): Boolean {
        val settings = settingsRepository.getSettingsOnce()
        return settings.botToken.isNotBlank() && settings.chatId.isNotBlank()
    }

    suspend fun setPendingAuthToken(token: String) {
        settingsRepository.setPendingAuthToken(token)
    }

    suspend fun clearPendingAuthToken() {
        settingsRepository.clearPendingAuthToken()
    }

    suspend fun completeTelegramOnboarding(
        telegramUserId: String,
        telegramUsername: String?,
        botToken: String,
        chatId: String
    ) {
        settingsRepository.setTelegramUserDetails(telegramUserId, telegramUsername)
        setTelegramCredentials(botToken, chatId)
        settingsRepository.clearPendingAuthToken()
        settingsRepository.setOnboardingCompleted(true)
    }

    suspend fun isOnboardingCompleted(): Boolean {
        return settingsRepository.getSettingsOnce().onboardingCompleted
    }

    private fun normalizeBotToken(raw: String): String {
        val trimmed = raw.trim()
        val matched = tokenPattern.find(trimmed)?.value
        if (matched != null) return matched
        return trimmed.removePrefix("bot")
    }

    private fun normalizeChatId(raw: String): String {
        return raw.trim()
            .removePrefix("<")
            .removeSuffix(">")
            .replace(" ", "")
    }
}
