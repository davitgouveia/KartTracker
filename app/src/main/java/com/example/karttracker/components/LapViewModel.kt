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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

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
    val currentLapTime: StateFlow<String> = _currentLapTime

    private val _lapTimes = mutableStateListOf<String>()
    val lapTimes: List<String> = _lapTimes

    private var lastLapStartTime: Long = 0L
    private var isInsideLapZone = false
    private var startTime: Long = 0L

    private val _elapsedTime = MutableStateFlow("00:00.00")
    val elapsedTime: StateFlow<String> = _elapsedTime

    private val _locationPoints = mutableStateListOf<Location>()
    val locationPoints: List<Location> = _locationPoints

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                updateSpeed(location)
                checkLapCompletion(location)
                updateElapsedTime()
                _locationPoints.add(location)
            }
        }
    }


    private val _lastLap = MutableStateFlow("00:00.00")
    val lastLap: StateFlow<String> = _lastLap

    private val _bestLap = MutableStateFlow("00:00.00")
    val bestLap: StateFlow<String> = _bestLap

    private val _lapCount = MutableStateFlow(0);
    val lapCount: StateFlow<Int> = _lapCount

    private val _lapDelta = MutableStateFlow(0.0)
    val lapDelta: MutableStateFlow<Double> = _lapDelta

    init {
        startTime = System.currentTimeMillis()
    }

    private fun updateElapsedTime() {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - startTime
        _elapsedTime.value = formatTime(diff)
    }

    private fun updateSpeed(location: Location) {
        val speedMps = location.speed.toDouble()
        val speedKph = speedMps * 3.6
        _speed.value = speedKph
    }

    private fun checkLapCompletion(currentLocation: Location) {
        val distance = currentLocation.distanceTo(startFinishLocation)
        val nowInside = distance <= radius

        if (isInsideLapZone && !nowInside) {
            // Crossed the finish line (exiting)
            val lapEndTime = System.currentTimeMillis()
            if (lastLapStartTime > 0) {
                val lapTimeMillis = lapEndTime - lastLapStartTime
                _lapTimes.add(formatTime(lapTimeMillis))
            }
            lastLapStartTime = lapEndTime
        }
        isInsideLapZone = nowInside
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val milliseconds = (millis % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION])
    fun startLocationUpdates() {
        startTime = System.currentTimeMillis() // Reset start time when tracking begins
        lastLapStartTime = startTime // Initialize last lap start time
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

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

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        generateAndSaveGpx()
    }


    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }



}
