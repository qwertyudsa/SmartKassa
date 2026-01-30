package com.example.smartkassa

import java.util.Date

data class IncomeRecord(
    val id: String,
    val date: Date,
    val supplierId: String,
    val invoiceNumber: String = "",
    val items: List<IncomeItem>,
    val totalCost: Double
)