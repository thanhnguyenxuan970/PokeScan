package com.snapdex.app.data.repository

import android.util.Log
import com.snapdex.app.data.local.SecureStorage
import com.snapdex.app.data.local.dao.CardRecordDao
import com.snapdex.app.data.local.entity.CardRecordEntity
import com.snapdex.app.data.local.entity.toEntity
import com.snapdex.app.data.remote.ApiService
import com.snapdex.app.data.remote.dto.CardInDto
import com.snapdex.app.domain.model.Card
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepository @Inject constructor(
    private val dao: CardRecordDao,
    private val apiService: ApiService,
    private val secureStorage: SecureStorage,
) {

    fun observeAll(): Flow<List<CardRecordEntity>> {
        val uid = secureStorage.getUserId() ?: return dao.observeByUserId("")
        return dao.observeByUserId(uid)
    }

    suspend fun saveLocal(card: Card) {
        val uid = secureStorage.getUserId() ?: ""
        dao.upsert(card.toEntity().copy(userId = uid))
    }

    suspend fun delete(entity: CardRecordEntity) {
        dao.delete(entity)
        entity.serverID?.let {
            try {
                apiService.deleteCard(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete card from server: ${e.message}")
            }
        }
    }

    suspend fun pushPending() {
        val uid = secureStorage.getUserId() ?: return
        val pending = dao.getPendingSyncByUserId(uid)
        coroutineScope {
            pending.map { entity ->
                async {
                    try {
                        val dto = CardInDto(
                            cardSku = "${entity.setCode}-${entity.setNumber.replace("/", "-")}",
                            name = entity.name,
                            setCode = entity.setCode,
                            setNumber = entity.setNumber,
                            language = entity.language,
                            marketPrice = entity.marketPrice,
                            priceSource = entity.priceSource,
                            scannedAt = Instant.ofEpochMilli(entity.scannedAt).toString(),
                        )
                        val response = apiService.postCard(dto)
                        dao.upsert(entity.copy(syncedAt = System.currentTimeMillis(), serverID = response.serverId))
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to push card ${entity.id}: ${e.message}")
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun pullFromServer() {
        val uid = secureStorage.getUserId() ?: return
        val dtos = apiService.getCollection()
        for (dto in dtos) {
            val existing = dao.getByServerId(dto.serverId)
            val scannedAtMs = try {
                Instant.parse(dto.scannedAt).toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
            val entity = CardRecordEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = dto.name,
                setCode = dto.setCode ?: "",
                setNumber = dto.setNumber ?: "",
                language = dto.language,
                marketPrice = dto.marketPrice,
                priceSource = dto.priceSource,
                scannedAt = scannedAtMs,
                syncedAt = System.currentTimeMillis(),
                serverID = dto.serverId,
                userId = uid,
            )
            dao.upsert(entity)
        }
    }

    suspend fun syncAll() {
        pushPending()
        pullFromServer()
    }

    private companion object {
        const val TAG = "CollectionRepository"
    }
}
