package com.example.objectscanner.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos_result")
data class PhotoResult (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val imagePath: String,
    val timestamp: Long
)