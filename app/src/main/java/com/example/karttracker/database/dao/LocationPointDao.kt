package com.example.karttracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.karttracker.database.entity.LocationPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoint(point: LocationPointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoints(points: List<LocationPointEntity>)

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLocationPointsForSession(sessionId: Long): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM location_points WHERE lapId = :lapId ORDER BY timestamp ASC")
    fun getLocationPointsForLap(lapId: Long): Flow<List<LocationPointEntity>>
}