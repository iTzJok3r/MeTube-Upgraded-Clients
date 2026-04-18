package com.itzjok3r.metubeapp.network

import com.itzjok3r.metubeapp.model.AddRequest

/**
 * Adapter layer that constructs the /add request payload.
 *
 * Currently uses the legacy API shape (url + quality + format) which the MeTube
 * backend auto-migrates via _migrate_legacy_request(). When the backend eventually
 * drops legacy support, update THIS class only — no changes needed in the ViewModel,
 * screens, or API interface.
 *
 * Quality mapping:
 * - "best"  → best available video quality
 * - "1080"  → 1080p video
 * - "720"   → 720p video
 * - "480"   → 480p video
 * - "mp3"   → audio-only as MP3 (maps to format="mp3", quality="best")
 */
object MeTubeRequestBuilder {

    /**
     * Build an [AddRequest] from user-facing configurations.
     *
     * @param url     The URL to download.
     * @param quality The user-selected quality.
     * @param format  The user-selected format (mp4, m4a, ios, etc.).
     * @param codec   The video codec (h264, vp9, etc.) or null/auto.
     */
    fun buildAddRequest(
        url: String,
        quality: String?,
        format: String?,
        codec: String?,
        downloadType: String?
    ): AddRequest {
        val finalFormat = if (format.isNullOrBlank() || format.lowercase() == "any") "any" else format.lowercase()
        val finalCodec = if (codec.isNullOrBlank() || codec.lowercase() == "auto") "auto" else codec.lowercase()
        val finalQuality = if (quality.isNullOrBlank()) "best" else quality.lowercase()
        val finalDownloadType = if (downloadType.isNullOrBlank()) "video" else downloadType.lowercase()

        // Apply fallback if legacy "mp3" quality was supplied directly
        val actualFormat = if (finalQuality == "mp3") "mp3" else finalFormat
        val actualQuality = if (finalQuality == "mp3") "best" else finalQuality
        
        // If legacy fallback is mp3, force downloadType to audio
        val actualDownloadType = if (finalQuality == "mp3") "audio" else finalDownloadType

        return AddRequest(
            url = url.trim(),
            quality = actualQuality,
            format = actualFormat,
            codec = finalCodec,
            download_type = actualDownloadType
        )
    }
}
