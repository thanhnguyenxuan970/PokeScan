package com.snapdex.app.agents

import com.snapdex.app.data.service.CardIdentificationService
import com.snapdex.app.data.service.SetDatabaseService
import com.snapdex.app.data.service.SetResolver
import com.snapdex.app.data.service.PHashService
import com.snapdex.app.domain.model.CardLanguage
import com.snapdex.app.domain.model.SetEntry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Vision Agent — OCR robustness under challenging scan conditions.
 *
 * Simulates: low-light (garbled/partial text), glare (merged OCR tokens),
 * protective sleeves (minimal visible text), and Japanese Unicode detection.
 *
 * Feedback collected:
 * - Which OCR conditions fail to identify the card?
 * - Does Japanese detection trigger correctly on kanji?
 * - Does "Unknown Card" surface cleanly when name is unreadable?
 */
class VisionAgentTest {

    private val mockSetDb = mockk<SetDatabaseService>()
    private val resolver = SetResolver()
    private val mockPHash = mockk<PHashService>(relaxed = true)
    private lateinit var service: CardIdentificationService

    private val sv1 = SetEntry("sv1", "Scarlet & Violet", total = 198, printedTotal = 198, releaseYear = 2023, series = "SV", language = "english")
    private val base1 = SetEntry("base1", "Base Set", total = 102, printedTotal = 102, releaseYear = 1999, series = "Base", language = "english")
    private val jpBase = SetEntry("base1-jp", "Expansion Pack", total = 102, printedTotal = null, releaseYear = 1996, series = "Base", language = "japanese")

    @Before
    fun setUp() {
        service = CardIdentificationService(mockSetDb, resolver, mockPHash)
    }

    // ---- Clear scan ----

    @Test
    fun `clear scan — all fields identified`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        val result = service.identify(listOf("Charizard ex", "Stage 2", "025/198", "230 HP"))
        assertNotNull(result)
        assertEquals("025/198", result!!.setNumber)
        assertEquals("Charizard ex", result.cardName)
        assertEquals("sv1", result.setCode)
        assertEquals(CardLanguage.ENGLISH, result.language)
    }

    // ---- Low-light simulation ----

    @Test
    fun `low-light — garbled card name but set number survives`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        val result = service.identify(listOf("Ch@riz@rd!!", "025/198"))
        assertNotNull(result)
        assertEquals("025/198", result!!.setNumber)
        assertEquals("sv1", result.setCode)
    }

    @Test
    fun `low-light — near-miss OCR (letter-O instead of zero) still resolves real number`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        // "O25/198" uses letter-O — should not match; real "025/198" on next line does
        val result = service.identify(listOf("Pikachu", "O25/198", "025/198"))
        assertNotNull(result)
        assertEquals("025/198", result!!.setNumber)
    }

    // ---- Glare simulation ----

    @Test
    fun `glare — set number embedded in noisy merged line`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        // Glare causes OCR to merge tokens on one line with surrounding whitespace
        val result = service.identify(listOf("Pikachu", "HP70  025/198  Basic"))
        assertNotNull(result)
        assertEquals("025/198", result!!.setNumber)
    }

    @Test
    fun `glare — symbol-corrupted line before clean set number line`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(base1))
        val result = service.identify(listOf("###GLARE###", "004/102"))
        assertNotNull(result)
        assertEquals("004/102", result!!.setNumber)
    }

    // ---- Sleeve simulation ----

    @Test
    fun `sleeve — only set number line visible, returns Unknown Card`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        val result = service.identify(listOf("025/198"))
        assertNotNull(result)
        assertEquals("025/198", result!!.setNumber)
        assertEquals("Unknown Card", result.cardName)
    }

    @Test
    fun `sleeve — blank lines around set number still resolves`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(base1))
        val result = service.identify(listOf("", "  ", "004/102", ""))
        assertNotNull(result)
        assertEquals("004/102", result!!.setNumber)
    }

    // ---- Japanese card detection ----

    @Test
    fun `japanese — hiragana name triggers JAPANESE language`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(base1, jpBase))
        val result = service.identify(listOf("ピカチュウ", "025/102"))
        assertNotNull(result)
        assertEquals(CardLanguage.JAPANESE, result!!.language)
        assertEquals("base1-jp", result.setCode)
    }

    @Test
    fun `japanese — kanji in card name triggers JAPANESE language`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(jpBase))
        val result = service.identify(listOf("リザードン", "006/102"))
        assertNotNull(result)
        assertEquals(CardLanguage.JAPANESE, result!!.language)
    }

    @Test
    fun `english card with no CJK characters stays ENGLISH`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(base1))
        val result = service.identify(listOf("Charizard", "004/102"))
        assertNotNull(result)
        assertEquals(CardLanguage.ENGLISH, result!!.language)
    }

    // ---- Null / empty cases ----

    @Test
    fun `pure noise — no set number pattern returns null`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        assertNull(service.identify(listOf("@#\$%^&", "HP70", "Stage 2 — Evolves")))
    }

    @Test
    fun `empty OCR output returns null`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        assertNull(service.identify(emptyList()))
    }

    @Test
    fun `single blank line returns null`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        assertNull(service.identify(listOf("   ")))
    }
}
