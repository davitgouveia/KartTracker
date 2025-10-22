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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.karttracker.components.Session.SessionDataBlock
import com.example.karttracker.components.SessionSummaryViewModel
import com.example.karttracker.database.entity.LapEntity
import com.example.karttracker.utils.TimeUtils
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Locale

val FastestLapPurple = Color(0xFF800080) // Standard F1 purple
val Lap1Color = Color.Blue // Example color for the first selected lap
val Lap2Color = Color.Red // Example color for the second selected lap

@OptIn(ExperimentalMaterial3Api::class)
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

    var sessionName by remember { mutableStateOf("") }
    var savedSessionName by remember { mutableStateOf("") }
    var sessionNamePlaceholder by remember { mutableStateOf("") }
    var isEditingName by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    val mapProperties = remember {
        MapProperties(
            mapType = MapType.SATELLITE)
    }



    LaunchedEffect(runSession) {

        runSession?.let {
            sessionNamePlaceholder = "${TimeUtils.getPeriodOfDay(it.startTimeMillis)} Session"

            savedSessionName = it.name

            sessionName = if (it.name.isNotBlank()) { it.name } else { sessionNamePlaceholder }

            isEditingName = false
        }
    }

    MaterialTheme {
        if (runSession == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CircularProgressIndicator()
                Text("Loading session data...")
            }
        } else {
            Scaffold(
                topBar = {
                    CustomTopBar(title = sessionName, backAction = { navController.navigate("mainScreen") })
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    runSession?.let { session ->
                        Row {
                            TextField(
                                value = sessionName,
                                onValueChange = { newValue ->



                                    isEditingName = savedSessionName != newValue.trim()

                                    sessionName = newValue


                                },
                                label = { Text("Session Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (isEditingName) {
                                Button(
                                    onClick = {
                                        summaryViewModel.updateSessionName(session, sessionName)
                                        isEditingName = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Name")
                                }
                            }

                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if ( selectedLapsWithPoints.size <= 2) {
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
                                        .height(240.dp)
                                ) {
                                    GoogleMap(
                                        modifier = Modifier.fillMaxSize(), // Make the map fill the Box
                                        cameraPositionState = rememberCameraPositionState {
                                            position = CameraPosition.fromLatLngZoom(initialLatLng, 15f)
                                        },
                                        properties = mapProperties
                                    ) {
                                        // ONLY place map elements (Polyline, Marker, etc.) here
                                        if (selectedLapsWithPoints.size == 2) {
                                            selectedLapsWithPoints.sortedBy { selectedLapData -> selectedLapData.isFastestOfSelected }.forEachIndexed { index, selectedLapData ->
                                                val polylineColor = when {
                                                    selectedLapData.isFastestOfSelected -> FastestLapPurple
                                                    else -> Lap1Color
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
                                                color = Lap1Color,
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
                                                .background(
                                                    Color.White.copy(alpha = 0.7f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(4.dp)
                                        ) {
                                            selectedLapsWithPoints.forEachIndexed { index, selectedLapData ->
                                                val color = when {
                                                    selectedLapData.isFastestOfSelected -> FastestLapPurple
                                                    else -> Lap1Color
                                                }
                                                Text(
                                                    text = "Lap ${selectedLapData.lap.lapNumber}: ${TimeUtils.formatTime(selectedLapData.lap.durationMillis)}",
                                                    color = color,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else if (selectedLapsWithPoints.size == 1) {
                                        val selectedLapData = selectedLapsWithPoints.first()
                                        Text(
                                            text = "Lap ${selectedLapData.lap.lapNumber}: ${TimeUtils.formatTime(selectedLapData.lap.durationMillis)}",
                                            color = Lap1Color,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .align(Alignment.TopStart) // Align to top-start of the Box
                                                .padding(8.dp)
                                                .background(
                                                    Color.White.copy(alpha = 0.5f),
                                                    RoundedCornerShape(4.dp)
                                                )
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier
                            .fillMaxWidth(1f)
                            .padding(16.dp, 0.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            SessionDataBlock("Practice", "🏁", "${session.lapCount} Laps")
                            SessionDataBlock("Duration", "⏱", TimeUtils.formatTime(session.totalDurationMillis))
                            SessionDataBlock("Avg. lap time", "⚡", TimeUtils.formatTime(session.avgLapTimeMillis))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            if(laps.isEmpty()){
                                Text("No laps completed", style = MaterialTheme.typography.headlineSmall)
                            } else {
                                Text("Laps:", style = MaterialTheme.typography.headlineSmall)

                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = { summaryViewModel.clearLapSelection() },
                                    // Apply alpha based on whether laps are selected
                                    modifier = Modifier
                                        .alpha(if (selectedLapIds.isNotEmpty()) 1f else 0f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear selected laps",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Clear laps",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                        }
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




                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "GPS Points: ${locationPoints.size}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { navController.navigate("mainScreen") }) {
                                Text("Go back",  style = MaterialTheme.typography.bodySmall)
                            }

                        }
                    }
                }
            }
        }
    }
}
@Preview
@Composable
fun CustomTopBar (
    title: String = "Unknown",
    backAction: () -> Unit = {},
    menuAction: () -> Unit = {}
){
    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(0.dp, 4.dp)) {

        IconButton(
            onClick = backAction,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }


        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleMedium
        )


        IconButton(
            onClick = menuAction,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
    }
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
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Lap ${lap.lapNumber}: ${TimeUtils.formatTime(lap.durationMillis)}",
                    color = textColor,
                    fontWeight = fontWeight,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (isFastestOverall) {
                    Text(
                        text = "(Fastest Lap)",
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