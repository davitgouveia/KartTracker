package com.example.karttracker.database

import android.content.Context
import androidx.room.Room
import com.example.karttracker.database.dao.LapDao
import com.example.karttracker.database.dao.LocationPointDao
import com.example.karttracker.database.dao.RunSessionDao
import com.example.karttracker.database.migrations.MIGRATION_1_2
import com.example.karttracker.database.migrations.MIGRATION_2_3
import com.example.karttracker.database.migrations.MIGRATION_3_4
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "kart_tracker_db"
        )
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .build()
    }

    @Provides
    fun provideRunSessionDao(database: AppDatabase): RunSessionDao = database.runSessionDao()

    @Provides
    fun provideLapDao(database: AppDatabase): LapDao = database.lapDao()

    @Provides
    fun provideLocationPointDao(database: AppDatabase): LocationPointDao = database.locationPointDao()
}