package com.example.smartkassa

data class SaleItem(
    val productId: String,
    val productName: String,
    val price: Double,
    var quantity: Int,
    var total: Double
)