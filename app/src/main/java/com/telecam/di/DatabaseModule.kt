package com.telecam.di

import android.content.Context
import androidx.room.Room
import com.telecam.data.local.TeleCamDatabase
import com.telecam.data.local.dao.UploadQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TeleCamDatabase {
        return Room.databaseBuilder(
            context,
            TeleCamDatabase::class.java,
            TeleCamDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUploadQueueDao(database: TeleCamDatabase): UploadQueueDao {
        return database.uploadQueueDao()
    }
}
