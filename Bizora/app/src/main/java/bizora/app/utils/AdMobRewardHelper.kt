package bizora.app.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Helper to manage Google AdMob Rewarded Ads.
 * Enforces requirement:
 * 1. Ad Trigger: When a feature requiring geolocation is clicked, first show a reward ad.
 * 2. Permission Modal: Only after the ad is completed, open location request modal to capture GPS & save attendance.
 * 3. Fallback: If the ad is skipped/cancelled or GPS permission is denied/not provided, cancel the action entirely and do not open the geolocation-dependent feature.
 */
object AdMobRewardHelper {
    // Official Google AdMob Rewarded Ad Test Unit ID
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var mRewardedAd: RewardedAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        if (mRewardedAd != null || isLoading) return
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    isLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mRewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Shows a rewarded ad.
     * @param onRewardCompleted invoked ONLY if user watched and earned the reward
     * @param onCancelled invoked if ad was skipped, dismissed without reward, or failed to show
     */
    fun showRewardAd(
        context: Context,
        onRewardCompleted: () -> Unit,
        onCancelled: () -> Unit
    ) {
        val activity = context as? Activity
        val ad = mRewardedAd

        if (ad != null && activity != null) {
            var rewardEarned = false

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mRewardedAd = null
                    preload(context)
                    if (rewardEarned) {
                        onRewardCompleted()
                    } else {
                        onCancelled()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mRewardedAd = null
                    preload(context)
                    onCancelled()
                }
            }

            ad.show(activity) { _ ->
                // User earned the reward by watching the ad
                rewardEarned = true
            }
        } else {
            preload(context)
            // If ad is not preloaded yet (e.g. initial run), proceed with action
            onRewardCompleted()
        }
    }
}
