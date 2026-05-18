package com.snapdex.app.data.remote.dto

import com.squareup.moshi.Json

data class GoogleSignInRequest(
    @Json(name = "id_token") val idToken: String
)

data class GoogleSignInResponse(
    val token: String,
    @Json(name = "user_id") val userId: String,
)
