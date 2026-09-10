package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_photos")
data class CapturedPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val addressTitle: String,
    val addressLine: String,
    val note: String,
    val compassDegrees: Float
)
