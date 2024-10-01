package com.example.objectscanner.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.objectscanner.models.PhotoResult
import com.example.objectscanner.database.dao.PhotoResultDao

@Database(entities = [PhotoResult::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoResultDao(): PhotoResultDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "photo_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}