package com.example.karttracker.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey

@Entity(
    tableName = "laps",
    foreignKeys = [ForeignKey(
        entity = RunSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = CASCADE // If a session is deleted, its laps are also deleted
    )]
)
data class LapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long, // Foreign key to link to RunSession
    val lapNumber: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long,
    val formattedTime: String
)