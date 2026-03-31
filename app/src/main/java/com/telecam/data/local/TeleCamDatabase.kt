package com.telecam.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.telecam.data.local.dao.UploadQueueDao
import com.telecam.data.local.entity.UploadQueueEntity

/**
 * Room database for TeleCam app.
 * Contains all local data storage.
 */
@Database(
    entities = [UploadQueueEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TeleCamDatabase : RoomDatabase() {
    
    /**
     * DAO for upload queue operations.
     */
    abstract fun uploadQueueDao(): UploadQueueDao

    companion object {
        const val DATABASE_NAME = "telecam_database"
    }
}
