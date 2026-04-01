package com.telecam.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class TelegramAuthStartRequest(
    val token: String
)

data class TelegramAuthStartResponse(
    val token: String,
    val authUrl: String,
    val expiresAt: Long
)

data class TelegramAuthVerifyResponse(
    val token: String,
    val telegramUserId: Long,
    val username: String?,
    val isVerified: Boolean
)

interface TelegramAuthApiService {
    @POST("api/auth/telegram/start")
    suspend fun startAuth(
        @Body body: TelegramAuthStartRequest
    ): Response<TelegramAuthStartResponse>

    @GET("api/auth/telegram/verify")
    suspend fun verifyAuth(
        @Query("token") token: String
    ): Response<TelegramAuthVerifyResponse>
}
