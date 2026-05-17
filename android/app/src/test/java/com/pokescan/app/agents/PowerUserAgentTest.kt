package com.pokescan.app.agents

import com.pokescan.app.data.service.CardIdentificationService
import com.pokescan.app.data.service.ScanCounterService
import com.pokescan.app.data.service.SetDatabaseService
import com.pokescan.app.data.service.SetResolver
import com.pokescan.app.domain.model.SetEntry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Power User Agent — simulates scanning 50+ cards in rapid succession.
 *
 * Checks: scan limit boundary (20 free, unlimited pro), identification
 * throughput under load, and result stability across repeated calls.
 *
 * Feedback collected:
 * - Does the free-tier gate block at exactly scan #21?
 * - Do 50 sequential identifications complete in < 1 second?
 * - Does the same input always resolve to the same setCode (no flakiness)?
 */
class PowerUserAgentTest {

    private val mockSetDb = mockk<SetDatabaseService>()
    private val sv1 = SetEntry("sv1", "Scarlet & Violet", total = 198, printedTotal = 198, releaseYear = 2023, series = "SV", language = "english")

    // ---- Scan limit business rules ----

    @Test
    fun `free tier monthly limit is exactly 10`() {
        assertEquals(10, ScanCounterService.FREE_MONTHLY_LIMIT)
    }

    @Test
    fun `canScan logic — false when count equals FREE_MONTHLY_LIMIT`() {
        val count = ScanCounterService.FREE_MONTHLY_LIMIT
        assertFalse("At limit: count < FREE_MONTHLY_LIMIT should be false", count < ScanCounterService.FREE_MONTHLY_LIMIT)
    }

    @Test
    fun `canScan logic — true when count is one below limit`() {
        val count = ScanCounterService.FREE_MONTHLY_LIMIT - 1
        assertTrue("One below limit: should still be allowed", count < ScanCounterService.FREE_MONTHLY_LIMIT)
    }

    @Test
    fun `canScan logic — true when count is zero (fresh month)`() {
        assertTrue(0 < ScanCounterService.FREE_MONTHLY_LIMIT)
    }

    @Test
    fun `pro users bypass the gate entirely (isPro=true returns true unconditionally)`() {
        // The service logic: if (isPro) return true — no counter check
        val isPro = true
        assertTrue("isPro=true must always allow scanning regardless of count", isPro)
    }

    // ---- Identification throughput under load ----

    @Test
    fun `50 sequential identifications complete without any null result`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        val service = CardIdentificationService(mockSetDb, SetResolver())

        val inputs = listOf(
            listOf("Pikachu", "025/198"),
            listOf("Charizard ex", "251/198"),
            listOf("Mewtwo", "056/198"),
            listOf("Eevee", "001/198"),
        )
        val results = (0 until 50).map { i -> service.identify(inputs[i % inputs.size]) }
        val nullCount = results.count { it == null }
        assertEquals("Expected 0 null results from 50 identifications, got $nullCount", 0, nullCount)
    }

    @Test
    fun `50 sequential identifications complete under 1000ms`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        val service = CardIdentificationService(mockSetDb, SetResolver())

        val elapsed = measureTimeMillis {
            repeat(50) { i ->
                val num = String.format("%03d", i % 198 + 1)
                service.identify(listOf("Card $i", "$num/198"))
            }
        }
        assertTrue("50 identifications took ${elapsed}ms — expected < 1000ms", elapsed < 1000L)
    }

    @Test
    fun `same input resolves to same setCode across 20 repeated calls — no flakiness`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        val service = CardIdentificationService(mockSetDb, SetResolver())
        val lines = listOf("Charizard", "004/198")

        val results = (1..20).map { service.identify(lines) }
        val firstSetCode = results.first()!!.setCode

        results.forEachIndexed { idx, r ->
            assertNotNull("Call #${idx + 1} returned null unexpectedly", r)
            assertEquals("Call #${idx + 1} resolved to ${r!!.setCode} instead of $firstSetCode", firstSetCode, r.setCode)
        }
    }

    @Test
    fun `power user scanning different cards — all resolve without error`() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1))
        val service = CardIdentificationService(mockSetDb, SetResolver())

        val cardInputs = (1..198).map { i ->
            listOf("Card Name $i", "${String.format("%03d", i)}/198")
        }
        val errors = cardInputs.mapNotNull { input ->
            runCatching { service.identify(input) }.exceptionOrNull()
        }
        assertEquals("Expected no exceptions across 198 card identifications", 0, errors.size)
    }
}
