package bizora.app.data.remote

import bizora.app.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var tokenProvider: (() -> String?)? = null
    private var refreshTokenProvider: (() -> String?)? = null
    private var onTokensRefreshed: ((accessToken: String, refreshToken: String?) -> Unit)? = null
    private var onAuthFailed: (() -> Unit)? = null

    fun setTokenManager(
        tokenProvider: () -> String?,
        refreshTokenProvider: () -> String?,
        onTokensRefreshed: (accessToken: String, refreshToken: String?) -> Unit,
        onAuthFailed: () -> Unit
    ) {
        this.tokenProvider = tokenProvider
        this.refreshTokenProvider = refreshTokenProvider
        this.onTokensRefreshed = onTokensRefreshed
        this.onAuthFailed = onAuthFailed
    }

    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        val token = tokenProvider?.invoke()
        if (!token.isNullOrBlank() && original.header("Authorization") == null) {
            builder.header("Authorization", "Bearer $token")
        }

        builder.header("Accept", "application/json")
        builder.header("Content-Type", "application/json")

        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        redactHeader("Authorization")
        redactHeader("Cookie")
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val refreshHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val tokenAuthenticator = Authenticator { _, response ->
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        if (count >= 3) {
            return@Authenticator null
        }

        val requestPath = response.request.url.encodedPath
        if (requestPath.contains("/auth/refresh") ||
            requestPath.contains("/auth/google") ||
            requestPath.contains("/auth/pin-login")
        ) {
            return@Authenticator null
        }

        synchronized(this) {
            val currentToken = tokenProvider?.invoke()
            val failedAuthHeader = response.request.header("Authorization")
            val failedToken = failedAuthHeader?.removePrefix("Bearer ")?.trim()

            if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            android.util.Log.i("TokenAuthenticator", "HTTP 401 Unauthorized received for ${response.request.url}. Triggering automatic token refresh...")
            val refreshToken = refreshTokenProvider?.invoke()
            if (refreshToken.isNullOrBlank()) {
                android.util.Log.w("TokenAuthenticator", "No refresh token available, failing auth.")
                onAuthFailed?.invoke()
                return@Authenticator null
            }

            try {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val jsonPayload = org.json.JSONObject().apply {
                    put("refreshToken", refreshToken)
                }.toString()

                val refreshRequest = Request.Builder()
                    .url("${getBaseUrl()}auth/refresh")
                    .post(jsonPayload.toRequestBody(mediaType))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .build()

                val refreshResponse = refreshHttpClient.newCall(refreshRequest).execute()
                val responseBody = refreshResponse.body?.string()

                if (refreshResponse.isSuccessful && !responseBody.isNullOrBlank()) {
                    val jsonObj = org.json.JSONObject(responseBody)
                    if (jsonObj.optBoolean("success", false) && jsonObj.has("data")) {
                        val dataObj = jsonObj.getJSONObject("data")
                        val newAccessToken = dataObj.getString("accessToken")
                        val newRefreshToken = if (dataObj.has("refreshToken") && !dataObj.isNull("refreshToken")) {
                            dataObj.getString("refreshToken")
                        } else {
                            refreshToken
                        }

                        android.util.Log.i("TokenAuthenticator", "Token refresh SUCCESSFUL. Retrying request with new token.")
                        onTokensRefreshed?.invoke(newAccessToken, newRefreshToken)

                        return@Authenticator response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    }
                }

                android.util.Log.e("TokenAuthenticator", "Token refresh failed with code ${refreshResponse.code}")
                if (refreshResponse.code in 400..403) {
                    onAuthFailed?.invoke()
                }
            } catch (e: Exception) {
                android.util.Log.e("TokenAuthenticator", "Token refresh exception: ${e.message}", e)
            }

            null
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .authenticator(tokenAuthenticator)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    class WorkplaceRoleAdapter {
        @com.squareup.moshi.ToJson
        fun toJson(role: bizora.app.data.model.WorkplaceRole): String = role.name

        @com.squareup.moshi.FromJson
        fun fromJson(roleStr: String?): bizora.app.data.model.WorkplaceRole {
            return if (roleStr.equals("EMPLOYEE", ignoreCase = true)) {
                bizora.app.data.model.WorkplaceRole.EMPLOYEE
            } else {
                bizora.app.data.model.WorkplaceRole.EMPLOYER
            }
        }
    }

    class WorkplaceMemberItemAdapter {
        @com.squareup.moshi.FromJson
        fun fromJson(reader: com.squareup.moshi.JsonReader): WorkplaceMemberItem {
            var mongoId: String? = null
            var id: String? = null
            var workplaceId: String? = null
            var userId: String? = null
            var employeeCode: String? = null
            var name: String? = null
            var role = "EMPLOYEE"

            if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                reader.nextNull<Unit>()
                return WorkplaceMemberItem()
            }

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "_id" -> mongoId = reader.readStringOrId()
                    "id" -> id = reader.readStringOrId()
                    "workplaceId" -> workplaceId = reader.readStringOrId()
                    "userId" -> userId = reader.readStringOrId()
                    "employeeCode" -> employeeCode = reader.readStringOrId()
                    "name" -> name = reader.nextStringSafe()
                    "role" -> role = reader.nextStringSafe() ?: "EMPLOYEE"
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            return WorkplaceMemberItem(
                mongoId = mongoId,
                id = id,
                workplaceId = workplaceId,
                userId = userId,
                employeeCode = employeeCode,
                name = name,
                role = role
            )
        }

        @com.squareup.moshi.ToJson
        fun toJson(writer: com.squareup.moshi.JsonWriter, value: WorkplaceMemberItem?) {
            if (value == null) {
                writer.nullValue()
                return
            }
            writer.beginObject()
            if (value.mongoId != null) writer.name("_id").value(value.mongoId)
            if (value.id != null) writer.name("id").value(value.id)
            if (value.workplaceId != null) writer.name("workplaceId").value(value.workplaceId)
            if (value.userId != null) writer.name("userId").value(value.userId)
            if (value.employeeCode != null) writer.name("employeeCode").value(value.employeeCode)
            if (value.name != null) writer.name("name").value(value.name)
            writer.name("role").value(value.role)
            writer.endObject()
        }
    }

    class RemoteMembershipAdapter {
        @com.squareup.moshi.FromJson
        fun fromJson(reader: com.squareup.moshi.JsonReader): RemoteMembership {
            var id: String? = null
            var workplaceId = ""
            var workplaceName: String? = "Workplace"
            var workplaceCode: String? = null
            var adminName: String? = null
            var address: String? = null
            var timezone: String? = "Asia/Kolkata"
            var role = "EMPLOYEE"
            var employeeCode: String? = null
            var status: String? = "ACTIVE"
            var allowEmployeeViewHistory: Boolean? = true

            if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                reader.nextNull<Unit>()
                return RemoteMembership(workplaceId = "")
            }

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> id = reader.readStringOrId()
                    "_id" -> if (id == null) id = reader.readStringOrId() else reader.skipValue()
                    "workplaceId" -> {
                        when (reader.peek()) {
                            com.squareup.moshi.JsonReader.Token.STRING,
                            com.squareup.moshi.JsonReader.Token.NUMBER -> workplaceId = reader.nextStringSafe() ?: ""
                            com.squareup.moshi.JsonReader.Token.BEGIN_OBJECT -> {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    val n = reader.nextName()
                                    if ((n == "_id" || n == "id") && workplaceId.isEmpty() && (reader.peek() == com.squareup.moshi.JsonReader.Token.STRING || reader.peek() == com.squareup.moshi.JsonReader.Token.NUMBER)) {
                                        workplaceId = reader.nextStringSafe() ?: ""
                                    } else if (n == "name" && (workplaceName == null || workplaceName == "Workplace") && reader.peek() == com.squareup.moshi.JsonReader.Token.STRING) {
                                        workplaceName = reader.nextStringSafe()
                                    } else if (n == "workplaceCode" && workplaceCode == null && reader.peek() == com.squareup.moshi.JsonReader.Token.STRING) {
                                        workplaceCode = reader.nextStringSafe()
                                    } else if (n == "timezone" && reader.peek() == com.squareup.moshi.JsonReader.Token.STRING) {
                                        timezone = reader.nextStringSafe() ?: "Asia/Kolkata"
                                    } else if (n == "address" && address == null && reader.peek() == com.squareup.moshi.JsonReader.Token.STRING) {
                                        address = reader.nextStringSafe()
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                reader.endObject()
                            }
                            else -> reader.skipValue()
                        }
                    }
                    "workplaceName" -> workplaceName = reader.nextStringSafe() ?: workplaceName
                    "workplaceCode" -> workplaceCode = reader.nextStringSafe()
                    "adminName" -> adminName = reader.nextStringSafe()
                    "address" -> address = reader.nextStringSafe()
                    "timezone" -> timezone = reader.nextStringSafe() ?: timezone
                    "role" -> role = reader.nextStringSafe() ?: "EMPLOYEE"
                    "employeeCode" -> employeeCode = reader.nextStringSafe()
                    "status" -> status = reader.nextStringSafe() ?: status
                    "allowEmployeeViewHistory" -> {
                        allowEmployeeViewHistory = when (reader.peek()) {
                            com.squareup.moshi.JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                            com.squareup.moshi.JsonReader.Token.STRING -> reader.nextString().toBoolean()
                            else -> {
                                reader.skipValue()
                                true
                            }
                        }
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            return RemoteMembership(
                id = id,
                workplaceId = workplaceId,
                workplaceName = workplaceName,
                workplaceCode = workplaceCode,
                adminName = adminName,
                address = address,
                timezone = timezone,
                role = role,
                employeeCode = employeeCode,
                status = status,
                allowEmployeeViewHistory = allowEmployeeViewHistory
            )
        }

        @com.squareup.moshi.ToJson
        fun toJson(writer: com.squareup.moshi.JsonWriter, value: RemoteMembership?) {
            if (value == null) {
                writer.nullValue()
                return
            }
            writer.beginObject()
            if (value.id != null) writer.name("id").value(value.id)
            writer.name("workplaceId").value(value.workplaceId)
            writer.name("workplaceName").value(value.workplaceName)
            if (value.workplaceCode != null) writer.name("workplaceCode").value(value.workplaceCode)
            if (value.adminName != null) writer.name("adminName").value(value.adminName)
            if (value.address != null) writer.name("address").value(value.address)
            writer.name("timezone").value(value.timezone)
            writer.name("role").value(value.role)
            if (value.employeeCode != null) writer.name("employeeCode").value(value.employeeCode)
            if (value.status != null) writer.name("status").value(value.status)
            if (value.allowEmployeeViewHistory != null) writer.name("allowEmployeeViewHistory").value(value.allowEmployeeViewHistory)
            writer.endObject()
        }
    }

    private fun com.squareup.moshi.JsonReader.readStringOrId(): String? {
        return when (peek()) {
            com.squareup.moshi.JsonReader.Token.NULL -> nextNull()
            com.squareup.moshi.JsonReader.Token.STRING -> nextString()
            com.squareup.moshi.JsonReader.Token.NUMBER -> nextString()
            com.squareup.moshi.JsonReader.Token.BEGIN_OBJECT -> {
                var extracted: String? = null
                beginObject()
                while (hasNext()) {
                    val n = nextName()
                    if ((n == "_id" || n == "id") && extracted == null && (peek() == com.squareup.moshi.JsonReader.Token.STRING || peek() == com.squareup.moshi.JsonReader.Token.NUMBER)) {
                        extracted = nextString()
                    } else {
                        skipValue()
                    }
                }
                endObject()
                extracted
            }
            else -> {
                skipValue()
                null
            }
        }
    }

    private fun com.squareup.moshi.JsonReader.nextStringSafe(): String? {
        return when (peek()) {
            com.squareup.moshi.JsonReader.Token.NULL -> nextNull()
            com.squareup.moshi.JsonReader.Token.STRING -> nextString()
            com.squareup.moshi.JsonReader.Token.NUMBER -> nextString()
            com.squareup.moshi.JsonReader.Token.BOOLEAN -> nextBoolean().toString()
            else -> {
                skipValue()
                null
            }
        }
    }

    private val moshi = Moshi.Builder()
        .add(WorkplaceRoleAdapter())
        .add(WorkplaceMemberItemAdapter())
        .add(RemoteMembershipAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    fun getBaseUrl(): String {
        var url = BuildConfig.API_BASE_URL.trim()
        if (!url.endsWith("/")) {
            url += "/"
        }
        return url
    }

    val service: BizoraApiService by lazy {
        Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BizoraApiService::class.java)
    }
}
