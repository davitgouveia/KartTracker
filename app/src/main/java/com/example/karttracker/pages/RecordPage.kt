package com.example.karttracker.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun GForceMeter() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    var gForce by remember { mutableStateOf(Offset(0f, 0f)) }

    // Sensor listener to update G-Force values
    DisposableEffect(Unit) {
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val gx = event.values[0] / 9.81f  // Convert to G
                    val gy = event.values[1] / 9.81f  // Convert to G
                    gForce = Offset(gx, gy)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width / 2

            // Draw circles for each G level (1G to 6G)
            for (i in 1..6) {
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.3f),
                    center = center,
                    radius = maxRadius * (i / 6f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                )
            }

            // Move the ball based on G-Force
            val ballRadius = 20.dp.toPx()
            val maxOffset = maxRadius - ballRadius
            val ballOffset = Offset(
                center.x + (gForce.x * maxOffset),
                center.y - (gForce.y * maxOffset)  // Negative Y since screen coords are flipped
            )

            // Draw moving ball
            drawCircle(
                color = Color.Red,
                center = ballOffset,
                radius = ballRadius
            )
        }
    }
}

@Composable
fun RecordPage(navController: NavController, modifier: Modifier = Modifier){
    Column (
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        GForceMeter()
    }
}