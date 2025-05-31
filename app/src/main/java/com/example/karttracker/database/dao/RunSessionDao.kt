package com.example.karttracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.karttracker.database.entity.RunSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRunSession(session: RunSessionEntity): Long

    @Update
    suspend fun updateRunSession(session: RunSessionEntity)

    @Query("SELECT * FROM run_sessions ORDER BY startTimeMillis DESC")
    fun getAllRunSessions(): Flow<List<RunSessionEntity>>

    @Query("SELECT * FROM run_sessions WHERE id = :sessionId")
    suspend fun getRunSessionById(sessionId: Long): RunSessionEntity?
}