package com.snapdex.app.data.repository

import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.snapdex.app.data.local.SecureStorage
import com.snapdex.app.data.local.dao.CardRecordDao
import com.snapdex.app.data.remote.ApiService
import com.snapdex.app.data.service.ScanCounterService
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AuthRepositoryTest {

    private val apiService = mockk<ApiService>(relaxed = true)
    private val secureStorage = mockk<SecureStorage>(relaxed = true)
    private val cardRecordDao = mockk<CardRecordDao>(relaxed = true)
    private val googleSignInClient = mockk<GoogleSignInClient>()
    private val scanCounterService = mockk<ScanCounterService>(relaxed = true)
    private val collectionRepository = mockk<CollectionRepository>(relaxed = true)

    private val authRepository = AuthRepository(
        apiService = apiService,
        secureStorage = secureStorage,
        cardRecordDao = cardRecordDao,
        googleSignInClient = googleSignInClient,
        scanCounterService = scanCounterService,
        collectionRepository = collectionRepository,
    )

    @Test
    fun `signOut — pushPending called before clearToken`() = runTest {
        val mockTask = mockk<Task<Void>>()
        every { mockTask.addOnCompleteListener(any()) } answers {
            firstArg<OnCompleteListener<Void>>().onComplete(mockTask)
            mockTask
        }
        every { googleSignInClient.signOut() } returns mockTask

        authRepository.signOut()

        coVerifyOrder {
            collectionRepository.pushPending()
            secureStorage.clearToken()
        }
    }
}
