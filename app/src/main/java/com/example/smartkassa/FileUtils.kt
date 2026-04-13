package com.example.smartkassa

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.smartkassa.DatabaseHelper.Companion.TABLE_ACTIVITIES
import com.example.smartkassa.DatabaseHelper.Companion.TABLE_INCOME_ITEMS
import com.example.smartkassa.DatabaseHelper.Companion.TABLE_INCOME_RECORDS
import com.example.smartkassa.DatabaseHelper.Companion.TABLE_PRODUCTS
import com.example.smartkassa.DatabaseHelper.Companion.TABLE_RECIPES
import com.example.smartkassa.DatabaseHelper.Companion.TABLE_SALES
import com.example.smartkassa.DatabaseHelper.Companion.TABLE_SALE_ITEMS
import com.example.smartkassa.DatabaseHelper.Companion.TABLE_SUPPLIERS
import com.example.smartkassa.DatabaseHelper.Companion.TABLE_UNITS
import com.google.gson.GsonBuilder
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    private const val BACKUP_DIR = "SmartKassaBackups"
    private const val EXPORT_DIR = "SmartKassaExports"
    private const val BACKUP_FILE_PREFIX = "backup_"
    private const val BACKUP_FILE_EXTENSION = ".json"
    // Получение директории для бэкапов (Download/SmartKassaBackups)
    private fun getBackupDirectory(context: Context): File? {
        return try {
            val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                createPublicDirectory(context, BACKUP_DIR)
                // Получаем путь к физической папке
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloadsDir, BACKUP_DIR)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloadsDir, BACKUP_DIR)
            }

            if (!dir.exists()) {
                val created = dir.mkdirs()
                Log.d("FileUtils", "Директория бэкапов создана: $created, путь: ${dir.absolutePath}")
            }
            // Создаем файл .nomedia чтобы папка не сканировалась галереей
            val noMediaFile = File(dir, ".nomedia")
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile()
            }

            Log.d("FileUtils", "Директория бэкапов: ${dir.absolutePath}, доступна: ${dir.canWrite()}")
            dir
        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка получения директории бэкапов: ${e.message}", e)
            // Fallback на внутреннее хранилище
            File(context.filesDir, BACKUP_DIR).apply { mkdirs() }
        }
    }
    // Создание публичной папки через MediaStore
    private fun createPublicDirectory(context: Context, folderName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$folderName")
                    put(MediaStore.MediaColumns.DISPLAY_NAME, ".nomedia")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    // Удаляем временный файл .nomedia
                    resolver.delete(uri, null, null)
                    Log.d("FileUtils", "Создана публичная папка: $folderName")
                }
            } catch (e: Exception) {
                Log.e("FileUtils", "Ошибка создания публичной папки $folderName: ${e.message}", e)
            }
        }
    }

    fun getExportDirectoryPath(context: Context): String? {
        return getExportDirectory(context)?.absolutePath ?: "Download/$EXPORT_DIR"
    }
    // Получение директории для экспорта
    private fun getExportDirectory(context: Context): File? {
        return try {
            val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - используем Download/SmartKassaExports
                createPublicDirectory(context, EXPORT_DIR)

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloadsDir, EXPORT_DIR)
            } else {
                // Android 9 и ниже
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloadsDir, EXPORT_DIR)
            }

            if (!dir.exists()) {
                dir.mkdirs()
            }

            // Создаем файл .nomedia
            val noMediaFile = File(dir, ".nomedia")
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile()
            }

            dir
        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка получения директории экспорта: ${e.message}", e)
            // Fallback на внутреннее хранилище
            File(context.filesDir, EXPORT_DIR).apply { mkdirs() }
        }
    }
    // Создание имени файла с датой
    private fun createFileName(prefix: String, extension: String): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "${prefix}${timestamp}$extension"
    }
    // Сохранение резервной копии с улучшенной обработкой ошибок
    fun saveBackup(context: Context, backupData: BackupData): Pair<Boolean, String> {
        return try {
            val fileName = createFileName(BACKUP_FILE_PREFIX, BACKUP_FILE_EXTENSION)
            // Конвертируем в JSON
            val gson = GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")  // Этот формат должен совпадать везде
                .create()

            val json = gson.toJson(backupData)

            var filePath = ""
            var savedViaMediaStore = false
            // Пытаемся сохранить через MediaStore (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                savedViaMediaStore = saveFileViaMediaStore(context, fileName, json, "application/json")
            }
            // Если не удалось через MediaStore или Android < 10, сохраняем напрямую в папку
            if (!savedViaMediaStore) {
                val backupDir = getBackupDirectory(context)
                if (backupDir == null) {
                    return Pair(false, "Директория для бэкапов недоступна")
                }

                if (!backupDir.canWrite()) {
                    return Pair(false, "Нет прав на запись в директорию: ${backupDir.absolutePath}")
                }

                val backupFile = File(backupDir, fileName)
                FileWriter(backupFile).use { writer ->
                    writer.write(json)
                }

                filePath = backupFile.absolutePath
                Log.d("FileUtils", "Бэкап сохранен напрямую: $filePath, размер: ${backupFile.length()} байт")
            }
            // Сохраняем информацию о последнем бэкапе
            saveLastBackupInfo(context, filePath, backupData.timestamp)
            // Также копируем в кэш приложения для быстрого доступа
            saveToInternalStorage(context, fileName, json)

            val message = if (savedViaMediaStore) {
                "Бэкап сохранен в папке Download/$BACKUP_DIR/$fileName"
            } else {
                "Бэкап сохранен: $fileName\nПуть: $filePath"
            }

            Pair(true, message)
        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка сохранения бэкапа: ${e.message}", e)
            Pair(false, "Ошибка сохранения: ${e.message}")
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveFileViaMediaStore(context: Context, fileName: String, content: String, mimeType: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$BACKUP_DIR")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(content.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    }
                    Log.d("FileUtils", "Файл сохранен через MediaStore: $fileName")
                    return true
                }
            } catch (e: Exception) {
                Log.e("FileUtils", "Ошибка сохранения через MediaStore: ${e.message}", e)
            }
        }
        return false
    }
    // Сохранение копии во внутреннее хранилище
    private fun saveToInternalStorage(context: Context, fileName: String, content: String) {
        try {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                output.write(content.toByteArray())
            }
            Log.d("FileUtils", "Бэкап сохранен во внутреннее хранилище: $fileName")
        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка сохранения во внутреннее хранилище: ${e.message}")
        }
    }
    // Сохранение информации о последнем бэкапе
    private fun saveLastBackupInfo(context: Context, filePath: String, timestamp: Long) {
        val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("last_backup_path", filePath)
            putString("last_backup_file_name", File(filePath).name)
            putLong("last_backup_time", timestamp)
            putInt("backup_count", prefs.getInt("backup_count", 0) + 1)
            apply()
        }
    }
    // Получение информации о последнем бэкапе
    fun getLastBackupInfo(context: Context): Triple<String?, String?, Long> {
        val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        val path = prefs.getString("last_backup_path", null)
        val fileName = prefs.getString("last_backup_file_name", null)
        val time = prefs.getLong("last_backup_time", 0)
        return Triple(path, fileName, time)
    }
    // Получение списка всех бэкапов
    fun getBackupFiles(context: Context): List<BackupFileInfo> {
        return try {
            // Сначала проверяем папку в Downloads
            val backupDir = getBackupDirectory(context)
            val files = mutableListOf<BackupFileInfo>()

            if (backupDir != null && backupDir.exists()) {
                backupDir.listFiles { file ->
                    file.name.startsWith(BACKUP_FILE_PREFIX) && file.name.endsWith(BACKUP_FILE_EXTENSION)
                }?.forEach { file ->
                    files.add(
                        BackupFileInfo(
                            name = file.name,
                            path = file.absolutePath,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            readableSize = formatFileSize(file.length())
                        )
                    )
                }
            }
            // Также проверяем внутренние бэкапы
            getInternalBackups(context).forEach { fileName ->
                val internalFile = File(context.filesDir, fileName)
                if (internalFile.exists()) {
                    files.add(
                        BackupFileInfo(
                            name = fileName,
                            path = internalFile.absolutePath,
                            size = internalFile.length(),
                            lastModified = internalFile.lastModified(),
                            readableSize = formatFileSize(internalFile.length())
                        )
                    )
                }
            }

            files.sortedByDescending { it.lastModified }
        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка получения списка бэкапов: ${e.message}")
            emptyList()
        }
    }
    // Получение бэкапов из внутреннего хранилища (для быстрого доступа)
    fun getInternalBackups(context: Context): List<String> {
        return try {
            context.fileList().filter { it.startsWith(BACKUP_FILE_PREFIX) && it.endsWith(BACKUP_FILE_EXTENSION) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    // Форматирование размера файла
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size Б"
            size < 1024 * 1024 -> "${size / 1024} КБ"
            else -> "${size / (1024 * 1024)} МБ"
        }
    }
    // Удаление старых бэкапов (оставляет последние 5)
    fun cleanupOldBackups(context: Context): Int {
        return try {
            val backupFiles = getBackupFiles(context)
            if (backupFiles.size > 5) {
                val filesToDelete = backupFiles.subList(5, backupFiles.size)
                var deletedCount = 0
                filesToDelete.forEach { fileInfo ->
                    val file = File(fileInfo.path)
                    if (file.delete()) {
                        deletedCount++
                        Log.d("FileUtils", "Удален старый бэкап: ${file.name}")

                        // Также удаляем из внутреннего хранилища
                        try {
                            context.deleteFile(file.name)
                        } catch (e: Exception) {
                            // Игнорируем ошибки удаления из внутреннего хранилища
                        }
                    }
                }
                deletedCount
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка очистки бэкапов: ${e.message}")
            0
        }
    }
    // Получение пути к директории для отображения пользователю
    fun getBackupDirectoryPath(context: Context): String? {
        return getBackupDirectory(context)?.absolutePath ?: "Download/$BACKUP_DIR"
    }
    // Проверка доступности хранилища
    fun isStorageAvailable(context: Context): Boolean {
        return try {
            val dir = getBackupDirectory(context)
            dir != null && dir.canWrite()
        } catch (e: Exception) {
            false
        }
    }
    fun exportToCSV(context: Context, data: List<Any>, fileName: String, headers: List<String>): Pair<Boolean, String> {
        return try {
            val exportDir = getExportDirectory(context)
            if (exportDir == null) {
                return Pair(false, "Директория для экспорта недоступна")
            }

            val file = File(exportDir, "$fileName.csv")

            FileWriter(file).use { writer ->
                // Записываем заголовки
                writer.write(headers.joinToString(";") + "\n")

                // Записываем данные
                data.forEach { item ->
                    when (item) {
                        is Product -> {
                            val row = listOf(
                                item.name,
                                item.category,
                                item.price.toString(),
                                item.costPrice.toString(),
                                item.stock.toString(),
                                item.unit,
                                if (item.isIngredient) "Ингредиент" else "Товар",
                                item.barcode ?: ""
                            )
                            writer.write(row.joinToString(";") + "\n")
                        }
                        is Sale -> {
                            val row = listOf(
                                item.id,
                                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(item.date),
                                item.totalAmount.toString(),
                                item.paymentMethod,
                                item.items.size.toString()
                            )
                            writer.write(row.joinToString(";") + "\n")
                        }
                        is IncomeRecord -> {
                            val row = listOf(
                                item.id,
                                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(item.date),
                                item.totalCost.toString(),
                                item.invoiceNumber,
                                item.items.size.toString()
                            )
                            writer.write(row.joinToString(";") + "\n")
                        }
                        // Добавьте другие типы по необходимости
                    }
                }
            }

            Log.d("FileUtils", "CSV экспорт сохранен: ${file.absolutePath}")
            Pair(true, "Экспорт сохранен: ${file.name}\nПуть: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка CSV экспорта: ${e.message}", e)
            Pair(false, "Ошибка экспорта: ${e.message}")
        }
    }
    // Экспорт в Excel (простой формат CSV с расширением .xlsx)
    fun exportToExcel(context: Context, data: Map<String, List<Any>>): Pair<Boolean, String> {
        return try {
            val exportDir = getExportDirectory(context)
            if (exportDir == null) {
                return Pair(false, "Директория для экспорта недоступна")
            }

            val fileName = createFileName("export_excel_", ".xlsx")
            val file = File(exportDir, fileName)

            // Создаем простой Excel-подобный файл (CSV с разными листами)
            FileWriter(file).use { writer ->
                data.forEach { (sheetName, sheetData) ->
                    writer.write("=== $sheetName ===\n")

                    if (sheetData.isNotEmpty()) {
                        // Определяем заголовки в зависимости от типа данных
                        val headers = when (sheetData.first()) {
                            is Product -> listOf("Название", "Категория", "Цена", "Себестоимость", "Остаток", "Единица", "Тип", "Штрих-код")
                            is Sale -> listOf("ID", "Дата", "Сумма", "Способ оплаты", "Количество позиций")
                            is IncomeRecord -> listOf("ID", "Дата", "Стоимость", "Номер накладной", "Количество позиций")
                            is User -> listOf("Email", "Название бизнеса", "Валюта", "Ставка НДС")
                            else -> emptyList()
                        }

                        writer.write(headers.joinToString("\t") + "\n")

                        sheetData.forEach { item ->
                            val row = when (item) {
                                is Product -> listOf(
                                    item.name,
                                    item.category,
                                    item.price.toString(),
                                    item.costPrice.toString(),
                                    item.stock.toString(),
                                    item.unit,
                                    if (item.isIngredient) "Ингредиент" else "Товар",
                                    item.barcode ?: ""
                                )
                                is Sale -> listOf(
                                    item.id,
                                    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(item.date),
                                    item.totalAmount.toString(),
                                    item.paymentMethod,
                                    item.items.size.toString()
                                )
                                is IncomeRecord -> listOf(
                                    item.id,
                                    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(item.date),
                                    item.totalCost.toString(),
                                    item.invoiceNumber,
                                    item.items.size.toString()
                                )
                                is User -> listOf(
                                    item.email,
                                    item.businessName,
                                    item.currency,
                                    item.taxRate.toString()
                                )
                                else -> emptyList()
                            }
                            writer.write(row.joinToString("\t") + "\n")
                        }
                    }
                    writer.write("\n\n")
                }
            }

            Log.d("FileUtils", "Excel экспорт сохранен: ${file.absolutePath}")
            Pair(true, "Excel экспорт сохранен: ${file.name}\nПуть: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка Excel экспорта: ${e.message}", e)
            Pair(false, "Ошибка экспорта: ${e.message}")
        }
    }
    // Экспорт в PDF (создаем текстовый файл с расширением .pdf для совместимости)
    fun exportToPDF(context: Context, title: String, content: String): Pair<Boolean, String> {
        return try {
            val exportDir = getExportDirectory(context)
            if (exportDir == null) {
                return Pair(false, "Директория для экспорта недоступна")
            }

            val fileName = createFileName("export_pdf_", ".pdf")
            val file = File(exportDir, fileName)

            FileWriter(file).use { writer ->
                writer.write("%PDF-1.4\n") // Заголовок PDF
                writer.write("1 0 obj\n")
                writer.write("<<\n")
                writer.write("/Type /Catalog\n")
                writer.write("/Pages 2 0 R\n")
                writer.write(">>\n")
                writer.write("endobj\n")
                writer.write("\n")
                writer.write("2 0 obj\n")
                writer.write("<<\n")
                writer.write("/Type /Pages\n")
                writer.write("/Kids [3 0 R]\n")
                writer.write("/Count 1\n")
                writer.write(">>\n")
                writer.write("endobj\n")
                writer.write("\n")
                writer.write("3 0 obj\n")
                writer.write("<<\n")
                writer.write("/Type /Page\n")
                writer.write("/Parent 2 0 R\n")
                writer.write("/MediaBox [0 0 612 792]\n")
                writer.write("/Contents 4 0 R\n")
                writer.write(">>\n")
                writer.write("endobj\n")
                writer.write("\n")

                // Добавляем текст
                writer.write("4 0 obj\n")
                writer.write("<<\n")
                writer.write("/Length 100\n")
                writer.write(">>\n")
                writer.write("stream\n")
                writer.write("BT\n")
                writer.write("/F1 12 Tf\n")
                writer.write("50 750 Td\n")
                writer.write("($title) Tj\n")
                writer.write("ET\n")
                writer.write("endstream\n")
                writer.write("endobj\n")

                writer.write("\n")
                writer.write("xref\n")
                writer.write("0 5\n")
                writer.write("0000000000 65535 f \n")
                writer.write("0000000010 00000 n \n")
                writer.write("0000000079 00000 n \n")
                writer.write("0000000173 00000 n \n")
                writer.write("0000000302 00000 n \n")
                writer.write("trailer\n")
                writer.write("<<\n")
                writer.write("/Size 5\n")
                writer.write("/Root 1 0 R\n")
                writer.write(">>\n")
                writer.write("startxref\n")
                writer.write("500\n")
                writer.write("%%EOF\n")

                // Простой текст в конце для читаемости
                writer.write("\n\n=== $title ===\n\n")
                writer.write(content)
            }

            Log.d("FileUtils", "PDF экспорт сохранен: ${file.absolutePath}")
            Pair(true, "PDF экспорт сохранен: ${file.name}\nПуть: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка PDF экспорта: ${e.message}", e)
            Pair(false, "Ошибка экспорта: ${e.message}")
        }
    }

    // Экспорт в JSON (уже есть, но улучшим)
    fun exportToJSON(context: Context, data: Map<String, Any>, exportName: String): Pair<Boolean, String> {
        return try {
            val exportDir = getExportDirectory(context)
            if (exportDir == null) {
                return Pair(false, "Директория для экспорта недоступна")
            }

            val fileName = createFileName("${exportName}_", ".json")
            val file = File(exportDir, fileName)

            val gson = GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create()

            val json = gson.toJson(data)

            FileWriter(file).use { writer ->
                writer.write(json)
            }

            Log.d("FileUtils", "JSON экспорт сохранен: ${file.absolutePath}")
            Pair(true, "JSON экспорт сохранен: ${file.name}\nПуть: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка JSON экспорта: ${e.message}", e)
            Pair(false, "Ошибка экспорта: ${e.message}")
        }
    }

    // Получение списка экспортированных файлов
    fun getExportFiles(context: Context): List<ExportFileInfo> {
        return try {
            val exportDir = getExportDirectory(context) ?: return emptyList()

            val files = exportDir.listFiles()
            files?.map { file ->
                ExportFileInfo(
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    readableSize = formatFileSize(file.length()),
                    extension = file.extension.lowercase(Locale.getDefault())
                )
            }?.sortedByDescending { it.lastModified } ?: emptyList()

        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка получения списка экспортов: ${e.message}")
            emptyList()
        }
    }

    // Генерация отчета для экспорта
    fun generateReportContent(dbHelper: DatabaseHelper, reportType: String): String {
        return when (reportType) {
            "products" -> generateProductsReport(dbHelper)
            "sales" -> generateSalesReport(dbHelper)
            "income" -> generateIncomeReport(dbHelper)
            "full" -> generateFullReport(dbHelper)
            else -> generateSummaryReport(dbHelper)
        }
    }

    private fun generateProductsReport(dbHelper: DatabaseHelper): String {
        val products = dbHelper.getAllProducts()
        val finishedProducts = products.filter { !it.isIngredient }
        val ingredients = products.filter { it.isIngredient }

        return buildString {
            appendLine("ОТЧЕТ ПО ТОВАРАМ")
            appendLine("=".repeat(50))
            appendLine("Дата генерации: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
            appendLine()

            appendLine("ТОВАРЫ (готовые изделия):")
            appendLine("-".repeat(40))
            finishedProducts.forEach { product ->
                val margin = product.price - product.costPrice
                val marginPercent = if (product.costPrice > 0) (margin / product.costPrice * 100) else 0.0
                appendLine("${product.name}")
                appendLine("  Категория: ${product.category}")
                appendLine("  Остаток: ${product.stock} ${product.unit}")
                appendLine("  Цена: ${String.format("%.2f", product.price)} ₽")
                appendLine("  Себестоимость: ${String.format("%.2f", product.costPrice)} ₽")
                appendLine("  Маржа: ${String.format("%.2f", margin)} ₽ (${String.format("%.1f", marginPercent)}%)")
                appendLine("  Стоимость на складе: ${String.format("%.2f", product.price * product.stock)} ₽")
                appendLine()
            }

            appendLine("ИНГРЕДИЕНТЫ:")
            appendLine("-".repeat(40))
            ingredients.forEach { ingredient ->
                appendLine("${ingredient.name}")
                appendLine("  Категория: ${ingredient.category}")
                appendLine("  Остаток: ${ingredient.stock} ${ingredient.unit}")
                appendLine("  Себестоимость: ${String.format("%.2f", ingredient.costPrice)} ₽")
                appendLine("  Стоимость на складе: ${String.format("%.2f", ingredient.costPrice * ingredient.stock)} ₽")
                appendLine()
            }

            appendLine("Итого:")
            appendLine("  Товаров: ${finishedProducts.size}")
            appendLine("  Ингредиентов: ${ingredients.size}")
            appendLine("  Общая стоимость на складе: ${String.format("%.2f",
                finishedProducts.sumOf { it.price * it.stock } +
                        ingredients.sumOf { it.costPrice * it.stock })} ₽")
        }
    }

    private fun generateSalesReport(dbHelper: DatabaseHelper): String {
        val sales = dbHelper.getAllSales()
        val products = dbHelper.getAllProducts()

        // Рассчитываем статистику
        val totalRevenue = sales.sumOf { it.totalAmount }
        val totalCost = sales.sumOf { sale ->
            sale.items.sumOf { item ->
                val product = products.find { it.id == item.productId }
                (product?.costPrice ?: 0.0) * item.quantity
            }
        }
        val totalProfit = totalRevenue - totalCost

        // Группируем по дням
        val salesByDay = sales.groupBy {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(it.date)
        }

        return buildString {
            appendLine("ОТЧЕТ ПО ПРОДАЖАМ")
            appendLine("=".repeat(50))
            appendLine("Период: все время")
            appendLine("Дата генерации: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
            appendLine()

            appendLine("ОБЩАЯ СТАТИСТИКА:")
            appendLine("-".repeat(40))
            appendLine("Всего продаж: ${sales.size}")
            appendLine("Общая выручка: ${String.format("%.2f", totalRevenue)} ₽")
            appendLine("Общая себестоимость: ${String.format("%.2f", totalCost)} ₽")
            appendLine("Прибыль: ${String.format("%.2f", totalProfit)} ₽")
            appendLine("Средний чек: ${String.format("%.2f", if (sales.isNotEmpty()) totalRevenue / sales.size else 0.0)} ₽")
            appendLine()

            appendLine("ПРОДАЖИ ПО ДНЯМ:")
            appendLine("-".repeat(40))
            salesByDay.forEach { (day, daySales) ->
                val dayRevenue = daySales.sumOf { it.totalAmount }
                appendLine("$day: ${daySales.size} продаж, ${String.format("%.2f", dayRevenue)} ₽")
            }

            if (sales.isNotEmpty()) {
                appendLine()
                appendLine("ПОСЛЕДНИЕ 10 ПРОДАЖ:")
                appendLine("-".repeat(40))
                sales.take(10).forEach { sale ->
                    appendLine("${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(sale.date)}")
                    appendLine("  Сумма: ${String.format("%.2f", sale.totalAmount)} ₽")
                    appendLine("  Способ: ${sale.paymentMethod}")
                    appendLine("  Товаров: ${sale.items.size}")
                    appendLine()
                }
            }
        }
    }

    private fun generateIncomeReport(dbHelper: DatabaseHelper): String {
        val incomeRecords = dbHelper.getAllIncomeRecords()
        val suppliers = dbHelper.getAllSuppliers()

        return buildString {
            appendLine("ОТЧЕТ ПО ПОСТАВКАМ")
            appendLine("=".repeat(50))
            appendLine("Период: все время")
            appendLine("Дата генерации: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
            appendLine()

            appendLine("ОБЩАЯ СТАТИСТИКА:")
            appendLine("-".repeat(40))
            appendLine("Всего поставок: ${incomeRecords.size}")
            appendLine("Общая стоимость: ${String.format("%.2f", incomeRecords.sumOf { it.totalCost })} ₽")
            appendLine("Средняя поставка: ${String.format("%.2f",
                if (incomeRecords.isNotEmpty()) incomeRecords.sumOf { it.totalCost } / incomeRecords.size else 0.0)} ₽")
            appendLine()

            // Группируем по поставщикам
            val bySupplier = incomeRecords.groupBy { it.supplierId }

            appendLine("ПО ПОСТАВЩИКАМ:")
            appendLine("-".repeat(40))
            bySupplier.forEach { (supplierId, supplierIncomes) ->
                val supplier = suppliers.find { it.id == supplierId }
                val total = supplierIncomes.sumOf { it.totalCost }
                appendLine("${supplier?.name ?: "Неизвестный поставщик"}")
                appendLine("  Поставок: ${supplierIncomes.size}")
                appendLine("  Общая сумма: ${String.format("%.2f", total)} ₽")
                appendLine("  Средняя: ${String.format("%.2f", total / supplierIncomes.size)} ₽")
                appendLine()
            }

            if (incomeRecords.isNotEmpty()) {
                appendLine("ПОСЛЕДНИЕ 10 ПОСТАВОК:")
                appendLine("-".repeat(40))
                incomeRecords.take(10).forEach { income ->
                    val supplier = suppliers.find { it.id == income.supplierId }
                    appendLine("${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(income.date)}")
                    appendLine("  Поставщик: ${supplier?.name ?: "Неизвестный"}")
                    appendLine("  Накладная: ${income.invoiceNumber}")
                    appendLine("  Сумма: ${String.format("%.2f", income.totalCost)} ₽")
                    appendLine("  Товаров: ${income.items.size}")
                    appendLine()
                }
            }
        }
    }

    private fun generateFullReport(dbHelper: DatabaseHelper): String {
        return buildString {
            appendLine(generateSummaryReport(dbHelper))
            appendLine("\n" + "=".repeat(80) + "\n")
            appendLine(generateProductsReport(dbHelper))
            appendLine("\n" + "=".repeat(80) + "\n")
            appendLine(generateSalesReport(dbHelper))
            appendLine("\n" + "=".repeat(80) + "\n")
            appendLine(generateIncomeReport(dbHelper))
        }
    }

    private fun generateSummaryReport(dbHelper: DatabaseHelper): String {
        val users = dbHelper.getAllUsers()
        val products = dbHelper.getAllProducts()
        val sales = dbHelper.getAllSales()
        val incomeRecords = dbHelper.getAllIncomeRecords()

        val finishedProducts = products.filter { !it.isIngredient }
        val ingredients = products.filter { it.isIngredient }

        val totalRevenue = sales.sumOf { it.totalAmount }
        val totalCost = sales.sumOf { sale ->
            sale.items.sumOf { item ->
                val product = products.find { it.id == item.productId }
                (product?.costPrice ?: 0.0) * item.quantity
            }
        }

        return buildString {
            appendLine("СВОДНЫЙ ОТЧЕТ ПО ПЕКАРНЕ")
            appendLine("=".repeat(50))
            appendLine("Дата генерации: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
            appendLine()

            appendLine("ОБЩАЯ ИНФОРМАЦИЯ:")
            appendLine("-".repeat(30))
            appendLine("Пользователей: ${users.size}")
            appendLine("Товаров (готовые изделия): ${finishedProducts.size}")
            appendLine("Ингредиентов: ${ingredients.size}")
            appendLine("Продаж: ${sales.size}")
            appendLine("Поставок: ${incomeRecords.size}")
            appendLine()

            appendLine("ФИНАНСОВЫЕ ПОКАЗАТЕЛИ:")
            appendLine("-".repeat(30))
            appendLine("Общая выручка: ${String.format("%.2f", totalRevenue)} ₽")
            appendLine("Общая себестоимость: ${String.format("%.2f", totalCost)} ₽")
            appendLine("Прибыль: ${String.format("%.2f", totalRevenue - totalCost)} ₽")
            appendLine("Средняя прибыль на продажу: ${String.format("%.2f",
                if (sales.isNotEmpty()) (totalRevenue - totalCost) / sales.size else 0.0)} ₽")
            appendLine()

            appendLine("ОСТАТКИ НА СКЛАДЕ:")
            appendLine("-".repeat(30))
            appendLine("Товары: ${String.format("%.2f", finishedProducts.sumOf { it.price * it.stock })} ₽")
            appendLine("Ингредиенты: ${String.format("%.2f", ingredients.sumOf { it.costPrice * it.stock })} ₽")
            appendLine("Всего на складе: ${String.format("%.2f",
                finishedProducts.sumOf { it.price * it.stock } +
                        ingredients.sumOf { it.costPrice * it.stock })} ₽")
        }
    }
    fun saveTextToFile(context: Context, fileName: String, content: String): Pair<Boolean, String> {
        return try {
            val exportDir = getExportDirectory(context)
            if (exportDir == null) {
                return Pair(false, "Директория для экспорта недоступна")
            }

            val file = File(exportDir, fileName)

            FileWriter(file).use { writer ->
                writer.write(content)
            }

            Log.d("FileUtils", "Текстовый файл сохранен: ${file.absolutePath}")
            Pair(true, "Файл сохранен: ${file.name}")
        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка сохранения текстового файла: ${e.message}", e)
            Pair(false, "Ошибка сохранения: ${e.message}")
        }
    }
    fun restoreFromBackup(context: Context, fileUri: Uri): RestoreResult {
        return try {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                restoreFromJson(context, jsonString)
            } ?: RestoreResult(false, "Не удалось открыть файл")
        } catch (e: Exception) {
            RestoreResult(false, "Ошибка чтения файла: ${e.message}")
        }
    }

    private fun restoreFromJson(context: Context, jsonString: String): RestoreResult {
        return try {
            val gson = GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create()

            val backupData = gson.fromJson(jsonString, BackupData::class.java)

            // Проверяем валидность данных
            if (!isBackupValid(backupData)) {
                return RestoreResult(false, "Некорректный формат бэкапа")
            }
            // Проверяем старые бэкапы без units
            if (backupData.units == null) {
                Log.w("FileUtils", "Старый бэкап без единиц измерения")
            }

            // Сохраняем данные временно (в SharedPreferences)
            val restoreInfo = gson.toJson(backupData)
            saveRestoreInfo(context, restoreInfo)

            RestoreResult(true, "Бэкап загружен успешно", backupData)

        } catch (e: Exception) {
            RestoreResult(false, "Ошибка парсинга JSON: ${e.message}")
        }
    }

    private fun isBackupValid(backupData: BackupData): Boolean {
        return backupData.timestamp > 0 && backupData.totalRecords > 0
    }

    fun saveRestoreInfo(context: Context, restoreInfo: String) {
        val prefs = context.getSharedPreferences("restore_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("pending_restore", restoreInfo).apply()
    }

    fun getPendingRestore(context: Context): BackupData? {
        val prefs = context.getSharedPreferences("restore_prefs", Context.MODE_PRIVATE)
        val restoreInfo = prefs.getString("pending_restore", null)
        return if (restoreInfo != null) {
            val gson = GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create()
            gson.fromJson(restoreInfo, BackupData::class.java)
        } else {
            null
        }
    }

    fun clearRestoreInfo(context: Context) {
        val prefs = context.getSharedPreferences("restore_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("pending_restore").apply()
    }

    fun performRestore(context: Context, dbHelper: DatabaseHelper): Map<String, Int> {
        val backupData = getPendingRestore(context) ?: return emptyMap()

        val restoredCount = mutableMapOf<String, Int>()

        try {
            clearAllTablesBeforeRestore(dbHelper)

            // 1. Восстанавливаем пользователей ПЕРВЫМИ
            backupData.users?.let { users ->
                users.forEach { user ->
                    try {
                        // Проверяем, существует ли уже пользователь с таким email
                        val existingUser = dbHelper.getUserByEmail(user.email)
                        if (existingUser == null) {
                            dbHelper.addUser(user)
                        } else {
                            // Если существует, обновляем его данные
                            Log.d("FileUtils", "Пользователь ${user.email} уже существует, обновляем...")
                            dbHelper.updateUser(user)
                        }
                    } catch (e: Exception) {
                        Log.e("FileUtils", "Ошибка восстановления пользователя ${user.email}: ${e.message}")
                    }
                }
                restoredCount["users"] = users.size
            }

            // 2. Восстанавливаем единицы измерения (важно для товаров!)
            backupData.units?.let { units ->
                units.forEach { unit ->
                    try {
                        // Проверяем существование единицы
                        val existingUnit = dbHelper.getUnitByName(unit.name)
                        if (existingUnit == null) {
                            dbHelper.addUnit(unit)
                        } else {
                            dbHelper.updateUnit(unit)
                        }
                    } catch (e: Exception) {
                        Log.e("FileUtils", "Ошибка восстановления единицы ${unit.name}: ${e.message}")
                    }
                }
                restoredCount["units"] = units.size
            }

            // 3. Восстанавливаем товары
            backupData.products?.let { products ->
                products.forEach { product ->
                    try {
                        // Проверяем существование товара
                        val existingProduct = dbHelper.getProductById(product.id)
                        if (existingProduct == null) {
                            dbHelper.addProduct(product)
                        } else {
                            dbHelper.updateProduct(product)
                        }
                    } catch (e: Exception) {
                        Log.e("FileUtils", "Ошибка восстановления товара ${product.name}: ${e.message}")
                    }
                }
                restoredCount["products"] = products.size
            }

            // 4. Восстанавливаем поставщиков
            backupData.suppliers?.let { suppliers ->
                suppliers.forEach { supplier ->
                    try {
                        val existingSupplier = dbHelper.getSupplierById(supplier.id)
                        if (existingSupplier == null) {
                            dbHelper.addSupplier(supplier)
                        }
                    } catch (e: Exception) {
                        Log.e("FileUtils", "Ошибка восстановления поставщика ${supplier.name}: ${e.message}")
                    }
                }
                restoredCount["suppliers"] = suppliers.size
            }

            // 5. Восстанавливаем рецепты
            backupData.recipes?.let { recipes ->
                recipes.forEach { recipe ->
                    try {
                        dbHelper.addRecipe(recipe)
                    } catch (e: Exception) {
                        Log.e("FileUtils", "Ошибка восстановления рецепта: ${e.message}")
                    }
                }
                restoredCount["recipes"] = recipes.size
            }

            // 6. Восстанавливаем продажи
            backupData.sales?.let { sales ->
                sales.forEach { sale ->
                    try {
                        dbHelper.addSale(sale)
                    } catch (e: Exception) {
                        Log.e("FileUtils", "Ошибка восстановления продажи ${sale.id}: ${e.message}")
                    }
                }
                restoredCount["sales"] = sales.size
            }

            // 7. Восстанавливаем поставки
            backupData.incomeRecords?.let { incomes ->
                incomes.forEach { income ->
                    try {
                        dbHelper.addIncomeRecord(income)
                    } catch (e: Exception) {
                        Log.e("FileUtils", "Ошибка восстановления поставки ${income.id}: ${e.message}")
                    }
                }
                restoredCount["incomeRecords"] = incomes.size
            }

            // 8. Восстанавливаем активности
            backupData.activities?.let { activities ->
                activities.forEach { activity ->
                    try {
                        dbHelper.addActivity(activity)
                    } catch (e: Exception) {
                        Log.e("FileUtils", "Ошибка восстановления активности ${activity.id}: ${e.message}")
                    }
                }
                restoredCount["activities"] = activities.size
            }

            // 9. Если нет единиц в бэкапе, добавляем стандартные
            if (backupData.units.isNullOrEmpty()) {
                addDefaultUnitsIfNeeded(dbHelper)
            }

            // Очищаем информацию о восстановлении
            clearRestoreInfo(context)

            return restoredCount

        } catch (e: Exception) {
            Log.e("FileUtils", "Критическая ошибка восстановления: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    private fun clearAllTablesBeforeRestore(dbHelper: DatabaseHelper) {
        try {
            // Используем существующий метод clearAllTables, но не трогаем пользователей
            val db = dbHelper.writableDatabase
            db.beginTransaction()

            try {
                // Удаляем данные в правильном порядке (с учетом foreign keys)
                db.execSQL("DELETE FROM $TABLE_ACTIVITIES")
                db.execSQL("DELETE FROM $TABLE_INCOME_ITEMS")
                db.execSQL("DELETE FROM $TABLE_INCOME_RECORDS")
                db.execSQL("DELETE FROM $TABLE_SALE_ITEMS")
                db.execSQL("DELETE FROM $TABLE_SALES")
                db.execSQL("DELETE FROM $TABLE_SUPPLIERS")
                db.execSQL("DELETE FROM $TABLE_RECIPES")
                db.execSQL("DELETE FROM $TABLE_PRODUCTS")
                db.execSQL("DELETE FROM $TABLE_UNITS")
                // Не удаляем пользователей, чтобы сохранить текущую сессию

                db.setTransactionSuccessful()
                Log.d("FileUtils", "Все таблицы (кроме пользователей) очищены")
            } catch (e: Exception) {
                Log.e("FileUtils", "Ошибка очистки таблиц: ${e.message}")
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("FileUtils", "Ошибка доступа к базе данных: ${e.message}")
        }
    }
    private fun addDefaultUnitsIfNeeded(dbHelper: DatabaseHelper) {
        val existingUnits = dbHelper.getAllUnits()
        if (existingUnits.isEmpty()) {
            // Добавляем стандартные единицы
            val defaultUnits = listOf(
                // ========== ВЕС ==========
                UnitDefinition(
                    id = IdGenerator.generateId(),
                    name = "кг",
                    category = UnitDefinition.CATEGORY_WEIGHT,
                    baseUnit = UnitDefinition.BASE_KG,
                    conversionRate = 1.0
                ),
                UnitDefinition(
                    id = IdGenerator.generateId(),
                    name = "г",
                    category = UnitDefinition.CATEGORY_WEIGHT,
                    baseUnit = UnitDefinition.BASE_KG,
                    conversionRate = 0.001 // 1 г = 0.001 кг
                ),
                // ========== ОБЪЕМ ==========
                UnitDefinition(
                    id = IdGenerator.generateId(),
                    name = "л",
                    category = UnitDefinition.CATEGORY_VOLUME,
                    baseUnit = UnitDefinition.BASE_LITER,
                    conversionRate = 1.0
                ),
                UnitDefinition(
                    id = IdGenerator.generateId(),
                    name = "мл",
                    category = UnitDefinition.CATEGORY_VOLUME,
                    baseUnit = UnitDefinition.BASE_LITER,
                    conversionRate = 0.001 // 1 мл = 0.001 л
                ),
                UnitDefinition(
                    id = IdGenerator.generateId(),
                    name = "ст",
                    category = UnitDefinition.CATEGORY_VOLUME,
                    baseUnit = UnitDefinition.BASE_LITER,
                    conversionRate = 0.2 // 1 стакан ≈ 200 мл = 0.2 л
                ),
                UnitDefinition(
                    id = IdGenerator.generateId(),
                    name = "ч.л.",
                    category = UnitDefinition.CATEGORY_VOLUME,
                    baseUnit = UnitDefinition.BASE_LITER,
                    conversionRate = 0.005 // 1 ч.л. ≈ 5 мл = 0.005 л
                ),
                UnitDefinition(
                    id = IdGenerator.generateId(),
                    name = "ст.л.",
                    category = UnitDefinition.CATEGORY_VOLUME,
                    baseUnit = UnitDefinition.BASE_LITER,
                    conversionRate = 0.015 // 1 ст.л. ≈ 15 мл = 0.015 л
                ),

                // ========== ШТУКИ ==========
                UnitDefinition(
                    id = IdGenerator.generateId(),
                    name = "шт",
                    category = UnitDefinition.CATEGORY_PIECE,
                    baseUnit = UnitDefinition.BASE_PIECE,
                    conversionRate = 1.0
                ),
                UnitDefinition(
                    id = IdGenerator.generateId(),
                    name = "уп",
                    category = UnitDefinition.CATEGORY_PIECE,
                    baseUnit = UnitDefinition.BASE_PIECE,
                    conversionRate = 10.0 // 1 упаковка = 10 штук
                ),
                UnitDefinition(
                    id = IdGenerator.generateId(),
                    name = "пак",
                    category = UnitDefinition.CATEGORY_PIECE,
                    baseUnit = UnitDefinition.BASE_PIECE,
                    conversionRate = 5.0 // 1 пакет = 5 штук
                )
            )

            defaultUnits.forEach { unit ->
                try {
                    dbHelper.addUnit(unit)
                    Log.d("UnitRestore", "Добавлена единица измерения: ${unit.name} (${unit.category})")
                } catch (e: Exception) {
                    Log.e("UnitRestore", "Ошибка добавления единицы ${unit.name}: ${e.message}")
                }
            }

            Log.d("UnitRestore", "Добавлено ${defaultUnits.size} стандартных единиц измерения")
        } else {
            Log.d("UnitRestore", "Единицы измерения уже существуют: ${existingUnits.size} шт")
        }
    }
}


