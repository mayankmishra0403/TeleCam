package com.telecam.domain.model

data class AppSettings(
    val autoUploadEnabled: Boolean = true,
    val wifiOnlyUpload: Boolean = false,
    val botToken: String = "",
    val chatId: String = "",
    val telegramUserId: String = "",
    val telegramUsername: String = "",
    val pendingAuthToken: String = "",
    val onboardingCompleted: Boolean = false,
    val maxRetries: Int = 3,
    val cameraFacing: CameraFacing = CameraFacing.BACK,
    val pendingCount: Int = 0
)

enum class CameraFacing {
    FRONT,
    BACK
}
