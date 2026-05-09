package com.pokescan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pokescan.app.data.local.entity.CardRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardRecordDao {

    @Query("SELECT * FROM card_records ORDER BY scannedAt DESC")
    fun observeAll(): Flow<List<CardRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: CardRecordEntity)

    @Delete
    suspend fun delete(record: CardRecordEntity)

    @Query("SELECT * FROM card_records WHERE syncedAt IS NULL")
    suspend fun getPendingSync(): List<CardRecordEntity>

    @Query("SELECT * FROM card_records WHERE serverID = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): CardRecordEntity?
}
