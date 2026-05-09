package com.pokescan.app.data.remote.dto

import com.squareup.moshi.Json

data class CardInDto(
    @Json(name = "card_sku") val cardSku: String,
    @Json(name = "name") val name: String,
    @Json(name = "set_code") val setCode: String?,
    @Json(name = "set_number") val setNumber: String?,
    @Json(name = "language") val language: String,
    @Json(name = "market_price") val marketPrice: Double?,
    @Json(name = "price_source") val priceSource: String?,
    @Json(name = "scanned_at") val scannedAt: String,
)

data class CardOutDto(
    @Json(name = "server_id") val serverId: String,
    @Json(name = "card_sku") val cardSku: String,
    @Json(name = "name") val name: String,
    @Json(name = "set_code") val setCode: String?,
    @Json(name = "set_number") val setNumber: String?,
    @Json(name = "language") val language: String,
    @Json(name = "market_price") val marketPrice: Double?,
    @Json(name = "price_source") val priceSource: String?,
    @Json(name = "scanned_at") val scannedAt: String,
)

data class PostCardResponseDto(
    @Json(name = "server_id") val serverId: String,
)

data class DeleteCardResponseDto(
    @Json(name = "deleted") val deleted: Boolean,
)
