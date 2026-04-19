package com.itzjok3r.metubeapp.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itzjok3r.metubeapp.model.DownloadItem
import com.itzjok3r.metubeapp.ui.theme.StatusCompleted
import com.itzjok3r.metubeapp.ui.theme.StatusDownloading
import com.itzjok3r.metubeapp.ui.theme.StatusError
import com.itzjok3r.metubeapp.ui.theme.StatusPending
import com.itzjok3r.metubeapp.util.FormatUtils
import com.itzjok3r.metubeapp.ads.BannerAd


/**
 * QueueScreen — Displays active and pending downloads with real-time progress.
 *
 * Items come from the ViewModel's queue StateFlow, which is hydrated from
 * GET /history on startup and updated via Socket.IO events in real time.
 * Each item shows a progress bar, speed, ETA, and status indicator.
 *
 * @param queueItems      List of active/pending download items.
 * @param isLoading        Whether the initial data load is in progress.
 * @param connectionStatus Current Socket.IO connection status string.
 * @param onRetry          Callback to retry loading if initial fetch failed.
 */
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    queueItems: List<DownloadItem>,
    isLoading: Boolean,
    connectionStatus: String,
    onRemove: (com.itzjok3r.metubeapp.model.DownloadItem) -> Unit,
    onStart: (String) -> Unit,
    onRetry: () -> Unit
) {
    val pullRefreshState = rememberPullToRefreshState()
    
    if (pullRefreshState.isRefreshing) {
        androidx.compose.runtime.LaunchedEffect(true) {
            onRetry()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            // ── Header with connection status ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Download Queue",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${queueItems.size} item(s) • $connectionStatus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Content ─────────────────────────────────────────────────────
            when {
                // Loading state (only show if not refreshing via pull)
                isLoading && !pullRefreshState.isRefreshing -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading queue...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Empty state
                queueItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "No downloads",
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No active downloads",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Add a URL from the Home tab to get started",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Queue items list
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = queueItems,
                            key = { it.id }
                        ) { item ->
                            QueueItemCard(
                                item = item, 
                                onRemove = { onRemove(it) },
                                onStart = onStart
                            )
                        }

                        // Bottom spacing
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
 * A single download item card in the queue list.
 *
 * Displays:
 * - Title with status-colored indicator
 * - Animated progress bar
 * - Speed and ETA metadata
 * - Error message if the download failed
 */
@Composable
private fun QueueItemCard(
    item: DownloadItem,
    onRemove: (DownloadItem) -> Unit,
    onStart: (String) -> Unit
) {
    // Animate the progress bar value for smooth transitions
    val animatedProgress by animateFloatAsState(
        targetValue = (item.percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    // Determine status color and icon — remember to avoid recalculation on every progress tick
    val (statusColor, statusIcon) = androidx.compose.runtime.remember(item.status) {
        when (item.status.lowercase()) {
            "downloading" -> StatusDownloading to Icons.Default.CloudDownload
            "finished" -> StatusCompleted to Icons.Default.CloudDownload
            "error" -> StatusError to Icons.Default.ErrorOutline
            else -> StatusPending to Icons.Default.HourglassTop
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // ── Title row with status icon ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = item.status,
                    modifier = Modifier.size(24.dp),
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.ifEmpty { "Fetching title..." },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }

                // Delete Button
                IconButton(onClick = { onRemove(item) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Cancel Download",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }

                // Start Button (for pending items)
                if (item.status.lowercase() == "pending") {
                    IconButton(onClick = { onStart(item.id) }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Download",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Percentage display
                if (item.status.lowercase() == "downloading") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${item.percent.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Progress bar ────────────────────────────────────────────
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.small),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // ── Speed and ETA row ───────────────────────────────────────
            if (item.speed != null || item.eta != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Speed
                    if (item.speed != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val formattedSpeed = androidx.compose.runtime.remember(item.speed) {
                                FormatUtils.formatSpeed(item.speed ?: "")
                            }
                            Text(
                                text = formattedSpeed,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // ETA
                    if (item.eta != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "ETA",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.eta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Error message ───────────────────────────────────────────
            if (item.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ ${item.error}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusError,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
