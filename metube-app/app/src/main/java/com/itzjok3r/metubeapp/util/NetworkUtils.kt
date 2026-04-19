package com.itzjok3r.metubeapp.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * NetworkUtils — Centralized utility for network classification and risk assessment.
 */
object NetworkUtils {

    enum class NetworkRisk {
        SAFE,      // Unmetered network (e.g. standard Wi-Fi, Ethernet)
        RISKY,     // Metered network (e.g. Cellular, Metered Wi-Fi, Hotspot)
        RESTRICTED, // Data Saver enabled or background data restricted
        OFFLINE    // No connectivity
    }

    /**
     * Classifies the current network based on risk of data consumption.
     */
    fun getNetworkRisk(context: Context): NetworkRisk {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkRisk.OFFLINE

        val activeNetwork = cm.activeNetwork ?: return NetworkRisk.OFFLINE
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkRisk.OFFLINE

        // 1. Check for basic connectivity
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkRisk.OFFLINE
        }

        // 2. Check for Data Saver / Restriction state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val restrictMode = cm.restrictBackgroundStatus
            if (restrictMode == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) {
                return NetworkRisk.RESTRICTED
            }
        }

        // 3. Robust metered check
        // isActiveNetworkMetered() accounts for cellular, metered Wi-Fi, and VPNs marked metered.
        // It is more reliable than just checking transport types.
        if (cm.isActiveNetworkMetered) {
            return NetworkRisk.RISKY
        }

        // 4. Secondary transport-based check (Safe fallback)
        // If not metered and we're on Wi-Fi, Ethernet, or LoWPAN, we consider it safe.
        return if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)) {
            NetworkRisk.SAFE
        } else {
            // If it's not metered but it's an "unknown" transport (like some VPNs or Cellular 
            // that doesn't report metered correctly), we default to RISKY for user safety.
            NetworkRisk.RISKY
        }
    }

    /**
     * Returns a human-readable description of the current network type.
     */
    fun getNetworkDescription(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Unknown"
        val activeNetwork = cm.activeNetwork ?: return "Disconnected"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "Disconnected"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Unknown Network"
        }
    }
}
