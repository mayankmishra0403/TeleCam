package com.telecam.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.telecam.domain.model.CameraFacing
import com.telecam.utils.FileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Camera manager using CameraX.
 * Handles camera initialization, photo capture, and video recording.
 */
@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager
) {
    private val tag = "CameraManager"

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    /**
     * Initialize camera with preview.
     */
    suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        cameraFacing: CameraFacing
    ): Boolean = suspendCancellableCoroutine { continuation ->
        Log.d(tag, "Initializing camera for facing=$cameraFacing")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                
                videoCapture = VideoCapture.withOutput(recorder)

                val cameraSelector = when (cameraFacing) {
                    CameraFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                    CameraFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )

                Log.d(tag, "Camera bound to lifecycle successfully")
                if (continuation.isActive) {
                    continuation.resume(true)
                }
            } catch (e: Exception) {
                Log.e(tag, "Camera binding failed", e)
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }, mainExecutor)
    }

    /**
     * Capture a photo.
     * @return Uri of the captured photo.
     */
    suspend fun takePhoto(): Uri? = suspendCancellableCoroutine { continuation ->
        val imageCapture = imageCapture ?: run {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val photoFile = fileManager.createTempFile(isVideo = false)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    continuation.resume(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resume(null)
                }
            }
        )
    }

    /**
     * Start video recording.
     * @return Uri of the recording file.
     */
    fun startVideoRecording(onComplete: (Uri?) -> Unit) {
        val videoCapture = videoCapture ?: run {
            onComplete(null)
            return
        }

        val videoFile = fileManager.createTempFile(isVideo = true)
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .apply {
                // Request audio permission if needed
                // .withAudioEnabled()
            }
            .start(mainExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            onComplete(null)
                        } else {
                            onComplete(Uri.fromFile(videoFile))
                        }
                    }
                }
            }
    }

    /**
     * Stop video recording.
     */
    fun stopVideoRecording() {
        recording?.stop()
        recording = null
    }

    /**
     * Check if currently recording.
     */
    fun isRecording(): Boolean = recording != null

    /**
     * Release camera resources.
     */
    fun release() {
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
    }

    /**
     * Switch between front and back camera.
     */
    suspend fun switchCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        currentFacing: CameraFacing
    ): CameraFacing {
        val newFacing = when (currentFacing) {
            CameraFacing.FRONT -> CameraFacing.BACK
            CameraFacing.BACK -> CameraFacing.FRONT
        }
        initializeCamera(lifecycleOwner, previewView, newFacing)
        return newFacing
    }
}
