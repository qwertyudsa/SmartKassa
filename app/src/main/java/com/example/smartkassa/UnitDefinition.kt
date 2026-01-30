package com.example.smartkassa

data class UnitDefinition(
    val id: String = "",
    val name: String, // название единицы (кг, шт и т.д.)
    val category: String, // категория: вес, объем, штучный
    val baseUnit: String, // базовая единица для этой категории
    val conversionRate: Double = 1.0 // коэффициент относительно базовой единицы
) {
    companion object {
        // Стандартные категории
        const val CATEGORY_WEIGHT = "weight"
        const val CATEGORY_VOLUME = "volume"
        const val CATEGORY_PIECE = "piece"

        // Стандартные базовые единицы
        const val BASE_KG = "кг"
        const val BASE_LITER = "л"
        const val BASE_PIECE = "шт"
    }
}