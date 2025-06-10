package com.example.karttracker.components

import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

@Composable
fun GForceMeter() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    var gForce by remember { mutableStateOf(Offset(0f, 0f)) }

    DisposableEffect(Unit) {
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        // Moving Average Filter parameters
        val windowSize = 10 // Number of samples to average. Experiment with this value.
        val xBuffer = mutableListOf<Float>()
        val yBuffer = mutableListOf<Float>()
        val zBuffer = mutableListOf<Float>()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Add current values to buffers
                    xBuffer.add(event.values[0])
                    yBuffer.add(event.values[1])
                    zBuffer.add(event.values[2])

                    // Keep buffer size within windowSize
                    if (xBuffer.size > windowSize) xBuffer.removeAt(0)
                    if (yBuffer.size > windowSize) yBuffer.removeAt(0)
                    if (zBuffer.size > windowSize) zBuffer.removeAt(0)

                    // Calculate average
                    val avgX = if (xBuffer.isNotEmpty()) xBuffer.average().toFloat() else 0f
                    val avgY = if (yBuffer.isNotEmpty()) yBuffer.average().toFloat() else 0f
                    val avgZ = if (zBuffer.isNotEmpty()) zBuffer.average().toFloat() else 0f

                    // Normalized acceleration
                    val rawX = avgX / 9.81f
                    val rawY = avgY / 9.81f
                    val rawZ = avgZ / 9.81f

                    // Adjust axes based on orientation
                    val gx: Float
                    val gy: Float
                    if (isLandscape) {
                        gx = rawY
                        gy = rawZ
                    } else {
                        gx = rawX
                        gy = rawZ
                    }

                    gForce = Offset(gx, gy)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME) // SENSOR_DELAY_GAME provides a good balance
        // You might also try SENSOR_DELAY_FASTEST if you need more samples for averaging,
        // but be mindful of battery consumption.

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp), // was 300.dp
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val gPerPixel = size.width / 2 / 3f

                for (i in 1..3) {
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.3f),
                        center = center,
                        radius = gPerPixel * i,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                val ballRadius = 10.dp.toPx()
                val ballOffset = Offset(
                    center.x + (gForce.x * gPerPixel),
                    center.y - (gForce.y * gPerPixel)
                )

                drawCircle(
                    color = Color.Red,
                    center = ballOffset,
                    radius = ballRadius
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val magnitude = sqrt(gForce.x * gForce.x + gForce.y * gForce.y)

        Text(
            text = "%.1f".format(magnitude),
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
        )/*
        Text(
            text = "X: %.2f  Y: %.2f".format(gForce.x, gForce.y),
            style = MaterialTheme.typography.bodyLarge,
        )*/
    }
}