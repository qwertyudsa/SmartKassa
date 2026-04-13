package com.example.smartkassa

data class IncomeItem(
    val productId: String,
    val productName: String,
    val costPerUnit: Double,
    var quantity: Int,
    var total: Double,
    val unit: String
)