package com.itzjok3r.metubeapp.util

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {
    /**
     * Formats a size in bytes to a human-readable string (e.g. "1.2 GB").
     * Uses standard decimal units: KB, MB, GB, TB.
     */
    fun formatFileSize(sizeStr: String?): String {
        if (sizeStr.isNullOrEmpty()) return ""
        val size = sizeStr.toDoubleOrNull() ?: return sizeStr
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return DecimalFormat("#,##0.##").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    /**
     * Formats a speed in bytes/s to a human-readable string (e.g. "2.5 MB/s").
     * Uses standard decimal units: KB/s, MB/s, GB/s.
     */
    fun formatSpeed(speedStr: String?): String {
        if (speedStr.isNullOrEmpty()) return ""
        val speed = speedStr.toDoubleOrNull() ?: return speedStr
        if (speed <= 0) return "0 B/s"
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
        val digitGroups = (log10(speed) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return DecimalFormat("#,##0.##").format(speed / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
