package com.example.smartkassa

import java.util.Date

data class Sale(
    val id: String,
    val date: Date,
    val items: List<SaleItem>,
    val totalAmount: Double,
    val paymentMethod: String = "Наличные",
    val userId: String
)
