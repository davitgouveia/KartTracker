package com.example.karttracker.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.karttracker.database.dao.LapDao
import com.example.karttracker.database.dao.LocationPointDao
import com.example.karttracker.database.dao.RunSessionDao
import com.example.karttracker.database.entity.LapEntity
import com.example.karttracker.database.entity.LocationPointEntity
import com.example.karttracker.database.entity.RunSessionEntity

@Database(
    entities = [RunSessionEntity::class, LapEntity::class, LocationPointEntity::class],
    version = 1,
    exportSchema = false // Set to true to export schema for migrations
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun runSessionDao(): RunSessionDao
    abstract fun lapDao(): LapDao
    abstract fun locationPointDao(): LocationPointDao
}