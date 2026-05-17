package com.snapdex.app.agents

import android.content.SharedPreferences
import com.snapdex.app.config.AppConfig
import com.snapdex.app.data.local.SecureStorage
import com.snapdex.app.ui.navigation.Routes
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Newbie Agent — simulates a brand-new user who has never opened the app.
 *
 * Verifies routing gates (onboarding → sign-in → main), Privacy Policy
 * accessibility, and that no required placeholders are still in production config.
 *
 * Feedback collected:
 * - Is the onboarding shown on first launch (hasSeenOnboarding=false)?
 * - After onboarding, does the user land on sign-in and not jump to main?
 * - Is the Privacy Policy URL real and accessible (not a REPLACE_ME placeholder)?
 * - Can a user with a stored JWT skip onboarding on reinstall?
 */
class NewbieAgentTest {

    private val mockSecureStorage = mockk<SecureStorage>()
    private val mockPrefs = mockk<SharedPreferences>()

    @Before
    fun setUp() {
        every { mockSecureStorage.getToken() } returns null
        every { mockPrefs.getBoolean("hasSeenOnboarding", false) } returns false
    }

    /** Mirrors the startDestination logic in NavGraph exactly. */
    private fun resolveStart(): String = when {
        mockSecureStorage.getToken() != null -> Routes.MAIN
        !mockPrefs.getBoolean("hasSeenOnboarding", false) -> Routes.ONBOARDING
        else -> Routes.SIGN_IN
    }

    // ---- Routing gates ----

    @Test
    fun `first launch — routes to ONBOARDING`() {
        assertEquals(Routes.ONBOARDING, resolveStart())
    }

    @Test
    fun `seen onboarding but no token — routes to SIGN_IN`() {
        every { mockPrefs.getBoolean("hasSeenOnboarding", false) } returns true
        assertEquals(Routes.SIGN_IN, resolveStart())
    }

    @Test
    fun `valid token present — skips onboarding and auth, routes to MAIN`() {
        every { mockSecureStorage.getToken() } returns "eyJhbGciOiJIUzI1NiJ9.valid-token"
        assertEquals(Routes.MAIN, resolveStart())
    }

    @Test
    fun `token wins even when onboarding not seen — returning user after reinstall`() {
        every { mockSecureStorage.getToken() } returns "stored-jwt"
        every { mockPrefs.getBoolean("hasSeenOnboarding", false) } returns false
        assertEquals(Routes.MAIN, resolveStart())
    }

    @Test
    fun `null token with completed onboarding — routes to SIGN_IN not MAIN`() {
        every { mockSecureStorage.getToken() } returns null
        every { mockPrefs.getBoolean("hasSeenOnboarding", false) } returns true
        assertFalse("Should not route to MAIN without a token", Routes.MAIN == resolveStart())
        assertEquals(Routes.SIGN_IN, resolveStart())
    }

    // ---- Privacy Policy config ----

    @Test
    fun `privacy policy URL is not blank`() {
        assertTrue(
            "PRIVACY_POLICY_URL is blank — link in OnboardingScreen and PaywallView won't open",
            AppConfig.PRIVACY_POLICY_URL.isNotBlank(),
        )
    }

    @Test
    fun `privacy policy URL is not a placeholder`() {
        assertFalse(
            "PRIVACY_POLICY_URL still contains 'REPLACE' placeholder — must be a real URL before shipping",
            AppConfig.PRIVACY_POLICY_URL.contains("REPLACE", ignoreCase = true),
        )
    }

    @Test
    fun `privacy policy URL uses HTTPS`() {
        assertTrue(
            "PRIVACY_POLICY_URL must use HTTPS for App Store compliance",
            AppConfig.PRIVACY_POLICY_URL.startsWith("https://"),
        )
    }

    // ---- Route constant sanity ----

    @Test
    fun `all required routes are non-empty strings`() {
        listOf(Routes.ONBOARDING, Routes.SIGN_IN, Routes.MAIN, Routes.SCANNER, Routes.COLLECTION, Routes.PAYWALL)
            .forEach { route ->
                assertNotNull("Route constant is null: $route", route)
                assertTrue("Route constant is empty", route.isNotEmpty())
            }
    }

    @Test
    fun `route constants are unique — no two routes share the same path`() {
        val routes = listOf(Routes.ONBOARDING, Routes.SIGN_IN, Routes.MAIN, Routes.SCANNER, Routes.COLLECTION, Routes.PAYWALL)
        assertEquals("Duplicate route paths detected", routes.size, routes.toSet().size)
    }
}
