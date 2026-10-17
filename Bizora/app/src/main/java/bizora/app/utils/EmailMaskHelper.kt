package bizora.app.utils

/**
 * Masks an email address showing:
 * - 2 letters at the start
 * - exactly 3 asterisks (***) in between
 * - 1 letter at the end
 * - domain name (e.g., @gmail.com)
 *
 * Examples:
 * - "shivamshankhdhar@gmail.com" -> "sh***r@gmail.com"
 * - "er.shivam.shankhdhar@gmail.com" -> "er***r@gmail.com"
 * - "admin@gmail.com" -> "ad***n@gmail.com"
 * - "reviewer@bizora.app" -> "re***r@bizora.app"
 */
fun maskEmail(email: String?): String {
    if (email.isNullOrBlank()) return ""
    if (!email.contains("@")) return email

    val atIdx = email.indexOf('@')
    val rawLocal = email.substring(0, atIdx).trim()
    val domain = email.substring(atIdx + 1).trim()

    // If already properly masked with ***, return as is
    if (rawLocal.contains("***")) {
        return "$rawLocal@$domain"
    }

    val cleanLocal = rawLocal.replace(Regex("[*•]+"), "")
    val start = cleanLocal.take(2)
    val end = if (cleanLocal.length > 2) cleanLocal.takeLast(1) else ""

    return "$start***$end@$domain"
}
