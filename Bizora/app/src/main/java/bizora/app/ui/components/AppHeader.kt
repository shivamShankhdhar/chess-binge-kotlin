package bizora.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.data.model.Workplace
import bizora.app.ui.theme.LocalBizoraColors

@Composable
fun AppHeader(
    workplace: Workplace,
    notificationCount: Int = 0,
    onWorkplaceClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val colors = LocalBizoraColors.current

    Surface(
        color = colors.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Workplace Switcher Trigger
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceMuted)
                        .clickable { onWorkplaceClick() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = workplace.name,
                                fontSize = 18.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Workplace",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = if (workplace.role.name == "EMPLOYER") "Employer Portal" else "Employee Access",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.brandPrimary
                        )
                    }
                }

                // Action Icons (Theme toggle, Notifications, Profile)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Theme Toggle
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceMuted)
                    ) {
                        Icon(
                            if (colors.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Notifications Button + Clean Badge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(
                            onClick = onNotificationsClick,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(colors.surfaceMuted)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (notificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = 0.dp)
                                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                    .clip(CircleShape)
                                    .background(colors.danger)
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }

                    // Profile Button
                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors.brandTint)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatherSearchIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            val strokeW = 2.2f
            drawCircle(
                color = color,
                radius = 7.5f,
                center = Offset(10.5f, 10.5f),
                style = Stroke(width = strokeW)
            )
            drawLine(
                color = color,
                start = Offset(15.8f, 15.8f),
                end = Offset(21f, 21f),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun FeatherBellIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            val strokeW = 2.2f
            // Feather bell body: M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9
            val body = Path().apply {
                moveTo(18f, 8f)
                cubicTo(18f, 4.686f, 15.314f, 2f, 12f, 2f)
                cubicTo(8.686f, 2f, 6f, 4.686f, 6f, 8f)
                cubicTo(6f, 15f, 3f, 17f, 3f, 17f)
                lineTo(21f, 17f)
                cubicTo(21f, 17f, 18f, 15f, 18f, 8f)
            }
            drawPath(body, color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Feather bell clapper: M13.73 21a2 2 0 0 1-3.46 0
            val clapper = Path().apply {
                moveTo(10.27f, 20.2f)
                cubicTo(10.6f, 22f, 13.4f, 22f, 13.73f, 20.2f)
            }
            drawPath(clapper, color, style = Stroke(strokeW, cap = StrokeCap.Round))
        }
    }
}

@Composable
fun HomeHeaderOptions(
    userName: String?,
    userEmail: String?,
    initials: String,
    avatarUrl: String? = null,
    notificationCount: Int = 0,
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current
    val buttonBg = if (colors.isDark) Color(0xFF1E293B) else Color(0xFFF8F9F3)
    val buttonBorder = if (colors.isDark) Color(0xFF334155) else Color(0xFFECEFE5)
    val iconColor = if (colors.isDark) Color(0xFFF1F5F9) else Color(0xFF1B2210)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.canvas)
            .statusBarsPadding()
            .clipToBounds()
    ) {
        // Decorative background art in top-right behind buttons (matching React Native HeaderBackgroundArt)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clipToBounds()
        ) {
            HeaderBackgroundArt(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-4).dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Avatar squircle + Name + Subtitle (Clickable lockup to open profile/settings)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onProfileClick() }
                    .padding(vertical = 2.dp, horizontal = 2.dp)
            ) {
                val firstName = userName?.trim()?.split(" ")?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Shivam"
                val displayInitial = firstName.take(1).uppercase()

                // Squircle Avatar Container (42x42, 14dp corner radius, 1dp border)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(buttonBg)
                        .border(1.dp, buttonBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayInitial,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Hello, $firstName",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Your workplace at a glance",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: Search & Notification Circular Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Button (Circular 40x40)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .border(1.dp, buttonBorder, CircleShape)
                        .clickable { onSearchClick() },
                    contentAlignment = Alignment.Center
                ) {
                    FeatherSearchIcon(
                        size = 18.dp,
                        color = iconColor
                    )
                }

                // Notifications Bell Button + Docked Unread Badge
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(buttonBg)
                            .border(1.dp, buttonBorder, CircleShape)
                            .clickable { onNotificationsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        FeatherBellIcon(
                            size = 18.dp,
                            color = iconColor
                        )
                    }

                    // Docked Unread Count Badge
                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                .clip(CircleShape)
                                .background(colors.brandPrimary)
                                .border(1.5.dp, if (colors.isDark) Color(0xFF0F172A) else Color.White, CircleShape)
                                .padding(horizontal = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (notificationCount > 9) "9+" else notificationCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

