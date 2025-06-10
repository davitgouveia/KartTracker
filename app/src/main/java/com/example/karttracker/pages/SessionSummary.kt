package com.example.karttracker.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

val FastestLapPurple = Color(0xFF800080) // Standard F1 purple
val Lap1Color = Color.Blue // Example color for the first selected lap
val Lap2Color = Color.Red // Example color for the second selected lap

@Composable
fun SessionSummaryScreen(
    sessionId: Long,
    navController: NavController,
    summaryViewModel: SessionSummaryViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId) {
        summaryViewModel.setSessionId(sessionId)
        Log.d("SummaryScreen", "Session ID set in LaunchedEffect: $sessionId")
    }

    val runSession by summaryViewModel.getRunSession(sessionId).collectAsState(initial = null)
    val laps by summaryViewModel.currentSessionLaps.collectAsState(initial = emptyList())
    val fastestLapId by summaryViewModel.currentFastestLapId.collectAsState(initial = null)
    val locationPoints by summaryViewModel.getLocationPointsForSession(sessionId) // For overall session path
        .collectAsState(initial = emptyList())

    val selectedLapIds by summaryViewModel.selectedLapIds.collectAsState()
    val selectedLapsWithPoints by summaryViewModel.selectedLapsWithPoints.collectAsState()

    LaunchedEffect(laps, fastestLapId, locationPoints, selectedLapIds, selectedLapsWithPoints) {
        Log.d("SummaryScreenData", "--- UI Data Update ---")
        Log.d("SummaryScreenData", "Total Laps: ${laps.size}")
        laps.forEach { lap ->
            Log.d("SummaryScreenData", "  Lap ${lap.lapNumber} (ID: ${lap.id}), Duration: ${lap.formattedTime}")
        }
        Log.d("SummaryScreenData", "Fastest Overall Lap ID: $fastestLapId")
        Log.d("SummaryScreenData", "Overall Session Location Points: ${locationPoints.size}")

        Log.d("SummaryScreenData", "Selected Lap IDs: $selectedLapIds")
        Log.d("SummaryScreenData", "Selected Laps With Points (size): ${selectedLapsWithPoints.size}")
        selectedLapsWithPoints.forEach { selectedLapData ->
            Log.d("SummaryScreenData", "  Selected Lap ${selectedLapData.lap.lapNumber} (ID: ${selectedLapData.lap.id}): ${selectedLapData.locationPoints.size} points. Is Fastest of Selected: ${selectedLapData.isFastestOfSelected}")
        }
        Log.d("SummaryScreenData", "----------------------")
    }

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
                            val isFastestOverall = lap.id == fastestLapId
                            val isSelected = selectedLapIds.contains(lap.id)
                            LapListItem(
                                lap = lap,
                                isFastestOverall = isFastestOverall,
                                isSelected = isSelected,
                                onSelectLap = {
                                    summaryViewModel.toggleLapSelection(lap.id)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Location Points Captured: ${locationPoints.size}",
                        style = MaterialTheme.typography.headlineSmall
                    )


                    if (laps.isNotEmpty() && selectedLapsWithPoints.size <= 2) {
                        val mapPoints = if (selectedLapsWithPoints.isNotEmpty()) {
                            selectedLapsWithPoints.flatMap { it.locationPoints }
                        } else {
                            locationPoints
                        }

                        if (mapPoints.isNotEmpty()) {
                            Log.d("MapRender", "Map is being rendered with ${mapPoints.size} points.")
                            val initialLatLng = LatLng(
                                mapPoints.first().latitude,
                                mapPoints.first().longitude
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)) // Clip the whole box, not just the map
                            ) {
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(), // Make the map fill the Box
                                    cameraPositionState = rememberCameraPositionState {
                                        position = CameraPosition.fromLatLngZoom(initialLatLng, 15f)
                                    }
                                ) {
                                    // ONLY place map elements (Polyline, Marker, etc.) here
                                    if (selectedLapsWithPoints.size == 2) {
                                        selectedLapsWithPoints.forEachIndexed { index, selectedLapData ->
                                            val polylineColor = when {
                                                selectedLapData.isFastestOfSelected -> FastestLapPurple
                                                index == 0 -> Lap1Color
                                                index == 1 -> Lap2Color
                                                else -> Color.Gray
                                            }
                                            Polyline(
                                                points = selectedLapData.locationPoints.map { LatLng(it.latitude, it.longitude) },
                                                color = polylineColor,
                                                width = 8f
                                            )
                                        }
                                    } else if (selectedLapsWithPoints.size == 1) {
                                        val selectedLapData = selectedLapsWithPoints.first()
                                        Polyline(
                                            points = selectedLapData.locationPoints.map { LatLng(it.latitude, it.longitude) },
                                            color = Lap1Color,
                                            width = 8f
                                        )
                                    } else {
                                        Polyline(
                                            points = locationPoints.map { LatLng(it.latitude, it.longitude) },
                                            color = Color.DarkGray,
                                            width = 6f
                                        )
                                    }
                                }

                                // Now, place your UI elements (legends, text) OVER the map within the Box
                                if (selectedLapsWithPoints.size == 2) {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.TopStart) // Align to top-start of the Box
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(4.dp)
                                    ) {
                                        selectedLapsWithPoints.forEachIndexed { index, selectedLapData ->
                                            val color = when {
                                                selectedLapData.isFastestOfSelected -> FastestLapPurple
                                                index == 0 -> Lap1Color
                                                index == 1 -> Lap2Color
                                                else -> Color.Gray
                                            }
                                            Text(
                                                text = "Lap ${selectedLapData.lap.lapNumber}: ${formatTime(selectedLapData.lap.durationMillis)}",
                                                color = color,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else if (selectedLapsWithPoints.size == 1) {
                                    val selectedLapData = selectedLapsWithPoints.first()
                                    Text(
                                        text = "Lap ${selectedLapData.lap.lapNumber}: ${formatTime(selectedLapData.lap.durationMillis)}",
                                        color = Lap1Color,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.TopStart) // Align to top-start of the Box
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(4.dp)
                                    )
                                }
                            }
                        } else {
                            Log.d("MapRender", "Map is NOT being rendered. mapPoints is empty.")
                            Text("No location data available for this session or selected laps.",
                                modifier = Modifier.padding(top = 8.dp))
                        }
                    } else if (laps.isNotEmpty() && selectedLapsWithPoints.size > 2) {
                        Text("You can select up to 2 laps for comparison.", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { summaryViewModel.clearLapSelection() }) {
                            Text("Clear Selection")
                        }
                    }


                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { navController.navigate("mainScreen") }) {
                            Text("Back to Home")
                        }
                        if (selectedLapIds.isNotEmpty()) {
                            IconButton(onClick = { summaryViewModel.clearLapSelection() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear selected laps")
                            }
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
fun LapListItem(
    lap: LapEntity,
    isFastestOverall: Boolean,
    isSelected: Boolean,
    onSelectLap: (LapEntity) -> Unit
) {

    val defaultLapColor = MaterialTheme.colorScheme.onSurface // For general text
    val backgroundColor = when {
        isFastestOverall -> FastestLapPurple.copy(alpha = 0.2f) // Lighter purple for list background
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) // Light primary for selected
        else -> Color.Transparent
    }
    val textColor = if (isFastestOverall) FastestLapPurple else defaultLapColor
    val fontWeight = if (isFastestOverall) FontWeight.Bold else FontWeight.Normal

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelectLap(lap) }, // Clickable for selection
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Lap ${lap.lapNumber}: ${formatTime(lap.durationMillis)}",
                    color = textColor,
                    fontWeight = fontWeight,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (isFastestOverall) {
                    Text(
                        text = "(Fastest Overall Lap)",
                        style = MaterialTheme.typography.bodySmall,
                        color = FastestLapPurple
                    )
                }
            }
            // Checkbox for selection
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectLap(lap) }, // Toggle selection on checkbox click
                enabled = true // Always allow checking/unchecking
            )
        }
    }
}
// You can add more lap details here if LapEntity has them
// For example: Text("Distance: ${lap.distance}", color = textColor)
