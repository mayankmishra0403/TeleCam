package com.telecam.ui.camera

enum class CameraCaptureMode {
    PHOTO,
    VIDEO,
    SLOW_MOTION,
    TIME_LAPSE
}

enum class WhiteBalancePreset {
    AUTO,
    DAYLIGHT,
    CLOUDY,
    INCANDESCENT,
    FLUORESCENT
}

data class ManualCameraSettings(
    val iso: Int? = null,
    val shutterSpeedNs: Long? = null,
    val whiteBalancePreset: WhiteBalancePreset = WhiteBalancePreset.AUTO
)

data class CameraSmartFeatures(
    val autoBlurDetectionEnabled: Boolean = false,
    val sceneDetectionEnabled: Boolean = false,
    val smartCompressionEnabled: Boolean = true,
    val duplicateDetectionEnabled: Boolean = false
)

data class CameraAdvancedFlags(
    val nightModeEnabled: Boolean = false,
    val videoStabilizationEnabled: Boolean = true,
    val manualControlsEnabled: Boolean = false,
    val smartFeatures: CameraSmartFeatures = CameraSmartFeatures()
)
