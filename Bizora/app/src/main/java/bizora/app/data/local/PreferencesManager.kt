package bizora.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import bizora.app.data.model.User
import bizora.app.data.model.WorkplaceRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "bizora_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_ACTIVE_WORKPLACE_ID = stringPreferencesKey("active_workplace_id")
        private val KEY_ACTIVE_WORKPLACE_NAME = stringPreferencesKey("active_workplace_name")
        private val KEY_ACTIVE_WORKPLACE_CODE = stringPreferencesKey("active_workplace_code")
        private val KEY_ACTIVE_WORKPLACE_TIMEZONE = stringPreferencesKey("active_workplace_timezone")
        private val KEY_ACTIVE_WORKPLACE_ADDRESS = stringPreferencesKey("active_workplace_address")
        private val KEY_ACTIVE_ROLE = stringPreferencesKey("active_role")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_HAS_MPIN = booleanPreferencesKey("has_mpin")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_LOCAL_MPIN_SALT = stringPreferencesKey("local_mpin_salt")
        private val KEY_LOCAL_MPIN_HASH = stringPreferencesKey("local_mpin_hash")
        private val KEY_MPIN_HASH = stringPreferencesKey("mpin_hash")
        private val KEY_HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        private val KEY_WORKPLACE_CONFIRMED = booleanPreferencesKey("workplace_confirmed")
        private val KEY_PENDING_INVITE_CODE = stringPreferencesKey("pending_invite_code")
    }

    @Volatile
    private var inMemoryUser: User? = null

    @Volatile
    private var inMemoryAccessToken: String? = null

    @Volatile
    private var inMemoryRefreshToken: String? = null

    @Volatile
    private var inMemoryHasMpin: Boolean = false

    @Volatile
    private var inMemoryAppLockEnabled: Boolean = false

    @Volatile
    private var inMemoryBiometricEnabled: Boolean = false

    @Volatile
    private var inMemorySalt: String? = null

    @Volatile
    private var inMemoryHash: String? = null

    @Volatile
    private var inMemoryPendingInviteCode: String? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                listOf(KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN, KEY_LOCAL_MPIN_SALT, KEY_LOCAL_MPIN_HASH, KEY_MPIN_HASH).forEach { key ->
                    prefs[key]?.takeIf(SecurePreferenceValue::needsMigration)?.let { prefs[key] = SecurePreferenceValue.encrypt(it) }
                }
            }
            context.dataStore.data.collect { prefs ->
                inMemoryAccessToken = SecurePreferenceValue.decrypt(prefs[KEY_ACCESS_TOKEN])
                inMemorySalt = SecurePreferenceValue.decrypt(prefs[KEY_LOCAL_MPIN_SALT])
                inMemoryHash = SecurePreferenceValue.decrypt(prefs[KEY_LOCAL_MPIN_HASH])
                val localHasHash = !inMemoryHash.isNullOrBlank()
                val storedHasMpin = prefs[KEY_HAS_MPIN] == true
                val storedLockEnabled = prefs[KEY_APP_LOCK_ENABLED] == true
                inMemoryHasMpin = storedHasMpin || storedLockEnabled || localHasHash
                inMemoryAppLockEnabled = storedLockEnabled || storedHasMpin || localHasHash
                inMemoryBiometricEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: false
                inMemoryPendingInviteCode = prefs[KEY_PENDING_INVITE_CODE]
                val uId = prefs[KEY_USER_ID]
                val uName = prefs[KEY_USER_NAME]
                val uEmail = prefs[KEY_USER_EMAIL]
                if (!uId.isNullOrBlank()) {
                    val resolvedName = uName?.takeIf { it.isNotBlank() }
                        ?: uEmail?.substringBefore("@")?.takeIf { it.isNotBlank() }
                        ?: "User"
                    inMemoryUser = User(id = uId, name = resolvedName, email = uEmail)
                } else {
                    inMemoryUser = null
                }
            }
        }
    }

    fun getUser(): User? {
        val mem = inMemoryUser
        if (mem != null) return mem
        return try {
            runBlocking(Dispatchers.IO) {
                val prefs = context.dataStore.data.firstOrNull() ?: return@runBlocking null
                val id = prefs[KEY_USER_ID] ?: return@runBlocking null
                val name = prefs[KEY_USER_NAME]
                val email = prefs[KEY_USER_EMAIL]
                val resolvedName = name?.takeIf { it.isNotBlank() }
                    ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                    ?: "User"
                User(id = id, name = resolvedName, email = email)
            }?.also { inMemoryUser = it }
        } catch (_: Exception) {
            mem
        }
    }

    fun getAccessToken(): String? {
        val mem = inMemoryAccessToken
        if (!mem.isNullOrBlank()) return mem
        return try {
            runBlocking(Dispatchers.IO) {
                SecurePreferenceValue.decrypt(context.dataStore.data.firstOrNull()?.get(KEY_ACCESS_TOKEN))
            }?.also { inMemoryAccessToken = it }
        } catch (_: Exception) {
            mem
        }
    }

    fun getRefreshToken(): String? {
        val mem = inMemoryRefreshToken
        if (!mem.isNullOrBlank()) return mem
        return try {
            runBlocking(Dispatchers.IO) {
                SecurePreferenceValue.decrypt(context.dataStore.data.firstOrNull()?.get(KEY_REFRESH_TOKEN))
            }?.also { inMemoryRefreshToken = it }
        } catch (_: Exception) {
            mem
        }
    }
    fun getLocalMpinSalt(): String? = inMemorySalt
    fun getLocalMpinHash(): String? = inMemoryHash
    fun isAppLockEnabled(): Boolean = inMemoryAppLockEnabled || inMemoryHasMpin || !inMemoryHash.isNullOrBlank()
    fun hasMpin(): Boolean = inMemoryHasMpin || inMemoryAppLockEnabled || !inMemoryHash.isNullOrBlank()
    fun isBiometricEnabled(): Boolean = inMemoryBiometricEnabled
    fun setMemoryAccessToken(token: String?) {
        inMemoryAccessToken = token
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        val token = SecurePreferenceValue.decrypt(prefs[KEY_ACCESS_TOKEN])
        inMemoryAccessToken = token
        token
    }

    val userFlow: Flow<User?> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_USER_ID]
        val name = prefs[KEY_USER_NAME]
        val email = prefs[KEY_USER_EMAIL]
        if (!id.isNullOrBlank()) {
            val resolvedName = name?.takeIf { it.isNotBlank() }
                ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: "User"
            val user = User(id, resolvedName, email)
            inMemoryUser = user
            user
        } else {
            inMemoryUser = null
            null
        }
    }

    val activeWorkplaceIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_ACTIVE_WORKPLACE_ID] }
    val activeRoleFlow: Flow<WorkplaceRole> = context.dataStore.data.map { prefs ->
        if (prefs[KEY_ACTIVE_ROLE] == "EMPLOYEE") WorkplaceRole.EMPLOYEE else WorkplaceRole.EMPLOYER
    }

    val savedWorkplaceFlow: Flow<bizora.app.data.model.Workplace?> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_ACTIVE_WORKPLACE_ID] ?: return@map null
        val name = prefs[KEY_ACTIVE_WORKPLACE_NAME] ?: return@map null
        val role = if (prefs[KEY_ACTIVE_ROLE] == "EMPLOYEE") WorkplaceRole.EMPLOYEE else WorkplaceRole.EMPLOYER
        bizora.app.data.model.Workplace(
            id = id,
            name = name,
            role = role,
            employeeCode = prefs[KEY_ACTIVE_WORKPLACE_CODE],
            timezone = prefs[KEY_ACTIVE_WORKPLACE_TIMEZONE] ?: "Asia/Kolkata",
            address = prefs[KEY_ACTIVE_WORKPLACE_ADDRESS]
        )
    }

    val startupStateFlow: Flow<AppStartupState> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_ACTIVE_WORKPLACE_ID]
        val name = prefs[KEY_ACTIVE_WORKPLACE_NAME]
        val role = if (prefs[KEY_ACTIVE_ROLE] == "EMPLOYEE") WorkplaceRole.EMPLOYEE else WorkplaceRole.EMPLOYER
        val savedWp = if (id != null && name != null) {
            bizora.app.data.model.Workplace(
                id = id,
                name = name,
                role = role,
                employeeCode = prefs[KEY_ACTIVE_WORKPLACE_CODE],
                timezone = prefs[KEY_ACTIVE_WORKPLACE_TIMEZONE] ?: "Asia/Kolkata",
                address = prefs[KEY_ACTIVE_WORKPLACE_ADDRESS]
            )
        } else null

        val salt = prefs[KEY_LOCAL_MPIN_SALT]
        val hash = prefs[KEY_LOCAL_MPIN_HASH]
        val hasLocalHash = !SecurePreferenceValue.decrypt(hash).isNullOrBlank()
        val storedHasMpin = prefs[KEY_HAS_MPIN] == true
        val storedLockEnabled = prefs[KEY_APP_LOCK_ENABLED] == true
        val hasMpin = storedHasMpin || storedLockEnabled || hasLocalHash
        val appLockEnabled = storedLockEnabled || storedHasMpin || hasLocalHash
        val biometricEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: false
        val isConfirmed = prefs[KEY_WORKPLACE_CONFIRMED] ?: (savedWp != null)

        AppStartupState(
            isReady = true,
            hasSeenWelcome = prefs[KEY_HAS_SEEN_WELCOME] ?: false,
            accessToken = SecurePreferenceValue.decrypt(prefs[KEY_ACCESS_TOKEN]),
            activeRole = role,
            savedWorkplace = savedWp,
            isWorkplaceConfirmed = isConfirmed,
            appLockEnabled = appLockEnabled,
            hasMpin = hasMpin,
            biometricEnabled = biometricEnabled,
            localMpinSalt = SecurePreferenceValue.decrypt(salt),
            localMpinHash = SecurePreferenceValue.decrypt(hash)
        )
    }

    val darkThemeFlow: Flow<Boolean?> = kotlinx.coroutines.flow.flowOf(false)
    val appLockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_APP_LOCK_ENABLED] ?: (it[KEY_HAS_MPIN] ?: false) }
    val mpinHashFlow: Flow<String?> = context.dataStore.data.map { SecurePreferenceValue.decrypt(it[KEY_MPIN_HASH]) }
    val hasSeenWelcomeFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAS_SEEN_WELCOME] ?: false }
    val isReadyFlow: Flow<Boolean> = context.dataStore.data.map { true }

    suspend fun setHasSeenWelcome(seen: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_SEEN_WELCOME] = seen
        }
    }

    suspend fun saveAuth(token: String, refreshToken: String, user: User) {
        inMemoryAccessToken = token
        inMemoryRefreshToken = refreshToken
        inMemoryUser = user
        val userHasLock = user.hasMpin || user.appLockEnabled == true
        inMemoryHasMpin = userHasLock
        inMemoryAppLockEnabled = userHasLock
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = SecurePreferenceValue.encrypt(token)
            prefs[KEY_REFRESH_TOKEN] = SecurePreferenceValue.encrypt(refreshToken)
            prefs[KEY_USER_ID] = user.id
            prefs[KEY_USER_NAME] = user.name
            if (userHasLock) {
                prefs[KEY_HAS_MPIN] = true
                prefs[KEY_APP_LOCK_ENABLED] = true
            } else {
                prefs.remove(KEY_HAS_MPIN)
                prefs.remove(KEY_APP_LOCK_ENABLED)
                prefs.remove(KEY_LOCAL_MPIN_HASH)
                prefs.remove(KEY_LOCAL_MPIN_SALT)
                prefs.remove(KEY_MPIN_HASH)
            }
            if (user.email != null) {
                prefs[KEY_USER_EMAIL] = user.email
            } else {
                prefs.remove(KEY_USER_EMAIL)
            }
        }
    }

    fun isWorkplaceConfirmed(): Boolean {
        return try {
            runBlocking(Dispatchers.IO) {
                context.dataStore.data.firstOrNull()?.get(KEY_WORKPLACE_CONFIRMED) == true
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateTokens(token: String, refreshToken: String? = null) {
        inMemoryAccessToken = token
        if (!refreshToken.isNullOrBlank()) {
            inMemoryRefreshToken = refreshToken
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = SecurePreferenceValue.encrypt(token)
            if (!refreshToken.isNullOrBlank()) {
                prefs[KEY_REFRESH_TOKEN] = SecurePreferenceValue.encrypt(refreshToken)
            }
        }
    }

    fun updateTokensSync(token: String, refreshToken: String? = null) {
        inMemoryAccessToken = token
        if (!refreshToken.isNullOrBlank()) {
            inMemoryRefreshToken = refreshToken
        }
        try {
            runBlocking(Dispatchers.IO) {
                context.dataStore.edit { prefs ->
                    prefs[KEY_ACCESS_TOKEN] = SecurePreferenceValue.encrypt(token)
                    if (!refreshToken.isNullOrBlank()) {
                        prefs[KEY_REFRESH_TOKEN] = SecurePreferenceValue.encrypt(refreshToken)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PreferencesManager", "Failed to synchronously update tokens: ${e.message}")
        }
    }

    suspend fun setActiveWorkplace(workplace: bizora.app.data.model.Workplace) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_WORKPLACE_ID] = workplace.id
            prefs[KEY_ACTIVE_WORKPLACE_NAME] = workplace.name
            prefs[KEY_ACTIVE_ROLE] = workplace.role.name
            if (workplace.employeeCode != null) {
                prefs[KEY_ACTIVE_WORKPLACE_CODE] = workplace.employeeCode
            } else {
                prefs.remove(KEY_ACTIVE_WORKPLACE_CODE)
            }
            prefs[KEY_ACTIVE_WORKPLACE_TIMEZONE] = workplace.timezone
            if (workplace.address != null) {
                prefs[KEY_ACTIVE_WORKPLACE_ADDRESS] = workplace.address
            }
            prefs[KEY_WORKPLACE_CONFIRMED] = true
        }
    }

    suspend fun setActiveWorkplace(workplaceId: String, role: WorkplaceRole) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_WORKPLACE_ID] = workplaceId
            prefs[KEY_ACTIVE_ROLE] = role.name
            prefs[KEY_WORKPLACE_CONFIRMED] = true
        }
    }

    suspend fun setWorkplaceConfirmed(confirmed: Boolean) {
        context.dataStore.edit { prefs ->
            if (confirmed) {
                prefs[KEY_WORKPLACE_CONFIRMED] = true
            } else {
                prefs.remove(KEY_WORKPLACE_CONFIRMED)
            }
        }
    }

    suspend fun clearActiveWorkplace() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACTIVE_WORKPLACE_ID)
            prefs.remove(KEY_ACTIVE_WORKPLACE_NAME)
            prefs.remove(KEY_ACTIVE_WORKPLACE_CODE)
            prefs.remove(KEY_ACTIVE_WORKPLACE_TIMEZONE)
            prefs.remove(KEY_ACTIVE_WORKPLACE_ADDRESS)
            prefs.remove(KEY_ACTIVE_ROLE)
            prefs.remove(KEY_WORKPLACE_CONFIRMED)
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_DARK_THEME)
        }
    }

    suspend fun setAppLock(enabled: Boolean, mpin: String? = null) {
        inMemoryAppLockEnabled = enabled
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
            if (mpin != null) {
                prefs[KEY_MPIN_HASH] = SecurePreferenceValue.encrypt(mpin)
            }
        }
    }

    suspend fun setAppLockConfig(hasMpin: Boolean, appLockEnabled: Boolean, biometricEnabled: Boolean? = null) {
        inMemoryHasMpin = hasMpin
        inMemoryAppLockEnabled = appLockEnabled
        if (biometricEnabled != null) inMemoryBiometricEnabled = biometricEnabled
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_MPIN] = hasMpin
            prefs[KEY_APP_LOCK_ENABLED] = appLockEnabled
            if (biometricEnabled != null) {
                prefs[KEY_BIOMETRIC_ENABLED] = biometricEnabled
            }
        }
    }

    suspend fun saveLocalMpinVerifier(salt: String, hash: String) {
        inMemorySalt = salt
        inMemoryHash = hash
        context.dataStore.edit { prefs ->
            prefs[KEY_LOCAL_MPIN_SALT] = SecurePreferenceValue.encrypt(salt)
            prefs[KEY_LOCAL_MPIN_HASH] = SecurePreferenceValue.encrypt(hash)
        }
    }

    suspend fun clearLocalMpinVerifier() {
        inMemorySalt = null
        inMemoryHash = null
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LOCAL_MPIN_SALT)
            prefs.remove(KEY_LOCAL_MPIN_HASH)
        }
    }

    suspend fun clearAuth(clearDeviceLock: Boolean = false) {
        inMemoryAccessToken = null
        inMemoryRefreshToken = null
        inMemoryUser = null
        if (clearDeviceLock) {
            inMemoryHasMpin = false
            inMemoryAppLockEnabled = false
            inMemoryBiometricEnabled = false
            inMemorySalt = null
            inMemoryHash = null
        }
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_ACTIVE_WORKPLACE_ID)
            prefs.remove(KEY_ACTIVE_WORKPLACE_NAME)
            prefs.remove(KEY_ACTIVE_WORKPLACE_CODE)
            prefs.remove(KEY_ACTIVE_WORKPLACE_TIMEZONE)
            prefs.remove(KEY_ACTIVE_WORKPLACE_ADDRESS)
            prefs.remove(KEY_ACTIVE_ROLE)
            prefs.remove(KEY_WORKPLACE_CONFIRMED)
            if (clearDeviceLock) {
                prefs.remove(KEY_APP_LOCK_ENABLED)
                prefs.remove(KEY_HAS_MPIN)
                prefs.remove(KEY_BIOMETRIC_ENABLED)
                prefs.remove(KEY_LOCAL_MPIN_SALT)
                prefs.remove(KEY_LOCAL_MPIN_HASH)
                prefs.remove(KEY_MPIN_HASH)
            }
        }
    }

    fun getPendingInviteCode(): String? = inMemoryPendingInviteCode

    suspend fun setPendingInviteCode(code: String?) {
        inMemoryPendingInviteCode = code
        context.dataStore.edit { prefs ->
            if (!code.isNullOrBlank()) {
                prefs[KEY_PENDING_INVITE_CODE] = code.trim()
            } else {
                prefs.remove(KEY_PENDING_INVITE_CODE)
            }
        }
    }
}

data class AppStartupState(
    val isReady: Boolean = false,
    val hasSeenWelcome: Boolean = false,
    val accessToken: String? = null,
    val activeRole: WorkplaceRole = WorkplaceRole.EMPLOYER,
    val savedWorkplace: bizora.app.data.model.Workplace? = null,
    val isWorkplaceConfirmed: Boolean = false,
    val appLockEnabled: Boolean = false,
    val hasMpin: Boolean = false,
    val biometricEnabled: Boolean = false,
    val localMpinSalt: String? = null,
    val localMpinHash: String? = null
)
