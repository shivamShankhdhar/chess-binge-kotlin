package bizora.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.R
import bizora.app.data.model.Workplace
import bizora.app.ui.components.BizoraSignInIcon
import kotlinx.coroutines.delay

@Composable
fun WorkplaceCreatedScreen(workplace: Workplace, onContinue: () -> Unit) {
    val olive = Color(0xFF4B5927)
    val ink = Color(0xFF1E281C)

    BackHandler { /* Keep the confirmation visible until navigation completes. */ }

    LaunchedEffect(workplace.id) {
        delay(3_000)
        onContinue()
    }

    Surface(color = Color(0xFFFAFAF7), modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BizoraSignInIcon()
                Spacer(Modifier.width(9.dp))
                Text("Bizora", color = ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(46.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(248.dp)
                    .background(Color(0xFFF0F3E7), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.workspace_hero_illustration),
                    contentDescription = "Team working together",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 10.dp)
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).size(64.dp),
                    shape = CircleShape,
                    color = olive,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }
            }

            Spacer(Modifier.height(42.dp))

            Text(
                text = "Workplace created!",
                color = ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = workplace.name,
                color = olive,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Your space is ready for your team. Taking you to your dashboard…",
                color = Color(0xFF68715E),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(34.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = olive,
                strokeWidth = 2.5.dp
            )
        }
    }
}
