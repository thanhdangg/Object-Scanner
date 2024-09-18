package com.example.objectscanner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PhotoResultDao {
    @Insert
    suspend fun insert(photo: PhotoResult)

    @Query("SELECT * FROM photos_result ORDER BY timestamp DESC")
    suspend fun getAllPhotos(): List<PhotoResult>
}