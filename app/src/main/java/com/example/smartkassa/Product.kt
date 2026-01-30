package com.example.smartkassa

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val stock: Double = 0.0,
    val category: String = "",
    val barcode: String? = null,
    val costPrice: Double = 0.0,
    val isIngredient: Boolean = false,
    val unit: String = "шт"
) {
    // Метод для конвертации между единицами измерения
    fun convertTo(newUnit: String, conversionRate: Double = 1.0): Product {
        if (unit == newUnit) return this

        val newStock = when {
            unit == "кг" && newUnit == "г" -> stock * 1000.0
            unit == "г" && newUnit == "кг" -> stock / 1000.0
            unit == "л" && newUnit == "мл" -> stock * 1000.0
            unit == "мл" && newUnit == "л" -> stock / 1000.0
            else -> stock * conversionRate
        }

        // Пересчитываем себестоимость
        val newCostPrice = if (stock > 0) {
            (costPrice * stock) / newStock
        } else {
            costPrice
        }

        return this.copy(
            stock = newStock,
            unit = newUnit,
            costPrice = newCostPrice
        )
    }

}