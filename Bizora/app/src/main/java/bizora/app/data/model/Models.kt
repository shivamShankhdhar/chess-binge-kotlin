package bizora.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val id: String,
    val name: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val phone: String? = null,
    val status: String? = null,
    val hasMpin: Boolean = false,
    val appLockEnabled: Boolean? = null,
    val biometricEnabled: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Workplace(
    val id: String,
    val name: String,
    val timezone: String = "Asia/Kolkata",
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val wifiSsid: String? = null,
    val role: WorkplaceRole = WorkplaceRole.EMPLOYER,
    val memberId: String? = null,
    val employeeCode: String? = null,
    val code: String? = null,
    val memberCount: Int? = null,
    val createdAt: String? = null,
    val attendanceSettings: AttendanceSettings? = null
) {
    val formattedCode: String
        get() {
            if (!code.isNullOrBlank() && code.startsWith("BWP-")) return code
            val raw = (if (id.startsWith("wp-", ignoreCase = true)) id.removePrefix("wp-") else id).uppercase()
            if (raw.length >= 7) {
                val p1 = raw.take(4)
                val p2 = raw.drop(4).take(3)
                return "BWP-$p1-$p2"
            }
            val padded = raw.padStart(7, '0')
            return "BWP-${padded.take(4)}-${padded.drop(4).take(3)}"
        }
}

enum class WorkplaceRole {
    @Json(name = "EMPLOYER") EMPLOYER,
    @Json(name = "EMPLOYEE") EMPLOYEE
}

@JsonClass(generateAdapter = true)
data class AttendanceSettings(
    val requireWifi: Boolean = false,
    val autoCloseHour: Int = 20,
    val allowEmployeeViewHistory: Boolean = true,
    val requireGeofence: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geofenceRadius: Double = 100.0
)

@JsonClass(generateAdapter = true)
data class AttendanceSession(
    val session: SessionDetails,
    val qrToken: String? = null,
    val qrPayload: String? = null,
    val checkoutQrPayload: String? = null
)

@JsonClass(generateAdapter = true)
data class SessionDetails(
    val id: String,
    val workplaceId: String,
    val attendanceDate: String,
    val status: SessionStatus = SessionStatus.CLOSED,
    val openedAt: String = "",
    val expiresAt: String = ""
)

enum class SessionStatus {
    @Json(name = "OPEN") OPEN,
    @Json(name = "CLOSED") CLOSED
}

enum class AttendanceStatus {
    @Json(name = "PRESENT") PRESENT,
    @Json(name = "PENDING") PENDING,
    @Json(name = "NOT_MARKED") NOT_MARKED,
    @Json(name = "ABSENT") ABSENT,
    @Json(name = "HALF_DAY") HALF_DAY,
    @Json(name = "LEAVE") LEAVE
}

@JsonClass(generateAdapter = true)
data class TodayRosterResponse(
    val attendanceDate: String,
    val counts: RosterCounts,
    val roster: List<RosterItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RosterCounts(
    val totalEmployees: Int = 0,
    val presentCount: Int = 0,
    val pendingCount: Int = 0,
    val notMarkedCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class RosterItem(
    val memberId: String,
    val name: String,
    val employeeCode: String,
    val avatarUrl: String? = null,
    val status: AttendanceStatus = AttendanceStatus.NOT_MARKED,
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val approvedAt: String? = null,
    val requestedAt: String? = null,
    val requestId: String? = null,
    val teamId: String? = null,
    val teamName: String? = null
)

@JsonClass(generateAdapter = true)
data class AttendanceRequestItem(
    val id: String,
    val attendanceDate: String,
    val requestedAt: String,
    val status: RequestStatus = RequestStatus.PENDING,
    val requestType: RequestType = RequestType.CHECK_IN,
    val employee: RequestEmployee,
    val verification: RequestVerification = RequestVerification(qrVerified = true, wifiVerified = true),
    val rejectionReason: String? = null,
    val reviewedAt: String? = null
)

enum class RequestStatus {
    @Json(name = "PENDING") PENDING,
    @Json(name = "APPROVED") APPROVED,
    @Json(name = "REJECTED") REJECTED
}

enum class RequestType {
    @Json(name = "CHECK_IN") CHECK_IN,
    @Json(name = "CHECK_OUT") CHECK_OUT
}

@JsonClass(generateAdapter = true)
data class RequestEmployee(
    val memberId: String,
    val name: String,
    val code: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class RequestVerification(
    val qrVerified: Boolean = false,
    val wifiVerified: Boolean = false,
    val locationVerified: Boolean? = null,
    val deviceSsid: String? = null,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class EmployeeItem(
    val id: String,
    val name: String,
    val employeeCode: String,
    val invitedEmail: String? = null,
    val avatarUrl: String? = null,
    val status: String = "ACTIVE",
    val teamId: String? = null,
    val teamName: String? = null,
    val hasPin: Boolean = false,
    val joinedAt: String? = null,
    val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class Team(
    val id: String,
    val workplaceId: String,
    val name: String,
    val teamCode: String? = null,
    val description: String? = null,
    val color: String = "#5B692D",
    val isDefault: Boolean = false,
    val memberCount: Int = 0,
    val members: List<TeamMemberSummary> = emptyList(),
    val createdAt: String? = null
) {
    val displayCode: String
        get() {
            if (!teamCode.isNullOrBlank() && teamCode.length in 7..8) return teamCode.uppercase()
            if (!teamCode.isNullOrBlank()) return teamCode.uppercase().take(8)
            val raw = id.replace("-", "").uppercase()
            return if (raw.length >= 6) "TM${raw.takeLast(6)}" else "TM${raw.padStart(6, '0')}"
        }
}

@JsonClass(generateAdapter = true)
data class TeamMemberSummary(
    val id: String,
    val name: String,
    val employeeCode: String? = null,
    val role: String = "EMPLOYEE",
    val status: String = "ACTIVE",
    val avatarUrl: String? = null,
    val email: String? = null
)

@JsonClass(generateAdapter = true)
data class AttendanceRecord(
    val id: String,
    val attendanceDate: String,
    val status: AttendanceStatus = AttendanceStatus.NOT_MARKED,
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val approvedAt: String? = null,
    val source: String? = null,
    val correctionReason: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val type: String = "ATTENDANCE",
    val timestamp: String,
    val read: Boolean = false
)
