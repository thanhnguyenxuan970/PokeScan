package com.pokescan.app.data.local.entity

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pokescan.app.domain.model.Card
import com.pokescan.app.domain.model.CardLanguage
import com.pokescan.app.domain.model.PriceSource

@Entity(tableName = "card_records")
data class CardRecordEntity(
    @PrimaryKey val id: String,
    val name: String,
    val setCode: String,
    val setNumber: String,
    val language: String,
    val marketPrice: Double?,
    val priceSource: String?,
    val scannedAt: Long,
    val syncedAt: Long?,
    val serverID: String?,
)

fun CardRecordEntity.toDomain(): Card = Card(
    id = id,
    name = name,
    setCode = setCode,
    setNumber = setNumber,
    language = CardLanguage.entries.firstOrNull { it.raw == language }
        ?: CardLanguage.ENGLISH.also { Log.w("CardRecordEntity", "Unknown language '$language', defaulting to ENGLISH") },
    marketPrice = marketPrice,
    priceSource = priceSource?.let { raw -> PriceSource.entries.firstOrNull { it.raw == raw } },
    scannedAt = scannedAt,
)

fun Card.toEntity(syncedAt: Long? = null, serverID: String? = null): CardRecordEntity =
    CardRecordEntity(
        id = id,
        name = name,
        setCode = setCode,
        setNumber = setNumber,
        language = language.raw,
        marketPrice = marketPrice,
        priceSource = priceSource?.raw,
        scannedAt = scannedAt,
        syncedAt = syncedAt,
        serverID = serverID,
    )
