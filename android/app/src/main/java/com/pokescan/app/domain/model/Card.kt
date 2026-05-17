package com.snapdex.app.domain.model

enum class CardLanguage(val raw: String) {
    ENGLISH("english"),
    JAPANESE("japanese"),
}

enum class PriceSource(val raw: String) {
    TCGPLAYER("tcgplayer"),
    EBAY("ebay"),
    CARDMARKET("cardmarket"),
    AGGREGATED("aggregated"),
}

data class Card(
    val id: String,
    val name: String,
    val setNumber: String,
    val setCode: String,
    val language: CardLanguage,
    val marketPrice: Double?,
    val priceSource: PriceSource?,
    val scannedAt: Long,
    val tcgPlayerPrice: Double? = null,
    val ebayPrice: Double? = null,
    val variant: String? = null,
    val setName: String? = null,
    val setYear: Int? = null,
    val isAuthentic: Boolean? = null,
    val priceUpdatedAt: Long? = null,
    val gradeRoiPsaGrade: Int? = null,
    val gradeRoiSellValue: Double? = null,
    val gradeRoiNetProfit: Double? = null,
) {
    val cardSKU: String get() = "$setCode-${setNumber.replace("/", "-")}"
}
