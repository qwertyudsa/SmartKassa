package com.example.smartkassa

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.remote.creation.compose.state.abs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.abs

class ReportsActivity : AppCompatActivity() {

    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvTotalSales: TextView
    private lateinit var tvAverageCheck: TextView
    private lateinit var tvProfit: TextView
    private lateinit var rvPopularProducts: RecyclerView
    private lateinit var btnToday: MaterialButton
    private lateinit var btnWeek: MaterialButton
    private lateinit var btnMonth: MaterialButton
    private lateinit var btnCustom: MaterialButton
    private lateinit var btnExportJson: MaterialButton
    private lateinit var btnExportExcel: MaterialButton

    private lateinit var dbHelper: DatabaseHelper

    // Период отчета
    private var periodType: PeriodType = PeriodType.TODAY
    private var startDate: Long = 0
    private var endDate: Long = 0

    enum class PeriodType {
        TODAY, WEEK, MONTH, CUSTOM
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        // Инициализируем DatabaseHelper
        dbHelper = DatabaseHelper(this)

        initViews()
        setupClickListeners()
        setupPeriod(PeriodType.TODAY)
        loadReportData()
    }

    private fun initViews() {
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue)
        tvTotalSales = findViewById(R.id.tvTotalSales)
        tvAverageCheck = findViewById(R.id.tvAverageCheck)
        tvProfit = findViewById(R.id.tvProfit)
        rvPopularProducts = findViewById(R.id.rvPopularProducts)
        btnToday = findViewById(R.id.btnToday)
        btnWeek = findViewById(R.id.btnWeek)
        btnMonth = findViewById(R.id.btnMonth)
        btnCustom = findViewById(R.id.btnCustom)
        btnExportJson = findViewById(R.id.btnExportJson)
        btnExportExcel = findViewById(R.id.btnExportExcel)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Отчеты"
    }

    private fun setupClickListeners() {
        btnToday.setOnClickListener { setupPeriod(PeriodType.TODAY) }
        btnWeek.setOnClickListener { setupPeriod(PeriodType.WEEK) }
        btnMonth.setOnClickListener { setupPeriod(PeriodType.MONTH) }
        btnCustom.setOnClickListener { showDatePicker() }
        btnExportJson.setOnClickListener { exportToJson() }
        btnExportExcel.setOnClickListener { exportToExcel() }
    }

    private fun setupPeriod(period: PeriodType) {
        periodType = period
        val calendar = Calendar.getInstance()

        when (period) {
            PeriodType.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis

                calendar.add(Calendar.DAY_OF_MONTH, 1)
                endDate = calendar.timeInMillis

                updatePeriodButtons(btnToday)
            }

            PeriodType.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis

                calendar.add(Calendar.DAY_OF_MONTH, 7)
                endDate = calendar.timeInMillis

                updatePeriodButtons(btnWeek)
            }

            PeriodType.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis

                calendar.add(Calendar.MONTH, 1)
                endDate = calendar.timeInMillis

                updatePeriodButtons(btnMonth)
            }

            PeriodType.CUSTOM -> {
                // Для кастомного периода даты будут установлены через datePicker
                updatePeriodButtons(btnCustom)
            }
        }

        loadReportData()
    }

    private fun updatePeriodButtons(activeButton: MaterialButton) {
        // Сброс стилей всех кнопок
        val buttons = listOf(btnToday, btnWeek, btnMonth, btnCustom)
        buttons.forEach { button ->
            button.setBackgroundColor(getColor(android.R.color.transparent))
            button.setTextColor(getColor(R.color.primary_color))
            button.strokeColor = getColorStateList(R.color.primary_color)
            button.strokeWidth = 1
        }

        // Установка стиля для активной кнопки
        activeButton.setBackgroundColor(getColor(R.color.primary_color))
        activeButton.setTextColor(getColor(android.R.color.white))
        activeButton.strokeWidth = 0
    }

    private fun loadReportData() {
        // Загружаем продажи за период
        val sales = dbHelper.getSalesByDateRange(startDate, endDate)
        val incomeRecords = dbHelper.getIncomeRecordsByDateRange(startDate, endDate)
        val allProducts = dbHelper.getAllProducts()

        // Расчет статистики
        val totalRevenue = sales.sumOf { it.totalAmount }
        val totalSales = sales.size
        val averageCheck = if (totalSales > 0) totalRevenue / totalSales else 0.0

        // Расчет прибыли (Выручка - Себестоимость проданных товаров)
        val profit = calculateProfit(sales, allProducts)

        // Обновляем UI
        tvTotalRevenue.text = "%,.0f ₽".format(totalRevenue)
        tvTotalSales.text = totalSales.toString()
        tvAverageCheck.text = "%,.0f ₽".format(averageCheck)
        tvProfit.text = "%,.0f ₽".format(profit)

        // Загружаем популярные товары
        loadPopularProducts(sales, allProducts)
    }

    private fun calculateProfit(sales: List<Sale>, products: List<Product>): Double {
        var totalRevenue = 0.0
        var totalCost = 0.0

        // Считаем выручку и себестоимость
        for (sale in sales) {
            totalRevenue += sale.totalAmount

            for (saleItem in sale.items) {
                val product = products.find { it.id == saleItem.productId }
                if (product != null) {
                    totalCost += product.costPrice * saleItem.quantity
                }
            }
        }

        return totalRevenue - totalCost
    }

    private fun loadPopularProducts(sales: List<Sale>, allProducts: List<Product>) {
        // Считаем продажи по товарам
        val productSales = mutableMapOf<String, ProductSales>()

        for (sale in sales) {
            for (saleItem in sale.items) {
                val product = allProducts.find { it.id == saleItem.productId }
                if (product != null && !product.isIngredient) {
                    val current = productSales[product.id] ?: ProductSales(product.name, 0, 0.0)
                    productSales[product.id] = ProductSales(
                        name = product.name,
                        quantity = current.quantity + saleItem.quantity,
                        revenue = current.revenue + saleItem.total
                    )
                }
            }
        }

        // Сортируем по количеству продаж
        val popularProducts = productSales.values
            .sortedByDescending { it.quantity }
            .take(5)
            .map {
                PopularProduct(
                    name = it.name,
                    salesCount = it.quantity,
                    revenue = it.revenue
                )
            }

        val adapter = PopularProductsAdapter(popularProducts)
        rvPopularProducts.layoutManager = LinearLayoutManager(this)
        rvPopularProducts.adapter = adapter
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Диалог для выбора начальной даты
        val startDatePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val startCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                startDate = startCalendar.timeInMillis

                // Диалог для выбора конечной даты
                val endDatePicker = DatePickerDialog(
                    this,
                    { _, endYear, endMonth, endDay ->
                        val endCalendar = Calendar.getInstance().apply {
                            set(endYear, endMonth, endDay, 23, 59, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        endDate = endCalendar.timeInMillis

                        // Обновляем интерфейс
                        periodType = PeriodType.CUSTOM
                        updatePeriodButtons(btnCustom)
                        loadReportData()

                        Toast.makeText(
                            this,
                            "Период выбран: ${TimeUtils.formatDate(Date(startDate))} - ${
                                TimeUtils.formatDate(
                                    Date(endDate)
                                )
                            }",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    year, month, day
                )
                endDatePicker.show()
            },
            year, month, day
        )
        startDatePicker.show()
    }

    private fun exportToJson() {
        try {
            // Генерируем отчет
            val sales = dbHelper.getSalesByDateRange(startDate, endDate)
            val incomeRecords = dbHelper.getIncomeRecordsByDateRange(startDate, endDate)
            val allProducts = dbHelper.getAllProducts()

            // Создаем JSON структуру
            val jsonObject = JSONObject().apply {
                put("report_title", "Отчет по пекарне")
                put("period_start", TimeUtils.formatDate(Date(startDate)))
                put("period_end", TimeUtils.formatDate(Date(endDate)))
                put("generated_at", TimeUtils.getCurrentDate() + " " + TimeUtils.getCurrentTime())

                // Основные показатели
                val totalRevenue = sales.sumOf { it.totalAmount }
                val totalSales = sales.size
                val averageCheck = if (totalSales > 0) totalRevenue / totalSales else 0.0
                val profit = calculateProfit(sales, allProducts)

                put("total_revenue", totalRevenue)
                put("total_sales", totalSales)
                put("average_check", averageCheck)
                put("profit", profit)

                // Продажи
                val salesArray = JSONArray()
                for (sale in sales) {
                    val saleObj = JSONObject().apply {
                        put("id", sale.id.substring(0, 8))
                        put("date", TimeUtils.formatDate(sale.date))
                        put("time", TimeUtils.formatTime(sale.date))
                        put("total_amount", sale.totalAmount)
                        put("payment_method", sale.paymentMethod)

                        val itemsArray = JSONArray()
                        for (item in sale.items) {
                            val product = allProducts.find { it.id == item.productId }
                            val itemObj = JSONObject().apply {
                                put("product_name", item.productName)
                                put("quantity", item.quantity)
                                put("price", item.price)
                                put("total", item.total)
                                put("cost_price", product?.costPrice ?: 0.0)
                            }
                            itemsArray.put(itemObj)
                        }
                        put("items", itemsArray)
                    }
                    salesArray.put(saleObj)
                }
                put("sales", salesArray)

                // Популярные товары
                val productSales = mutableMapOf<String, ProductSales>()
                for (sale in sales) {
                    for (saleItem in sale.items) {
                        val product = allProducts.find { it.id == saleItem.productId }
                        if (product != null && !product.isIngredient) {
                            val current = productSales[product.id] ?: ProductSales(product.name, 0, 0.0)
                            productSales[product.id] = ProductSales(
                                name = product.name,
                                quantity = current.quantity + saleItem.quantity,
                                revenue = current.revenue + saleItem.total
                            )
                        }
                    }
                }

                val popularProductsArray = JSONArray()
                productSales.values
                    .sortedByDescending { it.quantity }
                    .take(5)
                    .forEach { productSales ->
                        val productObj = JSONObject().apply {
                            put("name", productSales.name)
                            put("sales_count", productSales.quantity)
                            put("revenue", productSales.revenue)
                        }
                        popularProductsArray.put(productObj)
                    }
                put("popular_products", popularProductsArray)

                // Остатки
                val stockArray = JSONArray()
                allProducts.filter { !it.isIngredient }.forEach { product ->
                    val stockObj = JSONObject().apply {
                        put("name", product.name)
                        put("category", product.category)
                        put("stock", product.stock)
                        put("unit", product.unit)
                        put("price", product.price)
                        put("total_value", product.price * product.stock)
                    }
                    stockArray.put(stockObj)
                }
                put("products_stock", stockArray)
            }

            // Преобразуем в красивый JSON
            val jsonString = jsonObject.toString(4)

            // Сохраняем файл
            saveJsonFile(jsonString)

        } catch (e: Exception) {
            Log.e("ReportsActivity", "Ошибка экспорта в JSON", e)
            Toast.makeText(this, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveJsonFile(jsonString: String) {
        val fileName = "Отчет_пекарня_${System.currentTimeMillis()}.json"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Используем Download вместо Documents
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/SmartKassa")
            }
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }

                // Показываем диалог успеха
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Экспорт завершен")
                    .setMessage("Файл сохранен в папке Download/SmartKassa:\n$fileName")
                    .setPositiveButton("Открыть") { _, _ ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/json")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(
                                this,
                                "Нет приложения для открытия JSON файлов",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .setNegativeButton("Поделиться") { _, _ ->
                        shareFile(uri, fileName, "application/json")
                    }
                    .setNeutralButton("Закрыть", null)
                    .show()

            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка сохранения файла: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("ReportsActivity", "Ошибка сохранения JSON", e)
            }
        } ?: run {
            Toast.makeText(this, "Ошибка создания файла", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportToExcel() {
        try {
            val sales = dbHelper.getSalesByDateRange(startDate, endDate)
            val allProducts = dbHelper.getAllProducts()

            // Создаем CSV содержимое
            val csvContent = buildString {
                // Заголовок
                appendLine("Отчет по пекарне")
                appendLine("Период: ${TimeUtils.formatDate(Date(startDate))} - ${TimeUtils.formatDate(Date(endDate))}")
                appendLine("Сгенерирован: ${TimeUtils.getCurrentDate()} ${TimeUtils.getCurrentTime()}")
                appendLine()

                // Основные показатели
                val totalRevenue = sales.sumOf { it.totalAmount }
                val totalSales = sales.size
                val averageCheck = if (totalSales > 0) totalRevenue / totalSales else 0.0
                val profit = calculateProfit(sales, allProducts)

                appendLine("ОСНОВНЫЕ ПОКАЗАТЕЛИ")
                appendLine("Выручка;${"%.2f".format(totalRevenue)}")
                appendLine("Количество продаж;$totalSales")
                appendLine("Средний чек;${"%.2f".format(averageCheck)}")
                appendLine("Прибыль;${"%.2f".format(profit)}")
                appendLine()

                // Детализация продаж
                appendLine("ДЕТАЛИЗАЦИЯ ПРОДАЖ")
                appendLine("Дата;Время;ID продажи;Товар;Количество;Цена;Сумма;Метод оплаты")

                for (sale in sales) {
                    for (item in sale.items) {
                        append("${TimeUtils.formatDate(sale.date)};")
                        append("${TimeUtils.formatTime(sale.date)};")
                        append("${sale.id.substring(0, 8)};")
                        append("\"${item.productName}\";") // Экранируем кавычки
                        append("${item.quantity};")
                        append("${"%.2f".format(item.price)};")
                        append("${"%.2f".format(item.total)};")
                        append(sale.paymentMethod)
                        appendLine()
                    }
                }
                appendLine()

                // Популярные товары
                val productSales = mutableMapOf<String, ProductSales>()
                for (sale in sales) {
                    for (saleItem in sale.items) {
                        val product = allProducts.find { it.id == saleItem.productId }
                        if (product != null && !product.isIngredient) {
                            val current = productSales[product.id] ?: ProductSales(product.name, 0, 0.0)
                            productSales[product.id] = ProductSales(
                                name = product.name,
                                quantity = current.quantity + saleItem.quantity,
                                revenue = current.revenue + saleItem.total
                            )
                        }
                    }
                }

                appendLine("ПОПУЛЯРНЫЕ ТОВАРЫ")
                appendLine("Товар;Количество продаж;Выручка")

                productSales.values
                    .sortedByDescending { it.quantity }
                    .take(5)
                    .forEach { productSales ->
                        append("\"${productSales.name}\";")
                        append("${productSales.quantity};")
                        append("${"%.2f".format(productSales.revenue)}")
                        appendLine()
                    }
                appendLine()

                // Остатки товаров
                appendLine("ОСТАТКИ ТОВАРОВ")
                appendLine("Товар;Категория;Остаток;Ед.изм.;Цена;Стоимость на складе")

                allProducts
                    .filter { !it.isIngredient }
                    .sortedBy { it.category }
                    .forEach { product ->
                        append("\"${product.name}\";")
                        append("\"${product.category}\";")
                        append("${product.stock};")
                        append("${product.unit};")
                        append("${"%.2f".format(product.price)};")
                        append("${"%.2f".format(product.price * product.stock)}")
                        appendLine()
                    }
            }

            // Сохраняем CSV файл
            saveCsvFile(csvContent)

        } catch (e: Exception) {
            Log.e("ReportsActivity", "Ошибка экспорта в Excel", e)
            Toast.makeText(this, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveCsvFile(csvContent: String) {
        val fileName = "Отчет_пекарня_${System.currentTimeMillis()}.csv"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Используем Download вместо Documents
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/SmartKassa")
            }
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    // Добавляем BOM для корректного отображения кириллицы в Excel
                    outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Экспорт завершен")
                    .setMessage("Файл сохранен в папке Download/SmartKassa:\n$fileName")
                    .setPositiveButton("Открыть") { _, _ ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "text/csv")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(
                                this,
                                "Нет приложения для открытия CSV файлов",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .setNegativeButton("Поделиться") { _, _ ->
                        shareFile(uri, fileName, "text/csv")
                    }
                    .setNeutralButton("Закрыть", null)
                    .show()

            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка сохранения файла: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("ReportsActivity", "Ошибка сохранения CSV", e)
            }
        } ?: run {
            Toast.makeText(this, "Ошибка создания файла", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(uri: android.net.Uri, fileName: String, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Поделиться файлом"))
    }


    private fun shareReport(report: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Отчет по пекарне")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        startActivity(Intent.createChooser(intent, "Поделиться отчетом"))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.reports_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_refresh -> {
                refreshData()
                true
            }
            R.id.action_stock_report -> {
                showStockReport()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshData() {
        loadReportData()
        Toast.makeText(this, "Данные обновлены", Toast.LENGTH_SHORT).show()
    }


    private fun showStockReport() {
        val allProducts = dbHelper.getAllProducts()
        val products = allProducts.filter { !it.isIngredient }
        val ingredients = allProducts.filter { it.isIngredient }

        val report = buildString {
            appendLine("ОТЧЕТ ПО ОСТАТКАМ")
            appendLine("=".repeat(40))

            appendLine("\nТОВАРЫ (готовые изделия):")
            appendLine("-".repeat(30))

            var totalProductsValue = 0.0
            for (product in products.sortedBy { it.category }) {
                val value = product.price * product.stock
                totalProductsValue += value
                appendLine("${product.name}")
                appendLine("  Категория: ${product.category}")
                appendLine("  Остаток: ${product.stock} ${product.unit}")
                appendLine("  Цена: ${"%.2f".format(product.price)} ₽")
                appendLine("  Стоимость на складе: ${"%.2f".format(value)} ₽")
                appendLine()
            }

            appendLine("ИТОГО ТОВАРЫ:")
            appendLine("Количество позиций: ${products.size}")
            appendLine("Общая стоимость: ${"%.2f".format(totalProductsValue)} ₽")

            appendLine("\nИНГРЕДИЕНТЫ:")
            appendLine("-".repeat(30))

            var totalIngredientsValue = 0.0
            for (ingredient in ingredients.sortedBy { it.category }) {
                val value = ingredient.costPrice * ingredient.stock
                totalIngredientsValue += value
                appendLine("${ingredient.name}")
                appendLine("  Категория: ${ingredient.category}")
                appendLine("  Остаток: ${ingredient.stock} ${ingredient.unit}")
                appendLine("  Себестоимость: ${"%.2f".format(ingredient.costPrice)} ₽")
                appendLine("  Стоимость на складе: ${"%.2f".format(value)} ₽")
                appendLine()
            }

            appendLine("ИТОГО ИНГРЕДИЕНТЫ:")
            appendLine("Количество позиций: ${ingredients.size}")
            appendLine("Общая стоимость: ${"%.2f".format(totalIngredientsValue)} ₽")

            appendLine("\n" + "=".repeat(40))
            appendLine("ВСЕГО НА СКЛАДЕ: ${"%.2f".format(totalProductsValue + totalIngredientsValue)} ₽")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Отчет по остаткам")
            .setMessage(report)
            .setPositiveButton("Закрыть", null)
            .setNeutralButton("Экспорт") { _, _ ->
                shareReport(report)
            }
            .show()
    }

}




