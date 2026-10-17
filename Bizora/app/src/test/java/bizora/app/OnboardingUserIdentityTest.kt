package bizora.app

import bizora.app.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingUserIdentityTest {

    private fun resolveDisplayName(user: User?, cachedUser: User?): String {
        val directName = user?.name?.trim()?.takeIf { it.isNotBlank() }
        if (directName != null) return directName

        val cachedName = cachedUser?.name?.trim()?.takeIf { it.isNotBlank() }
        if (cachedName != null) return cachedName

        val email = user?.email?.trim()?.takeIf { it.isNotBlank() }
            ?: cachedUser?.email?.trim()?.takeIf { it.isNotBlank() }
        if (email != null) {
            val localPart = email.substringBefore("@")
            val formatted = localPart
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
            if (formatted.isNotBlank()) return formatted
        }

        return "User"
    }

    private fun resolveFirstName(displayName: String): String {
        return if (displayName.equals("User", ignoreCase = true)) {
            ""
        } else {
            displayName.split(" ").firstOrNull { it.isNotBlank() } ?: displayName
        }
    }

    private fun resolveGreeting(firstName: String): String {
        return if (firstName.isNotBlank()) "Welcome, $firstName" else "Welcome"
    }

    private fun resolveAvatarInitial(displayName: String): String {
        return (displayName.firstOrNull { it.isLetterOrDigit() } ?: 'U').uppercase()
    }

    @Test
    fun userWithFullName_showsFullNameAndFirstNameGreeting() {
        val user = User(id = "1", name = "Shivam Shankhdhar", email = "shivam@example.com")
        val displayName = resolveDisplayName(user, null)
        val firstName = resolveFirstName(displayName)

        assertEquals("Shivam Shankhdhar", displayName)
        assertEquals("Shivam", firstName)
        assertEquals("Welcome, Shivam", resolveGreeting(firstName))
        assertEquals("S", resolveAvatarInitial(displayName))
    }

    @Test
    fun userWithEmailOnly_formatsEmailHandleCleanly() {
        val user = User(id = "2", name = "", email = "shivam.shankhdhar@gmail.com")
        val displayName = resolveDisplayName(user, null)
        val firstName = resolveFirstName(displayName)

        assertEquals("Shivam Shankhdhar", displayName)
        assertEquals("Shivam", firstName)
        assertEquals("Welcome, Shivam", resolveGreeting(firstName))
        assertEquals("S", resolveAvatarInitial(displayName))
    }

    @Test
    fun nullUserWithCachedUser_resolvesFromCachedUser() {
        val cached = User(id = "3", name = "Demo Employer", email = "demo@bizora.app")
        val displayName = resolveDisplayName(null, cached)
        val firstName = resolveFirstName(displayName)

        assertEquals("Demo Employer", displayName)
        assertEquals("Demo", firstName)
        assertEquals("Welcome, Demo", resolveGreeting(firstName))
        assertEquals("D", resolveAvatarInitial(displayName))
    }

    @Test
    fun fallbackUser_neverProducesThereOrT() {
        val displayName = resolveDisplayName(null, null)
        val firstName = resolveFirstName(displayName)

        assertEquals("User", displayName)
        assertEquals("", firstName)
        assertEquals("Welcome", resolveGreeting(firstName))
        assertEquals("U", resolveAvatarInitial(displayName))
        org.junit.Assert.assertNotEquals("there", displayName.lowercase())
        org.junit.Assert.assertNotEquals("T", resolveAvatarInitial(displayName))
    }
}
