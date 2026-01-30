package com.example.smartkassa

data class User(
    val id: String,
    val email: String,
    val password: String,
    val businessName: String = "Мой бизнес",
    val currency: String = "RUB",
    val taxRate: Double = 20.0
)