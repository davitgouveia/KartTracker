package com.example.karttracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.karttracker.database.entity.LapEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLap(lap: LapEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaps(laps: List<LapEntity>)

    @Query("SELECT * FROM laps WHERE sessionId = :sessionId ORDER BY lapNumber ASC")
    fun getLapsForSession(sessionId: Long): Flow<List<LapEntity>>
}
