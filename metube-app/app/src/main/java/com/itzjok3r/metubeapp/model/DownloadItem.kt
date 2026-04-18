package com.itzjok3r.metubeapp.model

/**
 * Represents a single download item in the MeTube queue or history.
 *
 * Fields are intentionally nullable/defaulted to handle partial JSON payloads
 * from Socket.IO events defensively — not every event includes all fields.
 *
 * @property id        Unique identifier assigned by the MeTube backend.
 * @property title     Display title of the media (may be empty until metadata is fetched).
 * @property status    Current status: "pending", "preparing", "downloading", "finished", "error".
 * @property percent   Download progress as a float 0.0–100.0.
 * @property speed     Human-readable download speed (e.g. "2.5 MiB/s").
 * @property eta       Estimated time remaining as a human-readable string.
 * @property filename  Final filename on disk (populated after completion).
 * @property size      Human-readable file size (e.g. "150.3 MiB").
 * @property error     Error message if the download failed.
 * @property url       Original URL that was submitted.
 */
data class DownloadItem(
    val id: String = "",
    val title: String = "",
    val status: String = "pending",
    val percent: Float = 0f,
    val speed: String? = null,
    val eta: String? = null,
    val filename: String? = null,
    val size: String? = null,
    val error: String? = null,
    val url: String? = null,
    val downloadType: String? = null,
    val quality: String? = null,
    val format: String? = null,
    val codec: String? = null
)
