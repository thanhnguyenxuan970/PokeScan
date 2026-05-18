package com.snapdex.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.snapdex.app.data.local.dao.CardRecordDao
import com.snapdex.app.data.local.dao.SetEntryDao
import com.snapdex.app.data.local.entity.CardRecordEntity
import com.snapdex.app.data.local.entity.SetEntryEntity

@Database(
    entities = [CardRecordEntity::class, SetEntryEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardRecordDao(): CardRecordDao
    abstract fun setEntryDao(): SetEntryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE card_records ADD COLUMN tcgPlayerPrice REAL")
                database.execSQL("ALTER TABLE card_records ADD COLUMN ebayPrice REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE card_records ADD COLUMN variant TEXT")
                database.execSQL("ALTER TABLE card_records ADD COLUMN setName TEXT")
                database.execSQL("ALTER TABLE card_records ADD COLUMN setYear INTEGER")
                database.execSQL("ALTER TABLE card_records ADD COLUMN isAuthentic INTEGER")
                database.execSQL("ALTER TABLE card_records ADD COLUMN priceUpdatedAt INTEGER")
                database.execSQL("ALTER TABLE card_records ADD COLUMN gradeRoiPsaGrade INTEGER")
                database.execSQL("ALTER TABLE card_records ADD COLUMN gradeRoiSellValue REAL")
                database.execSQL("ALTER TABLE card_records ADD COLUMN gradeRoiNetProfit REAL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE card_records ADD COLUMN userId TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
