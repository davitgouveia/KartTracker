package com.example.karttracker.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.karttracker.components.SessionSummaryViewModel
import java.util.Date
import java.util.Locale

@Composable
fun SessionSummaryScreen(
    sessionId: Long,
    navController: NavController,
    summaryViewModel: SessionSummaryViewModel = hiltViewModel()
) {
    // Collect the run session details based on the sessionId
    // You'll need a ViewModel for this screen to fetch data from Room
    val runSession by summaryViewModel.getRunSession(sessionId).collectAsState(initial = null)
    val laps by summaryViewModel.getLapsForSession(sessionId).collectAsState(initial = emptyList())
    val locationPoints by summaryViewModel.getLocationPointsForSession(sessionId).collectAsState(initial = emptyList())

    if (runSession == null) {
        CircularProgressIndicator() // Show loading indicator
        Text("Loading session data...")
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Run Summary", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            runSession?.let { session ->
                Text("Total Duration: ${formatTime(session.totalDurationMillis)}")
                Text("Start Time: ${java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(
                    Date(session.startTimeMillis)
                )}")
                // Display other session details
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Laps:", style = MaterialTheme.typography.headlineSmall)
            LazyColumn {
                items(laps) { lap ->
                    Text("Lap ${lap.lapNumber}: ${lap.formattedTime}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Location Points Captured: ${locationPoints.size}", style = MaterialTheme.typography.headlineSmall)

            // You could integrate a map view here to show the path
            // Example:
            // GoogleMap(
            //     modifier = Modifier.fillMaxWidth().height(200.dp),
            //     cameraPositionState = rememberCameraPositionState {
            //         position = CameraPosition.fromLatLngZoom(LatLng(locationPoints.firstOrNull()?.latitude ?: 0.0, locationPoints.firstOrNull()?.longitude ?: 0.0), 15f)
            //     }
            // ) {
            //     val polylinePoints = locationPoints.map { LatLng(it.latitude, it.longitude) }
            //     Polyline(points = polylinePoints)
            // }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("mainScreen") }) {
                Text("Back to Home")
            }
        }
    }
}

// Helper function for formatting time (can be in a separate utility file)
fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val milliseconds = (millis % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
}