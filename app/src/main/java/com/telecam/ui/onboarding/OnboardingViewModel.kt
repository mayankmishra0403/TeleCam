package com.telecam.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telecam.BuildConfig
import com.telecam.data.repository.TelegramOnboardingRepository
import com.telecam.domain.model.Result
import com.telecam.domain.usecase.SettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val authUrl: String? = null,
    val pendingToken: String = "",
    val statusMessage: String? = null,
    val isCompleted: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsUseCase: SettingsUseCase,
    private val telegramOnboardingRepository: TelegramOnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsUseCase.getSettings().collect { settings ->
                _uiState.update {
                    it.copy(
                        pendingToken = settings.pendingAuthToken,
                        isCompleted = settings.onboardingCompleted
                    )
                }
            }
        }
    }

    fun startTelegramAuth() {
        viewModelScope.launch {
            val generatedToken = UUID.randomUUID().toString()
            settingsUseCase.setPendingAuthToken(generatedToken)

            _uiState.update {
                it.copy(
                    isLoading = true,
                    statusMessage = "Opening Telegram...",
                    authUrl = null
                )
            }

            when (val result = telegramOnboardingRepository.startSession(generatedToken)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authUrl = result.data.authUrl,
                            statusMessage = "Tap Start in Telegram. We'll verify automatically when you return."
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authUrl = null,
                            statusMessage = result.message
                                ?: "Could not contact auth server. Please ensure backend is running, then try again."
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun consumeAuthUrl() {
        _uiState.update { it.copy(authUrl = null) }
    }

    fun verifyPendingToken(tokenFromDeepLink: String? = null) {
        viewModelScope.launch {
            val token = tokenFromDeepLink ?: _uiState.value.pendingToken
            if (token.isBlank()) {
                _uiState.update { it.copy(statusMessage = "No pending auth token found. Please start setup again.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, statusMessage = "Verifying Telegram account...") }

            when (val result = telegramOnboardingRepository.verifyToken(token)) {
                is Result.Success -> {
                    val configuredBotToken = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
                    if (configuredBotToken.isBlank()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                statusMessage = "Server linked, but app bot token is not configured. Set TELEGRAM_BOT_TOKEN in BuildConfig."
                            )
                        }
                        return@launch
                    }

                    settingsUseCase.completeTelegramOnboarding(
                        telegramUserId = result.data.telegramUserId,
                        telegramUsername = result.data.username,
                        botToken = configuredBotToken,
                        chatId = result.data.telegramUserId
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isCompleted = true,
                            statusMessage = "Telegram connected successfully"
                        )
                    }
                }
                is Result.Error -> {
                    val rawMessage = result.message.orEmpty()
                    val normalized = rawMessage.lowercase()
                    val friendlyMessage = when {
                        "pending" in normalized -> "Waiting for Telegram confirmation. Please tap Start in bot chat."
                        "token not found" in normalized || "expired" in normalized -> "Session expired. Tap Continue with Telegram to create a new session."
                        "timeout" in normalized || "failed to connect" in normalized || "unable to resolve host" in normalized -> "Cannot reach auth server. Ensure phone and backend are on same Wi-Fi, then retry."
                        else -> result.message ?: "Verification failed. Please retry."
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = friendlyMessage
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}
