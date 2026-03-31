package com.telecam.ui.camera

import android.net.Uri
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telecam.camera.CameraManager
import com.telecam.domain.model.CameraFacing
import com.telecam.domain.model.Result
import com.telecam.domain.model.UploadQueueItem
import com.telecam.domain.model.UploadStatus
import com.telecam.domain.usecase.ManageQueueUseCase
import com.telecam.domain.usecase.UploadResult
import com.telecam.domain.usecase.UploadFileUseCase
import com.telecam.sync.SyncManager
import com.telecam.utils.FileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for camera screen.
 */
data class CameraUiState(
    val isPermissionGranted: Boolean = false,
    val isRecording: Boolean = false,
    val isCapturing: Boolean = false,
    val cameraFacing: CameraFacing = CameraFacing.BACK,
    val pendingUploads: Int = 0,
    val lastCapturedUri: Uri? = null,
    val errorMessage: String? = null,
    val flashEnabled: Boolean = false
)

/**
 * ViewModel for camera screen.
 * Handles camera operations and upload coordination.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    private val fileManager: FileManager,
    private val uploadFileUseCase: UploadFileUseCase,
    private val manageQueueUseCase: ManageQueueUseCase,
    private val syncManager: SyncManager
) : ViewModel() {
    private val tag = "CameraViewModel"

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        observePendingUploads()
        syncManager.startObserving()
    }

    private fun observePendingUploads() {
        viewModelScope.launch {
            manageQueueUseCase.getPendingCount().collect { count ->
                _uiState.update { it.copy(pendingUploads = count) }
            }
        }
    }

    /**
     * Called when CAMERA permission is granted.
     * Fix: keep camera permission as the gate for preview initialization.
     */
    fun onPermissionsGranted() {
        Log.d(tag, "Permission granted. Camera can initialize.")
        _uiState.update { it.copy(isPermissionGranted = true) }
    }

    /**
     * Called when CAMERA permission is denied.
     * Fix: show a clear fallback message instead of a blank screen.
     */
    fun onCameraPermissionDenied() {
        Log.e(tag, "Permission denied. Camera preview blocked.")
        _uiState.update {
            it.copy(
                isPermissionGranted = false,
                errorMessage = "Camera permission denied"
            )
        }
    }

    /**
     * Initialize camera with the same PreviewView rendered by Compose.
     * Fix: binding to a different PreviewView causes black preview.
     */
    fun initializeCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        viewModelScope.launch {
            try {
                val initialized = cameraManager.initializeCamera(
                    lifecycleOwner,
                    previewView,
                    _uiState.value.cameraFacing
                )

                if (initialized) {
                    Log.d(tag, "Camera binding success")
                    _uiState.update { it.copy(errorMessage = null) }
                }
            } catch (e: Exception) {
                Log.e(tag, "Camera initialization failed", e)
                _uiState.update {
                    it.copy(errorMessage = "Failed to initialize camera: ${e.message}")
                }
            }
        }
    }

    /**
     * Capture a photo.
     */
    fun capturePhoto() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true) }

            try {
                val uri = cameraManager.takePhoto()
                if (uri != null) {
                    _uiState.update { it.copy(lastCapturedUri = uri) }
                    processMediaCapture(uri, isVideo = false)
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to capture photo") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Capture failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isCapturing = false) }
            }
        }
    }

    /**
     * Start video recording.
     */
    fun startRecording() {
        _uiState.update { it.copy(isRecording = true) }
        cameraManager.startVideoRecording { uri ->
            if (uri != null) {
                viewModelScope.launch {
                    processMediaCapture(uri, isVideo = true)
                }
            }
            _uiState.update { it.copy(isRecording = false) }
        }
    }

    /**
     * Stop video recording.
     */
    fun stopRecording() {
        cameraManager.stopVideoRecording()
    }

    /**
     * Switch between front and back camera.
     */
    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        viewModelScope.launch {
            try {
                val newFacing = cameraManager.switchCamera(
                    lifecycleOwner,
                    previewView,
                    _uiState.value.cameraFacing
                )
                Log.d(tag, "Switched camera to $newFacing")
                _uiState.update { it.copy(cameraFacing = newFacing, errorMessage = null) }
            } catch (e: Exception) {
                Log.e(tag, "Failed to switch camera", e)
                _uiState.update { it.copy(errorMessage = "Failed to switch camera") }
            }
        }
    }

    /**
     * Toggle flash.
     */
    fun toggleFlash() {
        _uiState.update { it.copy(flashEnabled = !it.flashEnabled) }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Called by CameraScreen when a photo path is saved.
     */
    fun onPhotoCaptured(filePath: String) {
        viewModelScope.launch {
            Log.d(tag, "Photo captured: $filePath")
            val file = java.io.File(filePath)
            if (!file.exists()) {
                Log.e(tag, "Upload failed: photo file missing")
                _uiState.update { it.copy(errorMessage = "Photo file missing") }
                return@launch
            }
            uploadCapturedFile(file, "image/jpeg")
        }
    }

    /**
     * Called by CameraScreen when video finalize provides a MediaStore URI.
     */
    fun onVideoCaptured(videoUri: Uri) {
        viewModelScope.launch {
            Log.d(tag, "Video saved: $videoUri")
            val videoFile = fileManager.uriToFile(videoUri, "VID_")
            if (videoFile == null || !videoFile.exists()) {
                Log.e(tag, "Upload failed: unable to convert video URI to file")
                _uiState.update { it.copy(errorMessage = "Video save failed") }
                return@launch
            }
            uploadCapturedFile(videoFile, "video/mp4")
        }
    }

    /**
     * Process captured media: upload or queue.
     */
    private suspend fun processMediaCapture(uri: Uri, isVideo: Boolean) {
        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

        val file = if (uri.scheme == "content") {
            fileManager.uriToFile(uri, if (isVideo) "VID_" else "IMG_")
        } else {
            uri.path?.let { java.io.File(it) }
        } ?: run {
            Log.e(tag, "Upload failed: could not resolve file from URI $uri")
            _uiState.update { it.copy(errorMessage = "Failed to read captured media") }
            return
        }

        if (!file.exists()) {
            Log.e(tag, "Upload failed: file does not exist ${file.absolutePath}")
            _uiState.update { it.copy(errorMessage = "Captured file not found") }
            return
        }

        uploadCapturedFile(file, mimeType)
    }

    private suspend fun uploadCapturedFile(file: java.io.File, mimeType: String) {
        val queueItem = UploadQueueItem(
            filePath = file.absolutePath,
            status = UploadStatus.PENDING,
            fileName = file.name,
            fileSize = file.length(),
            mimeType = mimeType
        )

        Log.d(tag, "Uploading file: ${file.absolutePath}")

        when (val result = uploadFileUseCase.processMediaCapture(queueItem)) {
            is Result.Success -> {
                when (result.data) {
                    is UploadResult.UPLOADED -> {
                        Log.d(tag, "Upload success: ${file.name}")
                    }
                    is UploadResult.QUEUED -> {
                        Log.d(tag, "Upload queued: ${file.name}")
                        syncManager.requestImmediateSync()
                    }
                    is UploadResult.FAILED -> {
                        Log.e(tag, "Upload failed: ${result.data.error}")
                        _uiState.update { it.copy(errorMessage = result.data.error) }
                    }
                }
            }
            is Result.Error -> {
                Log.e(tag, "Upload failed: ${result.message}")
                _uiState.update { it.copy(errorMessage = result.message) }
                syncManager.requestImmediateSync()
            }
            is Result.Loading -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.release()
    }
}
