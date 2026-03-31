package com.telecam.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telecam.domain.model.UploadQueueItem
import com.telecam.domain.model.UploadStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Queue screen showing pending and recent uploads.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    viewModel: QueueViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    val queueItems by viewModel.queueItems.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Upload Queue")
                },
                actions = {
                    if (pendingCount > 0) {
                        Text(
                            text = "$pendingCount pending",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (queueItems.isEmpty()) {
            EmptyQueueState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(queueItems, key = { it.id }) { item ->
                    QueueItemCard(
                        item = item,
                        onDelete = { viewModel.deleteItem(item.id) },
                        onRetry = { viewModel.retryUpload(item.id) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Empty state for queue.
 */
@Composable
private fun EmptyQueueState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No uploads in queue",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Captured media will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Queue item card.
 */
@Composable
private fun QueueItemCard(
    item: UploadQueueItem,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            StatusIcon(status = item.status)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTimestamp(item.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Error message if failed
                if (item.status == UploadStatus.FAILED && item.errorMessage != null) {
                    Text(
                        text = item.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Actions
            when (item.status) {
                UploadStatus.PENDING, UploadStatus.UPLOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                UploadStatus.FAILED -> {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Retry",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                UploadStatus.UPLOADED -> {
                    // No action needed
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Status icon based on upload status.
 */
@Composable
private fun StatusIcon(status: UploadStatus) {
    val icon = when (status) {
        UploadStatus.PENDING -> Icons.Default.Schedule
        UploadStatus.UPLOADING -> Icons.Default.CloudUpload
        UploadStatus.UPLOADED -> Icons.Default.CheckCircle
        UploadStatus.FAILED -> Icons.Default.Error
    }
    
    val tint = when (status) {
        UploadStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        UploadStatus.UPLOADING -> MaterialTheme.colorScheme.primary
        UploadStatus.UPLOADED -> Color(0xFF4CAF50)
        UploadStatus.FAILED -> MaterialTheme.colorScheme.error
    }
    
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = tint,
        modifier = Modifier.size(24.dp)
    )
}

/**
 * Format timestamp to readable string.
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
