package com.telecam.data.repository

import com.telecam.data.remote.TelegramAuthApiService
import com.telecam.data.remote.TelegramAuthStartRequest
import com.telecam.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

data class TelegramAuthSession(
    val token: String,
    val authUrl: String,
    val expiresAt: Long
)

data class TelegramVerifiedUser(
    val token: String,
    val telegramUserId: String,
    val username: String?
)

@Singleton
class TelegramOnboardingRepository @Inject constructor(
    private val authApiService: TelegramAuthApiService
) {
    suspend fun startSession(token: String): Result<TelegramAuthSession> {
        return try {
            val response = authApiService.startAuth(TelegramAuthStartRequest(token))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.Success(
                    TelegramAuthSession(
                        token = body.token,
                        authUrl = body.authUrl,
                        expiresAt = body.expiresAt
                    )
                )
            } else {
                Result.Error(
                    Exception("Unable to start Telegram auth"),
                    response.errorBody()?.string() ?: "Unable to start Telegram auth"
                )
            }
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    suspend fun verifyToken(token: String): Result<TelegramVerifiedUser> {
        return try {
            val response = authApiService.verifyAuth(token)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (!body.isVerified) {
                    return Result.Error(
                        Exception("Token not verified yet"),
                        "Telegram verification is still pending"
                    )
                }
                Result.Success(
                    TelegramVerifiedUser(
                        token = body.token,
                        telegramUserId = body.telegramUserId.toString(),
                        username = body.username
                    )
                )
            } else {
                Result.Error(
                    Exception("Token verification failed"),
                    response.errorBody()?.string() ?: "Token verification failed"
                )
            }
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
}
