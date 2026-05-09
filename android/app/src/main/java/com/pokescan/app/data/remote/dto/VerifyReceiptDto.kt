package com.pokescan.app.data.remote.dto

import com.squareup.moshi.Json

data class AndroidVerifyReceiptRequest(
    @Json(name = "product_id") val productId: String,
    @Json(name = "purchase_token") val purchaseToken: String,
)

data class AndroidVerifyReceiptResponse(
    @Json(name = "active") val active: Boolean,
)
