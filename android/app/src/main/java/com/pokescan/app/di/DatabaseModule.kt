package com.snapdex.app.di

import android.content.Context
import androidx.room.Room
import com.snapdex.app.data.local.AppDatabase
import com.snapdex.app.data.local.dao.CardRecordDao
import com.snapdex.app.data.local.dao.SetEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "snapdex.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()

    @Provides
    fun provideCardRecordDao(db: AppDatabase): CardRecordDao = db.cardRecordDao()

    @Provides
    fun provideSetEntryDao(db: AppDatabase): SetEntryDao = db.setEntryDao()
}
