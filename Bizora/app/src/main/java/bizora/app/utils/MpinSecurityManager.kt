package bizora.app.utils

import java.security.MessageDigest

object MpinSecurityManager {
    @Volatile
    private var inMemorySalt: String? = null

    @Volatile
    private var inMemoryHash: String? = null

    @Volatile
    private var leaseExpiresAt: Long = 0L

    fun hashMpin(salt: String, mpin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$mpin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Fast local-first verification (<0.1ms).
     * Checks in-memory lease first, then persistent verifier.
     * Returns true if match, false if mismatch, or null if no local verifier exists.
     */
    fun verifyLocal(mpin: String, persistentSalt: String?, persistentHash: String?): Boolean? {
        val now = System.currentTimeMillis()
        // 1. In-memory fast lease (<0.05ms)
        val memSalt = inMemorySalt
        val memHash = inMemoryHash
        if (memSalt != null && memHash != null && now < leaseExpiresAt) {
            val computed = hashMpin(memSalt, mpin)
            return computed == memHash
        }

        // 2. Persistent verifier (<0.2ms)
        if (!persistentSalt.isNullOrEmpty() && !persistentHash.isNullOrEmpty()) {
            val computed = hashMpin(persistentSalt, mpin)
            if (computed == persistentHash) {
                // Populate in-memory lease (5 minutes)
                setLocalMpin(persistentSalt, persistentHash)
                return true
            } else {
                return false
            }
        }

        return null
    }

    fun setLocalMpin(salt: String, hash: String) {
        inMemorySalt = salt
        inMemoryHash = hash
        leaseExpiresAt = System.currentTimeMillis() + 5 * 60 * 1000L
    }

    fun clear() {
        inMemorySalt = null
        inMemoryHash = null
        leaseExpiresAt = 0L
    }
}
