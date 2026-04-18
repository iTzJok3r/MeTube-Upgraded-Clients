package com.itzjok3r.metubeapp.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus

/**
 * AdManager — Handles the lifecycle and initialization of the AdMob SDK.
 */
object AdManager {
    private var isInitialized = false

    /**
     * Initialize the Mobile Ads SDK.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        MobileAds.initialize(context) { status: InitializationStatus ->
            isInitialized = true
        }
    }

    fun isReady(): Boolean = isInitialized
}
