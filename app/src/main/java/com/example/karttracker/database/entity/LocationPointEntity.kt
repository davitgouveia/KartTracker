package com.example.karttracker.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_points",
    foreignKeys = [
        ForeignKey(
            entity = RunSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = LapEntity::class,
            parentColumns = ["id"],
            childColumns = ["lapId"],
            onDelete = CASCADE
        )
    ]
)
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long, // Foreign key to RunSession
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Float,
    val timestamp: Long,
    val lapId: Long? = null // Foreign key to Lap
)
