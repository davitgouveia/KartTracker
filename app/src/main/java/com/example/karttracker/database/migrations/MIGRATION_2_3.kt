package com.example.karttracker.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE run_sessions ADD COLUMN avgLapTimeMillis INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE run_sessions ADD COLUMN maxSpeed REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE run_sessions ADD COLUMN lapCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE run_sessions ADD COLUMN fastestLap INTEGER NOT NULL DEFAULT 0")
    }
}