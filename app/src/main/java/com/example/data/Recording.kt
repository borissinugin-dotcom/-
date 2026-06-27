package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val stationName: String,
    val filePath: String,
    val timestamp: Long,
    val durationMs: Long,
    val fileSize: Long
)
