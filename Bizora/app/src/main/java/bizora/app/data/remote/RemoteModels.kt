package bizora.app.data.remote

import bizora.app.data.model.Team
import bizora.app.data.model.User
import bizora.app.data.model.Workplace
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiErrorResponse? = null
)

@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    val code: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class PinLoginRequest(
    val employeeCode: String,
    val pin: String,
    val workplaceId: String? = null,
    val expoPushToken: String? = null
)

@JsonClass(generateAdapter = true)
data class DevMockProfile(
    val name: String,
    val email: String,
    val googleSub: String
)

@JsonClass(generateAdapter = true)
data class GoogleExchangeRequest(
    val idToken: String,
    val devMockProfile: DevMockProfile? = null,
    val expoPushToken: String? = null
)

@JsonClass(generateAdapter = true)
data class GetMeResponse(
    val user: User,
    val memberships: List<RemoteMembership> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MpinStatusResponse(
    val hasMpin: Boolean = false,
    val appLockEnabled: Boolean? = null,
    val biometricEnabled: Boolean = false,
    val attemptsRemaining: Int? = null,
    val lockedUntil: String? = null
)

@JsonClass(generateAdapter = true)
data class AppLockToggleRequest(
    val enabled: Boolean
)


@JsonClass(generateAdapter = true)
data class MpinVerifyRequest(
    val mpin: String
)

@JsonClass(generateAdapter = true)
data class MpinVerifyResponse(
    val verified: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MpinSetupRequest(
    val mpin: String,
    val enableBiometric: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MpinSetupResponse(
    val hasMpin: Boolean = false,
    val biometricEnabled: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RequestMpinOtpRequest(
    val purpose: String = "RESET_MPIN"
)

@JsonClass(generateAdapter = true)
data class RequestMpinOtpResponse(
    val maskedEmail: String? = null,
    val expiresInSeconds: Int? = null
)

@JsonClass(generateAdapter = true)
data class VerifyMpinOtpRequest(
    val otp: String,
    val purpose: String = "RESET_MPIN"
)

@JsonClass(generateAdapter = true)
data class VerifyMpinOtpResponse(
    val resetToken: String? = null
)

@JsonClass(generateAdapter = true)
data class ResetMpinRequest(
    val mpin: String,
    val enableBiometric: Boolean? = null,
    val resetToken: String? = null
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class AuthSessionResponse(
    val user: User? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val workplace: Workplace? = null,
    val member: WorkplaceMemberItem? = null,
    val memberships: List<RemoteMembership>? = null,
    val pendingApproval: Boolean? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteMembership(
    val id: String? = null,
    val workplaceId: String,
    val workplaceName: String? = "Workplace",
    val workplaceCode: String? = null,
    val adminName: String? = null,
    val address: String? = null,
    val timezone: String? = "Asia/Kolkata",
    val role: String = "EMPLOYEE",
    val employeeCode: String? = null,
    val status: String? = "ACTIVE",
    val allowEmployeeViewHistory: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkplaceMemberItem(
    @Json(name = "_id") val mongoId: String? = null,
    val id: String? = null,
    val workplaceId: String? = null,
    val userId: String? = null,
    val employeeCode: String? = null,
    val name: String? = null,
    val role: String = "EMPLOYEE"
)


@JsonClass(generateAdapter = true)
data class ScanAttendanceRequest(
    val qrToken: String? = null,
    val workplaceId: String? = null,
    val requestType: String = "CHECK_IN",
    val source: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val note: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateWorkplaceSettingsRequest(
    val attendanceSettings: AttendanceSettingsUpdatePayload? = null
)

@JsonClass(generateAdapter = true)
data class AttendanceSettingsUpdatePayload(
    val requireWifi: Boolean? = null,
    val autoCloseHour: Int? = null,
    val allowEmployeeViewHistory: Boolean? = null,
    val requireGeofence: Boolean? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geofenceRadius: Double? = null
)

@JsonClass(generateAdapter = true)
data class RejectAttendanceRequest(
    val reason: String
)

@JsonClass(generateAdapter = true)
data class ManualAttendanceRequest(
    val employeeMemberId: String,
    val attendanceDate: String,
    val status: String,
    val correctionReason: String
)

@JsonClass(generateAdapter = true)
data class AddEmployeeRequest(
    val name: String,
    val email: String? = null,
    val pin: String? = null,
    val teamId: String? = null,
    val teamCode: String? = null
)

@JsonClass(generateAdapter = true)
data class ResetPinRequest(
    val newPin: String
)

@JsonClass(generateAdapter = true)
data class CreateWorkplaceRequest(
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String = "Asia/Kolkata"
)

@JsonClass(generateAdapter = true)
data class CreateWorkplaceResponse(
    val workplace: WorkplacePayload,
    val member: WorkplaceMemberItem? = null
)

@JsonClass(generateAdapter = true)
data class WorkplacePayload(
    @Json(name = "_id") val mongoId: String? = null,
    val id: String? = null,
    val name: String,
    val workplaceCode: String? = null,
    val timezone: String? = "Asia/Kolkata",
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val wifiSsid: String? = null,
    val status: String? = "ACTIVE"
) {
    val realId: String get() = id ?: mongoId ?: ""
}

@JsonClass(generateAdapter = true)
data class JoinPreviewRequest(
    val token: String
)

@JsonClass(generateAdapter = true)
data class PreviewTeamInfo(
    val id: String? = null,
    val name: String? = null,
    val teamCode: String? = null,
    val color: String? = null
)

@JsonClass(generateAdapter = true)
data class JoinPreviewResponse(
    val workplaceId: String,
    val workplaceName: String,
    val workplaceCode: String? = null,
    val targetTeam: PreviewTeamInfo? = null,
    val address: String? = null,
    val description: String? = null,
    val timezone: String? = null,
    val ownerName: String? = null,
    val ownerAvatarUrl: String? = null,
    val activeMembersCount: Int? = 0,
    val memberCount: Int? = 0,
    val alreadyMember: Boolean = false,
    val isOwner: Boolean = false,
    val isEmployer: Boolean = false,
    val isInvited: Boolean = false,
    val invitedRole: String? = null,
    val qrToken: String? = null
)

@JsonClass(generateAdapter = true)
data class SubmitJoinRequest(
    val token: String,
    val note: String? = null
)

@JsonClass(generateAdapter = true)
data class JoinRequestItem(
    @Json(name = "_id") val mongoId: String? = null,
    val id: String? = null,
    val workplaceId: String? = null,
    val workplaceName: String? = "Workplace",
    val address: String? = null,
    val status: String = "PENDING",
    val requestedAt: String? = null,
    val rejectionReason: String? = null
) {
    val realId: String get() = id ?: mongoId ?: ""
}

@JsonClass(generateAdapter = true)
data class EmployerJoinRequestItem(
    @Json(name = "_id") val mongoId: String? = null,
    val id: String? = null,
    val userId: String? = null,
    val name: String? = "New Candidate",
    val email: String? = null,
    val employeeCode: String? = null,
    val avatarUrl: String? = null,
    val note: String? = null,
    val status: String = "PENDING",
    val teamId: String? = null,
    val teamName: String? = null,
    val teamCode: String? = null,
    val requestedAt: String? = null,
    val reviewedAt: String? = null,
    val rejectionReason: String? = null
) {
    val realId: String get() = id ?: mongoId ?: ""
}
