package com.example.karttracker.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.karttracker.database.dao.LapDao
import com.example.karttracker.database.dao.LocationPointDao
import com.example.karttracker.database.dao.RunSessionDao
import com.example.karttracker.database.entity.LapEntity
import com.example.karttracker.database.entity.LocationPointEntity
import com.example.karttracker.database.entity.RunSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun updateSessionName(session: RunSessionEntity, newName: String) {
        viewModelScope.launch {
            val updatedSession = session.copy(name = newName)
            runSessionDao.updateRunSession(updatedSession)
        }
    }

    val fastestLapId: StateFlow<Long?> =
        getLapsForSession(0L) // Initial call, actual sessionId will be used later when combined
            .map { laps ->
                laps.minByOrNull { it.durationMillis }?.id // Find the lap with the minimum duration
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    // IMPORTANT: The `getLapsForSession(0L)` in fastestLapId's initial call above is a placeholder.
    // To correctly link `fastestLapId` to the current `sessionId`, you need to:
    // 1. Pass `sessionId` into the ViewModel during its creation if it's static for the ViewModel's lifetime.
    // OR (more common for navigation arguments):
    // 2. Refactor `getLapsForSession` to use a `MutableStateFlow` for `sessionId` within the ViewModel,
    //    and then `fastestLapId` would observe that `MutableStateFlow`.

    // Let's refine `fastestLapId` to properly react to the sessionId.
    // A common pattern is to have a private mutable flow for the sessionId.
    private val _currentSessionId = kotlinx.coroutines.flow.MutableStateFlow(0L)

    // Expose a function to set the session ID from the UI
    fun setSessionId(id: Long) {
        _currentSessionId.value = id
    }

    // Now, derive the laps and fastest lap based on _currentSessionId
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSessionLaps: Flow<List<LapEntity>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != 0L) { // Avoid fetching for initial 0L state
                lapDao.getLapsForSession(sessionId)
                    .conflate()
                    .flowOn(Dispatchers.IO)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentFastestLapId: StateFlow<Long?> = currentSessionLaps
        .map { laps ->
            laps.minByOrNull { it.durationMillis }?.id
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}