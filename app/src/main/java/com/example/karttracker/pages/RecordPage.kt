package com.example.karttracker.pages

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.karttracker.components.GForceMeter

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION])
@Composable
fun RecordPage(
    navController: NavController,
    modifier: Modifier = Modifier,
    lapViewModel: LapViewModel = hiltViewModel()
) {
    val speed by lapViewModel.speed.collectAsState()
    val currentLapTime by lapViewModel.currentLapTime.collectAsState()
    val lapTimes = lapViewModel.lapTimes
    val elapsedTime by lapViewModel.elapsedTime.collectAsState()

    LaunchedEffect(Unit) {
        lapViewModel.startLocationUpdates()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            lapViewModel.stopLocationUpdates()
            // Trigger GPX data generation and saving here
        }) {
            Text("Stop Recording")
        }
        Text(
            text = "Elapsed Time: $elapsedTime",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = "${speed.toInt()} km/h",
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Current Lap:",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = currentLapTime,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Lap Times:",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxWidth(0.8f)) {
            items(lapTimes) { lapTime ->
                Text(text = lapTime, fontSize = 18.sp)
            }
        }
    }
}

