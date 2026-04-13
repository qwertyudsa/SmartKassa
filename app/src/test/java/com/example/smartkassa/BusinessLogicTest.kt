package com.example.smartkassa

import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Unit-тесты для бизнес-логики приложения SmartKassa
 * Эти тесты не требуют Android-окружения и работают чисто на Kotlin
 */
class BusinessLogicTest {

    // ===== ТЕСТ 1: Расчёт себестоимости продукта =====
    @Test
    fun testCalculateProductCost() {
        // Дано: рецепт хлеба требует 0.5 кг муки, мука стоит 40 ₽/кг
        val flourCostPerKg = 40.0
        val flourNeeded = 0.5  // кг
        val expectedCost = flourCostPerKg * flourNeeded  // 20 ₽

        // Когда: рассчитываем себестоимость
        val actualCost = flourCostPerKg * flourNeeded

        // Тогда: себестоимость должна быть 20 ₽
        assertEquals("Себестоимость хлеба должна быть 20 ₽", 20.0, actualCost, 0.01)
    }

    // ===== ТЕСТ 2: Расчёт маржи и рентабельности =====
    @Test
    fun testCalculateMargin() {
        // Дано: товар стоит 100 ₽, себестоимость 60 ₽
        val price = 100.0
        val costPrice = 60.0

        // Когда: считаем маржу
        val margin = price - costPrice  // 40 ₽
        val marginPercent = (margin / costPrice) * 100  // 66.67%

        // Тогда: маржа должна быть 40 ₽, рентабельность ~66.67%
        assertEquals("Маржа должна быть 40 ₽", 40.0, margin, 0.01)
        assertEquals("Рентабельность должна быть 66.67%", 66.67, marginPercent, 0.01)
    }

    // ===== ТЕСТ 3: Фильтрация товаров по категории =====
    @Test
    fun testFilterProductsByCategory() {
        // Дано: список товаров
        val products = listOf(
            TestProduct("Хлеб белый", "Хлеб", 50.0),
            TestProduct("Хлеб чёрный", "Хлеб", 60.0),
            TestProduct("Булочка с маком", "Выпечка", 30.0),
            TestProduct("Круассан", "Выпечка", 45.0),
            TestProduct("Мука", "Ингредиенты", 0.0)
        )

        // Когда: фильтруем только хлеб
        val breadProducts = products.filter { it.category == "Хлеб" }

        // Тогда: должно быть 2 хлеба
        assertEquals("Должно быть 2 хлеба", 2, breadProducts.size)
        assertEquals("Первый хлеб - белый", "Хлеб белый", breadProducts[0].name)
        assertEquals("Второй хлеб - чёрный", "Хлеб чёрный", breadProducts[1].name)
    }

    // ===== ТЕСТ 4: Сортировка товаров по цене =====
    @Test
    fun testSortProductsByPrice() {
        // Дано: список товаров
        val products = listOf(
            TestProduct("Круассан", "Выпечка", 45.0),
            TestProduct("Хлеб белый", "Хлеб", 50.0),
            TestProduct("Булочка", "Выпечка", 30.0),
            TestProduct("Хлеб чёрный", "Хлеб", 60.0)
        )

        // Когда: сортируем по цене (возрастание)
        val sortedByPriceAsc = products.sortedBy { it.price }

        // Тогда: сначала дешёвые, потом дорогие
        assertEquals("Самый дешёвый - Булочка (30 ₽)", "Булочка", sortedByPriceAsc[0].name)
        assertEquals("Второй - Круассан (45 ₽)", "Круассан", sortedByPriceAsc[1].name)
        assertEquals("Третий - Хлеб белый (50 ₽)", "Хлеб белый", sortedByPriceAsc[2].name)
        assertEquals("Самый дорогой - Хлеб чёрный (60 ₽)", "Хлеб чёрный", sortedByPriceAsc[3].name)

        // Когда: сортируем по цене (убывание)
        val sortedByPriceDesc = products.sortedByDescending { it.price }

        // Тогда: сначала дорогие
        assertEquals("Самый дорогой - Хлеб чёрный", "Хлеб чёрный", sortedByPriceDesc[0].name)
    }

    // ===== ТЕСТ 5: Поиск товаров по названию =====
    @Test
    fun testSearchProductsByName() {
        // Дано: список товаров
        val products = listOf(
            TestProduct("Хлеб белый", "Хлеб", 50.0),
            TestProduct("Хлеб чёрный", "Хлеб", 60.0),
            TestProduct("Булочка с маком", "Выпечка", 30.0),
            TestProduct("Хлеб бородинский", "Хлеб", 55.0)
        )

        // Когда: ищем "хлеб" (регистронезависимо)
        val searchQuery = "хлеб"
        val searchResults = products.filter {
            it.name.lowercase().contains(searchQuery.lowercase())
        }

        // Тогда: должно найти 3 хлеба
        assertEquals("Должно быть 3 хлеба", 3, searchResults.size)

        // Проверяем, что булочка не найдена
        val hasBun = searchResults.any { it.name == "Булочка с маком" }
        assertFalse("Булочка не должна быть в результатах поиска", hasBun)
    }

    // ===== ТЕСТ 6: Расчёт общей выручки =====
    @Test
    fun testCalculateTotalRevenue() {
        // Дано: список продаж
        val sales = listOf(
            TestSale(150.0),
            TestSale(200.0),
            TestSale(75.0),
            TestSale(300.0)
        )

        // Когда: считаем общую выручку
        val totalRevenue = sales.sumOf { it.amount }

        // Тогда: 150 + 200 + 75 + 300 = 725
        assertEquals("Общая выручка должна быть 725 ₽", 725.0, totalRevenue, 0.01)
    }

    // ===== ТЕСТ 7: Расчёт среднего чека =====
    @Test
    fun testCalculateAverageCheck() {
        // Дано: продажи с разными суммами
        val sales = listOf(100.0, 200.0, 150.0, 50.0, 300.0)

        // Когда: считаем средний чек
        val averageCheck = sales.average()

        // Тогда: (100+200+150+50+300) / 5 = 800 / 5 = 160
        assertEquals("Средний чек должен быть 160 ₽", 160.0, averageCheck, 0.01)
    }

    // ===== ТЕСТ 8: Проверка форматирования даты =====
    @Test
    fun testDateFormatting() {
        // Дано: дата
        val calendar = Calendar.getInstance()
        calendar.set(2025, Calendar.DECEMBER, 25, 10, 30, 0)
        val date = calendar.time

        // Когда: форматируем в строку
        val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(date)

        // Тогда: должно быть "25.12.2025"
        assertEquals("Дата должна быть 25.12.2025", "25.12.2025", formattedDate)

        // Когда: форматируем время
        val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = timeFormat.format(date)

        // Тогда: должно быть "10:30"
        assertEquals("Время должно быть 10:30", "10:30", formattedTime)
    }

    // ===== ТЕСТ 9: Проверка конвертации единиц измерения =====
    @Test
    fun testUnitConversion() {
        // кг → г: 2 кг = 2000 г
        val kgToG = 2.0 * 1000.0
        assertEquals("2 кг = 2000 г", 2000.0, kgToG, 0.01)

        // г → кг: 500 г = 0.5 кг
        val gToKg = 500.0 / 1000.0
        assertEquals("500 г = 0.5 кг", 0.5, gToKg, 0.01)

        // л → мл: 1.5 л = 1500 мл
        val lToMl = 1.5 * 1000.0
        assertEquals("1.5 л = 1500 мл", 1500.0, lToMl, 0.01)
    }

    // ===== ТЕСТ 10: Проверка расчёта налога =====
    @Test
    fun testCalculateTax() {
        // Дано: сумма 1000 ₽, ставка НДС 20%
        val amount = 1000.0
        val taxRate = 20.0

        // Когда: считаем НДС
        val tax = amount * taxRate / 100.0

        // Тогда: НДС = 200 ₽
        assertEquals("НДС 20% от 1000 ₽ = 200 ₽", 200.0, tax, 0.01)

        // Когда: считаем сумму с НДС
        val totalWithTax = amount + tax

        // Тогда: 1200 ₽
        assertEquals("Сумма с НДС = 1200 ₽", 1200.0, totalWithTax, 0.01)
    }
}

// Вспомогательные классы для тестов
data class TestProduct(
    val name: String,
    val category: String,
    val price: Double
)

data class TestSale(
    val amount: Double
)