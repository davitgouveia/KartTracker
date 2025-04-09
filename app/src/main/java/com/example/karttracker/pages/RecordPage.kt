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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Preview
@Composable
fun GForceMeter() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    var gForce by remember { mutableStateOf(Offset(0f, 0f)) }

    DisposableEffect(Unit) {
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val listener = object : SensorEventListener {
            var gravity = FloatArray(3)
            var linearAcceleration = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val alpha = 0.8f

                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                    linearAcceleration[0] = event.values[0] - gravity[0]
                    linearAcceleration[1] = event.values[1] - gravity[1]
                    linearAcceleration[2] = event.values[2] - gravity[2]

                    val gx = linearAcceleration[0] / 9.81f
                    val gy = linearAcceleration[2] / 9.81f // agora usamos Y real aqui

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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val gPerPixel = size.width / 2 / 3f // 1G por círculo

                // Desenha círculos concêntricos
                for (i in 1..3) {
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.3f),
                        center = center,
                        radius = gPerPixel * i,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Vetor único representando direção e intensidade
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
            text = "%.2f".format(magnitude),
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "X: %.2f  Y: %.2f".format( gForce.x, gForce.y),
            style = MaterialTheme.typography.bodyLarge,
        )
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