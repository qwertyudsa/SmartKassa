package com.example.smartkassa

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun getCurrentTime(): String = timeFormat.format(Date())

    fun getCurrentDate(): String = dateFormat.format(Date())

    fun formatTime(date: Date): String = timeFormat.format(date)

    fun formatDate(date: Date): String = dateFormat.format(date)
}