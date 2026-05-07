package com.pokescan.app.data.remote

import com.pokescan.app.data.remote.dto.GoogleSignInRequest
import com.pokescan.app.data.remote.dto.GoogleSignInResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/google")
    suspend fun signInWithGoogle(@Body request: GoogleSignInRequest): GoogleSignInResponse
}
