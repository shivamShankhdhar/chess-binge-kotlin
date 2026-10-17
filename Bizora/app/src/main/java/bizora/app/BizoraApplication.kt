package bizora.app

import android.app.Application
import bizora.app.data.local.PreferencesManager
import bizora.app.data.remote.ApiClient
import bizora.app.data.repository.BizoraRepository
import com.google.android.gms.ads.MobileAds

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BizoraApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var repository: BizoraRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        repository = BizoraRepository(preferencesManager)

        ApiClient.setTokenManager(
            tokenProvider = { preferencesManager.getAccessToken() },
            refreshTokenProvider = { preferencesManager.getRefreshToken() },
            onTokensRefreshed = { newAccess, newRefresh ->
                preferencesManager.updateTokensSync(newAccess, newRefresh)
            },
            onAuthFailed = {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.signOut()
                }
            }
        )

        // Let the first app frame render before starting the ads SDK and preloads.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            MobileAds.initialize(this) {
                bizora.app.utils.AdMobInterstitialHelper.preload(this)
                bizora.app.utils.AdMobRewardHelper.preload(this)
            }
        }, 1_000L)
    }
}
