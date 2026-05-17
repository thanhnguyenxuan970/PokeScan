package com.snapdex.app.data.remote.dto

import com.squareup.moshi.Json

data class PriceResponseDto(
    @Json(name = "market_price") val marketPrice: Double?,
    @Json(name = "price_source") val priceSource: String?,
    @Json(name = "card_name") val cardName: String?,
    @Json(name = "set_code") val setCode: String?,
    @Json(name = "tcgplayer_price") val tcgPlayerPrice: Double? = null,
    @Json(name = "ebay_price") val ebayPrice: Double? = null,
)
