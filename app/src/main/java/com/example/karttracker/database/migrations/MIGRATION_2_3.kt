package com.example.karttracker.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE run_sessions ADD COLUMN totalDurationMillis INTEGER NULL")
        db.execSQL("ALTER TABLE run_sessions ADD COLUMN avgLapTimeMillis INTEGER NULL")
        db.execSQL("ALTER TABLE run_sessions ADD COLUMN maxSpeed FLOAT NULL")
        db.execSQL("ALTER TABLE run_sessions ADD COLUMN lapCount INTEGER NULL")
        db.execSQL("ALTER TABLE run_sessions ADD COLUMN fastestLap INTEGER NULL")
    }
}