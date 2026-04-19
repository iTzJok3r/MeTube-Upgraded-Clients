package com.itzjok3r.metubeapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.itzjok3r.metubeapp.ads.BannerAd

/**
 * Available quality options for the download dropdown.
 *
 * These map to the legacy API quality values that MeTubeRequestBuilder
 * translates into the appropriate request payload.
 */
private val QUALITY_OPTIONS = listOf(
    "best" to "Best Quality",
    "1080" to "1080p",
    "720" to "720p",
    "480" to "480p",
    "mp3" to "MP3 (Audio Only)"
)

private val TYPE_OPTIONS = listOf(
    "video" to "Video",
    "audio" to "Audio",
    "captions" to "Captions",
    "thumbnail" to "Thumbnail"
)

private val FORMAT_OPTIONS = listOf(
    "any" to "Any (Auto)",
    "mp4" to "MP4",
    "ios" to "iOS Compatible",
    "mp3" to "MP3",
    "m4a" to "M4A",
    "opus" to "OPUS"
)

private val CODEC_OPTIONS = listOf(
    "auto" to "Auto Codec",
    "h264" to "H.264",
    "h265" to "H.265 (HEVC)",
    "av1" to "AV1",
    "vp9" to "VP9"
)

/**
 * HomeScreen — The primary screen for submitting download URLs.
 *
 * Features:
 * - URL text field with paste support
 * - Quality selector dropdown
 * - Submit button with loading state
 * - Pre-filled URL support from Android share intent
 *
 * @param defaultQuality  The default quality from settings.
 * @param isSubmitting     Whether a submission is in progress.
 * @param sharedUrl        Pre-filled URL from Android share intent, if any.
 * @param onSubmit         Callback when the user taps the submit button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    defaultQuality: String,
    defaultType: String,
    defaultFormat: String,
    defaultCodec: String,
    isSubmitting: Boolean,
    presets: List<String> = emptyList(),
    serverConfig: Map<String, Any> = emptyMap(),
    sharedUrl: String? = null,
    onSubmit: (url: String, quality: String, type: String, format: String, codec: String, preset: String?, folder: String?, autoStart: Boolean, splitByChapters: Boolean) -> Unit
) {
    // Local state for the URL input field
    var url by remember { mutableStateOf(sharedUrl ?: "") }

    // Local state for the dropdowns
    var selectedQuality by remember { mutableStateOf(defaultQuality) }
    var qualityExpanded by remember { mutableStateOf(false) }

    var selectedType by remember { mutableStateOf(defaultType) }
    var typeExpanded by remember { mutableStateOf(false) }

    var selectedFormat by remember { mutableStateOf(defaultFormat) }
    var formatExpanded by remember { mutableStateOf(false) }

    var selectedCodec by remember { mutableStateOf(defaultCodec) }
    var codecExpanded by remember { mutableStateOf(false) }

    var selectedPreset by remember { mutableStateOf<String?>(null) }
    var presetExpanded by remember { mutableStateOf(false) }
    var autoStart by remember { mutableStateOf(true) }
    var splitByChapters by remember { mutableStateOf(false) }

    // Mobile data warning dialog state
    var showMobileDataWarning by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Get the display label for the currently selected quality
    val qualityLabel = QUALITY_OPTIONS.find { it.first == selectedQuality }?.second ?: "Best Quality"

    val isCustomDirsEnabled = serverConfig["CUSTOM_DIRS"] == true
    var folder by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // ── Header ──────────────────────────────────────────────────────
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Download",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "MeTube Client",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Paste a URL to start downloading",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── URL Input Card ──────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // URL text field
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Video URL") },
                    placeholder = { Text("https://youtube.com/watch?v=...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "URL"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                // Optional Folder input (if enabled by server)
                androidx.compose.animation.AnimatedVisibility(visible = isCustomDirsEnabled) {
                    OutlinedTextField(
                        value = folder,
                        onValueChange = { folder = it },
                        label = { Text("Download Folder (Optional)") },
                        placeholder = { Text("movies, music, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                }

                // Quality dropdown
                ExposedDropdownMenuBox(
                    expanded = qualityExpanded,
                    onExpandedChange = { qualityExpanded = !qualityExpanded }
                ) {
                    OutlinedTextField(
                        value = qualityLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Quality") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = qualityExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    ExposedDropdownMenu(
                        expanded = qualityExpanded,
                        onDismissRequest = { qualityExpanded = false }
                    ) {
                        QUALITY_OPTIONS.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedQuality = value
                                    qualityExpanded = false
                                }
                            )
                        }
                    }
                }

                // Type dropdown
                val typeLabel = TYPE_OPTIONS.find { it.first == selectedType }?.second ?: "Video"
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = typeLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Download Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        TYPE_OPTIONS.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedType = value
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Format dropdown
                val formatLabel = FORMAT_OPTIONS.find { it.first == selectedFormat }?.second ?: "Any (Auto)"
                ExposedDropdownMenuBox(
                    expanded = formatExpanded,
                    onExpandedChange = { formatExpanded = !formatExpanded }
                ) {
                    OutlinedTextField(
                        value = formatLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Format") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    ExposedDropdownMenu(
                        expanded = formatExpanded,
                        onDismissRequest = { formatExpanded = false }
                    ) {
                        FORMAT_OPTIONS.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedFormat = value
                                    formatExpanded = false
                                }
                            )
                        }
                    }
                }

                val isAudioType = selectedType == "audio"
                val audioFormats = setOf("mp3", "m4a", "opus", "wav", "flac")
                val isAudioFormat = selectedFormat in audioFormats
                val showCodecDropdown = !isAudioType && !isAudioFormat

                androidx.compose.animation.AnimatedVisibility(visible = showCodecDropdown) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Codec dropdown
                        val codecLabel = CODEC_OPTIONS.find { it.first == selectedCodec }?.second ?: "Auto Codec"
                        ExposedDropdownMenuBox(
                            expanded = codecExpanded,
                            onExpandedChange = { codecExpanded = !codecExpanded }
                        ) {
                            OutlinedTextField(
                                value = codecLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Video Codec") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = codecExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                shape = MaterialTheme.shapes.medium
                            )

                            ExposedDropdownMenu(
                                expanded = codecExpanded,
                                onDismissRequest = { codecExpanded = false }
                            ) {
                                CODEC_OPTIONS.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedCodec = value
                                            codecExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Preset dropdown (if available)
                androidx.compose.animation.AnimatedVisibility(visible = presets.isNotEmpty()) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        ExposedDropdownMenuBox(
                            expanded = presetExpanded,
                            onExpandedChange = { presetExpanded = !presetExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedPreset ?: "No Preset",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("YTDL-DLP Preset") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                shape = MaterialTheme.shapes.medium
                            )

                            ExposedDropdownMenu(
                                expanded = presetExpanded,
                                onDismissRequest = { presetExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("No Preset") },
                                    onClick = {
                                        selectedPreset = null
                                        presetExpanded = false
                                    }
                                )
                                presets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(preset) },
                                        onClick = {
                                            selectedPreset = preset
                                            presetExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Advanced Options
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = autoStart,
                        onCheckedChange = { autoStart = it }
                    )
                    Text("Auto-start download", style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = splitByChapters,
                        onCheckedChange = { splitByChapters = it }
                    )
                    Text("Split by chapters", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Submit Button ───────────────────────────────────────────────
        Button(
            onClick = {
                // Check if on mobile data
                val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = cm.activeNetwork
                val caps = network?.let { cm.getNetworkCapabilities(it) }
                val isOnMobile = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                      && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

                if (isOnMobile) {
                    showMobileDataWarning = true
                } else {
                    onSubmit(url, selectedQuality, selectedType, selectedFormat, selectedCodec, selectedPreset, if (folder.isBlank()) null else folder, autoStart, splitByChapters)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = url.isNotBlank() && !isSubmitting,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            AnimatedVisibility(
                visible = isSubmitting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Submitting...")
                }
            }

            AnimatedVisibility(
                visible = !isSubmitting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Submit"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add to Queue",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Supported sites hint ────────────────────────────────────────
        Text(
            text = "Supports YouTube, Vimeo, SoundCloud, Twitter, and 1000+ sites via yt-dlp",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
        BannerAd()
    }

    // ── Mobile Data Warning Dialog ──────────────────────────────────────
    if (showMobileDataWarning) {
        AlertDialog(
            onDismissRequest = { showMobileDataWarning = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Mobile Data Detected") },
            text = {
                Text(
                    "You are currently using Mobile Data. Downloading files from the server " +
                    "may consume a significant amount of your data plan. " +
                    "Consider switching to Wi-Fi for large downloads.\n\n" +
                    "Do you want to continue anyway?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMobileDataWarning = false
                        onSubmit(url, selectedQuality, selectedType, selectedFormat, selectedCodec, selectedPreset, if (folder.isBlank()) null else folder, true, false)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Continue Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMobileDataWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
