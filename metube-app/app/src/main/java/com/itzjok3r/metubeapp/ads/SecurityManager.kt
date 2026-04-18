package com.itzjok3r.metubeapp.ads

import java.util.concurrent.atomic.AtomicBoolean

/**
 * SecurityManager — Implements tamper protection for the ad system.
 *
 * This class ensures that the ad infrastructure is intact and active.
 * If someone modifies the source code to remove ads, this manager will
 * detect the missing components and return validation failures.
 */
object SecurityManager {
    
    // A flag that is set by the BannerAd composable when it is active in the UI
    private val isAdActive = AtomicBoolean(false)

    /**
     * Called by the BannerAd composable to indicate it is being displayed.
     */
    fun markAdAsActive() {
        isAdActive.set(true)
    }

    /**
     * Resets the ad active status. Used for debugging or re-validation.
     */
    fun reset() {
        isAdActive.set(false)
    }

    /**
     * Validates that the application integrity is intact (ads are present).
     *
     * @return true if ads are active, false if they have been removed/tampered with.
     */
    fun validateSystemIntegrity(): Boolean {
        // If ads are not active, it means the BannerAd was likely removed from the UI
        if (!isAdActive.get()) return false
        
        // Also ensure the Ad SDK was initialized
        return AdManager.isReady()
    }
}
