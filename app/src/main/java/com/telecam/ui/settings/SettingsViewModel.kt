package com.telecam.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telecam.domain.model.AppSettings
import com.telecam.domain.usecase.ManageQueueUseCase
import com.telecam.domain.usecase.SettingsUseCase
import com.telecam.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsUseCase: SettingsUseCase,
    private val manageQueueUseCase: ManageQueueUseCase,
    private val syncManager: SyncManager
) : ViewModel() {
    private val tokenPattern = Regex("^\\d{6,}:[A-Za-z0-9_-]{20,}$")

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsUseCase.getSettingsOnce()
            _settings.update {
                it.copy(
                    autoUploadEnabled = settings.autoUploadEnabled,
                    wifiOnlyUpload = settings.wifiOnlyUpload,
                    botToken = settings.botToken,
                    chatId = settings.chatId,
                    telegramUserId = settings.telegramUserId,
                    telegramUsername = settings.telegramUsername,
                    pendingAuthToken = settings.pendingAuthToken,
                    onboardingCompleted = settings.onboardingCompleted,
                    maxRetries = settings.maxRetries
                )
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            manageQueueUseCase.getPendingCount().collect { pendingCount ->
                _settings.update {
                    it.copy(pendingCount = pendingCount)
                }
            }
        }
    }

    fun setAutoUpload(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.setAutoUpload(enabled)
            _settings.update { it.copy(autoUploadEnabled = enabled) }
            
            if (enabled) {
                syncManager.schedulePeriodicSync()
            } else {
                syncManager.cancelSync()
            }
        }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.setWifiOnly(enabled)
            _settings.update { it.copy(wifiOnlyUpload = enabled) }
            
            if (_settings.value.autoUploadEnabled) {
                syncManager.schedulePeriodicSync()
            }
        }
    }

    fun setBotToken(token: String) {
        _settings.update { it.copy(botToken = token) }
    }

    fun saveBotToken() {
        viewModelScope.launch {
            val token = _settings.value.botToken
            val chatId = _settings.value.chatId

            if (!isValidCredentials(token, chatId)) {
                _uiState.update {
                    it.copy(message = "Invalid token/chat ID. Use token from BotFather and numeric chat ID.")
                }
                return@launch
            }

            settingsUseCase.setTelegramCredentials(token, chatId)
            _uiState.update { it.copy(message = "Credentials saved") }
        }
    }

    fun setChatId(chatId: String) {
        _settings.update { it.copy(chatId = chatId) }
    }

    fun saveChatId() {
        viewModelScope.launch {
            val token = _settings.value.botToken
            val chatId = _settings.value.chatId

            if (!isValidCredentials(token, chatId)) {
                _uiState.update {
                    it.copy(message = "Invalid token/chat ID. Use token from BotFather and numeric chat ID.")
                }
                return@launch
            }

            settingsUseCase.setTelegramCredentials(token, chatId)
            _uiState.update { it.copy(message = "Credentials saved") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun isValidCredentials(token: String, chatId: String): Boolean {
        val extractedToken = token.trim().removePrefix("bot")
        val isTokenValid = tokenPattern.matches(extractedToken)
        val isChatIdValid = chatId.trim().isNotEmpty()
        return isTokenValid && isChatIdValid
    }
}
