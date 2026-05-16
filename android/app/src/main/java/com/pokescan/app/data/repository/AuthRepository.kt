package com.pokescan.app.data.repository

import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.pokescan.app.data.local.SecureStorage
import com.pokescan.app.data.local.dao.CardRecordDao
import com.pokescan.app.data.remote.ApiService
import com.pokescan.app.data.remote.dto.GoogleSignInRequest
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
) {
    suspend fun signInWithGoogle(idToken: String) {
        val response = apiService.signInWithGoogle(GoogleSignInRequest(idToken))
        secureStorage.saveToken(response.token)
    }

    suspend fun signOut() {
        secureStorage.clearToken()
        cardRecordDao.deleteAll()
        suspendCancellableCoroutine<Unit> { cont ->
            googleSignInClient.signOut().addOnCompleteListener { cont.resume(Unit) }
        }
    }
}
