package bizora.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.ui.theme.LocalBizoraColors

data class BottomTabItem<T>(
    val key: T,
    val label: String,
    val iconRenderer: @Composable (color: Color, size: Dp, active: Boolean) -> Unit,
    val badgeCount: Int = 0
)

/**
 * 01 Soft Capsule Bottom Tab Bar:
 * - Floating rounded capsule container with soft elevation and subtle border.
 * - Active tab: soft sage squircle behind active icon, bold dark olive label, bottom indicator dash.
 * - Inactive tab: soft olive and amber illustration, medium label, balanced layout spacing.
 */
@Composable
fun <T> BizoraBottomBar(
    tabs: List<BottomTabItem<T>>,
    activeTab: T,
    onTabSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current

    // Curated design palette matching "01 Soft capsule"
    val activeSquircleColor = Color(0xFFE2E8D2)
    val activeContentColor = Color(0xFF36451C)
    val activeIndicatorColor = Color(0xFF4E612B)
    val inactiveContentColor = Color(0xFF33382D)
    val capsuleBorderColor = Color(0xFFE8ECE1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, capsuleBorderColor),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isActive = tab.key == activeTab
                    val targetIconColor = if (isActive) activeContentColor else inactiveContentColor

                    val squircleBg by animateColorAsState(
                        targetValue = if (isActive) activeSquircleColor else Color.Transparent,
                        animationSpec = tween(durationMillis = 180),
                        label = "squircle_bg"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = targetIconColor,
                        animationSpec = tween(durationMillis = 180),
                        label = "content_color"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab.key) }
                            .padding(vertical = 1.dp)
                    ) {
                        // Icon Container (Soft squircle capsule on active)
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .height(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Background pill / squircle
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(squircleBg)
                            )

                            // Icon renderer
                            tab.iconRenderer(contentColor, 29.dp, isActive)

                            // Badge count indicator if present
                            if (tab.badgeCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-3).dp)
                                        .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(1.5.dp)
                                        .clip(CircleShape)
                                        .background(colors.danger)
                                        .padding(horizontal = 4.dp, vertical = 0.5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (tab.badgeCount > 99) "99+" else tab.badgeCount.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Tab Label Text
                        Text(
                            text = tab.label,
                            fontSize = 12.5.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.5.dp))

                        // Active Indicator Dash (under active label)
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(3.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(activeIndicatorColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
