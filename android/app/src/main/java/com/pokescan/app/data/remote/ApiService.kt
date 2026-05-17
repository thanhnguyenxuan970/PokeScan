package com.snapdex.app.data.remote

import com.snapdex.app.data.remote.dto.AndroidVerifyReceiptRequest
import com.snapdex.app.data.remote.dto.AndroidVerifyReceiptResponse
import com.snapdex.app.data.remote.dto.CardInDto
import com.snapdex.app.data.remote.dto.CardOutDto
import com.snapdex.app.data.remote.dto.DeleteCardResponseDto
import com.snapdex.app.data.remote.dto.GoogleSignInRequest
import com.snapdex.app.data.remote.dto.GoogleSignInResponse
import com.snapdex.app.data.remote.dto.PostCardResponseDto
import com.snapdex.app.data.remote.dto.PriceResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    @GET("collection")
    suspend fun getCollection(): List<CardOutDto>

    @POST("collection")
    suspend fun postCard(@Body card: CardInDto): PostCardResponseDto

    @DELETE("collection/{id}")
    suspend fun deleteCard(@Path("id") id: String): DeleteCardResponseDto

    @POST("auth/verify-receipt/android")
    suspend fun verifyAndroidReceipt(@Body body: AndroidVerifyReceiptRequest): AndroidVerifyReceiptResponse
}
