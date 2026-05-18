package com.snapdex.app.data.repository

import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.snapdex.app.data.local.SecureStorage
import com.snapdex.app.data.local.dao.CardRecordDao
import com.snapdex.app.data.remote.ApiService
import com.snapdex.app.data.remote.dto.GoogleSignInRequest
import com.snapdex.app.data.service.ScanCounterService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val secureStorage: SecureStorage,
    private val cardRecordDao: CardRecordDao,
    private val googleSignInClient: GoogleSignInClient,
    private val scanCounterService: ScanCounterService,
    private val collectionRepository: CollectionRepository,
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun signInWithGoogle(idToken: String) {
        val response = apiService.signInWithGoogle(GoogleSignInRequest(idToken))
        secureStorage.saveToken(response.token)
        secureStorage.saveUserId(response.userId)
    }

    suspend fun signOut() {
        runCatching { withTimeoutOrNull(1_000L) { collectionRepository.pushPending() } }
        val uid = secureStorage.getUserId() ?: ""
        secureStorage.clearToken()
        secureStorage.clearUserId()
        if (uid.isNotEmpty()) cardRecordDao.deleteByUserId(uid) else cardRecordDao.deleteAll()
        scanCounterService.resetCount()
        applicationScope.launch {
            withTimeoutOrNull(3_000L) {
                suspendCancellableCoroutine<Unit> { cont ->
                    googleSignInClient.signOut().addOnCompleteListener { cont.resume(Unit) }
                }
            }
        }
    }
}
