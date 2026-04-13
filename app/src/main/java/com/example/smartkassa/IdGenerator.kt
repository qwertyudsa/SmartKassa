package com.example.smartkassa

import java.util.UUID

object IdGenerator {
    fun generateId(): String = UUID.randomUUID().toString()
    fun generateSaleId(): String = "SALE_${System.currentTimeMillis()}_${(1000..9999).random()}"

    fun generateIncomeId(): String = "INC_${System.currentTimeMillis()}_${(1000..9999).random()}"
}