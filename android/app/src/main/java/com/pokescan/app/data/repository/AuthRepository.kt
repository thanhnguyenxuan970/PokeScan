package com.pokescan.app.data.repository

import com.pokescan.app.data.local.SecureStorage
import com.pokescan.app.data.local.dao.CardRecordDao
import com.pokescan.app.data.remote.ApiService
import com.pokescan.app.data.remote.dto.GoogleSignInRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val secureStorage: SecureStorage,
    private val cardRecordDao: CardRecordDao,
) {
    suspend fun signInWithGoogle(idToken: String) {
        val response = apiService.signInWithGoogle(GoogleSignInRequest(idToken))
        secureStorage.saveToken(response.token)
    }

    suspend fun signOut() {
        secureStorage.clearToken()
        cardRecordDao.deleteAll()
    }
}
