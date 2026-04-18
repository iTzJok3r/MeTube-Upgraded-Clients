package com.itzjok3r.metubeapp.ui.screens

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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itzjok3r.metubeapp.model.DownloadItem
import com.itzjok3r.metubeapp.ui.theme.StatusCompleted
import com.itzjok3r.metubeapp.ui.theme.StatusError
import com.itzjok3r.metubeapp.util.FormatUtils
import com.itzjok3r.metubeapp.ads.BannerAd


/**
 * HistoryScreen — Displays completed downloads.
 *
 * Data comes from the ViewModel's history StateFlow, hydrated from
 * GET /history ("done" array) on startup and updated via Socket.IO
 * "completed" events in real time.
 *
 * @param historyItems List of completed download items.
 * @param serverUrl    Current server URL for file links.
 * @param onRetry      Callback to retry a failed download.
 */
@Composable
fun HistoryScreen(
    historyItems: List<DownloadItem>,
    serverUrl: String,
    onRetry: (DownloadItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Text(
                text = "Download History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${historyItems.size} completed download(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Content ─────────────────────────────────────────────────────
        if (historyItems.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "No history",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No completed downloads yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Completed downloads will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // History items list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = historyItems,
                    key = { it.id }
                ) { item ->
                    HistoryItemCard(item = item, serverUrl = serverUrl, onRetry = onRetry)
                }

                // Bottom spacing for navigation bar
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }

        BannerAd(modifier = Modifier.padding(vertical = 8.dp))
    }
}

/**
 * A single completed download item card.
 *
 * Shows the title, filename, and file size with a success indicator.
 */
@Composable
private fun HistoryItemCard(
    item: DownloadItem, 
    serverUrl: String,
    onRetry: (DownloadItem) -> Unit
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            enqueueDownload(context, item, serverUrl)
        } else {
            Toast.makeText(context, "Storage permission is required to download files", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            val isError = item.status.lowercase() == "error"
            Icon(
                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                contentDescription = if (isError) "Failed" else "Completed",
                modifier = Modifier.size(40.dp),
                tint = if (isError) StatusError else StatusCompleted
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Item details
            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = item.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isError) StatusError else MaterialTheme.colorScheme.onSurface
                )
                if (isError) {
                    Text(
                        text = "Failed" + (if (item.error != null) ": ${item.error}" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusError,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Filename
                if (!item.filename.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = "File",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.filename,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // File size
                if (!item.size.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Size",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = FormatUtils.formatFileSize(item.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Actions
            val isCompleted = item.status == "finished" || item.status == "completed" || !item.filename.isNullOrBlank()
            
            if (isError) {
                // Retry Action
                IconButton(onClick = { onRetry(item) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry Download",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (isCompleted && !item.filename.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && 
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        enqueueDownload(context, item, serverUrl)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download File",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Share Action (for ADM/1DM)
                IconButton(onClick = {
                    val isAudio = item.downloadType == "audio" || item.filename?.endsWith(".mp3", ignoreCase = true) == true
                    val route = if (isAudio) "audio_download/" else "download/"
                    val downloadUrl = serverUrl + route + Uri.encode(item.filename)
                    
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, downloadUrl)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share link to..."))
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Link",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

private fun enqueueDownload(context: Context, item: DownloadItem, serverUrl: String) {
    val isAudio = item.downloadType == "audio" || item.filename?.endsWith(".mp3", ignoreCase = true) == true
    val route = if (isAudio) "audio_download/" else "download/"
    val filename = item.filename ?: return
    val downloadUrl = serverUrl + route + Uri.encode(filename)
    
    try {
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle(filename)
            setDescription(item.title)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        }
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
