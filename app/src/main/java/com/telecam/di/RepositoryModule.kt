package com.telecam.di

import com.telecam.data.repository.SettingsRepository
import com.telecam.data.repository.SettingsRepositoryImpl
import com.telecam.data.repository.TelegramRepository
import com.telecam.data.repository.TelegramRepositoryImpl
import com.telecam.data.repository.UploadQueueRepository
import com.telecam.data.repository.UploadQueueRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUploadQueueRepository(
        impl: UploadQueueRepositoryImpl
    ): UploadQueueRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindTelegramRepository(
        impl: TelegramRepositoryImpl
    ): TelegramRepository
}
