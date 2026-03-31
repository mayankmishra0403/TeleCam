package com.telecam.ui.camera

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * CameraX controller that keeps camera logic away from Compose UI.
 * Handles Preview + ImageCapture + VideoCapture with stable lifecycle binding.
 */
class CameraController(
    private val appContext: Context
) {
    private val tag = "CameraController"
    private val executor = ContextCompat.getMainExecutor(appContext)

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private val isRecordingState = androidx.compose.runtime.mutableStateOf(false)
    val isRecording: Boolean
        get() = isRecordingState.value

    var onError: ((String) -> Unit)? = null
    var onPhotoSaved: ((String) -> Unit)? = null
    var onVideoSaved: ((Uri) -> Unit)? = null

    /**
     * Bind Preview + ImageCapture + VideoCapture together.
     */
    fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(appContext)

        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    // Critical: use the composable PreviewView surface provider.
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()

                val boundVideoCapture = VideoCapture.withOutput(recorder)

                val boundImageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                    .build()

                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                // Critical: unbind first to avoid stale use-cases and black preview.
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    boundImageCapture,
                    boundVideoCapture
                )

                imageCapture = boundImageCapture
                videoCapture = boundVideoCapture
                Log.d(tag, "Camera started")
                Log.d(tag, "Camera use-cases bound successfully")
            } catch (e: Exception) {
                Log.e(tag, "Failed to bind camera use-cases", e)
                onError?.invoke("Failed to start camera: ${e.message}")
            }
        }, executor)
    }

    /**
     * Capture and save a photo to app external storage (or cache fallback).
     */
    fun takePhoto(context: Context) {
        val currentImageCapture = imageCapture
        if (currentImageCapture == null) {
            onError?.invoke("Camera is not ready")
            Log.e(tag, "takePhoto called before imageCapture was initialized")
            return
        }

        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.cacheDir
        if (!picturesDir.exists()) {
            picturesDir.mkdirs()
        }

        val photoFile = File(
            picturesDir,
            "IMG_${timestamp()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        currentImageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedPath = photoFile.absolutePath
                    Log.d(tag, "Photo saved successfully: $savedPath")
                    onPhotoSaved?.invoke(savedPath)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(tag, "Photo capture failed", exception)
                    onError?.invoke("Photo capture failed: ${exception.message}")
                }
            }
        )
    }

    /**
     * Start video recording to MediaStore with audio enabled.
     */
    fun startRecording(context: Context) {
        if (recording != null || isRecording) {
            Log.d(tag, "startRecording ignored because recording is already active")
            return
        }

        val currentVideoCapture = videoCapture
        if (currentVideoCapture == null) {
            onError?.invoke("Camera is not ready")
            Log.e(tag, "startRecording called before videoCapture was initialized")
            return
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "VID_${timestamp()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TeleCam")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        try {
            val hasAudioPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            val pendingRecording = currentVideoCapture.output
                .prepareRecording(context, outputOptions)
                .apply {
                    if (hasAudioPermission) {
                        withAudioEnabled()
                    }
                }

            recording = pendingRecording.start(executor) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            isRecordingState.value = true
                            Log.d(tag, "Video recording started")
                        }

                        is VideoRecordEvent.Finalize -> {
                            isRecordingState.value = false
                            val activeRecording = recording
                            recording = null
                            activeRecording?.close()

                            if (event.hasError()) {
                                Log.e(tag, "Video recording failed: ${event.error}")
                                onError?.invoke("Video recording failed: ${event.cause?.message ?: event.error}")
                            } else {
                                val uri = event.outputResults.outputUri
                                if (uri == Uri.EMPTY) {
                                    Log.e(tag, "Video finalize returned empty URI")
                                    onError?.invoke("Video was not saved")
                                } else {
                                    Log.d(tag, "Video saved: $uri")
                                    onVideoSaved?.invoke(uri)
                                }
                            }
                        }
                    }
                }
        } catch (e: SecurityException) {
            isRecordingState.value = false
            recording = null
            Log.e(tag, "Audio permission missing for recording", e)
            onError?.invoke("Audio permission is required for video recording")
        } catch (e: Exception) {
            isRecordingState.value = false
            recording = null
            Log.e(tag, "Failed to start recording", e)
            onError?.invoke("Failed to start recording: ${e.message}")
        }
    }

    /**
     * Stop active recording.
     */
    fun stopRecording() {
        val activeRecording = recording
        recording = null
        activeRecording?.stop()
        isRecordingState.value = false
        Log.d(tag, "stopRecording requested")
    }

    /**
     * Release camera resources when screen leaves composition.
     */
    fun release() {
        recording?.stop()
        recording = null
        isRecordingState.value = false
        cameraProvider?.unbindAll()
        Log.d(tag, "Camera resources released")
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    }
}
