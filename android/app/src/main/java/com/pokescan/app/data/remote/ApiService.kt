package com.pokescan.app.data.remote

import com.pokescan.app.data.remote.dto.GoogleSignInRequest
import com.pokescan.app.data.remote.dto.GoogleSignInResponse
import com.pokescan.app.data.remote.dto.PriceResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/google")
    suspend fun signInWithGoogle(@Body request: GoogleSignInRequest): GoogleSignInResponse

    @GET("price/{cardSku}")
    suspend fun getPrice(
        @Path("cardSku") cardSku: String,
        @Query("tier") tier: String,
    ): PriceResponseDto
}
