package com.telecam.data.repository

import android.util.Log
import com.telecam.data.remote.TelegramApiService
import com.telecam.domain.model.Result
import com.telecam.domain.model.UploadQueueItem
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TelegramRepository.
 * Handles file uploads to Telegram Bot API.
 */
@Singleton
class TelegramRepositoryImpl @Inject constructor(
    private val apiService: TelegramApiService
) : TelegramRepository {
    private val tag = "TelegramRepository"
    private val tokenPattern = Regex("(\\d{6,}:[A-Za-z0-9_-]{20,})")

    override suspend fun uploadFile(
        item: UploadQueueItem,
        botToken: String,
        chatId: String
    ): Result<String> {
        return try {
            val normalizedToken = normalizeBotToken(botToken)
            val normalizedChatId = normalizeChatId(chatId)

            if (normalizedToken.isBlank() || normalizedChatId.isBlank()) {
                return Result.Error(
                    Exception("Telegram credentials missing"),
                    "Telegram bot token/chat ID is empty"
                )
            }

            if (!tokenPattern.matches(normalizedToken)) {
                return Result.Error(
                    Exception("Invalid bot token"),
                    "Invalid bot token format. Paste token from BotFather (e.g. 123456:ABC...)"
                )
            }

            val encodedToken = encodeTokenForPath(normalizedToken)

            val file = File(item.filePath)
            if (!file.exists()) {
                return Result.Error(
                    Exception("File not found"),
                    "File does not exist at path: ${item.filePath}"
                )
            }

            Log.d(tag, "token value: $normalizedToken")
            Log.d(tag, "encoded token: $encodedToken")
            Log.d(tag, "chatId: $normalizedChatId")
            Log.d(tag, "file name: ${file.name}")
            Log.d(tag, "upload start")
            Log.d(tag, "Uploading file: ${file.absolutePath}")
            val requestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("document", file.name, requestBody)
            val chatIdBody = normalizedChatId.toRequestBody("text/plain".toMediaTypeOrNull())
            val caption = item.fileName.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.sendDocument(
                token = encodedToken,
                chatId = chatIdBody,
                document = filePart,
                caption = caption
            )

            if (response.isSuccessful && response.body()?.ok == true) {
                Log.d(tag, "upload success")
                Log.d(tag, "Upload success: ${file.name}")
                val fileId = response.body()?.result?.photo?.firstOrNull()?.file_id
                    ?: response.body()?.result?.video?.file_id
                    ?: response.body()?.result?.document?.file_id
                    ?: ""
                Result.Success(fileId)
            } else {
                val errorMsg = response.body()?.description ?: "Upload failed"
                Log.e(tag, "upload failed")
                Log.e(tag, "Upload failed: $errorMsg")
                Result.Error(Exception(errorMsg), errorMsg)
            }
        } catch (e: Exception) {
            Log.e(tag, "upload failed")
            Log.e(tag, "Upload failed", e)
            Result.Error(e, e.message)
        }
    }

    override suspend fun isConfigured(): Boolean {
        // This will be implemented with SettingsRepository
        // For now, just return false
        return false
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

    private fun encodeTokenForPath(token: String): String {
        return URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
    }
}
