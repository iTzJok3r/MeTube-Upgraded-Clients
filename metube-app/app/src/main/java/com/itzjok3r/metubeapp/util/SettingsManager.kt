package com.itzjok3r.metubeapp.util

import android.content.Context
import android.content.SharedPreferences

/**
 * SettingsManager — Handles persistent storage of app configuration using SharedPreferences.
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("metube_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SERVER_URL = "server_url"
        const val KEY_DEFAULT_QUALITY = "default_quality"
        const val KEY_DEFAULT_TYPE = "default_type"
        const val KEY_DEFAULT_FORMAT = "default_format"
        const val KEY_DEFAULT_CODEC = "default_codec"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_ALLOW_BACKGROUND = "allow_background"
        const val KEY_NETWORK_POLICY = "network_policy"
        
        const val DEFAULT_SERVER = "http://localhost:8081/"
    }

    fun getServerUrl(): String = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER) ?: DEFAULT_SERVER
    fun setServerUrl(url: String) = prefs.edit().putString(KEY_SERVER_URL, url).apply()

    fun getDefaultQuality(): String = prefs.getString(KEY_DEFAULT_QUALITY, "best") ?: "best"
    fun setDefaultQuality(quality: String) = prefs.edit().putString(KEY_DEFAULT_QUALITY, quality).apply()

    fun getDefaultType(): String = prefs.getString(KEY_DEFAULT_TYPE, "video") ?: "video"
    fun setDefaultType(type: String) = prefs.edit().putString(KEY_DEFAULT_TYPE, type).apply()

    fun getDefaultFormat(): String = prefs.getString(KEY_DEFAULT_FORMAT, "any") ?: "any"
    fun setDefaultFormat(format: String) = prefs.edit().putString(KEY_DEFAULT_FORMAT, format).apply()

    fun getDefaultCodec(): String = prefs.getString(KEY_DEFAULT_CODEC, "auto") ?: "auto"
    fun setDefaultCodec(codec: String) = prefs.edit().putString(KEY_DEFAULT_CODEC, codec).apply()

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, true)
    fun setDarkMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()

    fun getAllowBackground(): Boolean = prefs.getBoolean(KEY_ALLOW_BACKGROUND, false)
    fun setAllowBackground(enabled: Boolean) = prefs.edit().putBoolean(KEY_ALLOW_BACKGROUND, enabled).apply()

    /**
     * Policy for server-to-device downloads:
     * 0 = Always allow
     * 1 = Warn on metered (Default)
     * 2 = Unmetered only
     */
    fun getNetworkPolicy(): Int = prefs.getInt(KEY_NETWORK_POLICY, 1)
    fun setNetworkPolicy(policy: Int) = prefs.edit().putInt(KEY_NETWORK_POLICY, policy).apply()
}
