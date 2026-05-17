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
import org.junit.Before
import org.junit.Test

/**
 * Catalog Agent — scanned card maps to the correct database entry.
 *
 * Tests: reprint disambiguation (base1 vs ex5 via printedTotal),
 * language isolation (JP vs EN same total), SKU format the backend expects.
 *
 * Feedback collected:
 * - Does Base Set Charizard resolve to base1 and NOT ex5 (Hidden Legends)?
 * - Does the JP card get isolated from the EN set with the same card total?
 * - Is the SKU format `setCode-cardNum-setTotal` matching what `/price/{sku}` expects?
 */
class CatalogAgentTest {

    private val mockSetDb = mockk<SetDatabaseService>()
    private val resolver = SetResolver()
    private val mockPHash = mockk<PHashService>(relaxed = true)
    private lateinit var service: CardIdentificationService

    private val base1 = SetEntry("base1", "Base Set", total = 102, printedTotal = 102, releaseYear = 1999, series = "Base", language = "english")
    private val ex5 = SetEntry("ex5", "Hidden Legends", total = 102, printedTotal = 101, releaseYear = 2004, series = "EX", language = "english")
    private val sv1 = SetEntry("sv1", "Scarlet & Violet", total = 198, printedTotal = null, releaseYear = 2023, series = "SV", language = "english")
    private val neo2 = SetEntry("neo2", "Neo Discovery", total = 75, printedTotal = null, releaseYear = 2001, series = "Neo", language = "english")
    private val jpBase = SetEntry("base1-jp", "Expansion Pack", total = 102, printedTotal = null, releaseYear = 1996, series = "Base", language = "japanese")

    @Before
    fun setUp() {
        service = CardIdentificationService(mockSetDb, resolver, mockPHash)
    }

    // ---- Reprint disambiguation ----

    @Test
    fun `Base Set Charizard resolves to base1 via printedTotal disambiguation`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(base1, ex5))
        val result = service.identify(listOf("Charizard", "004/102"))
        assertNotNull(result)
        assertEquals("base1", result!!.setCode)
    }

    @Test
    fun `card 075 in 102-card set resolves to base1 (printedTotal=102 beats ex5 printedTotal=101)`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(base1, ex5))
        val result = service.identify(listOf("Venusaur", "015/102"))
        assertNotNull(result)
        assertEquals("base1", result!!.setCode)
    }

    @Test
    fun `when both sets lack printedTotal — newest-wins tiebreaker (ex5 over base1)`() {
        val b = base1.copy(printedTotal = null)
        val e = ex5.copy(printedTotal = null)
        every { mockSetDb.sets } returns MutableStateFlow(listOf(b, e))
        val result = service.identify(listOf("Pikachu", "025/102"))
        assertNotNull(result)
        assertEquals("ex5", result!!.setCode) // ex5 released 2004 > base1 1999
    }

    // ---- Modern set ----

    @Test
    fun `modern SV1 card resolves to sv1`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        val result = service.identify(listOf("Koraidon ex", "254/198"))
        assertNotNull(result)
        assertEquals("sv1", result!!.setCode)
    }

    @Test
    fun `unique total (75) resolves directly to neo2 with no ambiguity`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(neo2))
        val result = service.identify(listOf("Misdreavus", "025/075"))
        assertNotNull(result)
        assertEquals("neo2", result!!.setCode)
    }

    // ---- Language isolation ----

    @Test
    fun `JP card resolves to JP set even with same total as EN set`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(base1, jpBase))
        val result = service.identify(listOf("リザードン", "006/102"))
        assertNotNull(result)
        assertEquals("base1-jp", result!!.setCode)
        assertEquals(CardLanguage.JAPANESE, result.language)
    }

    @Test
    fun `EN card not contaminated by JP set with same total`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(base1, jpBase))
        val result = service.identify(listOf("Charizard", "006/102"))
        assertNotNull(result)
        assertEquals("base1", result!!.setCode)
        assertEquals(CardLanguage.ENGLISH, result.language)
    }

    // ---- SKU format ----

    @Test
    fun `SKU replaces slash with dash — matches backend GET slash price slash sku format`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        val result = service.identify(listOf("Pikachu", "025/198"))
        assertNotNull(result)
        val sku = "${result!!.setCode}-${result.setNumber.replace("/", "-")}"
        assertEquals("sv1-025-198", sku)
    }

    @Test
    fun `base1 card SKU format is correct`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(base1))
        val result = service.identify(listOf("Charizard", "004/102"))
        assertNotNull(result)
        val sku = "${result!!.setCode}-${result.setNumber.replace("/", "-")}"
        assertEquals("base1-004-102", sku)
    }

    // ---- Unknown / fallback ----

    @Test
    fun `no matching set in DB returns unknown setCode without crashing`() {
        every { mockSetDb.sets } returns MutableStateFlow(emptyList())
        val result = service.identify(listOf("Pikachu", "025/198"))
        assertNotNull(result)
        assertEquals("unknown", result!!.setCode)
    }

    @Test
    fun `set number matching no set total returns unknown`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        val result = service.identify(listOf("Mystery Card", "001/999"))
        assertNotNull(result)
        assertEquals("unknown", result!!.setCode)
    }
}
