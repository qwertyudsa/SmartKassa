package com.example.smartkassa

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.smartkassa.DatabaseHelper.Companion.COLUMN_CATEGORY
import com.example.smartkassa.DatabaseHelper.Companion.COLUMN_COST_PRICE
import com.example.smartkassa.DatabaseHelper.Companion.COLUMN_IS_INGREDIENT
import com.example.smartkassa.DatabaseHelper.Companion.COLUMN_NAME
import com.example.smartkassa.DatabaseHelper.Companion.COLUMN_PRICE
import com.example.smartkassa.DatabaseHelper.Companion.COLUMN_PRODUCT_ID
import com.example.smartkassa.DatabaseHelper.Companion.COLUMN_STOCK
import com.example.smartkassa.DatabaseHelper.Companion.COLUMN_UNIT
import com.example.smartkassa.DatabaseHelper.Companion.TABLE_PRODUCTS
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnQuickLogin: MaterialButton
    private lateinit var cbRememberMe: MaterialCheckBox

    private var doubleBackToExitPressedOnce = false
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Инициализируем DatabaseHelper
        dbHelper = DatabaseHelper(this)

        initViews()
        setupClickListeners()
        setupBackPressedHandler()
        checkRememberedUser()
        initializeSampleData() // Добавляем тестовые данные при первом запуске
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnQuickLogin = findViewById(R.id.btnQuickLogin)

    }

    private fun initializeSampleData() {
        // Проверяем, нужно ли создавать тестовые данные
        val productsCount = dbHelper.getAllProducts().size
        val usersCount = dbHelper.getAllUsers().size

        if (usersCount == 0) {
            // Создаем администратора
            val adminUser = User(
                id = IdGenerator.generateId(),
                email = "admin@example.com",
                password = "admin123",
                businessName = "Моя Пекарня",
                currency = "RUB",
                taxRate = 20.0
            )

            dbHelper.addUser(adminUser)
            Log.d("LoginActivity", "Администратор создан")
        }

        if (productsCount == 0) {
            // Создаем тестовые продукты
            createSampleProducts()
            createSampleActivities()
        }
    }


    private fun createSampleProducts() {
        // Убедимся, что таблица продуктов существует
        val db = dbHelper.writableDatabase

        // Готовые изделия
        val products = listOf(
            Product(
                id = IdGenerator.generateId(),
                name = "Хлеб белый",
                price = 50.0,
                stock = 20.0,
                category = "Хлеб",
                costPrice = 20.0,
                isIngredient = false,
                unit = "шт"
            ),
            Product(
                id = IdGenerator.generateId(),
                name = "Хлеб черный",
                price = 60.0,
                stock = 15.0,
                category = "Хлеб",
                costPrice = 25.0,
                isIngredient = false,
                unit = "шт"
            ),
            Product(
                id = IdGenerator.generateId(),
                name = "Булочка с маком",
                price = 30.0,
                stock = 50.0,
                category = "Выпечка",
                costPrice = 12.0,
                isIngredient = false,
                unit = "шт"
            )
        )

        // Ингредиенты
        val ingredients = listOf(
            Product(
                id = IdGenerator.generateId(),
                name = "Мука пшеничная",
                price = 0.0,
                stock = 100.0,
                category = "Ингредиенты",
                costPrice = 40.0,
                isIngredient = true,
                unit = "кг"
            ),
            Product(
                id = IdGenerator.generateId(),
                name = "Сахар",
                price = 0.0,
                stock = 50.0,
                category = "Ингредиенты",
                costPrice = 60.0,
                isIngredient = true,
                unit = "кг"
            )
        )

        // Сохраняем все
        for (product in products + ingredients) {
            val values = ContentValues().apply {
                put(COLUMN_PRODUCT_ID, product.id)
                put(COLUMN_NAME, product.name)
                put(COLUMN_PRICE, product.price)
                put(COLUMN_COST_PRICE, product.costPrice)
                put(COLUMN_STOCK, product.stock)
                put(COLUMN_CATEGORY, product.category)
                put(COLUMN_IS_INGREDIENT, if (product.isIngredient) 1 else 0)
                put(COLUMN_UNIT, product.unit)
            }

            val result = db.insert(TABLE_PRODUCTS, null, values)
            Log.d("LoginActivity", "Товар добавлен: ${product.name}, результат: $result")
        }

        db.close()
    }

    private fun createSampleActivities() {
        val activities = listOf(
            ActivityItem(
                id = IdGenerator.generateId(),
                type = "Поступление",
                description = "Мука пшеничная - 50 кг",
                amount = "-2,000 ₽",
                time = "09:00"
            ),
            ActivityItem(
                id = IdGenerator.generateId(),
                type = "Поступление",
                description = "Сахар - 30 кг",
                amount = "-1,800 ₽",
                time = "09:15"
            ),
            ActivityItem(
                id = IdGenerator.generateId(),
                type = "Продажа",
                description = "Хлеб белый - 5 шт",
                amount = "+250 ₽",
                time = "10:30"
            ),
            ActivityItem(
                id = IdGenerator.generateId(),
                type = "Продажа",
                description = "Булочка с маком - 10 шт",
                amount = "+300 ₽",
                time = "11:45"
            ),
            ActivityItem(
                id = IdGenerator.generateId(),
                type = "Продажа",
                description = "Круассан - 3 шт",
                amount = "+135 ₽",
                time = "12:20"
            )
        )

        // Добавляем активности
        for (activity in activities) {
            dbHelper.addActivity(activity)
        }

        Log.d("LoginActivity", "Создано ${activities.size} тестовых активностей")
    }

    private fun checkRememberedUser() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPref.getString("saved_email", null)

        if (!savedEmail.isNullOrEmpty()) {
            etEmail.setText(savedEmail)
            cbRememberMe.isChecked = true
        }
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            performLogin()
        }

        btnQuickLogin.setOnClickListener {
            performQuickLogin()
        }

    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (validateInput(email, password)) {
            // Ищем пользователя в базе данных
            val user = dbHelper.getUserByEmail(email)

            if (user != null && user.password == password) {
                // Сохраняем данные для запоминания
                if (cbRememberMe.isChecked) {
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit().putString("saved_email", email).apply()
                }

                // Сохраняем текущего пользователя
                saveCurrentUser(user)

                // Показываем анимацию успешного входа
                showSuccessAnimation()

                Toast.makeText(this, "Вход выполнен успешно!", Toast.LENGTH_SHORT).show()

                // Задержка перед переходом для отображения анимации
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }, 1000)

            } else {
                showErrorAnimation()
                Toast.makeText(this, "Неверный email или пароль", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performQuickLogin() {
        // Проверяем, есть ли гостевой пользователь
        var guestUser = dbHelper.getUserByEmail("guest@example.com")

        if (guestUser == null) {
            // Создаем гостевого пользователя
            guestUser = User(
                id = IdGenerator.generateId(),
                email = "guest@example.com",
                password = "guest123",
                businessName = "Гостевой доступ",
                currency = "RUB",
                taxRate = 20.0
            )
            dbHelper.addUser(guestUser)
        }

        saveCurrentUser(guestUser)

        showSuccessAnimation()
        Toast.makeText(this, "Быстрый вход выполнен!", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1000)
    }

    private fun saveCurrentUser(user: User) {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("current_user_id", user.id)
            putString("current_user_email", user.email)
            putString("current_business_name", user.businessName)
            apply()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        // Валидация email
        if (email.isEmpty()) {
            tilEmail.error = "Введите email"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Введите корректный email"
            isValid = false
        } else {
            tilEmail.error = null
        }

        // Валидация пароля
        if (password.isEmpty()) {
            tilPassword.error = "Введите пароль"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Пароль должен содержать минимум 6 символов"
            isValid = false
        } else {
            tilPassword.error = null
        }

        return isValid
    }


    private fun showSuccessAnimation() {
        val scaleX = ObjectAnimator.ofFloat(btnLogin, "scaleX", 1f, 0.9f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(btnLogin, "scaleY", 1f, 0.9f, 1.1f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }

    private fun showErrorAnimation() {
        val shake = ObjectAnimator.ofFloat(tilEmail, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        shake.duration = 500
        shake.start()
    }

    private fun setupBackPressedHandler() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    // Если уже нажали назад, завершаем приложение
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    return
                }

                doubleBackToExitPressedOnce = true
                Toast.makeText(this@LoginActivity, "Нажмите еще раз для выхода", Toast.LENGTH_SHORT).show()

                Handler(mainLooper).postDelayed({
                    doubleBackToExitPressedOnce = false
                }, 2000)
            }
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}