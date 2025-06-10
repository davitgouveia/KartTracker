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
import kotlinx.coroutines.flow.catch
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
        // First, combine the selected IDs and all laps to get the LapEntity objects
        combine(
            _selectedLapIds,
            currentSessionLaps
        ) { selectedIds, allLaps ->
            Log.d("LapPointsDebug", "Combine (Phase 1) triggered. Selected IDs: $selectedIds")
            if (selectedIds.isEmpty()) {
                emptyList() // Return empty list of LapEntities if nothing selected
            } else {
                // Filter LapEntities based on selected IDs and keep the order
                allLaps.filter { it.id in selectedIds }.sortedBy { selectedIds.indexOf(it.id) }
            }
        }
            // Now, for each LapEntity, fetch its LocationPoints Flow
            .flatMapLatest { lapEntities ->
                Log.d("LapPointsDebug", "flatMapLatest (Phase 2) triggered for ${lapEntities.size} lap entities.")
                if (lapEntities.isEmpty()) {
                    flowOf(emptyList()) // If no laps selected, emit an empty list of SelectedLapData
                } else {
                    // Create a list of Flows, one for each selected lap's points
                    val flowsForPoints = lapEntities.map { lapEntity ->
                        // Combine each lapEntity with its corresponding points Flow
                        locationPointDao.getLocationPointsForLap(lapEntity.id)
                            .flowOn(Dispatchers.IO)
                            .conflate()
                            .onEach { fetchedPoints ->
                                Log.d("LapPointsDebug", "DAO Flow emitted for Lap ${lapEntity.lapNumber} (ID: ${lapEntity.id}): ${fetchedPoints.size} points.")
                            }
                            .map { points -> // Map the points list to a SelectedLapData object
                                Log.d("LapPointsDebug", "Mapping points for Lap ${lapEntity.lapNumber} (ID: ${lapEntity.id}). Points count: ${points.size}")
                                // Calculate fastestOfSelected for this particular lap
                                // (This needs to be re-evaluated for the combined set later,
                                // or passed down from the combine block if performance is critical)
                                SelectedLapData(
                                    lap = lapEntity,
                                    locationPoints = points,
                                    // isFastestOfSelected will be set correctly after combining all results
                                    isFastestOfSelected = false // Temporarily set, will be updated below
                                )
                            }
                            .catch { e ->
                                Log.e("LapPointsError", "Error fetching points for Lap ${lapEntity.lapNumber} (ID: ${lapEntity.id}): ${e.message}")
                                emit(SelectedLapData(lap = lapEntity, locationPoints = emptyList(), isFastestOfSelected = false))
                            }
                    }
                    // Combine all the individual SelectedLapData Flows into one Flow of List<SelectedLapData>
                    if (flowsForPoints.isNotEmpty()) {
                        kotlinx.coroutines.flow.combine(flowsForPoints) { arrayOfSelectedLapData ->
                            Log.d("LapPointsDebug", "Combine (Phase 3) triggered for arrayOfSelectedLapData. Size: ${arrayOfSelectedLapData.size}")
                            val list = arrayOfSelectedLapData.toList()
                            val fastestOfSelectedDuration = list.minOfOrNull { it.lap.durationMillis }
                            val finalResults = list.map { selectedLapData ->
                                selectedLapData.copy(isFastestOfSelected = selectedLapData.lap.durationMillis == fastestOfSelectedDuration)
                            }
                            Log.d("LapPointsDebug", "Final results prepared with fastest of selected.")
                            finalResults
                        }
                    } else {
                        flowOf(emptyList())
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}