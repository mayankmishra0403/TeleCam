package com.telecam.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    // Keep a single PreviewView instance to avoid flicker/black frames on recomposition.
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Controller owns CameraX use cases and actions, UI only calls exposed methods.
    val cameraController = remember { CameraController(context.applicationContext) }

    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var hasAllPermissions by remember {
        mutableStateOf(context.hasCameraAndAudioPermissions())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val cameraGranted = result[Manifest.permission.CAMERA] == true
        val audioGranted = result[Manifest.permission.RECORD_AUDIO] == true
        hasAllPermissions = cameraGranted && audioGranted

        if (!hasAllPermissions) {
            statusMessage = "Camera and audio permissions are required"
        } else {
            statusMessage = null
        }
    }

    // Ask for permissions before CameraX initialization.
    LaunchedEffect(Unit) {
        if (!hasAllPermissions) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    // Bind all use cases when permissions are ready and lifecycle is available.
    LaunchedEffect(hasAllPermissions, lifecycleOwner, previewView) {
        if (hasAllPermissions) {
            cameraController.bindCameraUseCases(lifecycleOwner, previewView)
        }
    }

    // Attach callbacks once; UI surfaces errors and success toasts.
    DisposableEffect(cameraController, context) {
        cameraController.onError = { message ->
            statusMessage = message
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        cameraController.onPhotoSaved = { path ->
            Toast.makeText(context, "Photo captured", Toast.LENGTH_SHORT).show()
            viewModel.onPhotoCaptured(path)
        }
        cameraController.onVideoSaved = { uri ->
            Toast.makeText(context, "Video saved", Toast.LENGTH_SHORT).show()
            viewModel.onVideoCaptured(uri)
        }

        onDispose {
            cameraController.release()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            statusMessage = message
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val isRecording = cameraController.isRecording

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasAllPermissions) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewView },
                update = {
                    it.scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            )

            if (isRecording) {
                Text(
                    text = "Recording...",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            CameraControls(
                isRecording = isRecording,
                onCapturePhoto = {
                    cameraController.takePhoto(context)
                },
                onToggleRecording = {
                    if (isRecording) {
                        cameraController.stopRecording()
                    } else {
                        cameraController.startRecording(context)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (uiState.pendingUploads > 0) {
                Text(
                    text = "Pending uploads: ${uiState.pendingUploads}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.55f), MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        } else {
            PermissionDeniedContent(
                statusMessage = statusMessage,
                onRequestAgain = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                },
                onOpenSettingsTab = onNavigateToSettings
            )
        }

        statusMessage?.let { message ->
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun CameraControls(
    isRecording: Boolean,
    onCapturePhoto: () -> Unit,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        // Requirement: disable photo capture while video is recording.
        Button(
            onClick = onCapturePhoto,
            enabled = !isRecording,
            modifier = Modifier.height(52.dp)
        ) {
            Text("Capture")
        }

        Button(
            onClick = onToggleRecording,
            modifier = Modifier.height(52.dp)
        ) {
            Text(if (isRecording) "Stop" else "Record")
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    statusMessage: String?,
    onRequestAgain: () -> Unit,
    onOpenSettingsTab: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = statusMessage ?: "Camera permissions denied",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestAgain) {
            Text("Grant Permissions")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenSettingsTab) {
            Text("Open Settings Tab")
        }
    }
}

private fun Context.hasCameraAndAudioPermissions(): Boolean {
    val cameraGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val audioGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    return cameraGranted && audioGranted
}
