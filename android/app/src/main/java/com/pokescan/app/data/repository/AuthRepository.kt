package com.pokescan.app.data.repository

import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.pokescan.app.data.local.SecureStorage
import com.pokescan.app.data.local.dao.CardRecordDao
import com.pokescan.app.data.remote.ApiService
import com.pokescan.app.data.remote.dto.GoogleSignInRequest
import com.pokescan.app.data.service.ScanCounterService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val secureStorage: SecureStorage,
    private val cardRecordDao: CardRecordDao,
    private val googleSignInClient: GoogleSignInClient,
    private val scanCounterService: ScanCounterService,
    private val collectionRepository: CollectionRepository,
) {
    suspend fun signInWithGoogle(idToken: String) {
        val response = apiService.signInWithGoogle(GoogleSignInRequest(idToken))
        secureStorage.saveToken(response.token)
    }

    suspend fun signOut() {
        collectionRepository.pushPending()   // push while token is still valid
        secureStorage.clearToken()
        cardRecordDao.deleteAllSynced()
        scanCounterService.resetCount()
        suspendCancellableCoroutine<Unit> { cont ->
            googleSignInClient.signOut().addOnCompleteListener { cont.resume(Unit) }
        }
    }
}
