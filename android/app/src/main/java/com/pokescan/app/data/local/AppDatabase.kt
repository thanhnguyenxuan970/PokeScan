package com.pokescan.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pokescan.app.data.local.dao.CardRecordDao
import com.pokescan.app.data.local.dao.SetEntryDao
import com.pokescan.app.data.local.entity.CardRecordEntity
import com.pokescan.app.data.local.entity.SetEntryEntity

@Database(
    entities = [CardRecordEntity::class, SetEntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardRecordDao(): CardRecordDao
    abstract fun setEntryDao(): SetEntryDao
}
