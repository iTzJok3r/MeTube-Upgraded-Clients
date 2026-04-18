package com.itzjok3r.metubeapp.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// Test Banner Ad Unit ID provided by Google
private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"

/**
 * BannerAd — A Jetpack Compose wrapper for the AdMob AdView.
 *
 * This component satisfies the requirement of displaying ads in a
 * non-annoying way and signals the SecurityManager that the ad
 * infrastructure is technically present in the UI.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    // Notify SecurityManager that an ad component is being composed
    SecurityManager.markAdAsActive()

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                // Set the ad size and unit ID
                setAdSize(AdSize.BANNER)
                adUnitId = TEST_BANNER_ID
                
                // Load the ad
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
