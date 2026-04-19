package com.itzjok3r.metubeapp.model

/**
 * Request body for the MeTube POST /add endpoint.
 *
 * Currently uses the legacy API shape (url + quality + format) which the
 * backend auto-migrates via _migrate_legacy_request(). All request construction
 * is routed through [MeTubeRequestBuilder] so the payload schema can be updated
 * in one place when migrating to the new API contract.
 *
 * @property url           The URL to download (YouTube, or any yt-dlp supported site).
 * @property quality       Download quality: "best", "1080", "720", "480", or "best" for audio.
 * @property format        Media format hint: "any", "mp4", "m4a", "mp3", "opus", etc.
 *                         Null lets the backend decide.
 * @property folder        Optional subfolder within the download directory.
 * @property custom_name_prefix  Optional filename prefix for the downloaded file.
 */
data class AddRequest(
    val url: String,
    val quality: String = "best",
    val format: String? = null,
    val codec: String? = null,
    val download_type: String? = null,
    val folder: String? = null,
    val custom_name_prefix: String? = null,
    val ytdl_options_presets: List<String>? = null,
    val auto_start: Boolean = true,
    val split_by_chapters: Boolean? = null,
    val chapter_template: String? = null,
    val subtitle_language: String? = null,
    val subtitle_mode: String? = null,
    val ytdl_options_overrides: Map<String, Any>? = null
)
