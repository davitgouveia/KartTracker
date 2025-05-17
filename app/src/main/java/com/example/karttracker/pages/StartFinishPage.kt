package com.example.karttracker.pages

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

const val START_FINISH_LAT_ARG = "startFinishLat"
const val START_FINISH_LNG_ARG = "startFinishLng"
const val RADIUS_ARG = "radius"

@Composable
fun LapTrackerMapScreen(navController: NavHostController) {
    val context = LocalContext.current
    val mapProperties = remember { MapProperties(isMyLocationEnabled = true) }
    val uiSettings = remember { MapUiSettings(myLocationButtonEnabled = true) }

    var markerPosition by remember { mutableStateOf<LatLng?>(null) }
    var radiusMeters by remember { mutableStateOf(20f) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // Permission granted, no need to do anything here, the map will handle it
        } else {
            Toast.makeText(context, "Location permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (requiredPermissions.any { ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED }) {
            requestPermissionLauncher.launch(requiredPermissions)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            properties = mapProperties,
            uiSettings = uiSettings,
            onMapClick = { latLng -> markerPosition = latLng }
        ) {
            markerPosition?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Start/Finish"
                )
                Circle(
                    center = it,
                    radius = radiusMeters.toDouble(),
                    strokeColor = Color.Red,
                    strokeWidth = 4f,
                    fillColor = Color(0x44FF0000) // semi-transparent red
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.8f))
                .padding(8.dp)
                .align(Alignment.BottomCenter)
        ) {
            Text("Radius: ${radiusMeters.toInt()} meters")
            Slider(
                value = radiusMeters,
                onValueChange = { radiusMeters = it },
                valueRange = 5f..50f
            )
            Button(
                onClick = {
                    markerPosition?.let {
                        navController.navigate("record/${it.latitude}/${it.longitude}/${radiusMeters.toInt()}")
                    } ?: run {
                        Toast.makeText(context, "Please select the start/finish line on the map", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = markerPosition != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Tracking")
            }
        }
    }
}