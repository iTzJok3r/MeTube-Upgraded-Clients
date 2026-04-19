package com.itzjok3r.metubeapp.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for a MeTube subscription (channel or playlist).
 */
data class SubscriptionItem(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean,
    @SerializedName("check_interval_minutes") val checkIntervalMinutes: Int,
    @SerializedName("download_type") val downloadType: String,
    val codec: String,
    val format: String,
    val quality: String,
    val folder: String,
    @SerializedName("custom_name_prefix") val customNamePrefix: String = "",
    @SerializedName("auto_start") val autoStart: Boolean = true,
    @SerializedName("playlist_item_limit") val playlistItemLimit: Int = 0,
    @SerializedName("split_by_chapters") val splitByChapters: Boolean = false,
    @SerializedName("chapter_template") val chapterTemplate: String = "",
    @SerializedName("subtitle_language") val subtitleLanguage: String = "en",
    @SerializedName("subtitle_mode") val subtitleMode: String = "prefer_manual",
    @SerializedName("ytdl_options_presets") val ytdlOptionsPresets: List<String> = emptyList(),
    @SerializedName("ytdl_options_overrides") val ytdlOptionsOverrides: Map<String, Any> = emptyMap(),
    @SerializedName("last_checked") val lastChecked: Double?,
    @SerializedName("seen_count") val seenCount: Int,
    val error: String?
)
