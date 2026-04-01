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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var lensFacing by rememberSaveable { mutableStateOf(androidx.camera.core.CameraSelector.LENS_FACING_BACK) }
    var torchEnabled by rememberSaveable { mutableStateOf(false) }
    var zoomLevel by rememberSaveable { mutableStateOf(0f) }
    var exposureCompensation by rememberSaveable { mutableStateOf(0f) }
    var photoTimerSeconds by rememberSaveable { mutableStateOf(0) }
    var showProControls by rememberSaveable { mutableStateOf(false) }
    var photoCountdown by rememberSaveable { mutableStateOf(0) }
    var recordingSeconds by rememberSaveable { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

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
    LaunchedEffect(hasAllPermissions, lifecycleOwner, previewView, lensFacing) {
        if (hasAllPermissions) {
            cameraController.bindCameraUseCases(
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                lensFacing = lensFacing
            )
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
    val exposureRange = cameraController.getExposureCompensationRange()

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingSeconds += 1
            }
        } else {
            recordingSeconds = 0
        }
    }

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
                    text = "Recording... ${recordingSeconds.toTimerText()}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            if (photoCountdown > 0) {
                Text(
                    text = "${photoCountdown}",
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .padding(horizontal = 28.dp, vertical = 12.dp)
                )
            }

            if (showProControls) {
                ProControlsPanel(
                    torchEnabled = torchEnabled,
                    zoomLevel = zoomLevel,
                    exposureValue = exposureCompensation,
                    exposureRange = exposureRange,
                    timerValue = photoTimerSeconds,
                    onToggleTorch = {
                        torchEnabled = !torchEnabled
                        cameraController.setTorch(torchEnabled)
                    },
                    onZoomChanged = {
                        zoomLevel = it
                        cameraController.setLinearZoom(it)
                    },
                    onExposureChanged = { value ->
                        exposureCompensation = value
                        cameraController.setExposureCompensation(value.toInt())
                    },
                    onTimerSelected = { seconds ->
                        photoTimerSeconds = seconds
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, end = 12.dp, bottom = 156.dp)
                )
            }

            IconButton(
                onClick = { showProControls = !showProControls },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 24.dp)
                    .background(Color.Black.copy(alpha = 0.42f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = if (showProControls) "Hide pro controls" else "Show pro controls",
                    tint = Color.White
                )
            }

            CameraControls(
                isRecording = isRecording,
                onCapturePhoto = {
                    scope.launch {
                        if (photoTimerSeconds > 0) {
                            for (second in photoTimerSeconds downTo 1) {
                                photoCountdown = second
                                delay(1000)
                            }
                        }
                        photoCountdown = 0
                        cameraController.takePhoto(context)
                    }
                },
                onToggleRecording = {
                    if (isRecording) {
                        cameraController.stopRecording()
                    } else {
                        cameraController.startRecording(context)
                    }
                },
                onSwitchCamera = {
                    if (isRecording) {
                        statusMessage = "Stop recording before switching camera"
                    } else {
                        lensFacing = if (lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK) {
                            androidx.camera.core.CameraSelector.LENS_FACING_FRONT
                        } else {
                            androidx.camera.core.CameraSelector.LENS_FACING_BACK
                        }
                        torchEnabled = false
                        cameraController.setTorch(false)
                        statusMessage = if (lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_FRONT) {
                            "Front camera"
                        } else {
                            "Back camera"
                        }
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
    onSwitchCamera: () -> Unit,
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

        IconButton(
            onClick = onSwitchCamera,
            enabled = !isRecording,
            modifier = Modifier
                .height(52.dp)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Switch camera",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ProControlsPanel(
    torchEnabled: Boolean,
    zoomLevel: Float,
    exposureValue: Float,
    exposureRange: IntRange,
    timerValue: Int,
    onToggleTorch: () -> Unit,
    onZoomChanged: (Float) -> Unit,
    onExposureChanged: (Float) -> Unit,
    onTimerSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.82f)
            .widthIn(max = 360.dp)
            .background(Color.Black.copy(alpha = 0.45f), MaterialTheme.shapes.medium)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleTorch) {
                Icon(
                    imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Torch",
                    tint = Color.White
                )
            }
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Timer",
                tint = Color.White,
                modifier = Modifier.padding(start = 4.dp, end = 8.dp)
            )
            Text(
                text = "Timer",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            TimerChip(label = "Off", selected = timerValue == 0, onClick = { onTimerSelected(0) })
            TimerChip(label = "3s", selected = timerValue == 3, onClick = { onTimerSelected(3) })
            TimerChip(label = "10s", selected = timerValue == 10, onClick = { onTimerSelected(10) })
        }

        Text("Zoom", color = Color.White, style = MaterialTheme.typography.labelMedium)
        Slider(
            value = zoomLevel,
            onValueChange = onZoomChanged,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Exposure: ${exposureValue.toInt()}",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
        Slider(
            value = exposureValue,
            onValueChange = onExposureChanged,
            valueRange = exposureRange.first.toFloat()..exposureRange.last.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.padding(end = 6.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White,
            containerColor = Color.Black.copy(alpha = 0.2f),
            labelColor = Color.White
        )
    )
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

private fun Int.toTimerText(): String {
    val minutes = this / 60
    val seconds = this % 60
    return String.format("%02d:%02d", minutes, seconds)
}
