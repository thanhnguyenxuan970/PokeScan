package com.snapdex.app.agents

import com.snapdex.app.data.local.dao.CardRecordDao
import com.snapdex.app.data.remote.ApiService
import com.snapdex.app.data.local.entity.CardRecordEntity
import com.snapdex.app.data.repository.CollectionRepository
import com.snapdex.app.data.service.CardIdentificationService
import com.snapdex.app.data.service.PricingService
import com.snapdex.app.domain.model.CardLanguage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Offline Agent — simulates no/poor internet to verify graceful degradation.
 *
 * Network failures must never crash the app. Local data must survive outages.
 * The app should silently fall back rather than showing a hard crash screen.
 *
 * Feedback collected:
 * - Does a timeout from the pricing endpoint bring ScannerViewModel to Idle?
 * - If pushPending fails mid-sync, is local data still intact?
 * - Does the app handle a 502 without crashing?
 */
class OfflineAgentTest {

    private val mockApi = mockk<ApiService>()
    private val mockDao = mockk<CardRecordDao>(relaxed = true)
    private lateinit var pricingService: PricingService
    private lateinit var collectionRepo: CollectionRepository

    private val identified = CardIdentificationService.IdentifiedCard(
        cardName = "Pikachu",
        setNumber = "025/198",
        setCode = "sv1",
        language = CardLanguage.ENGLISH,
    )

    private val testEntity = CardRecordEntity(
        id = "local-id-1",
        name = "Pikachu",
        setCode = "sv1",
        setNumber = "025/198",
        language = "english",
        marketPrice = 5.00,
        priceSource = "tcgplayer",
        scannedAt = 1_000_000L,
        syncedAt = null,
        serverID = null,
    )

    @Before
    fun setUp() {
        pricingService = PricingService(mockApi)
        collectionRepo = CollectionRepository(mockDao, mockApi)
    }

    // ---- PricingService offline tests ----

    @Test
    fun `no internet — IOException from price endpoint propagates to caller`() = runTest {
        coEvery { mockApi.getPrice(any(), any()) } throws IOException("Network unreachable")
        val error = runCatching { pricingService.fetchPrice(identified, isPro = false) }.exceptionOrNull()
        assertTrue("Expected IOException, got ${error?.javaClass?.simpleName}", error is IOException)
    }

    @Test
    fun `timeout — SocketTimeoutException propagates to caller`() = runTest {
        coEvery { mockApi.getPrice(any(), any()) } throws SocketTimeoutException("Read timed out after 30s")
        val error = runCatching { pricingService.fetchPrice(identified, isPro = false) }.exceptionOrNull()
        assertTrue(error is SocketTimeoutException)
    }

    @Test
    fun `server 502 — any RuntimeException from Retrofit propagates`() = runTest {
        coEvery { mockApi.getPrice(any(), any()) } throws RuntimeException("502 Bad Gateway")
        val error = runCatching { pricingService.fetchPrice(identified, isPro = false) }.exceptionOrNull()
        assertTrue(error is RuntimeException)
    }

    @Test
    fun `correct SKU built and sent before network failure`() = runTest {
        coEvery { mockApi.getPrice("sv1-025-198", "free") } throws IOException("offline")
        runCatching { pricingService.fetchPrice(identified, isPro = false) }
        coVerify(exactly = 1) { mockApi.getPrice("sv1-025-198", "free") }
    }

    @Test
    fun `pro user sends tier=pro even when offline`() = runTest {
        coEvery { mockApi.getPrice("sv1-025-198", "pro") } throws IOException("offline")
        runCatching { pricingService.fetchPrice(identified, isPro = true) }
        coVerify(exactly = 1) { mockApi.getPrice("sv1-025-198", "pro") }
    }

    // ---- CollectionRepository offline tests ----

    @Test
    fun `pushPending — server failure does NOT wipe local record`() = runTest {
        coEvery { mockDao.getPendingSync() } returns listOf(testEntity)
        coEvery { mockApi.postCard(any()) } throws IOException("offline")

        collectionRepo.pushPending()

        // Entity must NOT be deleted; only soft-fails and logs
        coVerify(exactly = 0) { mockDao.delete(any()) }
    }

    @Test
    fun `pullFromServer — network failure propagates to caller (ViewModel handles it)`() = runTest {
        coEvery { mockApi.getCollection() } throws IOException("offline")
        val error = runCatching { collectionRepo.pullFromServer() }.exceptionOrNull()
        assertTrue("Expected IOException, got ${error?.javaClass?.simpleName}", error is IOException)
    }

    @Test
    fun `pullFromServer — SocketTimeout propagates to caller`() = runTest {
        coEvery { mockApi.getCollection() } throws SocketTimeoutException("timeout")
        val error = runCatching { collectionRepo.pullFromServer() }.exceptionOrNull()
        assertTrue(error is SocketTimeoutException)
    }

    @Test
    fun `delete card — server failure still completes local delete`() = runTest {
        val entityWithServerId = testEntity.copy(serverID = "server-uuid-abc")
        coEvery { mockApi.deleteCard("server-uuid-abc") } throws IOException("offline")

        collectionRepo.delete(entityWithServerId)

        // Local DAO delete was still called — server failure is fire-and-forget
        coVerify(exactly = 1) { mockDao.delete(entityWithServerId) }
    }

    @Test
    fun `syncAll — full outage completes without crash`() = runTest {
        coEvery { mockDao.getPendingSync() } returns emptyList()
        coEvery { mockApi.getCollection() } throws IOException("total outage")
        collectionRepo.syncAll()
    }
}
