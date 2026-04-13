package com.example.smartkassa

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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

class IncomeActivity : AppCompatActivity() {

    private lateinit var etSupplier: TextInputEditText
    private lateinit var etInvoiceNumber: TextInputEditText
    private lateinit var rvIncomeItems: RecyclerView
    private lateinit var tvTotalItems: TextView
    private lateinit var tvTotalCost: TextView
    private lateinit var btnAddProductToIncome: MaterialButton
    private lateinit var btnSaveIncome: MaterialButton
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var incomeAdapter: IncomeAdapter

    // Товары в текущей поставке
    private val incomeItems = mutableListOf<IncomeItem>()

    // Список поставщиков
    private val suppliers = mutableListOf<Supplier>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income)

        // Инициализируем DatabaseHelper
        dbHelper = DatabaseHelper(this)

        initViews()
        setupRecyclerView()
        setupClickListeners()
        updateTotals()
        loadSuppliers()
    }

    private fun initViews() {
        etSupplier = findViewById(R.id.etSupplier)
        etInvoiceNumber = findViewById(R.id.etInvoiceNumber)
        rvIncomeItems = findViewById(R.id.rvIncomeItems)
        tvTotalItems = findViewById(R.id.tvTotalItems)
        tvTotalCost = findViewById(R.id.tvTotalCost)
        btnAddProductToIncome = findViewById(R.id.btnAddProductToIncome)
        btnSaveIncome = findViewById(R.id.btnSaveIncome)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Поступление товаров"
    }

    private fun setupRecyclerView() {
        incomeAdapter = IncomeAdapter(
            items = incomeItems,
            onItemRemoved = { position ->
                updateTotals()
            },
            onQuantityChanged = { item, position ->
                updateTotals()
            }
        )

        rvIncomeItems.layoutManager = LinearLayoutManager(this)
        rvIncomeItems.adapter = incomeAdapter
    }

    private fun setupClickListeners() {
        btnAddProductToIncome.setOnClickListener {
            showAddProductDialog()
        }

        btnSaveIncome.setOnClickListener {
            saveIncome()
        }

        // При клике на поле поставщика показываем список поставщиков
        etSupplier.setOnClickListener {
            showSuppliersDialog()
        }
    }

    private fun loadSuppliers() {
        suppliers.clear()
        suppliers.addAll(dbHelper.getAllSuppliers())

        // Если есть поставщики, предлагаем выбрать
        if (suppliers.isNotEmpty()) {
            etSupplier.hint = "Нажмите чтобы выбрать поставщика"
        }
    }

    private fun showSuppliersDialog() {
        if (suppliers.isEmpty()) {
            // Если поставщиков нет, предлагаем добавить нового
            showAddSupplierDialog()
            return
        }

        val supplierNames = suppliers.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Выберите поставщика")
            .setItems(supplierNames) { _, which ->
                val selectedSupplier = suppliers[which]
                etSupplier.setText(selectedSupplier.name)
                etSupplier.tag = selectedSupplier.id // Сохраняем ID поставщика в tag
            }
            .setNeutralButton("Добавить нового") { _, _ ->
                showAddSupplierDialog()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showAddSupplierDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_supplier, null)

        val etName = dialogView.findViewById<TextInputEditText>(R.id.etSupplierName)
        val etContact = dialogView.findViewById<TextInputEditText>(R.id.etContactPerson)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.etPhone)
        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.etEmail)

        MaterialAlertDialogBuilder(this)
            .setTitle("Добавление поставщика")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val name = etName.text.toString().trim()
                val contact = etContact.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val email = etEmail.text.toString().trim()

                if (name.isNotEmpty()) {
                    val supplier = Supplier(
                        id = IdGenerator.generateId(),
                        name = name,
                        contactPerson = if (contact.isNotEmpty()) contact else null,
                        phone = if (phone.isNotEmpty()) phone else null,
                        email = if (email.isNotEmpty()) email else null
                    )

                    val result = dbHelper.addSupplier(supplier)
                    if (result != -1L) {
                        suppliers.add(supplier)
                        etSupplier.setText(supplier.name)
                        etSupplier.tag = supplier.id
                        Toast.makeText(this, "Поставщик добавлен", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Введите название поставщика", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showAddProductDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_product, null)

        val etSearch = dialogView.findViewById<TextInputEditText>(R.id.etSearchProduct)
        val rvProducts = dialogView.findViewById<RecyclerView>(R.id.rvProducts)
        val tvEmpty = dialogView.findViewById<TextView>(R.id.tvEmpty)

        val adapter = SearchProductForIncomeAdapter(emptyList()) { product ->
            addProductToIncome(product)
            // Закрываем диалог после выбора
            (etSearch.parent.parent as? DialogInterface)?.dismiss()
        }

        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = adapter

        // Загружаем все товары и ингредиенты
        loadProductsForIncome(adapter, tvEmpty, "")

        // Поиск при вводе
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                loadProductsForIncome(adapter, tvEmpty, s.toString())
            }
        })

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Выберите товар или ингредиент")
            .setView(dialogView)
            .setNegativeButton("Закрыть", null)
            .create()

        dialog.show()
    }

    private fun loadProductsForIncome(adapter: SearchProductForIncomeAdapter, tvEmpty: TextView, query: String) {
        val allProducts = if (query.isEmpty()) {
            // Загружаем все товары и ингредиенты
            dbHelper.getAllProducts()
        } else {
            // Ищем по запросу
            dbHelper.searchProducts(query)
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

    private fun addProductToIncome(product: Product) {
        // Проверяем, есть ли уже такой товар в поставке
        val existingItemIndex = incomeItems.indexOfFirst { it.productId == product.id }

        if (existingItemIndex != -1) {
            // Увеличиваем количество существующего товара
            val item = incomeItems[existingItemIndex]
            item.quantity++
            item.total = item.costPerUnit * item.quantity
            incomeAdapter.notifyItemChanged(existingItemIndex)
            updateTotals()
            Toast.makeText(this, "Количество увеличено", Toast.LENGTH_SHORT).show()
        } else {
            showCostInputDialog(product)
        }
    }

    private fun showCostInputDialog(product: Product) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cost_input, null)

        val tvProductName = dialogView.findViewById<TextView>(R.id.tvProductName)
        val etCost = dialogView.findViewById<TextInputEditText>(R.id.etCost)
        val tvUnit = dialogView.findViewById<TextView>(R.id.tvUnit)

        tvProductName.text = product.name
        tvUnit.text = "за ${product.unit}"

        // Устанавливаем текущую себестоимость в подсказку
        etCost.hint = "Текущая: ${"%.2f".format(product.costPrice)} ₽"

        // Устанавливаем фокус на поле ввода
        etCost.requestFocus()

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Введите стоимость")
            .setView(dialogView)
            .setPositiveButton("Добавить") { dialog, _ ->
                val costStr = etCost.text.toString().trim()
                val cost = costStr.toDoubleOrNull()

                if (cost != null) {
                    // Проверка на отрицательную стоимость
                    if (cost < 0) {
                        Toast.makeText(this, "Стоимость не может быть отрицательной", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Проверка на ноль
                    if (cost == 0.0) {
                        showZeroCostConfirmation(product, cost)
                        return@setPositiveButton
                    }

                    // Проверка на превышение текущей себестоимости
                    if (cost > product.costPrice && product.costPrice > 0) {
                        showCostExceedWarning(product, cost)
                        return@setPositiveButton
                    }

                    // Проверка на разницу в себестоимости
                    if (product.costPrice > 0 && Math.abs(cost - product.costPrice) > 0.01) {
                        showCostDifferenceDialog(product, cost)
                        return@setPositiveButton
                    }
                    if (cost != null && cost > 0) {
                        // ПРОВЕРКА: Себестоимость не должна превышать цену продажи
                        if (cost > product.price && product.price > 0) {
                            showCostExceedsPriceWarning(product, cost)
                        } else {
                            addIncomeItem(product, cost)
                        }
                    } else {
                        Toast.makeText(this, "Введите корректную стоимость", Toast.LENGTH_SHORT).show()
                    }

                    // Если все проверки пройдены, добавляем товар
                    addIncomeItem(product, cost)
                } else {
                    Toast.makeText(this, "Введите корректную стоимость", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()

        // Показываем клавиатуру
        Handler(Looper.getMainLooper()).postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etCost, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }
    private fun showCostExceedsPriceWarning(product: Product, cost: Double) {
        val warningMessage = """
        ⚠️ Себестоимость превышает цену продажи!
        
        Товар: ${product.name}
        Цена продажи: ${"%.2f".format(product.price)} ₽
        Введенная себестоимость: ${"%.2f".format(cost)} ₽
        Превышение: ${"%.2f".format(cost - product.price)} ₽
        
        При такой себестоимости продажа будет убыточной!
        
        Выберите действие:
    """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Предупреждение!")
            .setMessage(warningMessage)
            .setPositiveButton("Использовать цену продажи как себестоимость") { _, _ ->
                // Используем цену продажи как себестоимость
                addIncomeItem(product, product.price)
            }
            .setNegativeButton("Ввести другую стоимость") { _, _ ->
                // Показать диалог снова
                showCostInputDialog(product)
            }
            .setNeutralButton("Все равно добавить") { _, _ ->
                // Добавляем несмотря на предупреждение
                addIncomeItem(product, cost)
            }
            .show()
    }

    private fun showZeroCostConfirmation(product: Product, cost: Double) {
        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Нулевая стоимость")
            .setMessage("Вы ввели нулевую стоимость для товара:\n\n" +
                    "${product.name}\n\n" +
                    "Это может быть ошибкой.\n\n" +
                    "Вы уверены, что хотите продолжить?")
            .setPositiveButton("Да, продолжить") { _, _ ->
                // Проверка на разницу в себестоимости
                if (product.costPrice > 0 && Math.abs(cost - product.costPrice) > 0.01) {
                    showCostDifferenceDialog(product, cost)
                } else {
                    addIncomeItem(product, cost)
                }
            }
            .setNegativeButton("Изменить", null)
            .show()
    }

    private fun showCostExceedWarning(product: Product, newCost: Double) {
        val differencePercent = if (product.costPrice > 0) {
            ((newCost - product.costPrice) / product.costPrice * 100)
        } else {
            0.0
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Повышенная стоимость")
            .setMessage("Введенная стоимость ${"%.2f".format(newCost)} ₽ превышает текущую себестоимость ${"%.2f".format(product.costPrice)} ₽ на ${"%.1f".format(differencePercent)}%\n\n" +
                    "Товар: ${product.name}\n\n" +
                    "Это может быть:\n" +
                    "• Ошибка ввода\n" +
                    "• Повышение закупочной цены\n" +
                    "• Разный поставщик\n\n" +
                    "Выберите действие:")
            .setPositiveButton("Создать новый товар") { _, _ ->
                createNewProductWithDifferentCost(product, newCost)
            }
            .setNegativeButton("Изменить стоимость") { _, _ ->
                // Показываем диалог снова
                showCostInputDialog(product)
            }
            .setNeutralButton("Обновить себестоимость") { _, _ ->
                updateProductCostPrice(product, newCost)
            }
            .show()
    }

    private fun showCostDifferenceDialog(product: Product, newCost: Double) {
        val differencePercent = if (product.costPrice > 0) {
            ((newCost - product.costPrice) / product.costPrice * 100).toInt()
        } else {
            0
        }

        val differenceAbs = Math.abs(newCost - product.costPrice)

        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Разная себестоимость")
            .setMessage("Введенная стоимость ${"%.2f".format(newCost)} ₽ отличается от текущей себестоимости ${"%.2f".format(product.costPrice)} ₽ на ${"%.2f".format(differenceAbs)} ₽ (${differencePercent}%)\n\n" +
                    "Товар: ${product.name}\n\n" +
                    "Это может быть:\n" +
                    "• Новая партия товара\n" +
                    "• Разный поставщик\n" +
                    "• Изменение закупочной цены\n\n" +
                    "Выберите действие:")
            .setPositiveButton("Создать новый товар") { _, _ ->
                createNewProductWithDifferentCost(product, newCost)
            }
            .setNegativeButton("Использовать текущую себестоимость") { _, _ ->
                addIncomeItem(product, product.costPrice)
            }
            .setNeutralButton("Обновить себестоимость") { _, _ ->
                updateProductCostPrice(product, newCost)
            }
            .show()
    }

    private fun createNewProductWithDifferentCost(product: Product, newCost: Double) {
        // Создаем диалог для ввода названия нового товара
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_product_name, null)
        val etNewName = dialogView.findViewById<TextInputEditText>(R.id.etNewProductName)

        // Предлагаем название с указанием стоимости
        val suggestedName = "${product.name} (${"%.2f".format(newCost)} ₽)"
        etNewName.setText(suggestedName)

        MaterialAlertDialogBuilder(this)
            .setTitle("Создание нового товара")
            .setMessage("Создается копия товара с новой себестоимостью.\n\n" +
                    "Старая себестоимость: ${"%.2f".format(product.costPrice)} ₽\n" +
                    "Новая себестоимость: ${"%.2f".format(newCost)} ₽")
            .setView(dialogView)
            .setPositiveButton("Создать") { _, _ ->
                val newName = etNewName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val newProduct = product.copy(
                        id = IdGenerator.generateId(),
                        name = newName,
                        costPrice = newCost,
                        stock = 0.0 // Новый товар без остатка
                    )

                    // Сохраняем новый товар в базу
                    dbHelper.addProduct(newProduct)

                    // Добавляем в поставку
                    addIncomeItem(newProduct, newCost)

                    Toast.makeText(this, "Создан новый товар: $newName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Введите название нового товара", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateProductCostPrice(product: Product, newCostPrice: Double) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Обновление себестоимости")
            .setMessage("Вы обновляете себестоимость товара:\n\n" +
                    "${product.name}\n\n" +
                    "Старая: ${"%.2f".format(product.costPrice)} ₽\n" +
                    "Новая: ${"%.2f".format(newCostPrice)} ₽\n\n" +
                    "Это повлияет на расчет прибыли для всех продаж этого товара.\n\n" +
                    "Вы уверены?")
            .setPositiveButton("Обновить") { _, _ ->
                val updatedProduct = product.copy(costPrice = newCostPrice)
                val success = dbHelper.updateProduct(updatedProduct)

                if (success) {
                    Toast.makeText(this, "Себестоимость обновлена", Toast.LENGTH_SHORT).show()
                    addIncomeItem(updatedProduct, newCostPrice)
                } else {
                    Toast.makeText(this, "Ошибка обновления себестоимости", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addIncomeItem(product: Product, costPerUnit: Double) {
        // Проверяем, есть ли уже такой товар в поставке
        val existingItemIndex = incomeItems.indexOfFirst { it.productId == product.id }

        if (existingItemIndex != -1) {
            // Увеличиваем количество существующего товара
            val item = incomeItems[existingItemIndex]
            item.quantity++
            item.total = item.costPerUnit * item.quantity
            incomeAdapter.notifyItemChanged(existingItemIndex)
            updateTotals()
            Toast.makeText(this, "Количество увеличено", Toast.LENGTH_SHORT).show()
        } else {
            // Добавляем новый товар
            val newItem = IncomeItem(
                productId = product.id,
                productName = product.name,
                costPerUnit = costPerUnit,
                quantity = 1,
                total = costPerUnit,
                unit = product.unit
            )

            incomeItems.add(newItem)
            incomeAdapter.notifyItemInserted(incomeItems.size - 1)
            updateTotals()

            // Показываем информацию о добавленном товаре
            Toast.makeText(this,
                "Добавлен: ${product.name}\n" +
                        "Стоимость: ${"%.2f".format(costPerUnit)} ₽/${product.unit}",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun updateTotals() {
        val totalItems = incomeItems.sumOf { it.quantity }
        val totalCost = incomeItems.sumOf { it.total }

        tvTotalItems.text = totalItems.toString()
        tvTotalCost.text = "%.2f ₽".format(totalCost)

        // Активируем кнопку сохранения только если есть товары
        btnSaveIncome.isEnabled = incomeItems.isNotEmpty()
    }

    private fun saveIncome() {
        if (incomeItems.isEmpty()) {
            Toast.makeText(this, "Добавьте товары в поставку", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверяем, есть ли товары с нулевой стоимостью
        val zeroCostItems = incomeItems.filter { it.costPerUnit == 0.0 }
        if (zeroCostItems.isNotEmpty()) {
            showZeroCostItemsWarning(zeroCostItems)
            return
        }

        val supplierName = etSupplier.text.toString().trim()
        if (supplierName.isEmpty()) {
            Toast.makeText(this, "Введите поставщика", Toast.LENGTH_SHORT).show()
            return
        }


        // Создаем или получаем поставщика
        val supplier = if (suppliers.any { it.name == supplierName }) {
            suppliers.find { it.name == supplierName }!!
        } else {
            val newSupplier = Supplier(
                id = IdGenerator.generateId(),
                name = supplierName
            )
            dbHelper.addSupplier(newSupplier)
            newSupplier
        }

        // Создаем запись о поставке
        val incomeRecord = IncomeRecord(
            id = IdGenerator.generateIncomeId(),
            date = Date(),
            supplierId = supplier.id,
            invoiceNumber = etInvoiceNumber.text.toString().trim(),
            items = incomeItems.toList(),
            totalCost = incomeItems.sumOf { it.total }
        )

        try {
            // Используем метод DatabaseHelper
            val result = dbHelper.addIncomeRecord(incomeRecord)

            if (result != -1L) {
                // Добавляем активность
                val activity = ActivityItem(
                    id = IdGenerator.generateId(),
                    type = "Поступление",
                    description = "${incomeItems.size} позиций от $supplierName",
                    amount = "-${"%.2f".format(incomeRecord.totalCost)} ₽",
                    time = TimeUtils.getCurrentTime(),
                    date = Date()
                )

                dbHelper.addActivity(activity)

                Toast.makeText(this, "Поставка сохранена на сумму ${"%.2f".format(incomeRecord.totalCost)} ₽",
                    Toast.LENGTH_LONG).show()

                // Очищаем форму
                incomeItems.clear()
                incomeAdapter.notifyDataSetChanged()
                updateTotals()

                // Возвращаемся на главный экран
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }, 1500)

            } else {
                Toast.makeText(this, "Ошибка сохранения поставки", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    private fun showZeroCostItemsWarning(zeroCostItems: List<IncomeItem>) {
        val itemNames = zeroCostItems.joinToString("\n") { "• ${it.productName}" }

        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Товары с нулевой стоимостью")
            .setMessage("Обнаружены товары с нулевой стоимостью:\n\n" +
                    "$itemNames\n\n" +
                    "Это может быть ошибкой. Вы уверены, что хотите продолжить?")
            .setPositiveButton("Продолжить") { _, _ ->
                // Продолжить сохранение
                proceedWithSaveIncome()
            }
            .setNegativeButton("Исправить") { _, _ ->
                // Не сохраняем, возвращаем к редактированию
            }
            .show()
    }

    private fun proceedWithSaveIncome() {
        val supplierName = etSupplier.text.toString().trim()

        // Создаем или получаем поставщика
        val supplier = if (suppliers.any { it.name == supplierName }) {
            suppliers.find { it.name == supplierName }!!
        } else {
            val newSupplier = Supplier(
                id = IdGenerator.generateId(),
                name = supplierName
            )
            dbHelper.addSupplier(newSupplier)
            newSupplier
        }

        // Создаем запись о поставке
        val incomeRecord = IncomeRecord(
            id = IdGenerator.generateIncomeId(),
            date = Date(),
            supplierId = supplier.id,
            invoiceNumber = etInvoiceNumber.text.toString().trim(),
            items = incomeItems.toList(),
            totalCost = incomeItems.sumOf { it.total }
        )

        try {
            val result = dbHelper.addIncomeRecord(incomeRecord)

            if (result != -1L) {
                // Добавляем активность
                val activity = ActivityItem(
                    id = IdGenerator.generateId(),
                    type = "Поступление",
                    description = "${incomeItems.size} позиций от $supplierName",
                    amount = "-${"%.2f".format(incomeRecord.totalCost)} ₽",
                    time = TimeUtils.getCurrentTime(),
                    date = Date()
                )

                dbHelper.addActivity(activity)

                Toast.makeText(this,
                    "Поставка сохранена на сумму ${"%.2f".format(incomeRecord.totalCost)} ₽\n" +
                            "Товаров: ${incomeItems.sumOf { it.quantity }} шт",
                    Toast.LENGTH_LONG).show()

                // Очищаем форму
                incomeItems.clear()
                incomeAdapter.notifyDataSetChanged()
                updateTotals()
                etSupplier.text?.clear()
                etInvoiceNumber.text?.clear()

                // Возвращаемся на главный экран
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }, 2000)

            } else {
                Toast.makeText(this, "Ошибка сохранения поставки", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.income_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_add_supplier -> {
                showAddSupplierDialog()
                true
            }
            R.id.action_clear_income -> {
                clearIncome()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun clearIncome() {
        if (incomeItems.isNotEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Очистка поставки")
                .setMessage("Вы уверены, что хотите очистить все товары из поставки?")
                .setPositiveButton("Очистить") { _, _ ->
                    incomeItems.clear()
                    etSupplier.text?.clear()
                    etInvoiceNumber.text?.clear()
                    incomeAdapter.notifyDataSetChanged()
                    updateTotals()
                    Toast.makeText(this, "Поставка очищена", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            Toast.makeText(this, "Поставка уже пуста", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onBackPressed() {
        if (incomeItems.isNotEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Выход из поставки")
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


