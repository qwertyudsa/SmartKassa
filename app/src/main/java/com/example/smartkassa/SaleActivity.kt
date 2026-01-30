package com.example.smartkassa

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.*

class SaleActivity : AppCompatActivity() {

    private lateinit var etSearchProduct: TextInputEditText
    private lateinit var rvSaleItems: RecyclerView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvItemsCount: TextView
    private lateinit var btnCompleteSale: MaterialButton
    private lateinit var btnAddProduct: MaterialButton

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var saleAdapter: SaleAdapter

    // Товары в текущей продаже
    private val saleItems = mutableListOf<SaleItem>()

    // ID текущего пользователя
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale)

        // Инициализируем DatabaseHelper
        dbHelper = DatabaseHelper(this)

        // Загружаем ID текущего пользователя
        loadCurrentUser()

        initViews()
        setupRecyclerView()
        setupClickListeners()
        updateTotals()
    }

    private fun loadCurrentUser() {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        currentUserId = sharedPref.getString("current_user_id", null)
    }

    private fun initViews() {
        etSearchProduct = findViewById(R.id.etSearchProduct)
        rvSaleItems = findViewById(R.id.rvSaleItems)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvItemsCount = findViewById(R.id.tvItemsCount)
        btnCompleteSale = findViewById(R.id.btnCompleteSale)
        btnAddProduct = findViewById(R.id.btnAddProduct)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Новая продажа"
    }

    private fun setupRecyclerView() {
        saleAdapter = SaleAdapter(
            saleItems,
            onQuantityChanged = { item, position ->
                updateTotals()
            },
            onItemRemoved = { position ->
                removeItem(position)
            }
        )

        rvSaleItems.layoutManager = LinearLayoutManager(this)
        rvSaleItems.adapter = saleAdapter
    }

    private fun setupClickListeners() {
        btnCompleteSale.setOnClickListener {
            completeSale()
        }

        btnAddProduct.setOnClickListener {
            showProductSearchDialog()
        }

        etSearchProduct.setOnClickListener {
            showProductSearchDialog()
        }

        // Быстрый поиск при вводе в основную строку
        etSearchProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length ?: 0 > 2) {
                    // Не открываем диалог автоматически, а просто фильтруем
                    // Можно добавить автодополнение
                }
            }
        })
    }

    private fun showProductSearchDialog(prefilledSearch: String = "") {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_product, null)

        val etSearch = dialogView.findViewById<TextInputEditText>(R.id.etSearchProduct)
        val rvProducts = dialogView.findViewById<RecyclerView>(R.id.rvProducts)
        val tvEmpty = dialogView.findViewById<TextView>(R.id.tvEmpty)

        // Устанавливаем предзаполненный текст поиска
        if (prefilledSearch.isNotEmpty()) {
            etSearch.setText(prefilledSearch)
            etSearch.setSelection(prefilledSearch.length)
        }

        val adapter = SearchProductAdapter(emptyList()) { product ->
            // Не добавляем ингредиенты в продажу
            if (product.isIngredient) {
                Toast.makeText(this, "Ингредиенты не продаются", Toast.LENGTH_SHORT).show()
                return@SearchProductAdapter
            }

            addProductToSale(product)
            // Закрываем диалог после выбора
            (etSearch.parent.parent as? DialogInterface)?.dismiss()
        }

        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = adapter

        // Загружаем все товары (только не ингредиенты) при открытии
        loadProductsForSearch(adapter, tvEmpty, "")

        // Поиск при вводе
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                loadProductsForSearch(adapter, tvEmpty, s.toString())
            }
        })

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Выберите товар")
            .setView(dialogView)
            .setNegativeButton("Закрыть", null)
            .create()

        dialog.show()
    }

    private fun loadProductsForSearch(adapter: SearchProductAdapter, tvEmpty: TextView, query: String) {
        val allProducts = if (query.isEmpty()) {
            // Загружаем все товары (только не ингредиенты)
            dbHelper.getAllProducts().filter { !it.isIngredient }
        } else {
            // Ищем товары по запросу (только не ингредиенты)
            dbHelper.searchProducts(query).filter { !it.isIngredient }
        }

        adapter.updateProducts(allProducts)

        if (allProducts.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = if (query.isEmpty())
                "Товары не найдены"
            else
                "По запросу \"$query\" ничего не найдено"
        } else {
            tvEmpty.visibility = View.GONE
        }
    }

    private fun addProductToSale(product: Product) {
        // Проверяем, есть ли уже такой товар в продаже
        val existingItemIndex = saleItems.indexOfFirst { it.productId == product.id }

        if (existingItemIndex != -1) {
            // Увеличиваем количество существующего товара
            val item = saleItems[existingItemIndex]
            if (item.quantity < product.stock) {
                item.quantity++
                item.total = item.price * item.quantity
                saleAdapter.notifyItemChanged(existingItemIndex)
                updateTotals()
                Toast.makeText(this, "Количество увеличено", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Недостаточно товара на складе", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Добавляем новый товар
            if (product.stock > 0) {
                val newItem = SaleItem(
                    productId = product.id,
                    productName = product.name,
                    price = product.price,
                    quantity = 1,
                    total = product.price
                )
                saleItems.add(newItem)
                saleAdapter.notifyItemInserted(saleItems.size - 1)
                updateTotals()
                Toast.makeText(this, "Товар добавлен", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Товара нет в наличии", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeItem(position: Int) {
        if (position in 0 until saleItems.size) {
            saleItems.removeAt(position)
            saleAdapter.notifyItemRemoved(position)
            updateTotals()
        }
    }

    private fun updateTotals() {
        val totalItems = saleItems.sumOf { it.quantity }
        val totalAmount = saleItems.sumOf { it.total }

        tvItemsCount.text = "Товаров: $totalItems"
        tvTotalAmount.text = "%.2f ₽".format(totalAmount)

        // Активируем кнопку завершения продажи только если есть товары
        btnCompleteSale.isEnabled = saleItems.isNotEmpty()

        // Обновляем подсказку на кнопке
        if (saleItems.isNotEmpty()) {
            btnCompleteSale.text = "Завершить продажу (${"%.2f ₽".format(totalAmount)})"
        } else {
            btnCompleteSale.text = "Завершить продажу"
        }
    }

    private fun completeSale() {
        if (saleItems.isEmpty()) {
            Toast.makeText(this, "Добавьте товары в продажу", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверяем наличие товаров
        for (item in saleItems) {
            val product = dbHelper.getProductById(item.productId)
            if (product == null) {
                Toast.makeText(this, "Товар '${item.productName}' не найден", Toast.LENGTH_LONG).show()
                return
            }
            if (product.stock < item.quantity) {
                Toast.makeText(this,
                    "Недостаточно '${item.productName}': нужно ${item.quantity}, есть ${product.stock}",
                    Toast.LENGTH_LONG).show()
                return
            }
        }

        // Создаем продажу
        val sale = Sale(
            id = IdGenerator.generateSaleId(),
            date = Date(),
            items = saleItems.toList(),
            totalAmount = saleItems.sumOf { it.total },
            paymentMethod = "Наличные",
            userId = currentUserId ?: "guest"
        )

        try {
            // Используем метод DatabaseHelper
            val result = dbHelper.addSale(sale)

            if (result != -1L) {
                // Добавляем активность
                val activity = ActivityItem(
                    id = IdGenerator.generateId(),
                    type = "Продажа",
                    description = "${sale.items.size} товаров",
                    amount = "+${"%.2f".format(sale.totalAmount)} ₽",
                    time = TimeUtils.getCurrentTime(),
                    date = Date()
                )

                dbHelper.addActivity(activity)

                // Показываем чек
                showReceipt(sale)

                // Очищаем корзину
                saleItems.clear()
                saleAdapter.notifyDataSetChanged()
                updateTotals()

                Toast.makeText(this, "Продажа завершена успешно!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Ошибка сохранения продажи", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun showReceipt(sale: Sale) {
        val receiptText = buildString {
            appendLine("========== ЧЕК ==========")
            appendLine("Дата: ${TimeUtils.formatDate(sale.date)}")
            appendLine("Время: ${TimeUtils.formatTime(sale.date)}")
            appendLine("ID: ${sale.id.substring(0, 8)}")
            appendLine("-------------------------")

            sale.items.forEach { item ->
                appendLine("${item.productName}")
                appendLine(" ${item.quantity} x ${"%.2f".format(item.price)} = ${"%.2f".format(item.total)} ₽")
            }

            appendLine("-------------------------")
            appendLine("ИТОГО: ${"%.2f".format(sale.totalAmount)} ₽")
            appendLine("Оплата: ${sale.paymentMethod}")
            appendLine("=========================")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Чек продажи")
            .setMessage(receiptText)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                // Можно вернуться на главный экран
                // finish()
            }
            .setNeutralButton("Печать") { dialog, _ ->
                Toast.makeText(this, "Функция печати в разработке", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Новая продажа") { dialog, _ ->
                dialog.dismiss()
                // Очищаем форму для новой продажи
                saleItems.clear()
                saleAdapter.notifyDataSetChanged()
                updateTotals()
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sale_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_clear_cart -> {
                clearCart()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clearCart() {
        if (saleItems.isNotEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Очистка корзины")
                .setMessage("Вы уверены, что хотите очистить все товары из продажи?")
                .setPositiveButton("Очистить") { _, _ ->
                    saleItems.clear()
                    saleAdapter.notifyDataSetChanged()
                    updateTotals()
                    Toast.makeText(this, "Корзина очищена", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            Toast.makeText(this, "Корзина уже пуста", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (saleItems.isNotEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Выход из продажи")
                .setMessage("У вас есть несохраненные товары. Вы уверены, что хотите выйти?")
                .setPositiveButton("Выйти") { _, _ ->
                    super.onBackPressed()
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}

