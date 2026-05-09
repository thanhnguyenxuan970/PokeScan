package com.pokescan.app.data.service

import com.pokescan.app.data.remote.ApiService
import com.pokescan.app.domain.model.Card
import com.pokescan.app.domain.model.PriceSource
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PricingService @Inject constructor(private val api: ApiService) {

    suspend fun fetchPrice(identified: CardIdentificationService.IdentifiedCard, isPro: Boolean): Card {
        val cardSku = "${identified.setCode}-${identified.setNumber.replace("/", "-")}"
        val tier = if (isPro) "pro" else "free"
        val dto = api.getPrice(cardSku, tier)
        return Card(
            id = UUID.randomUUID().toString(),
            name = dto.cardName ?: identified.cardName,
            setNumber = identified.setNumber,
            setCode = identified.setCode,
            language = identified.language,
            marketPrice = dto.marketPrice,
            priceSource = dto.priceSource?.let { raw ->
                PriceSource.entries.find { it.raw == raw }
            },
            scannedAt = System.currentTimeMillis(),
        )
    }
}
