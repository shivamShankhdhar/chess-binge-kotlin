package bizora.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bizora.app.R

// Preserve the olive, sage and amber artwork colors instead of tinting the illustrations.

/**
 * 1. DashboardTabIcon:
 * Illustrated dashboard tiles with a chart and completed task.
 * Active: R.drawable.ic_tab_dashboard_active
 * Inactive: R.drawable.ic_tab_dashboard_inactive
 */
@Composable
fun DashboardTabIcon(
    modifier: Modifier = Modifier,
    size: Dp = 29.dp,
    active: Boolean = false
) {
    Icon(
        painter = painterResource(
            id = if (active) R.drawable.ic_tab_dashboard_active else R.drawable.ic_tab_dashboard_inactive
        ),
        contentDescription = "Dashboard",
        tint = Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * 2. AttendanceTabIcon:
 * Illustrated attendance clipboard with a verified checkmark.
 * Active: R.drawable.ic_tab_attendance_active
 * Inactive: R.drawable.ic_tab_attendance_inactive
 */
@Composable
fun AttendanceTabIcon(
    modifier: Modifier = Modifier,
    size: Dp = 29.dp,
    active: Boolean = false
) {
    Icon(
        painter = painterResource(
            id = if (active) R.drawable.ic_tab_attendance_active else R.drawable.ic_tab_attendance_inactive
        ),
        contentDescription = "Attendance",
        tint = Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * 3. EmployeesTabIcon:
 * Illustrated pair of teammates.
 * Active: R.drawable.ic_tab_employees_active
 * Inactive: R.drawable.ic_tab_employees_inactive
 */
@Composable
fun EmployeesTabIcon(
    modifier: Modifier = Modifier,
    size: Dp = 29.dp,
    active: Boolean = false
) {
    Icon(
        painter = painterResource(
            id = if (active) R.drawable.ic_tab_employees_active else R.drawable.ic_tab_employees_inactive
        ),
        contentDescription = "Employees",
        tint = Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * 4. WorkplaceTabIcon:
 * Illustrated workplace storefront with an olive awning.
 * Active: R.drawable.ic_tab_workplace_active
 * Inactive: R.drawable.ic_tab_workplace_inactive
 */
@Composable
fun WorkplaceTabIcon(
    modifier: Modifier = Modifier,
    size: Dp = 29.dp,
    active: Boolean = false
) {
    Icon(
        painter = painterResource(
            id = if (active) R.drawable.ic_tab_workplace_active else R.drawable.ic_tab_workplace_inactive
        ),
        contentDescription = "Workplace",
        tint = Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * 5. SettingsTabIcon:
 * Illustrated settings panel with three adjustable controls.
 * Active: R.drawable.ic_tab_settings_active
 * Inactive: R.drawable.ic_tab_settings_inactive
 */
@Composable
fun SettingsTabIcon(
    modifier: Modifier = Modifier,
    size: Dp = 29.dp,
    active: Boolean = false
) {
    Icon(
        painter = painterResource(
            id = if (active) R.drawable.ic_tab_settings_active else R.drawable.ic_tab_settings_inactive
        ),
        contentDescription = "Settings",
        tint = Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * 6. HistoryTabIcon:
 * Illustrated attendance record with a clock.
 * Active: R.drawable.ic_tab_history_active
 * Inactive: R.drawable.ic_tab_history_inactive
 */
@Composable
fun HistoryTabIcon(
    modifier: Modifier = Modifier,
    size: Dp = 29.dp,
    active: Boolean = false
) {
    Icon(
        painter = painterResource(
            id = if (active) R.drawable.ic_tab_history_active else R.drawable.ic_tab_history_inactive
        ),
        contentDescription = "History",
        tint = Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * 7. ProfileTabIcon:
 * Illustrated member portrait with a verification badge.
 * Active: R.drawable.ic_tab_profile_active
 * Inactive: R.drawable.ic_tab_profile_inactive
 */
@Composable
fun ProfileTabIcon(
    modifier: Modifier = Modifier,
    size: Dp = 29.dp,
    active: Boolean = false
) {
    Icon(
        painter = painterResource(
            id = if (active) R.drawable.ic_tab_profile_active else R.drawable.ic_tab_profile_inactive
        ),
        contentDescription = "Profile",
        tint = Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * 8. TodayTabIcon:
 * Illustrated daily calendar with a completed day badge.
 * Active: R.drawable.ic_tab_today_active
 * Inactive: R.drawable.ic_tab_today_inactive
 */
@Composable
fun TodayTabIcon(
    modifier: Modifier = Modifier,
    size: Dp = 29.dp,
    active: Boolean = false
) {
    Icon(
        painter = painterResource(
            id = if (active) R.drawable.ic_tab_today_active else R.drawable.ic_tab_today_inactive
        ),
        contentDescription = "Today",
        tint = Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * 9. HomeTabIcon:
 * Illustrated home with an olive roof and small plant.
 * Active: R.drawable.ic_tab_home_active
 * Inactive: R.drawable.ic_tab_home_inactive
 */
@Composable
fun HomeTabIcon(
    modifier: Modifier = Modifier,
    size: Dp = 29.dp,
    active: Boolean = false
) {
    Icon(
        painter = painterResource(
            id = if (active) R.drawable.ic_tab_home_active else R.drawable.ic_tab_home_inactive
        ),
        contentDescription = "Home",
        tint = Color.Unspecified,
        modifier = modifier.size(size)
    )
}
