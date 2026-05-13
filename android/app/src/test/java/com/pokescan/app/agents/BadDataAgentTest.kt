package com.pokescan.app.agents

import com.pokescan.app.data.service.CardIdentificationService
import com.pokescan.app.data.service.SetDatabaseService
import com.pokescan.app.data.service.SetResolver
import com.pokescan.app.domain.model.SetEntry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Bad Data Agent — enters invalid/malformed data into every input path.
 *
 * Covers: garbage OCR output, boundary set numbers, numbers-only lines,
 * extremely long inputs, and empty-string edge cases.
 *
 * Feedback collected:
 * - Does any malformed input crash the app (exception thrown)?
 * - Does a line with only digits get mistaken for a card name?
 * - Does a card name shorter than 3 chars leak through the name filter?
 * - Does a set number with 999/999 produce a crash or a clean "unknown"?
 */
class BadDataAgentTest {

    private val mockSetDb = mockk<SetDatabaseService>()
    private val sv1 = SetEntry("sv1", "Scarlet & Violet", total = 198, printedTotal = 198, releaseYear = 2023, series = "SV", language = "english")
    private lateinit var service: CardIdentificationService

    @Before
    fun setUp() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        service = CardIdentificationService(mockSetDb, SetResolver())
    }

    // ---- Null / empty inputs ----

    @Test
    fun `empty list — returns null`() = assertNull(service.identify(emptyList()))

    @Test
    fun `list of blank strings — returns null`() = assertNull(service.identify(listOf("", "  ", "\t", "\n")))

    @Test
    fun `single whitespace line — returns null`() = assertNull(service.identify(listOf("   ")))

    // ---- Malformed set number formats ----

    @Test
    fun `set number with missing denominator — returns null`() =
        assertNull(service.identify(listOf("Pikachu", "025/")))

    @Test
    fun `set number with missing numerator — returns null`() =
        assertNull(service.identify(listOf("Pikachu", "/198")))

    @Test
    fun `alphabetic set number — returns null`() =
        assertNull(service.identify(listOf("Pikachu", "abc/xyz")))

    @Test
    fun `float set number — returns null`() =
        assertNull(service.identify(listOf("Pikachu", "02.5/198")))

    @Test
    fun `all digits no slash — returns null (not a set number)`() =
        assertNull(service.identify(listOf("1234567890")))

    // ---- Card name filter validation ----

    @Test
    fun `line that is only digits is not used as card name`() {
        val result = service.identify(listOf("12345", "025/198"))
        // "12345" matches ^\d+(/\d+)?$ filter — should be skipped → Unknown Card
        assertEquals("Unknown Card", result?.cardName)
    }

    @Test
    fun `card name shorter than 3 chars is skipped`() {
        val result = service.identify(listOf("HP", "025/198"))
        // "HP" is 2 chars, below the >= 3 minimum → Unknown Card
        assertEquals("Unknown Card", result?.cardName)
    }

    @Test
    fun `card name exactly 3 chars is accepted`() {
        val result = service.identify(listOf("Mew", "025/198"))
        assertNotNull(result)
        assertEquals("Mew", result!!.cardName)
    }

    // ---- Out-of-range set numbers ----

    @Test
    fun `set number 999 slash 999 — returns unknown setCode, no crash`() {
        val result = service.identify(listOf("Pikachu", "999/999"))
        assertNotNull(result)
        assertEquals("unknown", result!!.setCode)
    }

    @Test
    fun `card number exceeding set total — returns unknown setCode`() {
        val result = service.identify(listOf("Card", "999/198"))
        // 999 > 198, sv1 has total=198 → no match
        assertNotNull(result)
        assertEquals("unknown", result!!.setCode)
    }

    @Test
    fun `000 slash 000 — returns unknown setCode`() {
        val result = service.identify(listOf("Card", "000/000"))
        assertNotNull(result)
        assertEquals("unknown", result!!.setCode)
    }

    // ---- Stress: extremely long inputs ----

    @Test
    fun `extremely long garbage line does not crash`() {
        val garbage = "a".repeat(5_000) + " 025/198 " + "b".repeat(5_000)
        // Must not throw — result may be null or resolved
        runCatching { service.identify(listOf(garbage)) }.getOrElse {
            throw AssertionError("Extremely long line caused exception: ${it.message}", it)
        }
    }

    @Test
    fun `1000 short garbage lines does not crash`() {
        val lines = (1..1_000).map { "garbage$it" }
        runCatching { service.identify(lines) }.getOrElse {
            throw AssertionError("1000-line input caused exception: ${it.message}", it)
        }
    }

    // ---- Empty DB edge case ----

    @Test
    fun `empty set DB with valid OCR — unknown setCode, no crash`() {
        every { mockSetDb.sets } returns MutableStateFlow(emptyList())
        val result = service.identify(listOf("Pikachu", "025/198"))
        assertNotNull(result)
        assertEquals("unknown", result!!.setCode)
    }
}
