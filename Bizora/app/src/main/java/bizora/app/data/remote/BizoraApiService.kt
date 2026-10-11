package bizora.app.data.remote

import bizora.app.data.model.*
import retrofit2.http.*

interface BizoraApiService {

    // Auth
    @POST("auth/google/exchange")
    suspend fun exchangeGoogleToken(
        @Body body: GoogleExchangeRequest
    ): ApiResponse<AuthSessionResponse>

    @POST("auth/employee-pin-login")
    suspend fun loginWithPin(
        @Body body: PinLoginRequest
    ): ApiResponse<AuthSessionResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body body: RefreshTokenRequest
    ): ApiResponse<TokenRefreshResponse>

    @GET("auth/me")
    suspend fun getMe(): ApiResponse<GetMeResponse>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Map<String, Any>>

    // MPIN & Security
    @GET("auth/mpin/status")
    suspend fun getMpinStatus(): ApiResponse<MpinStatusResponse>

    @POST("auth/mpin/verify")
    suspend fun verifyMpin(
        @Body body: MpinVerifyRequest
    ): ApiResponse<MpinVerifyResponse>

    @POST("auth/mpin/setup")
    suspend fun setupMpin(
        @Body body: MpinSetupRequest
    ): ApiResponse<MpinSetupResponse>

    @POST("auth/mpin/otp/request")
    suspend fun requestMpinOtp(
        @Body body: RequestMpinOtpRequest
    ): ApiResponse<RequestMpinOtpResponse>

    @POST("auth/mpin/otp/verify")
    suspend fun verifyMpinOtp(
        @Body body: VerifyMpinOtpRequest
    ): ApiResponse<VerifyMpinOtpResponse>

    @POST("auth/mpin/reset")
    suspend fun resetMpin(
        @Body body: ResetMpinRequest
    ): ApiResponse<MpinStatusResponse>

    @POST("auth/mpin/app-lock")
    suspend fun setAppLock(
        @Body body: AppLockToggleRequest
    ): ApiResponse<MpinStatusResponse>


    // Workplaces
    @GET("workplaces")
    suspend fun getWorkplaces(): ApiResponse<List<Workplace>>

    @POST("workplaces")
    suspend fun createWorkplace(
        @Body body: CreateWorkplaceRequest
    ): ApiResponse<CreateWorkplaceResponse>

    @POST("workplaces/join-preview")
    suspend fun previewJoin(
        @Body body: JoinPreviewRequest
    ): ApiResponse<JoinPreviewResponse>

    @POST("workplaces/join-requests")
    suspend fun submitJoinRequest(
        @Body body: SubmitJoinRequest
    ): ApiResponse<Map<String, Any>>

    @GET("workplaces/my-join-requests")
    suspend fun getMyJoinRequests(): ApiResponse<List<JoinRequestItem>>

    @POST("workplaces/my-join-requests/{requestId}/cancel")
    suspend fun cancelMyJoinRequest(
        @Path("requestId") requestId: String
    ): ApiResponse<Map<String, Any>>

    @GET("workplaces/{workplaceId}/join-requests")
    suspend fun getWorkplaceJoinRequests(
        @Path("workplaceId") workplaceId: String,
        @Query("status") status: String? = null
    ): ApiResponse<List<EmployerJoinRequestItem>>

    @POST("workplaces/{workplaceId}/join-requests/{requestId}/approve")
    suspend fun approveJoinRequest(
        @Path("workplaceId") workplaceId: String,
        @Path("requestId") requestId: String,
        @Body body: Map<String, String> = emptyMap()
    ): ApiResponse<Map<String, Any>>

    @POST("workplaces/{workplaceId}/join-requests/{requestId}/reject")
    suspend fun rejectJoinRequest(
        @Path("workplaceId") workplaceId: String,
        @Path("requestId") requestId: String,
        @Body body: Map<String, String> = emptyMap()
    ): ApiResponse<Map<String, Any>>

    @GET("workplaces/{workplaceId}")
    suspend fun getWorkplace(
        @Path("workplaceId") workplaceId: String
    ): ApiResponse<Workplace>

    @PATCH("workplaces/{workplaceId}")
    suspend fun updateWorkplace(
        @Path("workplaceId") workplaceId: String,
        @Body body: @JvmSuppressWildcards Map<String, Any>
    ): ApiResponse<Workplace>

    @GET("workplaces/{workplaceId}/join-qr")
    suspend fun getWorkplaceJoinQr(
        @Path("workplaceId") workplaceId: String,
        @Query("teamId") teamId: String? = null
    ): ApiResponse<Map<String, Any>>

    @POST("workplaces/{workplaceId}/join-qr/rotate")
    suspend fun rotateWorkplaceJoinQr(
        @Path("workplaceId") workplaceId: String,
        @Body body: Map<String, String> = emptyMap()
    ): ApiResponse<Map<String, Any>>

    // Attendance Sessions
    @POST("workplaces/{workplaceId}/attendance-sessions/open")
    suspend fun openSession(
        @Path("workplaceId") workplaceId: String
    ): ApiResponse<AttendanceSession>

    @GET("workplaces/{workplaceId}/attendance-sessions/today")
    suspend fun getTodaySession(
        @Path("workplaceId") workplaceId: String
    ): ApiResponse<AttendanceSession?>

    @POST("workplaces/{workplaceId}/attendance-sessions/close")
    suspend fun closeSession(
        @Path("workplaceId") workplaceId: String
    ): ApiResponse<Map<String, Any>>

    // Attendance Requests
    @GET("workplaces/{workplaceId}/attendance/requests")
    suspend fun getRequests(
        @Path("workplaceId") workplaceId: String,
        @Query("status") status: String? = null
    ): ApiResponse<List<AttendanceRequestItem>>

    @POST("workplaces/{workplaceId}/attendance/requests/{requestId}/approve")
    suspend fun approveRequest(
        @Path("workplaceId") workplaceId: String,
        @Path("requestId") requestId: String
    ): ApiResponse<Map<String, Any>>

    @POST("workplaces/{workplaceId}/attendance/requests/{requestId}/reject")
    suspend fun rejectRequest(
        @Path("workplaceId") workplaceId: String,
        @Path("requestId") requestId: String,
        @Body body: RejectAttendanceRequest
    ): ApiResponse<Map<String, Any>>

    @POST("attendance/requests")
    suspend fun submitScanRequest(
        @Body body: ScanAttendanceRequest
    ): ApiResponse<Map<String, Any>>

    @GET("workplaces/{workplaceId}/attendance/requests/my-today")
    suspend fun getMyTodayRequest(
        @Path("workplaceId") workplaceId: String
    ): ApiResponse<AttendanceRequestItem?>

    // Roster & History
    @GET("workplaces/{workplaceId}/attendance/today")
    suspend fun getTodayRoster(
        @Path("workplaceId") workplaceId: String,
        @Query("date") date: String? = null
    ): ApiResponse<TodayRosterResponse>

    @GET("workplaces/{workplaceId}/attendance/history")
    suspend fun getMyHistory(
        @Path("workplaceId") workplaceId: String,
        @Query("month") month: String? = null
    ): ApiResponse<List<AttendanceRecord>>

    @POST("workplaces/{workplaceId}/attendance/manual")
    suspend fun markManualAttendance(
        @Path("workplaceId") workplaceId: String,
        @Body body: ManualAttendanceRequest
    ): ApiResponse<Map<String, Any>>

    // Employees
    @GET("workplaces/{workplaceId}/employees")
    suspend fun getEmployees(
        @Path("workplaceId") workplaceId: String
    ): ApiResponse<List<EmployeeItem>>

    @POST("workplaces/{workplaceId}/employees")
    suspend fun addEmployee(
        @Path("workplaceId") workplaceId: String,
        @Body body: AddEmployeeRequest
    ): ApiResponse<Map<String, Any>>

    @PATCH("workplaces/{workplaceId}/employees/{memberId}")
    suspend fun updateEmployee(
        @Path("workplaceId") workplaceId: String,
        @Path("memberId") memberId: String,
        @Body body: Map<String, Any>
    ): ApiResponse<Map<String, Any>>

    @POST("workplaces/{workplaceId}/employees/{memberId}/reset-pin")
    suspend fun resetPin(
        @Path("workplaceId") workplaceId: String,
        @Path("memberId") memberId: String,
        @Body body: ResetPinRequest
    ): ApiResponse<Map<String, Any>>

    @DELETE("workplaces/{workplaceId}/employees/{memberId}")
    suspend fun deleteEmployee(
        @Path("workplaceId") workplaceId: String,
        @Path("memberId") memberId: String
    ): ApiResponse<Map<String, Any>>

    // Notifications
    @GET("notifications")
    suspend fun getNotifications(): ApiResponse<List<NotificationItem>>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(
        @Path("id") id: String
    ): ApiResponse<Map<String, Any>>

    // Teams
    @GET("workplaces/{workplaceId}/teams")
    suspend fun getTeams(
        @Path("workplaceId") workplaceId: String
    ): ApiResponse<List<Team>>

    @POST("workplaces/{workplaceId}/teams")
    suspend fun createTeam(
        @Path("workplaceId") workplaceId: String,
        @Body body: Map<String, String>
    ): ApiResponse<Team>
}
