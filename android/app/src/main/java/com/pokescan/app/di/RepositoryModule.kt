package com.pokescan.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Phase 4 adds repository @Binds after all repositories are defined.
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule
