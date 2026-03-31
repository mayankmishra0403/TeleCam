package com.telecam.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Telegram Bot API service interface.
 * Handles all communication with Telegram servers.
 */
interface TelegramApiService {

    /**
     * Send a text message to a chat.
     */
    @FormUrlEncoded
    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path(value = "token", encoded = true) token: String,
        @Field("chat_id") chatId: String,
        @Field("text") text: String
    ): Response<TelegramResponse>

    /**
     * Send a photo to a chat.
     * @param token Bot token
     * @param chatId Target chat ID
     * @param photo Multipart photo file
     * @param caption Optional photo caption
     */
    @Multipart
    @POST("bot{token}/sendPhoto")
    suspend fun sendPhoto(
        @Path(value = "token", encoded = true) token: String,
        @Part("chat_id") chatId: RequestBody,
        @Part photo: MultipartBody.Part,
        @Part("caption") caption: RequestBody? = null
    ): Response<TelegramResponse>

    /**
     * Send a video to a chat.
     * @param token Bot token
     * @param chatId Target chat ID
     * @param video Multipart video file
     * @param caption Optional video caption
     */
    @Multipart
    @POST("bot{token}/sendVideo")
    suspend fun sendVideo(
        @Path(value = "token", encoded = true) token: String,
        @Part("chat_id") chatId: okhttp3.RequestBody,
        @Part video: MultipartBody.Part,
        @Part("caption") caption: okhttp3.RequestBody? = null
    ): Response<TelegramResponse>

    /**
     * Send a document to a chat.
     */
    @Multipart
    @POST("bot{token}/sendDocument")
    suspend fun sendDocument(
        @Path(value = "token", encoded = true) token: String,
        @Part("chat_id") chatId: RequestBody,
        @Part document: MultipartBody.Part,
        @Part("caption") caption: RequestBody? = null
    ): Response<TelegramResponse>

    /**
     * Get file info from Telegram.
     */
    @FormUrlEncoded
    @POST("bot{token}/getFile")
    suspend fun getFile(
        @Path(value = "token", encoded = true) token: String,
        @Field("file_id") fileId: String
    ): Response<GetFileResponse>
}

/**
 * Generic Telegram API response.
 */
data class TelegramResponse(
    val ok: Boolean,
    val result: TelegramResult?,
    val error_code: Int?,
    val description: String?
)

/**
 * Result from Telegram API call.
 */
data class TelegramResult(
    val message_id: Int?,
    val date: Int?,
    val chat: TelegramChat?,
    val photo: List<TelegramPhotoSize>?,
    val video: TelegramVideo?,
    val document: TelegramDocument?,
    val file_id: String?,
    val file_unique_id: String?
)

/**
 * Telegram chat info.
 */
data class TelegramChat(
    val id: Long,
    val type: String,
    val title: String? = null,
    val username: String? = null
)

/**
 * Photo size info.
 */
data class TelegramPhotoSize(
    val file_id: String,
    val file_unique_id: String,
    val width: Int,
    val height: Int,
    val file_size: Int?
)

/**
 * Video info.
 */
data class TelegramVideo(
    val file_id: String,
    val file_unique_id: String,
    val width: Int,
    val height: Int,
    val duration: Int,
    val file_size: Int?
)

/**
 * Document info.
 */
data class TelegramDocument(
    val file_id: String,
    val file_unique_id: String,
    val file_name: String?,
    val file_size: Int?
)

/**
 * Response for getFile API.
 */
data class GetFileResponse(
    val ok: Boolean,
    val result: FileResult?
)

/**
 * File result from getFile API.
 */
data class FileResult(
    val file_id: String,
    val file_unique_id: String,
    val file_size: Int?,
    val file_path: String?
)
