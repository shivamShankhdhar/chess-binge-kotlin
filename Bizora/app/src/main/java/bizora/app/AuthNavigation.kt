package bizora.app

import bizora.app.data.model.Workplace
import bizora.app.data.model.WorkplaceRole

internal fun initialAuthDestination(hasSeenWelcome: Boolean, accessToken: String?, hasMpin: Boolean): String =
    when {
        accessToken.isNullOrBlank() && !hasSeenWelcome -> "welcome"
        accessToken.isNullOrBlank() -> "sign_in"
        hasMpin -> "mpin_unlock"
        else -> "mpin_setup"
    }

internal fun postSignInDestination(hasMpin: Boolean): String =
    if (hasMpin) "mpin_unlock" else "mpin_setup"

internal fun postMpinDestination(workplaces: List<Workplace>): String {
    if (workplaces.isEmpty()) return "onboarding"
    return if (workplaces.any { it.role == WorkplaceRole.EMPLOYER }) "workplace_select" else "employee_dashboard"
}

internal fun resolvePostMpinDestination(
    workplaces: List<Workplace>,
    activeWorkplace: Workplace? = null,
    isWorkplaceConfirmed: Boolean = false
): String {
    if (isWorkplaceConfirmed && activeWorkplace != null) {
        return if (activeWorkplace.role == WorkplaceRole.EMPLOYER) "employer_dashboard" else "employee_dashboard"
    }
    if (workplaces.isEmpty() && activeWorkplace == null) return "onboarding"
    val list = workplaces.ifEmpty { listOfNotNull(activeWorkplace) }
    if (list.isEmpty()) return "onboarding"
    if (list.size == 1 && list.first().role == WorkplaceRole.EMPLOYEE) {
        return "employee_dashboard"
    }
    return if (list.any { it.role == WorkplaceRole.EMPLOYER } || list.size > 1) "workplace_select" else "employee_dashboard"
}
