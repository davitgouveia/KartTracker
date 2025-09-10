package com.example.karttracker.components

import android.Manifest
import android.app.Application
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.karttracker.database.dao.LapDao
import com.example.karttracker.database.dao.LocationPointDao
import com.example.karttracker.database.dao.RunSessionDao
import com.example.karttracker.database.entity.LapEntity
import com.example.karttracker.database.entity.LocationPointEntity
import com.example.karttracker.database.entity.RunSessionEntity
import com.example.karttracker.pages.RADIUS_ARG
import com.example.karttracker.pages.START_FINISH_LAT_ARG
import com.example.karttracker.pages.START_FINISH_LNG_ARG
import com.example.karttracker.utils.TimeUtils
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.stateIn

data class Lap(
    val lapNumber: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long,
    val formattedTime: String
)

data class BestLap(
    val lapNumber: Int,
    val timeMillis: Long
)

@HiltViewModel
class LapViewModel @Inject constructor(
    application: Application,
    private val runSessionDao: RunSessionDao,
    private val lapDao: LapDao,
    private val locationPointDao: LocationPointDao,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 1000L
    ).apply {
        setMinUpdateIntervalMillis(500L)
    }.build()

    private val startFinishLat: Double = savedStateHandle.get<String>(START_FINISH_LAT_ARG)?.toDoubleOrNull() ?: 0.0
    private val startFinishLng: Double = savedStateHandle.get<String>(START_FINISH_LNG_ARG)?.toDoubleOrNull() ?: 0.0
    private val radius: Float = savedStateHandle.get<String>(RADIUS_ARG)?.toFloatOrNull() ?: 20f
    private val startFinishLocation = Location("startFinish").apply {
        latitude = startFinishLat
        longitude = startFinishLng
    }


    /*
    * StateFlows (UI)
    * */
    private val _speed = MutableStateFlow(0.0)
    val speed: StateFlow<Double> = _speed

    private val _currentLapTime = MutableStateFlow("00:00.00")
    val currentLapTime: StateFlow<String> = _currentLapTime // Now updates as a live clock

    private val _laps = MutableStateFlow<List<Lap>>(emptyList())
    val laps: StateFlow<List<Lap>> = _laps.asStateFlow()

    private val _elapsedTime = MutableStateFlow("00:00.00")
    val elapsedTime: StateFlow<String> = _elapsedTime

    private val _locationPoints = MutableStateFlow<List<Location>>(emptyList())
    val locationPoints: StateFlow<List<Location>> = _locationPoints.asStateFlow()

    private val _sessionCompleted = MutableSharedFlow<Long>()
    val sessionCompleted: SharedFlow<Long> = _sessionCompleted.asSharedFlow()

    /*
    * Lap stats
    * */

    val lastLap: StateFlow<String> = _laps.map { laps ->
        laps.lastOrNull()?.formattedTime ?: "00:00.00"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "00:00.00"
    )

    val bestLap: StateFlow<BestLap?> = _laps
        .map { laps ->
            laps.minByOrNull { it.durationMillis }?.let { best ->
                BestLap(best.lapNumber, best.durationMillis)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val lapCount: StateFlow<Int> = _laps.map { it.size }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0
    )

    private var hasStartedLapSession = false
    // Job for updating currentLapTime continuously
    private var currentLapTimerJob: Job? = null

    private var isInsideLapZone = false
    private var sessionStartTime: Long = 0L
    private var lastLapStartTime: Long = 0L
    private var lapCounter: Int = 0

    private var currentSessionId: Long? = null

    private var _avgLapTime: Long = 0L

    /*
    * Lap logic
    * */

    private fun startCurrentLapTimer() {
        currentLapTimerJob?.cancel() // Cancel any previous timer
        if (lastLapStartTime == 0L) return // Don't start if lastLapStartTime isn't set

        currentLapTimerJob = viewModelScope.launch {
            while (true) {
                val diff = System.currentTimeMillis() - lastLapStartTime
                _currentLapTime.value = TimeUtils.formatTime(diff)
                delay(10) // Update every 10 milliseconds for smooth display
            }
        }
    }

    private var firstZone = true

    private fun checkLapCompletion(currentLocation: Location) {
        val distance = currentLocation.distanceTo(startFinishLocation)
        val nowInside = distance <= radius

        if (!hasStartedLapSession && nowInside) {
            // First entry into the lap zone → just start timing the first lap
            hasStartedLapSession = true
            sessionStartTime = System.currentTimeMillis()
            lastLapStartTime = sessionStartTime
            startCurrentLapTimer()
        } else if (hasStartedLapSession) {
            if (isInsideLapZone && !nowInside) {
                // Exiting the lap zone — complete the lap
                if(!firstZone){
                    if (lastLapStartTime > 0) {
                        processCompletedLap(System.currentTimeMillis())
                    }
                    lastLapStartTime = System.currentTimeMillis()
                    startCurrentLapTimer()
                } else {
                    firstZone = false;
                }

            }
        }

        isInsideLapZone = nowInside
    }

    private fun processCompletedLap(lapEndTime: Long) {
        lapCounter++
        val lapTimeMillis = lapEndTime - lastLapStartTime
        val formattedTime = TimeUtils.formatTime(lapTimeMillis)
        val newLap = Lap(lapCounter, lastLapStartTime, lapEndTime, lapTimeMillis, formattedTime)

        Log.d("ProcessLap", "--- Processing Lap ${newLap.lapNumber} ---")
        Log.d("ProcessLap", "Lap Start Time (lastLapStartTime): ${newLap.startTimeMillis}")
        Log.d("ProcessLap", "Lap End Time (lapEndTime): ${newLap.endTimeMillis}")
        Log.d("ProcessLap", "Lap Duration: ${newLap.durationMillis} ms")

        viewModelScope.launch {
            currentSessionId?.let { sessionId ->
                val lapEntity = LapEntity(
                    sessionId = sessionId,
                    lapNumber = newLap.lapNumber,
                    startTimeMillis = newLap.startTimeMillis,
                    endTimeMillis = newLap.endTimeMillis,
                    durationMillis = newLap.durationMillis,
                    formattedTime = newLap.formattedTime
                )
                val insertedLapId = lapDao.insertLap(lapEntity)
                Log.d("ProcessLap", "Inserted Lap Entity. ID: $insertedLapId, SessionID: $sessionId")

                // BEFORE UPDATE: Check how many points currently exist for this session within the time range with NULL lapId
                val pointsBeforeUpdate = locationPointDao
                    .getLocationPointInTimeRange(
                        sessionId = sessionId,
                        startTime = lapEntity.startTimeMillis,
                        endTime = lapEntity.endTimeMillis
                    )
                    .firstOrNull() // <--- Correctly collect the first emitted list
                    ?: emptyList()
                Log.d("ProcessLap", "BEFORE UPDATE: ${pointsBeforeUpdate.size} points found for session $sessionId in range ${lapEntity.startTimeMillis}-${lapEntity.endTimeMillis}")
                pointsBeforeUpdate.forEach { point ->
                    Log.d("ProcessLap", "  Point Time: ${point.timestamp}, Current LapId: ${point.lapId}")
                }

                // Execute the update
                val rowsUpdated = locationPointDao.updateLapIdForPointsInTimeRange(
                    sessionId = sessionId,
                    lapId = insertedLapId,
                    startTime = lapEntity.startTimeMillis,
                    endTime = lapEntity.endTimeMillis
                )
                Log.d("ProcessLap", "UPDATE completed. Rows affected: $rowsUpdated for Lap ID $insertedLapId")

                // AFTER UPDATE: Check how many points now have the assigned lapId
                val pointsAfterUpdate = locationPointDao
                    .getLocationPointsForLap(insertedLapId) // Query by the new lapId
                    .firstOrNull() // <--- Correctly collect the first emitted list
                    ?: emptyList()
                Log.d("ProcessLap", "AFTER UPDATE: ${pointsAfterUpdate.size} points now have Lap ID $insertedLapId.")
                pointsAfterUpdate.forEach { point ->
                    Log.d("ProcessLap", "  Point Time: ${point.timestamp}, New LapId: ${point.lapId}")
                }
            }
            _laps.value += newLap
            processAvgLapTime(_laps.value)
            Log.d("ProcessLap", "Lap ${newLap.lapNumber} added to _laps StateFlow.")
        }
    }

    private fun processAvgLapTime(laps: List<Lap>){
        if (laps.isNotEmpty()) {
            val totalDuration = laps.sumOf { it.durationMillis }
            _avgLapTime = totalDuration / laps.size
        } else {
            _avgLapTime = 0L
        }
    }



    /*
    * Location Updates
    * */

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                updateSpeed(location)
                checkLapCompletion(location)
                updateElapsedTime()
                _locationPoints.value += location

                // Save location point to DB
                viewModelScope.launch {
                    currentSessionId?.let { sessionId ->
                        locationPointDao.insertLocationPoint(
                            LocationPointEntity(
                                sessionId = sessionId,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                altitude = location.altitude,
                                speed = location.speed,
                                timestamp = location.time,
                                lapId = null
                            )
                        )
                    }
                }
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION])
    fun startLocationUpdates() {
        viewModelScope.launch {
            // Create a new RunSession when starting
            val newSession = RunSessionEntity(
                name = "",
                startTimeMillis = System.currentTimeMillis(),
                // Will be updated on stop
                endTimeMillis = 0L,
                totalDurationMillis = 0L,
                avgLapTimeMillis = 0L,
                maxSpeed = 0.0,
                lapCount = 0,
                fastestLap = 0
            )
            currentSessionId = runSessionDao.insertRunSession(newSession)
            sessionStartTime = newSession.startTimeMillis
            lastLapStartTime = 0L // Reset for a clean start of the first lap
            lapCounter = 0 // Reset lap count
            _laps.value = emptyList() // Clear previous laps
            _locationPoints.value = emptyList() // Clear previous location points
            isInsideLapZone = false // Reset zone status
            hasStartedLapSession = false
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        currentLapTimerJob?.cancel() // Stop the current lap timer

        // Update the RunSession with end time and total duration
        viewModelScope.launch {
            currentSessionId?.let { sessionId ->
                val session = runSessionDao.getRunSessionById(sessionId).firstOrNull()

                session?.let { existingSession ->
                    val updatedSession = existingSession.copy(
                        endTimeMillis = System.currentTimeMillis(),
                        totalDurationMillis = System.currentTimeMillis() - existingSession.startTimeMillis,
                        lapCount = lapCounter,
                        maxSpeed = _maxSpeed,
                        fastestLap = bestLap.value?.lapNumber ?: 0,
                        avgLapTimeMillis = _avgLapTime
                    )
                    runSessionDao.updateRunSession(updatedSession)
                }

                //Emit flag after session saved
                _sessionCompleted.emit(sessionId)
            }
        }

        // Reset all relevant MutableStateFlows and internal state
        _speed.value = 0.0
        _currentLapTime.value = "00:00.00"
        _elapsedTime.value = "00:00.00"
        _laps.value = emptyList()
        _locationPoints.value = emptyList()
        lastLapStartTime = 0L
        sessionStartTime = 0L
        lapCounter = 0
        _maxSpeed = 0.0
        _avgLapTime = 0L
        isInsideLapZone = false
        hasStartedLapSession = false
        currentSessionId = null
    }



    /*
    * Utils
    * */

    private fun updateElapsedTime() {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - sessionStartTime
        _elapsedTime.value = TimeUtils.formatTime(diff)
    }

    var _maxSpeed: Double = 0.0

    private fun updateSpeed(location: Location) {
        val speedMps = location.speed.toDouble()
        val speedKph = speedMps * 3.6
        _speed.value = speedKph

        if (speedKph > _maxSpeed){
            _maxSpeed = speedKph
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

}
