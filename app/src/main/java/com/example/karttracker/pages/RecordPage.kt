package com.example.karttracker.pages

import SpeedViewModel
import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.karttracker.components.GForceMeter

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
@Composable
fun Speedometer(viewModel: SpeedViewModel = viewModel()) {
    val speed = viewModel.speed.collectAsState()

    Text(
        text = "${speed.value.toInt()} km/h",
        fontSize = 60.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@Composable
fun RecordPage(navController: NavController, modifier: Modifier = Modifier){
    val speedViewModel: SpeedViewModel = viewModel()
    LaunchedEffect(Unit) {
        speedViewModel.startLocationUpdates()
    }
    Column (
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Speedometer(viewModel = speedViewModel)
        GForceMeter()
    }
}

