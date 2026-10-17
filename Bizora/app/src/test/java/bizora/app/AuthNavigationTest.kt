package bizora.app

import bizora.app.data.model.Workplace
import bizora.app.data.model.WorkplaceRole
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthNavigationTest {
    private val employer = Workplace(id = "employer-1", name = "Office", role = WorkplaceRole.EMPLOYER)
    private val employee = Workplace(id = "employee-1", name = "Store", role = WorkplaceRole.EMPLOYEE)

    @Test
    fun startup_requiresMpinBeforeWorkplaceRouting() {
        assertEquals("welcome", initialAuthDestination(false, null, false))
        assertEquals("sign_in", initialAuthDestination(true, null, false))
        assertEquals("sign_in", initialAuthDestination(true, "", false))
        assertEquals("sign_in", initialAuthDestination(true, "   ", false))
        assertEquals("mpin_setup", initialAuthDestination(true, "token", false))
        assertEquals("mpin_unlock", initialAuthDestination(true, "token", true))
    }

    @Test
    fun signInAlwaysLeadsToMpinStep() {
        assertEquals("mpin_setup", postSignInDestination(false))
        assertEquals("mpin_unlock", postSignInDestination(true))
    }

    @Test
    fun postMpinRoutesByActualWorkplaceRole() {
        assertEquals("onboarding", postMpinDestination(emptyList()))
        assertEquals("workplace_select", postMpinDestination(listOf(employer)))
        assertEquals("employee_dashboard", postMpinDestination(listOf(employee)))
        assertEquals("workplace_select", postMpinDestination(listOf(employer, employee)))
        assertEquals("workplace_select", postMpinDestination(listOf(employee, employer)))
    }

    @Test
    fun firstTimeUser_afterMpinSetupOrDismiss_landsOnOnboarding() {
        // When a first-time user completes or skips MPIN setup with 0 workplaces, destination must be onboarding
        val firstTimeUserWorkplaces = emptyList<Workplace>()
        val nextDestination = postMpinDestination(firstTimeUserWorkplaces)
        assertEquals("onboarding", nextDestination)
    }

    @Test
    fun resolvePostMpinDestination_differentiatesReopenVsFreshSignIn() {
        // On app reopen with confirmed workplace: employer goes to employer_dashboard
        assertEquals("employer_dashboard", resolvePostMpinDestination(listOf(employer), employer, true))
        // On app reopen with confirmed workplace: employee goes to employee_dashboard
        assertEquals("employee_dashboard", resolvePostMpinDestination(listOf(employee), employee, true))

        // On fresh sign-in (unconfirmed): employer goes to workplace_select
        assertEquals("workplace_select", resolvePostMpinDestination(listOf(employer), employer, false))
        // On fresh sign-in (unconfirmed): employee with 1 workplace goes to employee_dashboard
        assertEquals("employee_dashboard", resolvePostMpinDestination(listOf(employee), employee, false))

        // On 0 workplaces: goes to onboarding
        assertEquals("onboarding", resolvePostMpinDestination(emptyList(), null, false))
    }
}
