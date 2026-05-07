package com.pokescan.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Phase 1 adds Room @Provides after AppDatabase is defined.
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule
