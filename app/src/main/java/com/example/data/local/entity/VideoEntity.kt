package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val uriString: String,
    val durationMs: Long = 0L,
    val resolution: String = "1080x1920",
    val isSample: Boolean = false,
    val orderIndex: Int = 0,
    val addedDate: Long = System.currentTimeMillis()
)
