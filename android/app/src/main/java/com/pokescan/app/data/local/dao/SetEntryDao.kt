package com.snapdex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.snapdex.app.data.local.entity.SetEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetEntryDao {

    @Query("SELECT * FROM set_entries")
    fun observeAll(): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries")
    suspend fun getAll(): List<SetEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<SetEntryEntity>)

    @Query("DELETE FROM set_entries")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entries: List<SetEntryEntity>) {
        deleteAll()
        upsertAll(entries)
    }
}
