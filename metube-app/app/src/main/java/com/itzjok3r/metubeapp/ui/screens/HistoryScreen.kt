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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.itzjok3r.metubeapp.util.NetworkUtils
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
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyItems: List<DownloadItem>,
    serverUrl: String,
    networkPolicy: Int,
    isLoading: Boolean,
    onRemove: (DownloadItem) -> Unit,
    onRetry: (DownloadItem) -> Unit,
    onRefresh: () -> Unit
) {
    val pullRefreshState = rememberPullToRefreshState()
    
    if (pullRefreshState.isRefreshing) {
        androidx.compose.runtime.LaunchedEffect(true) {
            onRefresh()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
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
            when {
                // Loading state (only show if not refreshing via pull)
                isLoading && !pullRefreshState.isRefreshing -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                historyItems.isEmpty() -> {
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
                }

                else -> {
                    // History items list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = historyItems,
                            key = { it.id }
                        ) { item ->
                            HistoryItemCard(
                                item = item, 
                                serverUrl = serverUrl, 
                                networkPolicy = networkPolicy,
                                onRemove = onRemove, 
                                onRetry = onRetry
                            )
                        }

                        // Bottom spacing for navigation bar
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }

            BannerAd(modifier = Modifier.padding(vertical = 8.dp))
        }

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullRefreshState
        )
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
    networkPolicy: Int,
    onRemove: (DownloadItem) -> Unit,
    onRetry: (DownloadItem) -> Unit
) {
    val context = LocalContext.current
    
    // State for network confirmation dialog
    var showNetworkDialog by remember { mutableStateOf(false) }
    var networkRisk by remember { mutableStateOf(NetworkUtils.NetworkRisk.SAFE) }

    val onDownloadClick = {
        val risk = NetworkUtils.getNetworkRisk(context)
        networkRisk = risk
        
        when {
            // Always Allow
            networkPolicy == 0 -> enqueueDownload(context, item, serverUrl)
            
            // Offline
            risk == NetworkUtils.NetworkRisk.OFFLINE -> {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            }
            
            // Wi-Fi Only
            networkPolicy == 2 && risk != NetworkUtils.NetworkRisk.SAFE -> {
                Toast.makeText(context, "Download blocked: Wi-Fi only policy enabled", Toast.LENGTH_LONG).show()
            }
            
            // Warn on Metered
            networkPolicy == 1 && risk != NetworkUtils.NetworkRisk.SAFE -> {
                showNetworkDialog = true
            }
            
            // Safe network
            else -> enqueueDownload(context, item, serverUrl)
        }
    }

    if (showNetworkDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNetworkDialog = false },
            title = { Text("Network Warning") },
            text = {
                val description = NetworkUtils.getNetworkDescription(context)
                val message = when (networkRisk) {
                    NetworkUtils.NetworkRisk.RESTRICTED -> 
                        "Data Saver is enabled. Downloading this file may be restricted or incur costs."
                    else -> 
                        "You are currently on a metered network ($description). Downloading this file from the server to your device will consume your data plan."
                }
                Text(message)
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showNetworkDialog = false
                    enqueueDownload(context, item, serverUrl)
                }) {
                    Text("Download Now")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showNetworkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
            // Status icon — remember to avoid recalculation on recomposition
            val isError = androidx.compose.runtime.remember(item.status) {
                item.status.lowercase() == "error"
            }
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
                        val formattedSize = androidx.compose.runtime.remember(item.size) {
                            FormatUtils.formatFileSize(item.size ?: "")
                        }
                        Text(
                            text = formattedSize,
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
                        onDownloadClick()
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
                    // Normalize serverUrl to ensure it ends with slash, then append route (which doesn't start with slash)
                    val base = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
                    val downloadUrl = base + route + Uri.encode(item.filename)
                    
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

                // Delete Action
                IconButton(onClick = { onRemove(item) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete from History",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
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
    
    // Normalize serverUrl: ensure exactly one slash between host and route
    val base = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    val downloadUrl = base + route + Uri.encode(filename)
    
    android.util.Log.d("HistoryScreen", "Enqueuing download: $downloadUrl")
    
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
