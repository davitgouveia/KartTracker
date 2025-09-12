package com.example.karttracker.utils

import java.util.Calendar
import java.util.TimeZone

object TimeUtils {

    /**
    * Formats milliseconds to 00:00:00
    */
    fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val milliseconds = (millis % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
    }

    fun getPeriodOfDay(millis: Long): String {
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = millis
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY) // 0..23

        return when (hour) {
            in 5..11 -> "Morning"
            in 12..17 -> "Afternoon"
            else -> "Night"
        }
    }

}