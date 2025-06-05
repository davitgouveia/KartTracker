package com.example.karttracker.components

import androidx.lifecycle.ViewModel
import com.example.karttracker.database.dao.LapDao
import com.example.karttracker.database.dao.LocationPointDao
import com.example.karttracker.database.dao.RunSessionDao
import com.example.karttracker.database.entity.LapEntity
import com.example.karttracker.database.entity.LocationPointEntity
import com.example.karttracker.database.entity.RunSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn

@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    private val runSessionDao: RunSessionDao,
    private val lapDao: LapDao,
    private val locationPointDao: LocationPointDao
) : ViewModel() {

     fun getRunSession(sessionId: Long): Flow<RunSessionEntity?> {
        return runSessionDao.getRunSessionById(sessionId)
            .conflate() // Optimizes flow collection for latest value
            .flowOn(Dispatchers.IO) // Ensure database operations run on IO dispatcher
    }

    fun getLapsForSession(sessionId: Long): Flow<List<LapEntity>> {
        return lapDao.getLapsForSession(sessionId)
            .conflate()
            .flowOn(Dispatchers.IO)
    }

    fun getLocationPointsForSession(sessionId: Long): Flow<List<LocationPointEntity>> {
        return locationPointDao.getLocationPointsForSession(sessionId)
            .conflate()
            .flowOn(Dispatchers.IO)
    }
}