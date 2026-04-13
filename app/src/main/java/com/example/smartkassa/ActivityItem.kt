package com.example.smartkassa

import java.util.Date

data class ActivityItem(
    val id: String,
    val type: String,
    val description: String,
    val amount: String,
    val time: String,
    val date: Date = Date()
)
