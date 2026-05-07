package com.pokescan.app.di

import android.content.Context
import androidx.room.Room
import com.pokescan.app.data.local.AppDatabase
import com.pokescan.app.data.local.dao.CardRecordDao
import com.pokescan.app.data.local.dao.SetEntryDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "pokescan.db").build()

    @Provides
    fun provideCardRecordDao(db: AppDatabase): CardRecordDao = db.cardRecordDao()

    @Provides
    fun provideSetEntryDao(db: AppDatabase): SetEntryDao = db.setEntryDao()
}
