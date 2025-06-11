package com.example.karttracker.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_sessions")
data class RunSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    var name: String = "",
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val totalDurationMillis: Long
)
