package com.example.karttracker.helpers.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import android.util.Log
import androidx.compose.ui.graphics.Color
import java.io.File
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun drawPathToImage(
    context: Context,
    points: List<Location>,
    sessionId: Long
): String?  = withContext(Dispatchers.IO){
    try {
        if (points.size < 2) {
            Log.w("RunSession", "Not enough points to draw path for session $sessionId")
            return@withContext null
        }

    val width = 800
    val height = 600
    val padding = 40f
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.BLUE
        strokeWidth = 5f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    // Normalize coordinates to image space
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLng = points.minOf { it.longitude }
    val maxLng = points.maxOf { it.longitude }

    fun mapX(lng: Double) = padding + ((lng - minLng) / (maxLng - minLng) * (width - 2 * padding)).toFloat()
    fun mapY(lat: Double) = padding + ((maxLat - lat) / (maxLat - minLat) * (height - 2 * padding)).toFloat()


    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        canvas.drawLine(mapX(p1.longitude), mapY(p1.latitude), mapX(p2.longitude), mapY(p2.latitude), paint)
    }

    val file = File(context.filesDir, "run_session_$sessionId.png")
    Log.d("drawPathToImage", "File created")
    file.outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
    }

        Log.d("RunSession", "✅ Saved path image at: ${file.absolutePath}")
        return@withContext file.absolutePath
    } catch (e: Exception) {
        Log.e("RunSession", "❌ Failed to draw path image", e)
        return@withContext null
    }
}