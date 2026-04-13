package com.example.smartkassa

data class Recipe(
    val id: String,
    val productId: String,
    val ingredientId: String,
    val quantityNeeded: Double, // Количество ингредиента на 1 единицу товара
    val productName: String = "",
    val ingredientName: String = "",
    val productUnit: String = "шт",
    val ingredientUnit: String = "кг",
    val ingredientStock: Double = 0.0,
    val ingredientCost: Double = 0.0 // Себестоимость ингредиента за единицу
) {
    // Рассчитать стоимость этого ингредиента для одного товара
    fun calculateCostPerProduct(): Double = quantityNeeded * ingredientCost
}