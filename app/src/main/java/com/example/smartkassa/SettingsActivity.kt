package com.example.smartkassa

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvBusinessName: TextView
    private lateinit var tvTaxSettings: TextView
    private lateinit var tvLastBackup: TextView
    private lateinit var tvLastExport: TextView
    private lateinit var tvAppVersion: TextView
    private lateinit var btnLogout: MaterialButton

    private lateinit var dbHelper: DatabaseHelper
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        dbHelper = DatabaseHelper(this)
        loadCurrentUser()

        if (currentUser == null) {
            Toast.makeText(this, "Ошибка загрузки пользователя", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupClickListeners()
        loadSettings()
        updateBackupInfo()
        updateExportInfo()
    }

    private fun loadCurrentUser() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("current_user_id", null)
        currentUser = if (userId != null) dbHelper.getUserById(userId) else null
    }

    private fun initViews() {
        tvBusinessName = findViewById(R.id.tvBusinessName)
        tvTaxSettings = findViewById(R.id.tvTaxSettings)
        tvLastBackup = findViewById(R.id.tvLastBackup)
        tvLastExport = findViewById(R.id.tvLastExport) // НАХОДИМ VIEW
        tvAppVersion = findViewById(R.id.tvAppVersion)
        btnLogout = findViewById(R.id.btnLogout)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupClickListeners() {
        findViewById<android.view.View>(R.id.businessNameLayout).setOnClickListener {
            editBusinessName()
        }

        findViewById<android.view.View>(R.id.taxLayout).setOnClickListener {
            configureTaxes()
        }

        findViewById<android.view.View>(R.id.backupLayout).setOnClickListener {
            showBackupMenu()
        }

        findViewById<android.view.View>(R.id.exportLayout).setOnClickListener {
            showExportMenu()
        }

        findViewById<android.view.View>(R.id.feedbackLayout).setOnClickListener {
            sendFeedback()
        }

        btnLogout.setOnClickListener {
            logout()
        }
    }


    private fun loadSettings() {
        currentUser?.let { user ->
            tvBusinessName.text = user.businessName
            tvTaxSettings.text = "НДС ${user.taxRate}%"
            tvAppVersion.text = "УмнаяКасса для пекарни v1.0.0"
        }
    }

    private fun updateBackupInfo() {
        val (_, fileName, lastBackupTime) = FileUtils.getLastBackupInfo(this)

        if (fileName != null && lastBackupTime > 0) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(lastBackupTime))
            tvLastBackup.text = "Последний: $dateStr\nФайл: $fileName"
        } else {
            tvLastBackup.text = "Еще не создавалась"
        }
    }
    private fun updateExportInfo() {
        val exportFiles = FileUtils.getExportFiles(this)
        if (exportFiles.isNotEmpty()) {
            val lastExport = exportFiles.maxByOrNull { it.lastModified }
            lastExport?.let {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val dateStr = dateFormat.format(Date(it.lastModified))
                tvLastExport.text = "Последний: $dateStr\nФайлов: ${exportFiles.size}"
            }
        } else {
            tvLastExport.text = "Не экспортировалось"
        }
    }

    private fun editBusinessName() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_business_name, null)
        val etBusinessName = dialogView.findViewById<TextInputEditText>(R.id.etBusinessName)
        etBusinessName.setText(currentUser?.businessName)

        MaterialAlertDialogBuilder(this)
            .setTitle("Название бизнеса")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = etBusinessName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateBusinessName(newName)
                } else {
                    Toast.makeText(this, "Введите название бизнеса", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateBusinessName(newName: String) {
        currentUser = currentUser?.copy(businessName = newName)
        currentUser?.let { user ->
            if (dbHelper.updateUser(user)) {
                tvBusinessName.text = newName
                val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                sharedPref.edit().putString("current_business_name", newName).apply()
                Toast.makeText(this, "Название бизнеса обновлено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configureTaxes() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_configure_taxes, null)
        val etTaxRate = dialogView.findViewById<TextInputEditText>(R.id.etTaxRate)
        etTaxRate.setText(currentUser?.taxRate?.toString())

        MaterialAlertDialogBuilder(this)
            .setTitle("Настройка налогов")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val taxRateStr = etTaxRate.text.toString().trim()
                val taxRate = taxRateStr.toDoubleOrNull()

                if (taxRate != null && taxRate in 0.0..100.0) {
                    currentUser = currentUser?.copy(taxRate = taxRate)
                    currentUser?.let { user ->
                        if (dbHelper.updateUser(user)) {
                            tvTaxSettings.text = "НДС ${taxRate}%"
                            Toast.makeText(this, "Налоговые настройки обновлены", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Введите корректную ставку налога (0-100%)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showBackupMenu() {
        // Проверяем доступность хранилища
        if (!FileUtils.isStorageAvailable(this)) {
            showStoragePermissionDialog()
            return
        }

        val backupOptions = arrayOf(
            "Создать резервную копию",
            "Восстановить из копии",
            "Показать все копии",
            "Открыть папку с копиями",
            "Удалить старые копии"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Резервное копирование")
            .setItems(backupOptions) { _, which ->
                when (which) {
                    0 -> createBackup()
                    1 -> restoreFromBackup()
                    2 -> showAllBackups()
                    3 -> openBackupFolder()
                    4 -> deleteOldBackups()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    private fun restoreFromBackup() {
        // Показываем предупреждение
        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ ВОССТАНОВЛЕНИЕ ДАННЫХ")
            .setMessage("ВНИМАНИЕ! Восстановление из резервной копии:\n\n" +
                    "• Перезапишет ВСЕ текущие данные\n" +
                    "• Удалит все продажи, товары и настройки\n" +
                    "• После восстановления откат невозможен\n\n" +
                    "Вы уверены, что хотите продолжить?")
            .setPositiveButton("Продолжить") { _, _ ->
                selectBackupFile()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun selectBackupFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            putExtra(Intent.EXTRA_TITLE, "Выберите файл резервной копии")
        }

        try {
            startActivityForResult(intent, REQUEST_CODE_RESTORE_BACKUP)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Файловый менеджер не найден", Toast.LENGTH_SHORT).show()
        }
    }

    // Константа для requestCode
    companion object {
        private const val REQUEST_CODE_RESTORE_BACKUP = 1001
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_RESTORE_BACKUP -> {
                if (resultCode == RESULT_OK) {
                    data?.data?.let { uri ->
                        processRestoreFile(uri)
                    } ?: run {
                        Toast.makeText(this, "Файл не выбран", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun processRestoreFile(uri: Uri) {
        // Показываем прогресс
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Загрузка резервной копии")
            .setMessage("Чтение файла...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                // Загружаем бэкап
                val restoreResult = FileUtils.restoreFromBackup(this, uri)

                runOnUiThread {
                    progressDialog.dismiss()

                    if (restoreResult.success) {
                        showRestorePreview(restoreResult.backupData)
                    } else {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Ошибка")
                            .setMessage("Не удалось загрузить резервную копию:\n${restoreResult.message}")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showRestorePreview(backupData: BackupData?) {
        backupData ?: return

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(backupData.timestamp))

        val previewInfo = """
    ⚠️ ВНИМАНИЕ! ВОССТАНОВЛЕНИЕ ДАННЫХ ⚠️
    
    📅 Дата создания копии: $dateStr
    📊 Всего записей: ${backupData.totalRecords}
    
    📁 Содержимое копии:
    👥 Пользователи: ${backupData.users?.size ?: 0}
    🛒 Товары: ${backupData.products?.size ?: 0}
    💰 Продажи: ${backupData.sales?.size ?: 0}
    📦 Поставки: ${backupData.incomeRecords?.size ?: 0}
    🤝 Поставщики: ${backupData.suppliers?.size ?: 0}
    📝 Активности: ${backupData.activities?.size ?: 0}
    📋 Рецепты: ${backupData.recipes?.size ?: 0}
    📏 Единицы измерения: ${backupData.units?.size ?: 0}
    
    ⚠️ ВАЖНО:
    1. ВСЕ текущие данные будут УДАЛЕНЫ (кроме пользователей)
    2. Текущие пользователи будут сохранены
    3. Процесс нельзя отменить!
    4. Рекомендуется создать новый бэкап перед восстановлением
    
    Вы уверены, что хотите продолжить?
""".trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Подтверждение восстановления")
            .setMessage(previewInfo)
            .setPositiveButton("ДА, ВОССТАНОВИТЬ") { _, _ ->
                confirmRestore(backupData)
            }
            .setNegativeButton("Отмена") { _, _ ->
                FileUtils.clearRestoreInfo(this)
                Toast.makeText(this, "Восстановление отменено", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Создать бэкап перед восстановлением") { _, _ ->
                createBackupForRestore(backupData)
            }
            .show()
    }
    private fun createBackupForRestore(backupData: BackupData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Создание резервной копии")
            .setMessage("Рекомендуется создать резервную копию текущих данных перед восстановлением")
            .setPositiveButton("Создать бэкап") { _, _ ->
                performBackup()
                // Сохраняем данные для восстановления на потом
                val gson = Gson()
                val json = gson.toJson(backupData)
                val prefs = getSharedPreferences("restore_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("pending_restore_delayed", json).apply()
                Toast.makeText(this, "Бэкап создан. Начните восстановление заново.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Продолжить без бэкапа") { _, _ ->
                confirmRestore(backupData)
            }
            .setNeutralButton("Отмена") { _, _ ->
                FileUtils.clearRestoreInfo(this)
            }
            .show()
    }

    private fun confirmRestore(backupData: BackupData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ ПОДТВЕРЖДЕНИЕ")
            .setMessage("Вы действительно хотите восстановить данные?\n\n" +
                    "Это действие НЕЛЬЗЯ отменить!")
            .setPositiveButton("ДА, ВОССТАНОВИТЬ") { _, _ ->
                performRestoreOperation(backupData)
            }
            .setNegativeButton("Отмена") { _, _ ->
                FileUtils.clearRestoreInfo(this)
            }
            .show()
    }

    private fun performRestoreOperation(backupData: BackupData) {
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Восстановление данных")
            .setMessage("Пожалуйста, подождите...\nЭто может занять несколько минут.")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                // Сначала создаем резервную копию текущих данных (на всякий случай)
                val currentBackup = BackupData(
                    users = dbHelper.getAllUsers(),
                    products = dbHelper.getAllProducts(),
                    sales = dbHelper.getAllSales(),
                    incomeRecords = dbHelper.getAllIncomeRecords(),
                    suppliers = dbHelper.getAllSuppliers(),
                    activities = dbHelper.getAllActivities(),
                    recipes = dbHelper.getAllRecipes(),
                    timestamp = System.currentTimeMillis()
                )

                // Сохраняем текущую копию
                FileUtils.saveBackup(this@SettingsActivity, currentBackup)

                // Восстанавливаем данные
                val restoredCount = FileUtils.performRestore(this@SettingsActivity, dbHelper)

                runOnUiThread {
                    progressDialog.dismiss()
                    showRestoreSuccess(restoredCount, backupData)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    showRestoreError(e)
                }
            }
        }.start()
    }

    private fun showRestoreSuccess(restoredCount: Map<String, Int>, backupData: BackupData) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(backupData.timestamp))

        val successInfo = """
        ✅ ВОССТАНОВЛЕНИЕ ЗАВЕРШЕНО УСПЕШНО!
        
        📅 Восстановлена копия от: $dateStr
        📊 Всего восстановлено записей: ${backupData.totalRecords}
        
        📋 Детали восстановления:
        👥 Пользователи: ${restoredCount["users"] ?: 0}
        🛒 Товары: ${restoredCount["products"] ?: 0}
        💰 Продажи: ${restoredCount["sales"] ?: 0}
        📦 Поставки: ${restoredCount["incomeRecords"] ?: 0}
        🤝 Поставщики: ${restoredCount["suppliers"] ?: 0}
        📝 Активности: ${restoredCount["activities"] ?: 0}
        📋 Рецепты: ${restoredCount["recipes"] ?: 0}
        
        📌 Текущая копия сохранена как:
        auto_backup_${System.currentTimeMillis()}.json
        
        Приложение будет перезапущено для применения изменений.
    """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Восстановление завершено")
            .setMessage(successInfo)
            .setPositiveButton("Перезапустить") { _, _ ->
                restartApp()
            }
            .setCancelable(false)
            .show()
    }

    private fun showRestoreError(e: Exception) {
        val errorInfo = """
        ❌ ОШИБКА ВОССТАНОВЛЕНИЯ
        
        При восстановлении произошла ошибка:
        ${e.message}
        
        Ваши текущие данные НЕ были изменены.
        Резервная копия текущих данных сохранена.
        
        Попробуйте:
        1. Проверить файл бэкапа
        2. Создать новую резервную копию
        3. Повторить попытку
    """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Ошибка восстановления")
            .setMessage(errorInfo)
            .setPositiveButton("OK", null)
            .setNeutralButton("Посмотреть логи") { _, _ ->
                showErrorLog(e)
            }
            .show()
    }

    private fun showErrorLog(e: Exception) {
        val logInfo = """
        StackTrace:
        ${e.stackTraceToString()}
        
        Message: ${e.message}
        Cause: ${e.cause}
    """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Детали ошибки")
            .setMessage(logInfo)
            .setPositiveButton("Закрыть", null)
            .setNeutralButton("Скопировать") { _, _ ->
                copyToClipboard(logInfo)
            }
            .show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Ошибка восстановления", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Лог скопирован в буфер", Toast.LENGTH_SHORT).show()
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finishAffinity()
    }

    private fun showStoragePermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Доступ к хранилищу")
            .setMessage("Приложению необходим доступ к хранилищу для экспорта данных.\n\n" +
                    "1. Проверьте разрешения приложения\n" +
                    "2. Убедитесь, что на устройстве достаточно свободного места")
            .setPositiveButton("Настройки") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }


    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }


    private fun createBackup() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Создание резервной копии")
            .setMessage("Создать резервную копию всех данных?\n\nЭто займет несколько секунд.")
            .setPositiveButton("Создать") { _, _ ->
                performBackup()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performBackup() {
        // Показываем уведомление о начале
        Toast.makeText(this, "Создание резервной копии...", Toast.LENGTH_SHORT).show()

        // Запускаем в отдельном потоке
        Thread {
            try {
                // Собираем данные
                val backupData = BackupData(
                    users = dbHelper.getAllUsers(),
                    products = dbHelper.getAllProducts(),
                    sales = dbHelper.getAllSales(),
                    incomeRecords = dbHelper.getAllIncomeRecords(),
                    suppliers = dbHelper.getAllSuppliers(),
                    activities = dbHelper.getAllActivities(),
                    recipes = dbHelper.getAllRecipes(),
                    timestamp = System.currentTimeMillis()
                )

                // Сохраняем
                val (success, message) = FileUtils.saveBackup(this, backupData)

                runOnUiThread {
                    if (success) {
                        // Очищаем старые бэкапы
                        val deletedCount = FileUtils.cleanupOldBackups(this)

                        // Обновляем информацию
                        updateBackupInfo()

                        // Показываем результат
                        showBackupResult(backupData, message, deletedCount)
                    } else {
                        Toast.makeText(this, "Ошибка: $message", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun showBackupResult(backupData: BackupData, filePath: String, deletedCount: Int) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(backupData.timestamp))
        val fileName = File(filePath).name

        val info = """
            ✅ Резервная копия создана успешно!
            
            📅 Дата: $dateStr
            📁 Файл: $fileName
            📊 Записей: ${backupData.totalRecords}
            🗑️ Удалено старых: $deletedCount
            
            📍 Путь: ${FileUtils.getBackupDirectoryPath(this)}
            
            Содержимое:
            👥 Пользователи: ${backupData.users?.size}
            🛒 Товары: ${backupData.products?.size}
            💰 Продажи: ${backupData.sales?.size}
            📦 Поставки: ${backupData.incomeRecords?.size}
            🤝 Поставщики: ${backupData.suppliers?.size}
            📝 Активности: ${backupData.activities?.size}
            📋 Рецепты: ${backupData.recipes?.size}
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Резервная копия создана")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .setNeutralButton("Поделиться") { _, _ ->
                shareBackupFile(File(filePath))
            }
            .show()
    }

    private fun shareBackupFile(file: File) {
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this, "$packageName.provider", file)
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Резервная копия SmartKassa")
                putExtra(Intent.EXTRA_TEXT, "Резервная копия данных пекарни от ${SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date())}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Поделиться резервной копией"))
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при открытии файла: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAllBackups() {
        val backupFiles = FileUtils.getBackupFiles(this)

        if (backupFiles.isEmpty()) {
            Toast.makeText(this, "Резервные копии не найдены", Toast.LENGTH_SHORT).show()
            return
        }

        val info = buildString {
            appendLine("📂 ВСЕ РЕЗЕРВНЫЕ КОПИИ")
            appendLine("=".repeat(40))
            appendLine()

            backupFiles.forEachIndexed { index, file ->
                val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified))
                appendLine("${index + 1}. ${file.name}")
                appendLine("   📅 $date")
                appendLine("   📏 ${file.readableSize}")
                appendLine()
            }

            appendLine("=".repeat(40))
            appendLine("Всего файлов: ${backupFiles.size}")
            appendLine("Общий размер: ${backupFiles.sumOf { it.size } / 1024} КБ")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Все резервные копии")
            .setMessage(info)
            .setPositiveButton("Закрыть", null)
            .setNeutralButton("Открыть папку") { _, _ ->
                openBackupFolder()
            }
            .show()
    }

    private fun openBackupFolder() {
        val backupPath = FileUtils.getBackupDirectoryPath(this)
        if (backupPath != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = Uri.parse("file://$backupPath")
                setDataAndType(uri, "resource/folder")
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Путь: $backupPath\nСкопируйте этот путь в файловый менеджер", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Папка с бэкапами недоступна", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteOldBackups() {
        val deletedCount = FileUtils.cleanupOldBackups(this)
        if (deletedCount > 0) {
            updateBackupInfo()
            Toast.makeText(this, "Удалено $deletedCount старых копий", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Нет старых копий для удаления", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendFeedback() {
        val feedbackOptions = arrayOf(
            "Отправить отзыв на почту",
            "Сообщить об ошибке на почту",
            "Предложить улучшение на почту",
            "Посмотреть отправленные отзывы",
            "Написать отзыв локально"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Обратная связь")
            .setItems(feedbackOptions) { _, which ->
                when (which) {
                    0 -> sendFeedbackByEmail("Отзыв о приложении SmartKassa")
                    1 -> sendFeedbackByEmail("Сообщение об ошибке в SmartKassa")
                    2 -> sendFeedbackByEmail("Предложение по улучшению SmartKassa")
                    3 -> showSentFeedback()
                    4 -> showNoEmailClientDialog("Локально")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    private fun sendFeedbackByEmail(subject: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822" // Тип для email
            putExtra(Intent.EXTRA_EMAIL, arrayOf("munisausmonowa@yandex.ru")) // Email получателя
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT,
                """
            Приветствую команду SmartKassa!
            
            Тема: $subject
            
            Версия приложения: 1.0.0
            Дата отправки: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}
            
            Мой отзыв/предложение/описание ошибки:
            
            [Здесь напишите ваш текст]
            
            ---
            Отправлено из приложения SmartKassa для пекарни
            """.trimIndent()
            )
        }

        // Пытаемся запустить почтовый клиент
        try {
            startActivity(Intent.createChooser(intent, "Выберите почтовое приложение"))
            // Сохраняем в историю что отзыв был отправлен
            saveFeedbackToHistory(subject, "отправлен на почту")
        } catch (e: Exception) {
            // Если нет почтового клиента, предлагаем альтернативу
            showNoEmailClientDialog(subject)
        }
    }
    @SuppressLint("ServiceCast")
    private fun showNoEmailClientDialog(subject: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_feedback, null)
        val etFeedback = dialogView.findViewById<TextInputEditText>(R.id.etFeedback)

        MaterialAlertDialogBuilder(this)
            .setTitle("Почтовый клиент не найден")
            .setMessage("На устройстве не установлено почтовое приложение.\n\n" +
                    "Вы можете:\n" +
                    "1. Установить Gmail или другое почтовое приложение\n" +
                    "2. Сохранить отзыв локально\n" +
                    "3. Скопировать текст для отправки позже")
            .setView(dialogView)
            .setPositiveButton("Сохранить локально") { _, _ ->
                val feedback = etFeedback.text.toString().trim()
                if (feedback.isNotEmpty()) {
                    saveFeedback(feedback)
                    Toast.makeText(this, "Отзыв сохранен локально", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Скопировать шаблон") { _, _ ->
                val template = """
                Тема: $subject
                Версия приложения: 1.0.0
                Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}
                
                Мой текст:
                
            """.trimIndent()

                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Отзыв SmartKassa", template)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(this, "Шаблон скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun saveFeedbackToHistory(subject: String, status: String) {
        val prefs = getSharedPreferences("feedback_prefs", Context.MODE_PRIVATE)
        val feedbackCount = prefs.getInt("feedback_count", 0)

        with(prefs.edit()) {
            putString("feedback_$feedbackCount", subject)
            putString("feedback_status_$feedbackCount", status)
            putLong("feedback_time_$feedbackCount", System.currentTimeMillis())
            putInt("feedback_count", feedbackCount + 1)
            apply()
        }

        val activity = ActivityItem(
            id = IdGenerator.generateId(),
            type = "Обратная связь",
            description = subject,
            amount = "",
            time = TimeUtils.getCurrentTime(),
            date = Date()
        )

        dbHelper.addActivity(activity)
    }




    // Старый метод оставляем для совместимости
    private fun saveFeedback(feedback: String) {
        val prefs = getSharedPreferences("feedback_prefs", Context.MODE_PRIVATE)
        val feedbackCount = prefs.getInt("feedback_count", 0)

        with(prefs.edit()) {
            putString("feedback_$feedbackCount", feedback)
            putString("feedback_status_$feedbackCount", "сохранен локально")
            putLong("feedback_time_$feedbackCount", System.currentTimeMillis())
            putInt("feedback_count", feedbackCount + 1)
            apply()
        }

        val activity = ActivityItem(
            id = IdGenerator.generateId(),
            type = "Обратная связь",
            description = "Отзыв сохранен локально: ${feedback.take(50)}...",
            amount = "",
            time = TimeUtils.getCurrentTime(),
            date = Date()
        )

        dbHelper.addActivity(activity)
    }

    private fun showSentFeedback() {
        val prefs = getSharedPreferences("feedback_prefs", Context.MODE_PRIVATE)
        val feedbackCount = prefs.getInt("feedback_count", 0)

        if (feedbackCount == 0) {
            Toast.makeText(this, "Вы еще не отправляли отзывы", Toast.LENGTH_SHORT).show()
            return
        }

        val feedbacks = mutableListOf<String>()

        for (i in 0 until feedbackCount) {
            val feedback = prefs.getString("feedback_$i", null)
            val status = prefs.getString("feedback_status_$i", "сохранен локально")
            val time = prefs.getLong("feedback_time_$i", 0)

            if (feedback != null) {
                val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(time))
                val statusIcon = when (status) {
                    "отправлен на почту" -> "📧"
                    "сохранен локально" -> "💾"
                    else -> "📝"
                }

                feedbacks.add("$statusIcon $date - $feedback ($status)")
            }
        }

        val info = buildString {
            appendLine("📨 ИСТОРИЯ ОБРАТНОЙ СВЯЗИ")
            appendLine("=".repeat(50))
            appendLine()

            if (feedbacks.isEmpty()) {
                appendLine("Нет отправленных отзывов")
            } else {
                feedbacks.forEachIndexed { index, feedback ->
                    appendLine("${index + 1}. $feedback")
                    appendLine()
                }
            }

            appendLine("=".repeat(50))
            appendLine("Всего: $feedbackCount")
            appendLine()
            appendLine("📧 - отправлен на почту")
            appendLine("💾 - сохранен локально")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("История обратной связи")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun logout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performLogout() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("current_user_id")
            remove("current_user_email")
            remove("current_business_name")
            apply()
        }

        Toast.makeText(this, "Выход выполнен", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    private fun showExportMenu() {
        if (!FileUtils.isStorageAvailable(this)) {
            showStoragePermissionDialog()
            return
        }

        val exportOptions = arrayOf(
            //"Полный отчет (PDF)",
            //"Товары и ингредиенты (Excel)",
            "Продажи (Excel)",
            "Поставки (Excel)",
            "Все данные (JSON)",
            "Просмотр экспортированных файлов"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Экспорт данных")
            .setItems(exportOptions) { _, which ->
                when (which) {
                    //0 -> exportFullReport()
                    //1 -> exportProducts()
                    0 -> exportSales()
                    1 -> exportIncome()
                    2 -> exportAllData()
                    3 -> showExportedFiles()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun exportSales() {
        Toast.makeText(this, "Экспорт продаж...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val sales = dbHelper.getAllSales()
                val totalRevenue = sales.sumOf { it.totalAmount }

                val (success, message) = FileUtils.exportToCSV(
                    this@SettingsActivity,
                    sales,
                    "sales_export_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}",
                    listOf("ID", "Дата", "Сумма", "Способ оплаты", "Количество позиций")
                )

                runOnUiThread {
                    if (success) {
                        updateExportInfo()
                        showExportResult("Продажи", message,
                            "Продаж: ${sales.size}\nОбщая выручка: ${String.format("%.2f", totalRevenue)} ₽")
                    } else {
                        Toast.makeText(this@SettingsActivity, "Ошибка: $message", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun exportIncome() {
        Toast.makeText(this, "Экспорт поставок...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val incomeRecords = dbHelper.getAllIncomeRecords()
                val totalCost = incomeRecords.sumOf { it.totalCost }

                val (success, message) = FileUtils.exportToCSV(
                    this@SettingsActivity,
                    incomeRecords,
                    "income_export_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}",
                    listOf("ID", "Дата", "Стоимость", "Номер накладной", "Количество позиций")
                )

                runOnUiThread {
                    if (success) {
                        updateExportInfo()
                        showExportResult("Поставки", message,
                            "Поставок: ${incomeRecords.size}\nОбщая стоимость: ${String.format("%.2f", totalCost)} ₽")
                    } else {
                        Toast.makeText(this@SettingsActivity, "Ошибка: $message", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun exportAllData() {
        Toast.makeText(this, "Экспорт всех данных...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val allData = mapOf(
                    "users" to dbHelper.getAllUsers(),
                    "products" to dbHelper.getAllProducts(),
                    "sales" to dbHelper.getAllSales(),
                    "income_records" to dbHelper.getAllIncomeRecords(),
                    "suppliers" to dbHelper.getAllSuppliers(),
                    "activities" to dbHelper.getAllActivities(),
                    "recipes" to dbHelper.getAllRecipes(),
                    "export_info" to mapOf(
                        "export_date" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                        "app_version" to "1.0.0",
                        "total_records" to (
                                dbHelper.getAllUsers().size +
                                        dbHelper.getAllProducts().size +
                                        dbHelper.getAllSales().size +
                                        dbHelper.getAllIncomeRecords().size +
                                        dbHelper.getAllSuppliers().size +
                                        dbHelper.getAllActivities().size +
                                        dbHelper.getAllRecipes().size
                                )
                    )
                )

                val (success, message) = FileUtils.exportToJSON(this@SettingsActivity, allData, "full_export")

                runOnUiThread {
                    if (success) {
                        updateExportInfo()
                        showExportResult("Все данные", message,
                            "Экспортировано всех записей: ${allData["export_info"]?.let {
                                (it as Map<*, *>)["total_records"]
                            }}")
                    } else {
                        Toast.makeText(this@SettingsActivity, "Ошибка: $message", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }


    private fun showExportResult(title: String, filePath: String, summary: String) {
        val fileName = File(filePath.substringAfter(": ")).name

        val info = """
            ✅ Экспорт завершен успешно!
            
            📄 Название: $title
            📁 Файл: $fileName
            
            📊 Сводка:
            $summary
            
            Файл готов для передачи или печати.
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Экспорт завершен")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .setNeutralButton("Поделиться") { _, _ ->
                shareExportFile(File(filePath.substringAfter(": ")))
            }
            .show()
    }

    private fun showExportedFiles() {
        val exportFiles = FileUtils.getExportFiles(this)

        if (exportFiles.isEmpty()) {
            Toast.makeText(this, "Экспортированные файлы не найдены", Toast.LENGTH_SHORT).show()
            return
        }

        val info = buildString {
            appendLine("📂 ЭКСПОРТИРОВАННЫЕ ФАЙЛЫ")
            appendLine("=".repeat(50))
            appendLine()

            exportFiles.forEachIndexed { index, file ->
                val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified))
                val type = when (file.extension) {
                    "pdf" -> "📄 PDF"
                    "xlsx", "xls" -> "📊 Excel"
                    "csv" -> "📈 CSV"
                    "json" -> "🔤 JSON"
                    else -> "📁 ${file.extension.uppercase(Locale.getDefault())}"
                }

                appendLine("${index + 1}. ${file.name}")
                appendLine("   $type | 📅 $date | 📏 ${file.readableSize}")
                appendLine()
            }

            appendLine("=".repeat(50))
            appendLine("Всего файлов: ${exportFiles.size}")
            appendLine("Общий размер: ${exportFiles.sumOf { it.size } / 1024} КБ")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Экспортированные файлы")
            .setMessage(info)
            .setPositiveButton("Закрыть", null)
            .setNeutralButton("Поделиться всеми") { _, _ ->
                shareAllExports(exportFiles)
            }
            .show()
    }

    private fun shareExportFile(file: File) {
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this, "$packageName.provider", file)
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when (file.extension.lowercase(Locale.getDefault())) {
                    "pdf" -> "application/pdf"
                    "xlsx", "xls" -> "application/vnd.ms-excel"
                    "csv" -> "text/csv"
                    "json" -> "application/json"
                    else -> "*/*"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                putExtra(Intent.EXTRA_TEXT, "Экспорт данных из SmartKassa от ${SimpleDateFormat("dd.MM.yyyy").format(Date())}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Поделиться файлом"))
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при открытии файла: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareAllExports(exportFiles: List<ExportFileInfo>) {
        if (exportFiles.isEmpty()) return

        val text = buildString {
            appendLine("📊 ЭКСПОРТ ДАННЫХ ИЗ SMARTKASSA")
            appendLine("Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
            appendLine("Всего файлов: ${exportFiles.size}")
            appendLine()

            exportFiles.forEach { file ->
                val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(file.lastModified))
                appendLine("📁 ${file.name}")
                appendLine("   📅 $date | 📏 ${file.readableSize}")
                appendLine()
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Экспорт данных SmartKassa")
            putExtra(Intent.EXTRA_TEXT, text)
        }

        startActivity(Intent.createChooser(intent, "Поделиться информацией об экспорте"))
    }


}