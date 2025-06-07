package com.example.karttracker.components

import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class SelectedLapData(
    val lap: LapEntity,
    val locationPoints: List<LocationPointEntity>,
    val isFastestOfSelected: Boolean = false
)


@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    private val runSessionDao: RunSessionDao,
    private val lapDao: LapDao,
    private val locationPointDao: LocationPointDao
) : ViewModel() {

    // --- Session Data ---
    fun getRunSession(sessionId: Long): Flow<RunSessionEntity?> {
        return runSessionDao.getRunSessionById(sessionId)
            .conflate()
            .flowOn(Dispatchers.IO)
    }

    // --- Laps Data (for the main list) ---
    private val _currentSessionId = MutableStateFlow(0L)
    fun setSessionId(id: Long) {
        if (_currentSessionId.value != id) {
            _currentSessionId.value = id
            _selectedLapIds.value = emptyList() // Clear selected laps when session changes
            Log.d("SessionSummaryVM", "Session ID set to: $id")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSessionLaps: StateFlow<List<LapEntity>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != 0L) {
                lapDao.getLapsForSession(sessionId)
                    .conflate()
                    .flowOn(Dispatchers.IO)
            } else {
                flowOf(emptyList())
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

    // --- Lap Selection for Comparison ---
    private val _selectedLapIds = MutableStateFlow<List<Long>>(emptyList())
    val selectedLapIds: StateFlow<List<Long>> = _selectedLapIds.asStateFlow()

    fun toggleLapSelection(lapId: Long) {
        val currentSelection = _selectedLapIds.value.toMutableList()
        if (currentSelection.contains(lapId)) {
            currentSelection.remove(lapId)
        } else {
            if (currentSelection.size < 2) {
                currentSelection.add(lapId)
            } else {
                currentSelection.removeAt(0)
                currentSelection.add(lapId)
            }
        }
        _selectedLapIds.value = currentSelection
        Log.d("SelectedLaps", "Selected lap IDs: ${_selectedLapIds.value}")
    }

    fun clearLapSelection() {
        _selectedLapIds.value = emptyList()
        Log.d("SelectedLaps", "Cleared selected lap IDs.")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedLapsWithPoints: StateFlow<List<SelectedLapData>> =
        combine(
            _selectedLapIds,
            currentSessionLaps // Ensure we have access to the full list of laps
        ) { selectedIds, allLaps ->
            Log.d("LapPointsDebug", "Combine triggered for selected laps. Selected IDs: $selectedIds")

            if (selectedIds.isEmpty()) {
                Log.d("LapPointsDebug", "No laps selected, returning empty list.")
                emptyList()
            } else {
                val selectedLapEntities = allLaps.filter { it.id in selectedIds }
                Log.d("LapPointsDebug", "Filtered selected lap entities: ${selectedLapEntities.map { "Lap ${it.lapNumber} (ID: ${it.id})" }}")

                val fastestOfSelectedDuration = selectedLapEntities.minOfOrNull { it.durationMillis }
                val fastestOfSelectedLapId = selectedLapEntities.firstOrNull { it.durationMillis == fastestOfSelectedDuration }?.id

                // This is the critical part for debugging getLocationPointsForLap
                val results = selectedLapEntities.map { lapEntity ->
                    Log.d("LapPointsDebug", "Attempting to get points for Lap ${lapEntity.lapNumber} (ID: ${lapEntity.id})")

                    // Step 1: Observe the Flow directly (you can add a Log.d in a .onEach() here if you want to see emissions)
                    val pointsFlow = locationPointDao.getLocationPointsForLap(lapEntity.id)
                        .flowOn(Dispatchers.IO)
                        .conflate()
                        .onEach { fetchedPoints -> // This will log every time the DAO Flow emits
                            Log.d("LapPointsDebug", "DAO Flow emitted for Lap ${lapEntity.lapNumber} (ID: ${lapEntity.id}): ${fetchedPoints.size} points.")
                        }

                    // Step 2: Convert to StateFlow and get its current value
                    val points = pointsFlow.stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000), // Ensure it's active
                        emptyList()
                    ).value // <--- Get the actual list of points here

                    Log.d("LapPointsDebug", "StateFlow.value for Lap ${lapEntity.lapNumber} (ID: ${lapEntity.id}): ${points.size} points.")

                    SelectedLapData(
                        lap = lapEntity,
                        locationPoints = points,
                        isFastestOfSelected = lapEntity.id == fastestOfSelectedLapId
                    )
                }.sortedBy { selectedIds.indexOf(it.lap.id) } // Keep original selection order

                Log.d("LapPointsDebug", "Final list of SelectedLapData prepared. Total items: ${results.size}")
                results
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}