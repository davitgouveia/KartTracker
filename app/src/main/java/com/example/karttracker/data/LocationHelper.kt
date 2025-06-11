package com.example.karttracker.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.Locale

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    private val _currentCity = MutableStateFlow("Loading city...")
    val currentCity: StateFlow<String> = _currentCity.asStateFlow()

    fun fetchCurrentCity() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            _currentCity.value = "Location permission needed"
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    try {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            // Prioritize locality (city), then adminArea (state/province), else fallback
                            _currentCity.value = addresses[0].locality ?: addresses[0].adminArea ?: "Unknown City"
                            Log.d("LocationHelper", "Found city: ${_currentCity.value}")
                        } else {
                            _currentCity.value = "City not found for coordinates"
                            Log.w("LocationHelper", "No addresses found for ${it.latitude}, ${it.longitude}")
                        }
                    } catch (e: IOException) {
                        _currentCity.value = "Network error"
                        Log.e("LocationHelper", "Network error getting city: ${e.message}")
                    } catch (e: IllegalArgumentException) {
                        _currentCity.value = "Invalid coordinates"
                        Log.e("LocationHelper", "Invalid lat/lng for city: ${e.message}")
                    }
                } ?: run {
                    _currentCity.value = "Location unavailable"
                    Log.w("LocationHelper", "Last known location is null.")
                }
            }
            .addOnFailureListener { e ->
                _currentCity.value = "Error fetching location"
                Log.e("LocationHelper", "Error getting location: ${e.message}", e)
            }
    }
}