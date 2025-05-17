package com.example.karttracker.components

import android.Manifest
import android.app.Application
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateListOf
import com.google.android.gms.location.LocationRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.example.karttracker.pages.RADIUS_ARG
import com.example.karttracker.pages.START_FINISH_LAT_ARG
import com.example.karttracker.pages.START_FINISH_LNG_ARG
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                updateSpeed(location)
                checkLapCompletion(location)
                updateElapsedTime()
            }
        }
    }

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

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
