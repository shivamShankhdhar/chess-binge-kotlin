package bizora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import bizora.app.ui.theme.LocalBizoraColors
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-3940256099942544/6300978111", // Google AdMob Standard Test Banner
    availableWidthDp: Int? = null,
    showPlaceholder: Boolean = false
) {
    val colors = LocalBizoraColors.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = availableWidthDp ?: configuration.screenWidthDp
    val context = LocalContext.current
    val adSize = remember(context, screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }
    var adLoaded by remember(adUnitId, screenWidthDp) { mutableStateOf(false) }

    Surface(
        color = if (showPlaceholder) Color(0xFFF0F1F1) else colors.surfaceMuted,
        border = if (showPlaceholder) null else BorderStroke(1.dp, colors.border),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showPlaceholder && !adLoaded) {
                Box(Modifier.fillMaxWidth().height(adSize.height.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Image, null, tint = Color(0xFFAAADAE), modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(24.dp))
                    Text("Advertisement", color = Color(0xFF828587), fontSize = 11.sp)
                }
            }
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(adSize.height.dp).alpha(if (showPlaceholder && !adLoaded) 0f else 1f),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(adSize)
                        this.adUnitId = adUnitId
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                adLoaded = true
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                adLoaded = false
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}
