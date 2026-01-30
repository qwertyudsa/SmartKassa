package com.example.smartkassa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvTodaySales: TextView
    private lateinit var tvTodayRevenue: TextView
    private lateinit var tvProductsCount: TextView
    private lateinit var rvRecentActivity: RecyclerView
    private lateinit var btnViewAll: MaterialButton

    private lateinit var dbHelper: DatabaseHelper
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dbHelper = DatabaseHelper(this)

        // Загружаем текущего пользователя
        loadCurrentUser()

        // Если пользователь не найден, возвращаем на экран логина
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupClickListeners()
        updateStatistics()

    }

    private fun loadCurrentUser() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("current_user_id", null)

        if (userId != null) {
            // Ищем пользователя в базе данных
            currentUser = dbHelper.getUserById(userId)
        }

        // Если пользователь не найден в базе, пробуем найти по email
        if (currentUser == null) {
            val userEmail = sharedPref.getString("current_user_email", null)
            if (userEmail != null) {
                currentUser = dbHelper.getUserByEmail(userEmail)
            }
        }
    }

    private fun initViews() {
        tvTodaySales = findViewById(R.id.tvTodaySales)
        tvTodayRevenue = findViewById(R.id.tvTodayRevenue)
        tvProductsCount = findViewById(R.id.tvProductsCount)
        rvRecentActivity = findViewById(R.id.rvRecentActivity)
        btnViewAll = findViewById(R.id.btnViewAll)

        // Установка тулбара
        setSupportActionBar(findViewById(R.id.toolbar))

        // Устанавливаем название бизнеса в тулбар
        currentUser?.let { user ->
            supportActionBar?.title = user.businessName
        }
    }

    private fun setupRecyclerView() {
        // Загружаем активности из базы данных
        val activities = dbHelper.getRecentActivities(4) // Последние 4 операции

        val adapter = ActivityAdapter(activities) { activity ->
            // Обработчик клика на активность в главном экране
            showActivityDetails(activity)
        }
        rvRecentActivity.layoutManager = LinearLayoutManager(this)
        rvRecentActivity.adapter = adapter
    }

    private fun updateStatistics() {
        // Получаем статистику из базы данных
        val todaySalesCount = dbHelper.getTodaySalesCount()
        val todayRevenue = dbHelper.getTodayRevenue()
        val productsCount = dbHelper.getProductsCount()

        // Обновляем UI
        tvTodaySales.text = todaySalesCount.toString()
        tvTodayRevenue.text = "%,.0f ₽".format(todayRevenue)
        tvProductsCount.text = productsCount.toString()

        // Обновляем список активностей
        updateRecentActivities()
    }

    private fun updateRecentActivities() {
        val activities = dbHelper.getRecentActivities(4)
        val adapter = ActivityAdapter(activities) { activity ->
            // Обработчик клика на активность в главном экране
            showActivityDetails(activity)
        }
        rvRecentActivity.adapter = adapter
    }
    private fun showActivityDetails(activity: ActivityItem) {
        val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(activity.date)

        val details = """
        Тип: ${activity.type}
        Дата: $formattedDate
        Сумма: ${activity.amount}
        Описание: ${activity.description}
    """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Детали операции")
            .setMessage(details)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun setupClickListeners() {
        try {
            // Карточка продажи
            val cardSale = findViewById<MaterialCardView>(R.id.cardSale)
            cardSale?.setOnClickListener {
                showToast("Открытие экрана продажи")
                startActivity(Intent(this, SaleActivity::class.java))
            }

            // Карточка поступления
            val cardIncome = findViewById<MaterialCardView>(R.id.cardIncome)
            cardIncome?.setOnClickListener {
                showToast("Открытие экрана поступления товаров")
                startActivity(Intent(this, IncomeActivity::class.java))
            }

            // Карточка товаров
            val cardProducts = findViewById<MaterialCardView>(R.id.cardProducts)
            cardProducts?.setOnClickListener {
                showToast("Открытие списка товаров")
                startActivity(Intent(this, ProductsActivity::class.java))
            }

            // Карточка отчетов
            val cardReports = findViewById<MaterialCardView>(R.id.cardReports)
            cardReports?.setOnClickListener {
                showToast("Открытие отчетов")
                startActivity(Intent(this, ReportsActivity::class.java))
            }
            val cardRecipes = findViewById<MaterialCardView>(R.id.cardRecipes)
            cardRecipes?.setOnClickListener {
                startActivity(Intent(this, RecipesActivity::class.java))
            }

            // Кнопка просмотра всех операций
            btnViewAll?.setOnClickListener {
                showToast("Просмотр всех операций")
                startActivity(Intent(this, AllActivitiesActivity::class.java))
            }

        } catch (e: Exception) {
            showToast("Ошибка инициализации кнопок: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем статистику при возвращении на главный экран
        updateStatistics()
        // Загружаем актуальные данные пользователя
        loadCurrentUser()

        // Обновляем заголовок
        currentUser?.let { user ->
            supportActionBar?.title = user.businessName
        }
        updateToolbarTitle()
    }
    private fun updateToolbarTitle() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val businessName = sharedPref.getString("current_business_name", "Мой бизнес")
        supportActionBar?.title = businessName
    }


    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_logout -> {
                performLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun performLogout() {
        // Очищаем данные текущего пользователя
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("current_user_id")
            remove("current_user_email")
            apply()
        }

        // Возвращаем на экран логина
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}