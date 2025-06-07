package com.example.karttracker.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.karttracker.components.DefaultLayout
import com.example.karttracker.components.SessionSummaryViewModel
import com.example.karttracker.database.entity.LapEntity
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionSummaryScreen(
    sessionId: Long,
    navController: NavController,
    summaryViewModel: SessionSummaryViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId) {
        summaryViewModel.setSessionId(sessionId)
    }

    val runSession by summaryViewModel.getRunSession(sessionId).collectAsState(initial = null)
    val laps by summaryViewModel.getLapsForSession(sessionId).collectAsState(initial = emptyList())
    val fastestLapId by summaryViewModel.currentFastestLapId.collectAsState(initial = null)
    val locationPoints by summaryViewModel.getLocationPointsForSession(sessionId)
        .collectAsState(initial = emptyList())

    var sessionName by remember { mutableStateOf("") }
    var isEditingName by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    LaunchedEffect(runSession) {
        runSession?.let {
            sessionName = if (it.name.isBlank()) {
                "Run on ${dateFormat.format(Date(it.startTimeMillis))}"
            } else {
                it.name
            }
            // Reset editing state if the session data changes (e.g., after save)
            isEditingName = false
        }
    }

    MaterialTheme {
        DefaultLayout(title = "Session Summary") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                LaunchedEffect(runSession) {
                    runSession?.let {
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        sessionName = if (it.name.isBlank()) {
                            "Run on ${dateFormat.format(java.util.Date(it.startTimeMillis))}"
                        } else {
                            it.name
                        }
                    }
                }
                if (runSession == null) {
                    CircularProgressIndicator() // Show loading indicator
                    Text("Loading session data...")
                } else {
                        runSession?.let { session ->
                            OutlinedTextField(
                                value = sessionName,
                                onValueChange = { newValue ->
                                    sessionName = newValue
                                    val currentDisplayNameIfBlank = "Run on ${dateFormat.format(Date(session.startTimeMillis))}"
                                    val isCurrentlyDefaultDisplay = session.name.isBlank() && sessionName == currentDisplayNameIfBlank

                                    if (newValue.trim() != session.name.trim() || (isCurrentlyDefaultDisplay && !newValue.isBlank())) {
                                        isEditingName = true
                                    } else if (newValue.isBlank() && !session.name.isBlank()){
                                        isEditingName = true
                                    }
                                    else {
                                        isEditingName = false
                                    }
                                },
                                label = { Text("Session Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (isEditingName) {
                                Button(
                                    onClick = {
                                        summaryViewModel.updateSessionName(session, sessionName)
                                        isEditingName = false // Reset editing state
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Name")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Total Duration: ${formatTime(session.totalDurationMillis)}")
                            Text("Start Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(session.startTimeMillis))}"
                            )
                            // Display other session details
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                    Text("Laps:", style = MaterialTheme.typography.headlineSmall)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(laps) { lap ->
                            // Determine if this is the fastest lap
                            val isFastestLap = lap.id == fastestLapId

                            LapItem(lap = lap, isFastestLap = isFastestLap)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Location Points Captured: ${locationPoints.size}",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    // Google Map integration
                    if (locationPoints.isNotEmpty()) {
                        val initialLatLng = LatLng(
                            locationPoints.first().latitude,
                            locationPoints.first().longitude
                        )
                        GoogleMap(
                           modifier = Modifier.fillMaxWidth().height(200.dp),
                          cameraPositionState = rememberCameraPositionState {
                              position = CameraPosition.fromLatLngZoom(LatLng(locationPoints.firstOrNull()?.latitude ?: 0.0, locationPoints.firstOrNull()?.longitude ?: 0.0), 15f)
                            }
                       ) {
                            val polylinePoints = locationPoints.map { LatLng(it.latitude, it.longitude) }
                         Polyline(points = polylinePoints)
                     }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.navigate("mainScreen") }) {
                            Text("Back to Home")
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val milliseconds = (millis % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
}

@Composable
fun LapItem(lap: LapEntity, isFastestLap: Boolean) {
    val backgroundColor = if (isFastestLap) Color(0xFF800080) else Color.Transparent // Purple color
    val textColor = if (isFastestLap) Color.White else MaterialTheme.colorScheme.onSurface
    val fontWeight = if (isFastestLap) FontWeight.Bold else FontWeight.Normal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp) // Add padding for visual separation
    ) {
        Text(
            text = "Lap ${lap.lapNumber}: ${formatTime(lap.durationMillis)}",
            color = textColor,
            fontWeight = fontWeight,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
// You can add more lap details here if LapEntity has them
// For example: Text("Distance: ${lap.distance}", color = textColor)
