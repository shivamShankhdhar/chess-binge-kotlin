package bizora.app.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Helper to preload and present Google AdMob Interstitial Ads
 * before executing sensitive actions such as file export.
 */
object AdMobInterstitialHelper {
    // Official Google AdMob Interstitial Test Unit ID
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private var mInterstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        if (mInterstitialAd != null || isLoading) return
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    private tailrec fun findActivity(context: Context): Activity? = when (context) {
        is Activity -> context
        is android.content.ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }

    /**
     * Shows an interstitial ad if available, then executes onComplete.
     * If the ad is not ready or fails, onComplete is called immediately without blocking the user.
     */
    fun showAdThenExecute(context: Context, onComplete: () -> Unit) {
        val activity = findActivity(context)
        val ad = mInterstitialAd
        if (ad != null && activity != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    preload(context)
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    preload(context)
                    onComplete()
                }
            }
            ad.show(activity)
        } else {
            preload(context)
            onComplete()
        }
    }
}
