package bizora.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bizora.app.ui.theme.LocalBizoraColors

/**
 * Shimmer gradient brush for skeleton placeholders
 */
@Composable
fun shimmerBrush(
    targetValue: Float = 1200f
): Brush {
    val colors = LocalBizoraColors.current
    val shimmerColors = if (colors.isDark) {
        listOf(
            Color(0xFF1E293B).copy(alpha = 0.5f),
            Color(0xFF334155).copy(alpha = 0.8f),
            Color(0xFF1E293B).copy(alpha = 0.5f)
        )
    } else {
        listOf(
            Color(0xFFE2E8F0).copy(alpha = 0.6f),
            Color(0xFFF1F5F9).copy(alpha = 0.95f),
            Color(0xFFE2E8F0).copy(alpha = 0.6f)
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
}

/**
 * Atomic skeleton placeholder box
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp,
    shape: Shape = RoundedCornerShape(6.dp)
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(shape)
            .background(brush)
    )
}

// =========================================================================
// 1. Employee Today Skeleton (Matches EmployeeTodayContent)
// =========================================================================
@Composable
fun EmployeeTodaySkeleton(
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Horizontal Calendar Weekdays Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(7) {
                    SkeletonBox(
                        modifier = Modifier.weight(1f),
                        height = 68.dp,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        // 2. Section: Today Attendance Header
        item {
            SkeletonBox(width = 150.dp, height = 20.dp, shape = RoundedCornerShape(4.dp))
        }

        // 3. 2x2 Rounded Cards Grid (exact match to EmployeeTodayContent)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Check In & Check Out
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(2) {
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SkeletonBox(width = 38.dp, height = 38.dp, shape = RoundedCornerShape(12.dp))
                                    SkeletonBox(width = 54.dp, height = 20.dp, shape = RoundedCornerShape(8.dp))
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                SkeletonBox(width = 55.dp, height = 12.dp, shape = RoundedCornerShape(4.dp))
                                Spacer(modifier = Modifier.height(5.dp))
                                SkeletonBox(width = 75.dp, height = 18.dp, shape = RoundedCornerShape(4.dp))
                            }
                        }
                    }
                }

                // Row 2: Break Time & Total Days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(2) {
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SkeletonBox(width = 38.dp, height = 38.dp, shape = RoundedCornerShape(12.dp))
                                    SkeletonBox(width = 54.dp, height = 20.dp, shape = RoundedCornerShape(8.dp))
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                SkeletonBox(width = 55.dp, height = 12.dp, shape = RoundedCornerShape(4.dp))
                                Spacer(modifier = Modifier.height(5.dp))
                                SkeletonBox(width = 75.dp, height = 18.dp, shape = RoundedCornerShape(4.dp))
                            }
                        }
                    }
                }
            }
        }

        // 4. Action Card Skeleton
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBox(width = 44.dp, height = 44.dp, shape = RoundedCornerShape(14.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonBox(width = 130.dp, height = 16.dp, shape = RoundedCornerShape(4.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            SkeletonBox(width = 180.dp, height = 12.dp, shape = RoundedCornerShape(4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    SkeletonBox(height = 48.dp, shape = RoundedCornerShape(14.dp))
                }
            }
        }

        // 5. Recent Days Section Header
        item {
            SkeletonBox(width = 110.dp, height = 19.dp, shape = RoundedCornerShape(4.dp))
        }

        // 6. Recent Days List Items
        items(3) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        SkeletonBox(width = 90.dp, height = 16.dp, shape = RoundedCornerShape(4.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBox(width = 140.dp, height = 12.dp, shape = RoundedCornerShape(4.dp))
                    }
                    SkeletonBox(width = 64.dp, height = 24.dp, shape = RoundedCornerShape(999.dp))
                }
            }
        }
    }
}

// =========================================================================
// 2. Employee History Skeleton (Matches EmployeeHistoryContent)
// =========================================================================
@Composable
fun EmployeeHistorySkeleton(
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Card
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    repeat(3) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SkeletonBox(width = 44.dp, height = 28.dp, shape = RoundedCornerShape(6.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            SkeletonBox(width = 54.dp, height = 12.dp, shape = RoundedCornerShape(4.dp))
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            SkeletonBox(width = 170.dp, height = 20.dp, shape = RoundedCornerShape(4.dp))
        }

        // 8 History List Rows
        items(8) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(width = 110.dp, height = 16.dp, shape = RoundedCornerShape(4.dp))
                    SkeletonBox(width = 64.dp, height = 24.dp, shape = RoundedCornerShape(999.dp))
                }
            }
        }
    }
}

// =========================================================================
// 3. Employer Today Skeleton (Matches TodayScreenView)
// =========================================================================
@Composable
fun EmployerTodaySkeleton(
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. ACTIVE WORKPLACE BANNER (Full-width light olive banner matching TodayScreenView)
        item {
            val bannerBg = if (colors.isDark) Color(0xFF222C18) else Color(0xFFEFF5E4)
            val borderLine = if (colors.isDark) Color(0xFF2E3A20) else Color(0xFFE2ECCF)

            Surface(
                color = bannerBg,
                shape = RectangleShape,
                border = BorderStroke(1.dp, borderLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Workplace Icon placeholder
                    SkeletonBox(width = 46.dp, height = 46.dp, shape = RoundedCornerShape(12.dp))

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(width = 120.dp, height = 18.dp, shape = RoundedCornerShape(4.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonBox(width = 130.dp, height = 22.dp, shape = CircleShape)
                    }

                    // Dropdown indicator circle
                    SkeletonBox(width = 34.dp, height = 34.dp, shape = CircleShape)
                }
            }
        }

        // 2. TODAY'S SNAPSHOT CARD
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                SkeletonBox(
                    width = 150.dp,
                    height = 19.dp,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFECEFE5)),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Top Row: Date Pill + Team Status + Open QR Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Date Pill (TUE 06)
                            SkeletonBox(width = 52.dp, height = 54.dp, shape = RoundedCornerShape(12.dp))

                            Spacer(modifier = Modifier.width(12.dp))

                            // Separator line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(38.dp)
                                    .background(if (colors.isDark) Color(0xFF334155) else Color(0xFFEFF2EA))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Team Status
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SkeletonBox(width = 20.dp, height = 20.dp, shape = CircleShape)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SkeletonBox(width = 110.dp, height = 15.dp, shape = RoundedCornerShape(4.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                SkeletonBox(width = 150.dp, height = 13.dp, shape = RoundedCornerShape(4.dp))
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Open QR Pill Button
                            SkeletonBox(width = 82.dp, height = 32.dp, shape = CircleShape)
                        }

                        // Divider
                        HorizontalDivider(
                            color = if (colors.isDark) Color(0xFF334155) else Color(0xFFEFF2EA),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )

                        // Bottom Row: Present & Pending requests
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Present Stat
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SkeletonBox(width = 46.dp, height = 46.dp, shape = CircleShape)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    SkeletonBox(width = 28.dp, height = 22.dp, shape = RoundedCornerShape(4.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SkeletonBox(width = 52.dp, height = 13.dp, shape = RoundedCornerShape(4.dp))
                                }
                            }

                            // Vertical divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(44.dp)
                                    .background(if (colors.isDark) Color(0xFF334155) else Color(0xFFEFF2EA))
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            // Pending requests Stat
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SkeletonBox(width = 46.dp, height = 46.dp, shape = CircleShape)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    SkeletonBox(width = 28.dp, height = 22.dp, shape = RoundedCornerShape(4.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SkeletonBox(width = 95.dp, height = 13.dp, shape = RoundedCornerShape(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. QUICK ACTIONS SECTION (2x2 Grid)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                SkeletonBox(
                    width = 110.dp,
                    height = 19.dp,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // 2x2 Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            repeat(2) {
                                Surface(
                                    color = colors.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFECEFE5)),
                                    shadowElevation = 0.5.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SkeletonBox(width = 38.dp, height = 38.dp, shape = RoundedCornerShape(10.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            SkeletonBox(width = 80.dp, height = 14.dp, shape = RoundedCornerShape(4.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            SkeletonBox(width = 90.dp, height = 12.dp, shape = RoundedCornerShape(4.dp))
                                        }
                                        SkeletonBox(width = 12.dp, height = 12.dp, shape = CircleShape)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. WORKPLACE TEAMS SECTION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Header with "Workplace teams" and "View all >"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(width = 130.dp, height = 19.dp, shape = RoundedCornerShape(4.dp))
                    SkeletonBox(width = 56.dp, height = 14.dp, shape = RoundedCornerShape(4.dp))
                }

                // Unified Card for teams
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFECEFE5)),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        repeat(2) { index ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SkeletonBox(width = 10.dp, height = 10.dp, shape = CircleShape)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    SkeletonBox(width = 85.dp, height = 16.dp, shape = RoundedCornerShape(4.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SkeletonBox(width = 130.dp, height = 13.dp, shape = RoundedCornerShape(4.dp))
                                }
                                SkeletonBox(width = 14.dp, height = 14.dp, shape = CircleShape)
                            }

                            if (index == 0) {
                                HorizontalDivider(
                                    color = if (colors.isDark) Color(0xFF334155) else Color(0xFFF1F3EE),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. Employer Requests Skeleton (Matches AttendanceRequestsSubTab)
// =========================================================================
@Composable
fun EmployerRequestsSkeleton(
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current

    Column(modifier = modifier.fillMaxSize()) {
        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonBox(width = 90.dp, height = 32.dp, shape = RoundedCornerShape(999.dp))
            SkeletonBox(width = 90.dp, height = 32.dp, shape = RoundedCornerShape(999.dp))
            SkeletonBox(width = 90.dp, height = 32.dp, shape = RoundedCornerShape(999.dp))
        }

        // Request Cards
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(3) {
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Top info row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SkeletonBox(width = 40.dp, height = 40.dp, shape = CircleShape)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                SkeletonBox(width = 130.dp, height = 15.dp, shape = RoundedCornerShape(4.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                SkeletonBox(width = 80.dp, height = 11.dp, shape = RoundedCornerShape(4.dp))
                            }
                            SkeletonBox(width = 64.dp, height = 24.dp, shape = RoundedCornerShape(8.dp))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Actions Row (Approve / Reject)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SkeletonBox(modifier = Modifier.weight(1f), height = 40.dp, shape = RoundedCornerShape(10.dp))
                            SkeletonBox(modifier = Modifier.weight(1f), height = 40.dp, shape = RoundedCornerShape(10.dp))
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 5. Employer Daily Skeleton (Matches AttendanceDailySubTab)
// =========================================================================
@Composable
fun EmployerDailySkeleton(
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current

    Column(modifier = modifier.fillMaxSize()) {
        // Date & Status Header
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(width = 140.dp, height = 18.dp, shape = RoundedCornerShape(4.dp))
                SkeletonBox(width = 75.dp, height = 14.dp, shape = RoundedCornerShape(4.dp))
            }
        }

        // Filter Pills Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(4) {
                SkeletonBox(width = 68.dp, height = 28.dp, shape = RoundedCornerShape(999.dp))
            }
        }

        // Roster Cards List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(6) {
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBox(width = 40.dp, height = 40.dp, shape = CircleShape)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonBox(width = 120.dp, height = 14.dp, shape = RoundedCornerShape(4.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            SkeletonBox(width = 65.dp, height = 11.dp, shape = RoundedCornerShape(4.dp))
                        }
                        SkeletonBox(width = 64.dp, height = 24.dp, shape = RoundedCornerShape(999.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// 6. Employer Employees Skeleton (Matches EmployeesScreenView)
// =========================================================================
@Composable
fun EmployerEmployeesSkeleton(
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Subtabs
        SkeletonBox(height = 42.dp, shape = RoundedCornerShape(14.dp))
        Spacer(modifier = Modifier.height(14.dp))

        // Search Field
        SkeletonBox(height = 48.dp, shape = RoundedCornerShape(14.dp))
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Join QR card
            item {
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBox(width = 42.dp, height = 42.dp, shape = RoundedCornerShape(12.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonBox(width = 140.dp, height = 15.dp, shape = RoundedCornerShape(4.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            SkeletonBox(width = 180.dp, height = 12.dp, shape = RoundedCornerShape(4.dp))
                        }
                    }
                }
            }

            // Employee Cards
            items(5) {
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBox(width = 42.dp, height = 42.dp, shape = CircleShape)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonBox(width = 130.dp, height = 14.dp, shape = RoundedCornerShape(4.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            SkeletonBox(width = 75.dp, height = 11.dp, shape = RoundedCornerShape(4.dp))
                        }
                        SkeletonBox(width = 52.dp, height = 22.dp, shape = RoundedCornerShape(6.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// 7. Employer Reports Skeleton (Matches AttendanceReportsSubTab)
// =========================================================================
@Composable
fun EmployerReportsSkeleton(
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Analytics Hero Banner
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBox(width = 44.dp, height = 44.dp, shape = RoundedCornerShape(12.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            SkeletonBox(width = 180.dp, height = 16.dp, shape = RoundedCornerShape(4.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            SkeletonBox(width = 220.dp, height = 12.dp, shape = RoundedCornerShape(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4-Card Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SkeletonBox(modifier = Modifier.weight(1f), height = 74.dp, shape = RoundedCornerShape(14.dp))
                        SkeletonBox(modifier = Modifier.weight(1f), height = 74.dp, shape = RoundedCornerShape(14.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SkeletonBox(modifier = Modifier.weight(1f), height = 74.dp, shape = RoundedCornerShape(14.dp))
                        SkeletonBox(modifier = Modifier.weight(1f), height = 74.dp, shape = RoundedCornerShape(14.dp))
                    }
                }
            }
        }

        // Export Report Options Card
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SkeletonBox(width = 120.dp, height = 16.dp, shape = RoundedCornerShape(4.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    SkeletonBox(height = 44.dp, shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    SkeletonBox(height = 46.dp, shape = RoundedCornerShape(12.dp))
                }
            }
        }
    }
}
