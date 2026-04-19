package com.itzjok3r.metubeapp.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
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
    SecurityManager.markAdAsActive()

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = TEST_BANNER_ID
                loadAd(AdRequest.Builder().build())
                adViewRef = this
            }
        },
        onRelease = { adView ->
            adView.destroy()
            adViewRef = null
        }
    )

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> adViewRef?.resume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> adViewRef?.pause()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> adViewRef?.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
