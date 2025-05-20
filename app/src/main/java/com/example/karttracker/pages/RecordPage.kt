package com.example.karttracker.pages

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.karttracker.components.GForceMeter

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.karttracker.components.LapViewModel


@Composable
fun Speedometer(viewModel: LapViewModel = viewModel()) {
    val speed = viewModel.speed.collectAsState()

    Text(
        text = "${speed.value.toInt()} km/h",
        fontSize = 60.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun LabelValue(label: String, value: String, color: Color = Color.Black) {
    Column {
        Text(text = label, fontSize = 14.sp)
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// Extension functions to format time and delta
fun Double.formatTime(): String = String.format("%.3f", this)
fun Double.formatDelta(): String = (if (this >= 0) "+%.3f" else "%.3f").format(this)

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION])
@Composable
fun RecordPage(
    navController: NavController,
    modifier: Modifier = Modifier,
    lapViewModel: LapViewModel = hiltViewModel()
) {

    val lapTimes = lapViewModel.lapTimes
    val elapsedTime by lapViewModel.elapsedTime.collectAsState()
    val speed by lapViewModel.speed.collectAsState()
    val currentLapTime by lapViewModel.currentLapTime.collectAsState()
    val lastLap by lapViewModel.lastLap.collectAsState()
    val bestLap by lapViewModel.bestLap.collectAsState()
    val lapCount by lapViewModel.lapCount.collectAsState()
    val lapDelta by lapViewModel.lapDelta.collectAsState()

    LaunchedEffect(Unit) {
        lapViewModel.startLocationUpdates()
    }

    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Speed in the center top
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Text(
                text = "${speed.toInt()}",
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "km/h",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Lap info on the left
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            LabelValue("Best lap", bestLap)
            LabelValue("Last lap", lastLap)
            LabelValue("Current", currentLapTime)
            LabelValue("LAP", lapCount.toString())
            LabelValue(
                "DELTA - Best lap",
                lapDelta.formatDelta(),
                color = if (lapDelta < 0) Color.Green else Color.Red
            )
        }

        // Circle indicator and STOP button on the right
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .align(Alignment.CenterEnd).fillMaxHeight()
        ) {
            Box(contentAlignment = Alignment.Center) {
                GForceMeter()
            }

            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
            ) {
                Text("STOP", color = Color.Black)
            }
        }
    }
}

