package com.example.karttracker.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState


@Preview
@Composable
fun LapTrackerMapScreen() {
    val context = LocalContext.current
    val mapProperties = remember { MapProperties(isMyLocationEnabled = true) }
    val uiSettings = remember { MapUiSettings(myLocationButtonEnabled = true) }

    var markerPosition by remember { mutableStateOf<LatLng?>(null) }
    var radiusMeters by remember { mutableStateOf(20f) }

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
    ) {
        Text("Radius: ${radiusMeters.toInt()} meters")
        Slider(
            value = radiusMeters,
            onValueChange = { radiusMeters = it },
            valueRange = 5f..50f
        )
    }
}