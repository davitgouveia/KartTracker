package com.example.karttracker.components

import android.Manifest
import android.app.Application
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateListOf
import com.google.android.gms.location.LocationRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.core.content.FileProvider
import com.example.karttracker.pages.RADIUS_ARG
import com.example.karttracker.pages.START_FINISH_LAT_ARG
import com.example.karttracker.pages.START_FINISH_LNG_ARG
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices
import com.google.maps.android.ktx.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import java.io.IOException
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class Lap(
    val lapNumber: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long,
    val formattedTime: String
)

@HiltViewModel
class LapViewModel @Inject constructor(
    application: Application,
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

    private val _speed = MutableStateFlow(0.0)
    val speed: StateFlow<Double> = _speed

    private val _currentLapTime = MutableStateFlow("00:00.00")
    val currentLapTime: StateFlow<String> = _currentLapTime // Now updates as a live clock

    private var hasStartedLapSession = false

    // Job for updating currentLapTime continuously
    private var currentLapTimerJob: Job? = null

    private val _laps = MutableStateFlow<List<Lap>>(emptyList())
    val laps: StateFlow<List<Lap>> = _laps.asStateFlow()

    private var lastLapStartTime: Long = 0L
    private var isInsideLapZone = false
    private var sessionStartTime: Long = 0L // Renamed for clarity (overall session start)
    private var lapCounter: Int = 0 // Tracks the current lap number

    private val _elapsedTime = MutableStateFlow("00:00.00")
    val elapsedTime: StateFlow<String> = _elapsedTime

    private val _locationPoints = MutableStateFlow<List<Location>>(emptyList())
    val locationPoints: StateFlow<List<Location>> = _locationPoints.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                updateSpeed(location)
                checkLapCompletion(location)
                updateElapsedTime()
                // Add location to a mutable list, then update the StateFlow
                _locationPoints.value = _locationPoints.value + location
            }
        }
    }


    val lastLap: StateFlow<String> = _laps.map { laps ->
        laps.lastOrNull()?.formattedTime ?: "00:00.00"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "00:00.00"
    )

    val bestLap: StateFlow<String> = _laps
        .map { laps ->
            laps.minByOrNull { it.durationMillis }?.formattedTime ?: "00:00.00"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = "00:00.00"
        )

    val lapCount: StateFlow<Int> = _laps.map { it.size }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0
    )


    val lapDelta: StateFlow<Double> = combine(
        currentLapTime, // Observe current lap time (as a trigger)
        _laps // Observe the list of laps (to get best lap duration)
    ) { currentLapTimeString, laps ->
        val bestLapDuration = laps.minByOrNull { it.durationMillis }?.durationMillis
        val currentLapDuration = try {
            parseTime(currentLapTimeString)
        } catch (e: Exception) {
            0L // Handle parsing error
        }

        if (bestLapDuration == null || bestLapDuration == 0L || currentLapDuration == 0L) {
            0.0 // Not enough data for a meaningful delta
        } else {
            (currentLapDuration - bestLapDuration).toDouble() / 1000.0 // Delta in seconds
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    init {

    }

    private fun updateElapsedTime() {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - sessionStartTime
        _elapsedTime.value = formatTime(diff)
    }

    private fun updateSpeed(location: Location) {
        val speedMps = location.speed.toDouble()
        val speedKph = speedMps * 3.6
        _speed.value = speedKph
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
        val formattedTime = formatTime(lapTimeMillis)
        val newLap = Lap(lapCounter, lastLapStartTime, lapEndTime, lapTimeMillis, formattedTime)

        viewModelScope.launch {
            _laps.value = _laps.value + newLap // Add the new lap to the list
        }
    }

    private fun startCurrentLapTimer() {
        currentLapTimerJob?.cancel() // Cancel any previous timer
        if (lastLapStartTime == 0L) return // Don't start if lastLapStartTime isn't set

        currentLapTimerJob = viewModelScope.launch {
            while (true) {
                val diff = System.currentTimeMillis() - lastLapStartTime
                _currentLapTime.value = formatTime(diff)
                delay(10) // Update every 10 milliseconds for smooth display
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val milliseconds = (millis % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
    }

    private fun parseTime(formattedTime: String): Long {
        val parts = formattedTime.split(":", ".")
        if (parts.size != 3) throw IllegalArgumentException("Invalid time format: $formattedTime")

        val minutes = parts[0].toLong()
        val seconds = parts[1].toLong()
        val milliseconds = parts[2].toLong() * 10 // Convert back to actual milliseconds

        return minutes * 60 * 1000 + seconds * 1000 + milliseconds
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION])
    fun startLocationUpdates() {
        sessionStartTime = System.currentTimeMillis() // Reset overall session start time
        lastLapStartTime = 0L // Reset for a clean start of the first lap
        lapCounter = 0 // Reset lap count
        _laps.value = emptyList() // Clear previous laps
        _locationPoints.value = emptyList() // Clear previous location points
        isInsideLapZone = false // Reset zone status
        hasStartedLapSession = false
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        /*generateAndSaveGpx()*/
        currentLapTimerJob?.cancel() // Stop the current lap timer
        // Reset all relevant MutableStateFlows and internal state
        _speed.value = 0.0
        _currentLapTime.value = "00:00.00"
        _elapsedTime.value = "00:00.00"
        _laps.value = emptyList()
        _locationPoints.value = emptyList()
        lastLapStartTime = 0L
        sessionStartTime = 0L
        lapCounter = 0
        isInsideLapZone = false
    }


    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

    /*
    private fun generateAndSaveGpx() {
        if (_locationPoints.isEmpty()) {
            // Handle case where no data was recorded
            return
        }

        val gpxString = buildGpxString(_locationPoints)
        saveGpxToFile(gpxString)
    }

    private fun buildGpxString(locations: List<Location>): String {
        val gpxBuilder = StringBuilder()
        gpxBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n")
        gpxBuilder.append("<gpx version=\"1.1\" creator=\"YourAppName\">\n")
        gpxBuilder.append("  <trk>\n")
        gpxBuilder.append("    <trkseg>\n")

        val formatter = DateTimeFormatter.ISO_INSTANT

        for (location in locations) {
            gpxBuilder.append("      <trkpt lat=\"${location.latitude}\" lon=\"${location.longitude}\">\n")
            gpxBuilder.append("        <ele>${location.altitude}</ele>\n")
            gpxBuilder.append("        <time>${formatter.format(Instant.ofEpochMilli(location.time))}Z</time>\n")
            gpxBuilder.append("        <speed>${location.speed}</speed>\n")
            gpxBuilder.append("      </trkpt>\n")
        }

        gpxBuilder.append("    </trkseg>\n")
        gpxBuilder.append("  </trk>\n")
        gpxBuilder.append("</gpx>\n")

        return gpxBuilder.toString()
    }

    private fun shareGpxFile(fileUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Share GPX File")
        getApplication<Application>().startActivity(chooserIntent)
    }

    private fun saveGpxToFile(gpxData: String) {
        val fileName = "lap_track_${System.currentTimeMillis()}.gpx"
        val file = File(getApplication<Application>().getExternalFilesDir(null), fileName)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                file.writeText(gpxData)
                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    "com.example.karttracker.fileprovider",
                    file
                )
                shareGpxFile(uri)
            } catch (e: IOException) {
                e.printStackTrace()

            }
        }
    }
*/




}
