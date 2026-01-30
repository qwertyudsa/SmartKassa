package com.example.smartkassa


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class ProductsActivity : AppCompatActivity() {

    private lateinit var etSearch: TextInputEditText
    private lateinit var rvProducts: RecyclerView
    private lateinit var btnFilter: MaterialButton
    private lateinit var btnSort: MaterialButton
    private lateinit var fabAddProduct: FloatingActionButton
    private lateinit var tvEmptyState: TextView
    private val SELECT_CSV_REQUEST = 101
    private lateinit var ingredientsMenuItem: MenuItem
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ProductsAdapter

    // Переменные для состояния
    private var currentProducts = listOf<Product>()
    private var currentFilter: String? = null
    private var currentSort: String = "name_asc"
    private var showOnlyIngredients = false // Флаг для показа только ингредиентов

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        // Инициализируем DatabaseHelper
        dbHelper = DatabaseHelper(this)

        initViews()
        setupRecyclerView()
        setupClickListeners()
        loadProducts()
        // Проверка на невыгодные товары при открытии
        checkUnprofitableProducts()
    }
    private fun checkUnprofitableProducts() {
        Thread {
            val allProducts = dbHelper.getAllProducts()
            val unprofitableCount = allProducts.count { product ->
                !product.isIngredient && product.price > 0 && product.costPrice > 0 &&
                        (product.price - product.costPrice) < 0
            }

            val lowMarginCount = allProducts.count { product ->
                if (!product.isIngredient && product.price > 0 && product.costPrice > 0) {
                    val margin = product.price - product.costPrice
                    val marginPercent = (margin / product.costPrice * 100)
                    margin >= 0 && marginPercent < 10
                } else {
                    false
                }
            }

            runOnUiThread {
                if (unprofitableCount > 0) {
                    showUnprofitableWarning(unprofitableCount, lowMarginCount)
                }
            }
        }.start()
    }

    private fun showUnprofitableWarning(unprofitableCount: Int, lowMarginCount: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Внимание: Невыгодные товары")
            .setMessage("Обнаружено $unprofitableCount убыточных товаров и $lowMarginCount товаров с низкой рентабельностью (<10%).\n\n" +
                    "Рекомендуется:\n" +
                    "1. Пересмотреть цены\n" +
                    "2. Увеличить маржу\n" +
                    "3. Использовать фильтр 'Невыгодные товары' для просмотра")
            .setPositiveButton("Показать убыточные") { _, _ ->
                showOnlyIngredients = false
                currentFilter = "Убыточные"
                loadProducts()
                Toast.makeText(this, "Показаны убыточные товары", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        rvProducts = findViewById(R.id.rvProducts)
        btnFilter = findViewById(R.id.btnFilter)
        btnSort = findViewById(R.id.btnSort)
        fabAddProduct = findViewById(R.id.fabAddProduct)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Товары"
    }
    private fun updateMenuTitle() {
        ingredientsMenuItem.title = if (showOnlyIngredients) "Товары" else "Ингредиенты"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.products_menu, menu)

        // Сохраняем ссылку на пункт меню
        ingredientsMenuItem = menu.findItem(R.id.action_view_ingredients)

        // Обновляем заголовок
        updateMenuTitle()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_view_ingredients -> {
                // Переключаем режим отображения
                showOnlyIngredients = !showOnlyIngredients
                updateMenuTitle()
                // Обновляем заголовок меню
                item.title = if (showOnlyIngredients) "Товары" else "Ингредиенты"

                // Обновляем заголовок активности
                supportActionBar?.title = if (showOnlyIngredients) "Ингредиенты" else "Товары"

                // Загружаем соответствующие данные
                loadProducts()
                true
            }
            R.id.action_categories -> {
                manageCategories()
                true
            }
            R.id.action_bulk_edit -> {
                showBulkEditDialog()
                true
            }
            R.id.action_manage_units -> {
                manageUnits()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun manageUnits() {
        try {
            val units = dbHelper.getAllUnits()

            val dialogView = layoutInflater.inflate(R.layout.dialog_manage_units, null)
            val rvUnits = dialogView.findViewById<RecyclerView>(R.id.rvUnits)
            val btnAddUnit = dialogView.findViewById<MaterialButton>(R.id.btnAddUnit)
            val tvEmpty = dialogView.findViewById<TextView>(R.id.tvEmpty)

            // Проверяем, есть ли единицы
            if (units.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvUnits.visibility = View.GONE
                // Предлагаем добавить стандартные единицы
                btnAddUnit.setOnClickListener {
                    addDefaultUnits()
                    Toast.makeText(this, "Добавлены стандартные единицы", Toast.LENGTH_SHORT).show()
                    manageUnits() // Обновляем диалог
                }
            } else {
                tvEmpty.visibility = View.GONE
                rvUnits.visibility = View.VISIBLE

                val adapter = UnitsAdapter(units) { unit ->
                    showUnitActionsDialog(unit)
                }

                rvUnits.layoutManager = LinearLayoutManager(this)
                rvUnits.adapter = adapter

                btnAddUnit.setOnClickListener {
                    showAddUnitDialog(adapter)
                }
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("Управление единицами измерения")
                .setView(dialogView)
                .setPositiveButton("Закрыть", null)
                .show()

        } catch (e: Exception) {
            Log.e("ProductsActivity", "Ошибка в manageUnits: ${e.message}")
            Toast.makeText(this, "Ошибка загрузки единиц измерения", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showUnitActionsDialog(unit: UnitDefinition) {
        val actions = arrayOf(
            "Редактировать",
            "Удалить",
            "Изменить коэффициент"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Единица: ${unit.name}")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showEditUnitDialog(unit)
                    1 -> showDeleteUnitDialog(unit)
                    2 -> showChangeConversionRateDialog(unit)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showAddUnitDialog(adapter: UnitsAdapter) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_unit, null)

        val etUnitName = dialogView.findViewById<TextInputEditText>(R.id.etUnitName)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spCategory)
        val spBaseUnit = dialogView.findViewById<Spinner>(R.id.spBaseUnit)
        val etConversionRate = dialogView.findViewById<TextInputEditText>(R.id.etConversionRate)

        // Настройка категорий
        val categories = arrayOf("Вес", "Объем", "Штучный")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = categoryAdapter

        // Настройка базовых единиц в зависимости от категории
        val updateBaseUnits = { category: String ->
            val baseUnits = when (category) {
                "Вес" -> arrayOf("кг", "г")
                "Объем" -> arrayOf("л", "мл")
                "Штучный" -> arrayOf("шт", "уп", "пак")
                else -> arrayOf("шт")
            }
            val baseUnitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, baseUnits)
            baseUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spBaseUnit.adapter = baseUnitAdapter
        }

        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateBaseUnits(categories[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        updateBaseUnits("Штучный") // по умолчанию

        MaterialAlertDialogBuilder(this)
            .setTitle("Добавить единицу измерения")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val name = etUnitName.text.toString().trim()
                val category = when (spCategory.selectedItem.toString()) {
                    "Вес" -> UnitDefinition.CATEGORY_WEIGHT
                    "Объем" -> UnitDefinition.CATEGORY_VOLUME
                    "Штучный" -> UnitDefinition.CATEGORY_PIECE
                    else -> UnitDefinition.CATEGORY_PIECE
                }
                val baseUnit = spBaseUnit.selectedItem.toString()
                val conversionRate = etConversionRate.text.toString().toDoubleOrNull() ?: 1.0

                if (name.isNotBlank() && conversionRate > 0) {
                    val unit = UnitDefinition(
                        id = IdGenerator.generateId(),
                        name = name,
                        category = category,
                        baseUnit = baseUnit,
                        conversionRate = conversionRate
                    )

                    if (dbHelper.addUnit(unit) != -1L) {
                        adapter.updateUnits(dbHelper.getAllUnits())
                        Toast.makeText(this, "Единица добавлена", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Такая единица уже существует", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditUnitDialog(unit: UnitDefinition) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_unit, null)

        val etUnitName = dialogView.findViewById<TextInputEditText>(R.id.etUnitName)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spCategory)
        val spBaseUnit = dialogView.findViewById<Spinner>(R.id.spBaseUnit)
        val etConversionRate = dialogView.findViewById<TextInputEditText>(R.id.etConversionRate)

        etUnitName.setText(unit.name)
        etConversionRate.setText(unit.conversionRate.toString())

        // Настройка категорий
        val categories = arrayOf("Вес", "Объем", "Штучный")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = categoryAdapter

        // Устанавливаем выбранную категорию
        val categoryIndex = when (unit.category) {
            UnitDefinition.CATEGORY_WEIGHT -> 0
            UnitDefinition.CATEGORY_VOLUME -> 1
            UnitDefinition.CATEGORY_PIECE -> 2
            else -> 2
        }
        spCategory.setSelection(categoryIndex)

        // Настройка базовых единиц
        val updateBaseUnits = { category: String ->
            val baseUnits = when (category) {
                "Вес" -> arrayOf("кг", "г")
                "Объем" -> arrayOf("л", "мл")
                "Штучный" -> arrayOf("шт", "уп", "пак")
                else -> arrayOf("шт")
            }
            val baseUnitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, baseUnits)
            baseUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spBaseUnit.adapter = baseUnitAdapter

            // Устанавливаем выбранную базовую единицу
            val baseUnitIndex = baseUnits.indexOf(unit.baseUnit)
            if (baseUnitIndex != -1) {
                spBaseUnit.setSelection(baseUnitIndex)
            }
        }

        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateBaseUnits(categories[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        updateBaseUnits(categories[categoryIndex])

        MaterialAlertDialogBuilder(this)
            .setTitle("Редактировать единицу")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = etUnitName.text.toString().trim()
                val category = when (spCategory.selectedItem.toString()) {
                    "Вес" -> UnitDefinition.CATEGORY_WEIGHT
                    "Объем" -> UnitDefinition.CATEGORY_VOLUME
                    "Штучный" -> UnitDefinition.CATEGORY_PIECE
                    else -> UnitDefinition.CATEGORY_PIECE
                }
                val baseUnit = spBaseUnit.selectedItem.toString()
                val conversionRate = etConversionRate.text.toString().toDoubleOrNull() ?: 1.0

                if (name.isNotBlank() && conversionRate > 0) {
                    val updatedUnit = unit.copy(
                        name = name,
                        category = category,
                        baseUnit = baseUnit,
                        conversionRate = conversionRate
                    )

                    if (dbHelper.updateUnit(updatedUnit)) {
                        Toast.makeText(this, "Единица обновлена", Toast.LENGTH_SHORT).show()
                        manageUnits() // обновляем список
                    } else {
                        Toast.makeText(this, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showChangeConversionRateDialog(unit: UnitDefinition) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_conversion, null)

        val tvCurrentRate = dialogView.findViewById<TextView>(R.id.tvCurrentRate)
        val etNewRate = dialogView.findViewById<TextInputEditText>(R.id.etNewRate)

        tvCurrentRate.text = "Текущий коэффициент: 1 ${unit.name} = ${unit.conversionRate} ${unit.baseUnit}"
        etNewRate.hint = "Новый коэффициент (1 ${unit.name} = ? ${unit.baseUnit})"
        etNewRate.setText(unit.conversionRate.toString())

        MaterialAlertDialogBuilder(this)
            .setTitle("Изменить коэффициент")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newRate = etNewRate.text.toString().toDoubleOrNull()

                if (newRate != null && newRate > 0) {
                    val updatedUnit = unit.copy(conversionRate = newRate)

                    if (dbHelper.updateUnit(updatedUnit)) {
                        Toast.makeText(this, "Коэффициент изменен", Toast.LENGTH_SHORT).show()
                        manageUnits() // обновляем список
                    } else {
                        Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Введите корректный коэффициент", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteUnitDialog(unit: UnitDefinition) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Удаление единицы")
            .setMessage("Вы уверены, что хотите удалить единицу измерения \"${unit.name}\"?\n\n" +
                    "Если эта единица используется в товарах, удаление будет невозможно.")
            .setPositiveButton("Удалить") { _, _ ->
                if (dbHelper.deleteUnit(unit.id)) {
                    Toast.makeText(this, "Единица удалена", Toast.LENGTH_SHORT).show()
                    manageUnits() // обновляем список
                } else {
                    Toast.makeText(this, "Нельзя удалить единицу, которая используется в товарах", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addDefaultUnits() {
        val defaultUnits = listOf(
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
                conversionRate = 0.001
            ),
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
                conversionRate = 0.001
            ),
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
                conversionRate = 10.0
            ),
            UnitDefinition(
                id = IdGenerator.generateId(),
                name = "пак",
                category = UnitDefinition.CATEGORY_PIECE,
                baseUnit = UnitDefinition.BASE_PIECE,
                conversionRate = 5.0
            )
        )

        defaultUnits.forEach { unit ->
            dbHelper.addUnit(unit)
        }
    }
    private fun setupRecyclerView() {
        adapter = ProductsAdapter(
            currentProducts,
            onProductClick = { product ->
                openProductEditDialog(product)
            },
            onProductLongClick = { product ->
                showProductActionsDialog(product)
            }
        )

        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = adapter
    }

    private fun setupClickListeners() {
        btnFilter.setOnClickListener {
            showFilterDialog()
        }

        btnSort.setOnClickListener {
            showSortDialog()
        }

        fabAddProduct.setOnClickListener {
            showAddProductDialog()
        }

        // Поиск при изменении текста в реальном времени
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch()
            }
        })
    }

    private fun loadProducts() {
        // Загружаем все товары (не ингредиенты по умолчанию)
        currentProducts = if (showOnlyIngredients) {
            dbHelper.getAllIngredients()
        } else {
            dbHelper.getAllProducts().filter { !it.isIngredient }
        }

        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        var filtered = currentProducts

        // Применяем фильтр по категории
        currentFilter?.let { filter ->
            when (filter) {
                "Все" -> {
                    // Ничего не фильтруем
                }
                "Убыточные" -> {
                    filtered = filtered.filter {
                        !it.isIngredient && it.price > 0 && it.costPrice > 0 && (it.price - it.costPrice) < 0
                    }
                }
                "Низкорентабельные" -> {
                    filtered = filtered.filter {
                        if (!it.isIngredient && it.price > 0 && it.costPrice > 0) {
                            val margin = it.price - it.costPrice
                            val marginPercent = (margin / it.costPrice * 100)
                            margin >= 0 && marginPercent < 10
                        } else {
                            false
                        }
                    }
                }
                "Высокорентабельные" -> {
                    filtered = filtered.filter {
                        if (!it.isIngredient && it.price > 0 && it.costPrice > 0) {
                            val margin = it.price - it.costPrice
                            val marginPercent = (margin / it.costPrice * 100)
                            marginPercent > 30
                        } else {
                            false
                        }
                    }
                }
                else -> {
                    // Фильтр по обычной категории
                    filtered = filtered.filter { it.category == filter }
                }
            }
        }

        // Применяем сортировку
        filtered = when (currentSort) {
            "name_asc" -> filtered.sortedBy { it.name }
            "name_desc" -> filtered.sortedByDescending { it.name }
            "price_asc" -> filtered.sortedBy { it.price }
            "price_desc" -> filtered.sortedByDescending { it.price }
            "stock_asc" -> filtered.sortedBy { it.stock }
            "stock_desc" -> filtered.sortedByDescending { it.stock }
            "category_asc" -> filtered.sortedBy { it.category }
            "profitability_asc" -> { // Сортировка по рентабельности (от низкой к высокой)
                filtered.sortedBy { product ->
                    if (!product.isIngredient && product.price > 0 && product.costPrice > 0) {
                        val margin = product.price - product.costPrice
                        val marginPercent = (margin / product.costPrice * 100)
                        marginPercent
                    } else {
                        0.0
                    }
                }
            }
            "profitability_desc" -> { // Сортировка по рентабельности (от высокой к низкой)
                filtered.sortedByDescending { product ->
                    if (!product.isIngredient && product.price > 0 && product.costPrice > 0) {
                        val margin = product.price - product.costPrice
                        val marginPercent = (margin / product.costPrice * 100)
                        marginPercent
                    } else {
                        0.0
                    }
                }
            }
            else -> filtered
        }

        adapter.updateProducts(filtered)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val products = adapter.itemCount

        if (products == 0) {
            tvEmptyState.visibility = View.VISIBLE
            rvProducts.visibility = View.GONE

            // Показываем разный текст в зависимости от ситуации
            val searchText = etSearch.text.toString().trim()
            if (searchText.isNotEmpty()) {
                tvEmptyState.text = "По запросу \"$searchText\" ничего не найдено"
            } else if (currentFilter != null && currentFilter != "Все") {
                tvEmptyState.text = "В категории \"$currentFilter\" товаров нет"
            } else if (showOnlyIngredients) {
                tvEmptyState.text = "Ингредиенты не найдены\n\nНажмите + чтобы добавить первый ингредиент"
            } else {
                tvEmptyState.text = "Товары не найдены\n\nНажмите + чтобы добавить первый товар"
            }
        } else {
            tvEmptyState.visibility = View.GONE
            rvProducts.visibility = View.VISIBLE
        }
    }

    private fun performSearch() {
        val query = etSearch.text.toString().trim()

        if (query.isEmpty()) {
            // Если поиск пустой, показываем все товары с учетом фильтров
            applyFilterAndSort()
            return
        }

        // Ищем товары в базе данных
        val searchResults = dbHelper.searchProducts(query)

        // Фильтруем по типу (ингредиенты/товары)
        val filteredResults = if (showOnlyIngredients) {
            searchResults.filter { it.isIngredient }
        } else {
            searchResults.filter { !it.isIngredient }
        }

        adapter.updateProducts(filteredResults)
        updateEmptyState()

        if (filteredResults.isEmpty()) {
            Toast.makeText(this, "Товары не найдены", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddProductDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_product, null)

        val etName = dialogView.findViewById<TextInputEditText>(R.id.etProductName)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.etPrice)
        val etCostPrice = dialogView.findViewById<TextInputEditText>(R.id.etCostPrice)
        val etStock = dialogView.findViewById<TextInputEditText>(R.id.etStock)
        val etCategory = dialogView.findViewById<TextInputEditText>(R.id.etCategory)
        val etBarcode = dialogView.findViewById<TextInputEditText>(R.id.etBarcode)
        val spUnit = dialogView.findViewById<Spinner>(R.id.spUnit)
        val cbIsIngredient = dialogView.findViewById<CheckBox>(R.id.cbIsIngredient)
        val tvCostInfo = dialogView.findViewById<TextView>(R.id.tvCostInfo)
        val containerTotalCost = dialogView.findViewById<LinearLayout>(R.id.containerTotalCost)

        // Настройка Spinner для единиц измерения
        val units = getAvailableUnits()
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spUnit.adapter = unitAdapter

        var previousUnit = "шт" // переменная для хранения предыдущей единицы измерения

        // Если показываем только ингредиенты
        if (showOnlyIngredients) {
            cbIsIngredient.isChecked = true
            etPrice.setText("0")
            etPrice.isEnabled = false
            spUnit.setSelection(1) // По умолчанию "кг" для ингредиентов
            containerTotalCost.visibility = View.GONE // НЕ показываем для ингредиентов
            previousUnit = "кг"
        } else {
            spUnit.setSelection(0) // По умолчанию "шт" для товаров
            containerTotalCost.visibility = View.VISIBLE // Показываем для товаров
            previousUnit = "шт"
        }

        // Слушатель для расчета общей стоимости в реальном времени
        val calculateTotalCost = {
            val cost = etCostPrice.text.toString().toDoubleOrNull() ?: 0.0
            val stock = etStock.text.toString().toDoubleOrNull() ?: 0.0
            val totalCost = cost * stock
            tvCostInfo.text = "Общая стоимость запаса: ${"%.2f".format(totalCost)} ₽"
        }

        // Устанавливаем слушатели
        etCostPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!cbIsIngredient.isChecked) calculateTotalCost()
            }
        })

        etStock.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!cbIsIngredient.isChecked) calculateTotalCost()
            }
        })

        // Слушатель для чекбокса "Ингредиент"
        cbIsIngredient.setOnCheckedChangeListener { _, isChecked ->
            etPrice.isEnabled = !isChecked
            if (isChecked) {
                etPrice.setText("0")
                // Для ингредиентов НЕ показываем контейнер стоимости
                containerTotalCost.visibility = View.GONE
            } else {
                // Для товаров показываем контейнер стоимости
                containerTotalCost.visibility = View.VISIBLE
                calculateTotalCost()
            }
        }

        // Слушатель для конвертации единиц измерения при их изменении
        spUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newUnit = spUnit.selectedItem.toString()

                // Автоматическая конвертация количества
                val currentStock = etStock.text.toString().toDoubleOrNull() ?: 0.0
                if (currentStock > 0) {
                    val convertedStock = convertUnitValue(previousUnit, newUnit, currentStock)
                    etStock.setText(convertedStock.toString())
                }

                // Автоматическая конвертация цены (только для товаров, не для ингредиентов)
                val currentPrice = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                if (!cbIsIngredient.isChecked && currentPrice > 0) {
                    val convertedPrice = convertPriceForUnit(currentPrice, previousUnit, newUnit)
                    etPrice.setText(convertedPrice.toString())
                }

                // Автоматическая конвертация себестоимости
                val currentCost = etCostPrice.text.toString().toDoubleOrNull() ?: 0.0
                if (currentCost > 0) {
                    val convertedCost = convertPriceForUnit(currentCost, previousUnit, newUnit)
                    etCostPrice.setText(convertedCost.toString())
                }

                // Обновляем предыдущую единицу
                previousUnit = newUnit
                calculateTotalCost()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Вызываем начальный расчет только если это товар
        if (!showOnlyIngredients && !cbIsIngredient.isChecked) {
            calculateTotalCost()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (showOnlyIngredients) "Добавление ингредиента" else "Добавление товара")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { dialogInterface, _ ->
                if (validateAndSaveProduct(
                        product = null,
                        name = etName.text.toString(),
                        priceStr = etPrice.text.toString(),
                        costPriceStr = etCostPrice.text.toString(),
                        stockStr = etStock.text.toString(),
                        category = etCategory.text.toString(),
                        barcode = etBarcode.text.toString(),
                        unit = spUnit.selectedItem.toString(), // Берем из Spinner!
                        isIngredient = cbIsIngredient.isChecked,
                        isNewProduct = true
                    )) {
                    dialogInterface.dismiss()
                }
            }
            .setNegativeButton("Отмена") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()
    }

    // Метод для пересчета цены при изменении единицы измерения
    // Метод для пересчета цены при изменении единицы измерения
    private fun convertPriceForUnit(price: Double, fromUnit: String, toUnit: String): Double {
        val fromUnitDef = dbHelper.getUnitByName(fromUnit)
        val toUnitDef = dbHelper.getUnitByName(toUnit)

        if (fromUnitDef != null && toUnitDef != null) {
            // Если единицы из одной категории
            if (fromUnitDef.category == toUnitDef.category) {
                // Цена пересчитывается обратно пропорционально
                // Если 1 кг = 1000 г, то цена за г = цена за кг / 1000
                val valueInBase = price / fromUnitDef.conversionRate
                return valueInBase * toUnitDef.conversionRate
            }
        }

        return price // если нет коэффициента, оставляем как есть
    }
    // Получение всех доступных единиц измерения для Spinner
    private fun getAvailableUnits(): Array<String> {
        val units = dbHelper.getAllUnits()
        return units.map { it.name }.toTypedArray()
    }

    private fun openProductEditDialog(product: Product) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_product, null)

        val etName = dialogView.findViewById<TextInputEditText>(R.id.etProductName)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.etPrice)
        val etCostPrice = dialogView.findViewById<TextInputEditText>(R.id.etCostPrice)
        val etStock = dialogView.findViewById<TextInputEditText>(R.id.etStock)
        val etCategory = dialogView.findViewById<TextInputEditText>(R.id.etCategory)
        val etBarcode = dialogView.findViewById<TextInputEditText>(R.id.etBarcode)
        val spUnit = dialogView.findViewById<Spinner>(R.id.spUnit)
        val cbIsIngredient = dialogView.findViewById<CheckBox>(R.id.cbIsIngredient)
        val tvCostInfo = dialogView.findViewById<TextView>(R.id.tvCostInfo)
        val containerTotalCost = dialogView.findViewById<LinearLayout>(R.id.containerTotalCost)

        // Настройка Spinner для единиц измерения
        val units = getAvailableUnits()
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spUnit.adapter = unitAdapter

        // Устанавливаем выбранную единицу измерения
        val unitPosition = units.indexOf(product.unit)
        if (unitPosition != -1) {
            spUnit.setSelection(unitPosition)
        } else {
            spUnit.setSelection(0)
        }

        var currentUnit = product.unit // храним текущую единицу измерения

        // Заполняем поля данными товара
        etName.setText(product.name)
        etPrice.setText(if (product.price > 0) product.price.toString() else "")
        etCostPrice.setText(if (product.costPrice > 0) product.costPrice.toString() else "")
        etStock.setText(if (product.stock > 0) product.stock.toString() else "")
        etCategory.setText(product.category)
        etBarcode.setText(product.barcode ?: "")
        cbIsIngredient.isChecked = product.isIngredient

        // Устанавливаем видимость контейнера стоимости
        if (product.isIngredient) {
            // Для ингредиентов НЕ показываем контейнер стоимости
            containerTotalCost.visibility = View.GONE
            etPrice.isEnabled = false
            etPrice.setText("0")
        } else {
            // Для товаров показываем контейнер стоимости
            containerTotalCost.visibility = View.VISIBLE
            etPrice.isEnabled = true
        }

        // Слушатель для расчета общей стоимости в реальном времени (ТОЛЬКО для товаров)
        val calculateTotalCost = {
            if (!product.isIngredient) {
                val cost = etCostPrice.text.toString().toDoubleOrNull() ?: 0.0
                val stock = etStock.text.toString().toDoubleOrNull() ?: 0.0
                val totalCost = cost * stock
                tvCostInfo.text = "Общая стоимость запаса: ${"%.2f".format(totalCost)} ₽"
            }
        }

        // Устанавливаем слушатели ТОЛЬКО если это не ингредиент
        if (!product.isIngredient) {
            etCostPrice.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { calculateTotalCost() }
            })

            etStock.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { calculateTotalCost() }
            })

            // Вызываем начальный расчет
            calculateTotalCost()
        }

        // Обработка изменения состояния чекбокса "Ингредиент"
        cbIsIngredient.setOnCheckedChangeListener { _, isChecked ->
            etPrice.isEnabled = !isChecked
            if (isChecked) {
                etPrice.setText("0")
                // Для ингредиентов НЕ показываем контейнер стоимости
                containerTotalCost.visibility = View.GONE
            } else {
                // Для товаров показываем контейнер стоимости
                containerTotalCost.visibility = View.VISIBLE
                calculateTotalCost()
            }
        }

        // Слушатель для конвертации единиц измерения при их изменении
        spUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newUnit = units[position]
                if (newUnit != currentUnit) {
                    // Автоматическая конвертация количества
                    val currentStock = etStock.text.toString().toDoubleOrNull() ?: 0.0
                    if (currentStock > 0) {
                        val convertedStock = convertUnitValue(currentUnit, newUnit, currentStock)
                        etStock.setText(convertedStock.toString())
                    }

                    // Автоматическая конвертация цены (только для товаров, не для ингредиентов)
                    val currentPrice = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                    if (!cbIsIngredient.isChecked && currentPrice > 0) {
                        val convertedPrice = convertPriceForUnit(currentPrice, currentUnit, newUnit)
                        etPrice.setText(convertedPrice.toString())
                    }

                    // Автоматическая конвертация себестоимости
                    val currentCost = etCostPrice.text.toString().toDoubleOrNull() ?: 0.0
                    if (currentCost > 0) {
                        val convertedCost = convertPriceForUnit(currentCost, currentUnit, newUnit)
                        etCostPrice.setText(convertedCost.toString())
                    }

                    // Обновляем текущую единицу
                    currentUnit = newUnit
                    calculateTotalCost()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val dialogTitle = if (product.isIngredient) "Редактирование ингредиента" else "Редактирование товара"

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton("Сохранить") { dialogInterface, _ ->
                if (validateAndSaveProduct(
                        product = product,
                        name = etName.text.toString(),
                        priceStr = etPrice.text.toString(),
                        costPriceStr = etCostPrice.text.toString(),
                        stockStr = etStock.text.toString(),
                        category = etCategory.text.toString(),
                        barcode = etBarcode.text.toString(),
                        unit = spUnit.selectedItem.toString(),
                        isIngredient = cbIsIngredient.isChecked,
                        isNewProduct = false
                    )) {
                    dialogInterface.dismiss()
                }
            }
            .setNegativeButton("Отмена") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setNeutralButton("Удалить") { dialogInterface, _ ->
                showDeleteConfirmationDialog(product)
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()

        // Меняем цвет кнопки удаления на красный
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(
            ContextCompat.getColor(this, R.color.accent_color)
        )
    }

    // Метод для конвертации значений между единицами измерения
    private fun convertUnitValue(fromUnit: String, toUnit: String, value: Double): Double {
        val fromUnitDef = dbHelper.getUnitByName(fromUnit)
        val toUnitDef = dbHelper.getUnitByName(toUnit)

        if (fromUnitDef != null && toUnitDef != null) {
            // Если единицы из одной категории
            if (fromUnitDef.category == toUnitDef.category) {
                // Конвертируем через базовые единицы
                val valueInBase = value * fromUnitDef.conversionRate
                return valueInBase / toUnitDef.conversionRate
            }
        }

        return value // если нет коэффициента, оставляем как есть
    }

    private fun validateAndSaveProduct(
        product: Product?,
        name: String,
        priceStr: String,
        costPriceStr: String,
        stockStr: String,
        category: String,
        barcode: String,
        unit: String, // Теперь это строка из Spinner
        isIngredient: Boolean,
        isNewProduct: Boolean
    ): Boolean {
        // Валидация названия
        if (name.isBlank()) {
            Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
            return false
        }

        // Валидация цены (только для товаров, не для ингредиентов)
        val price = if (isIngredient) {
            0.0
        } else {
            priceStr.toDoubleOrNull() ?: 0.0
        }

        if (!isIngredient && price <= 0) {
            Toast.makeText(this, "Введите корректную цену", Toast.LENGTH_SHORT).show()
            return false
        }

        // Валидация себестоимости
        val costPrice = costPriceStr.toDoubleOrNull() ?: 0.0
        if (costPrice < 0) {
            Toast.makeText(this, "Себестоимость не может быть отрицательной", Toast.LENGTH_SHORT).show()
            return false
        }

        // Валидация количества
        val stock = stockStr.toDoubleOrNull() ?: 0.0
        if (stock < 0) {
            Toast.makeText(this, "Количество не может быть отрицательным", Toast.LENGTH_SHORT).show()
            return false
        }

        // Валидация единицы измерения (уже пришла из Spinner)
        if (unit.isBlank()) {
            Toast.makeText(this, "Выберите единицу измерения", Toast.LENGTH_SHORT).show()
            return false
        }

        val finalUnit = unit

        // Валидация категории
        val finalCategory = if (category.isBlank()) {
            if (isIngredient) "Ингредиенты" else "Без категории"
        } else {
            category
        }

        // Обновляем или создаем товар
        val updatedProduct = if (isNewProduct) {
            Product(
                id = IdGenerator.generateId(),
                name = name.trim(),
                price = price,
                stock = stock,
                category = finalCategory.trim(),
                barcode = if (barcode.isBlank()) null else barcode.trim(),
                costPrice = costPrice,
                isIngredient = isIngredient,
                unit = finalUnit.trim()
            )
        } else {
            product!!.copy(
                name = name.trim(),
                price = price,
                stock = stock,
                category = finalCategory.trim(),
                barcode = if (barcode.isBlank()) null else barcode.trim(),
                costPrice = costPrice,
                isIngredient = isIngredient,
                unit = finalUnit.trim()
            )
        }

        // Сохраняем в базу данных
        if (isNewProduct) {
            val result = dbHelper.addProduct(updatedProduct)
            if (result != -1L) {
                Toast.makeText(this,
                    if (isIngredient) "Ингредиент добавлен" else "Товар добавлен",
                    Toast.LENGTH_SHORT).show()
                loadProducts()
                return true
            } else {
                Toast.makeText(this, "Ошибка при добавлении", Toast.LENGTH_SHORT).show()
                return false
            }
        } else {
            val success = dbHelper.updateProduct(updatedProduct)
            if (success) {
                Toast.makeText(this,
                    if (isIngredient) "Ингредиент обновлен" else "Товар обновлен",
                    Toast.LENGTH_SHORT).show()
                loadProducts()
                return true
            } else {
                Toast.makeText(this, "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
                return false
            }
        }
    }


    private fun showProductActionsDialog(product: Product) {
        val actions = if (product.isIngredient) {
            arrayOf("Редактировать", "Удалить", "Показать детали", "Использовать в рецептах")
        } else {
            arrayOf("Редактировать", "Удалить", "Показать детали", "Рецепт приготовления")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(product.name)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> openProductEditDialog(product)
                    1 -> showDeleteConfirmationDialog(product)
                    2 -> showProductDetails(product)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(product: Product) {
        val productType = if (product.isIngredient) "ингредиент" else "товар"

        MaterialAlertDialogBuilder(this)
            .setTitle("Удаление $productType")
            .setMessage("Вы уверены, что хотите удалить $productType \"${product.name}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteProduct(product)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteProduct(product: Product) {
        val success = dbHelper.deleteProduct(product.id)
        if (success) {
            val productType = if (product.isIngredient) "Ингредиент" else "Товар"
            Toast.makeText(this, "$productType \"${product.name}\" удален", Toast.LENGTH_SHORT).show()
            loadProducts()
        } else {
            Toast.makeText(this,
                "Нельзя удалить ${if (product.isIngredient) "ингредиент" else "товар"}, " +
                        "который есть в продажах или поставках",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun showProductDetails(product: Product) {
        val margin = product.price - product.costPrice
        val marginPercent = if (product.costPrice > 0) (margin / product.costPrice * 100) else 0.0

        val details = """
            Название: ${product.name}
            Тип: ${if (product.isIngredient) "Ингредиент" else "Товар"}
            ${if (!product.isIngredient) "Цена: ${"%.2f".format(product.price)} ₽" else ""}
            Себестоимость: ${"%.2f".format(product.costPrice)} ₽
            На складе: ${product.stock} ${product.unit}
            Категория: ${product.category}
            ${if (product.barcode != null) "Штрих-код: ${product.barcode}" else ""}
            
            ${if (!product.isIngredient) "Маржа: ${"%.2f".format(margin)} ₽\nРентабельность: ${"%.1f".format(marginPercent)}%" else ""}
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Детали ${if (product.isIngredient) "ингредиента" else "товара"}")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }




    private fun showFilterDialog() {
        // Получаем все категории из базы
        val allProducts = dbHelper.getAllProducts()
        val categories = allProducts.map { it.category }
            .distinct()
            .sorted()

        val allCategories = listOf("Все") + categories

        // Добавляем фильтры по типу и прибыльности
        val filterOptions = mutableListOf<String>().apply {
            addAll(allCategories)
            add("Невыгодные товары (убыточные)")
            add("Низкорентабельные (<10%)")
            add("Высокорентабельные (>30%)")
        }

        val filterArray = filterOptions.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Фильтр товаров")
            .setItems(filterArray) { _, which ->
                when {
                    which == 0 -> { // "Все"
                        currentFilter = null
                        showOnlyIngredients = false
                    }
                    which < allCategories.size -> { // Категория
                        val selectedCategory = allCategories[which]
                        currentFilter = if (selectedCategory == "Все") null else selectedCategory
                    }
                    which == filterOptions.size - 3 -> { // "Невыгодные товары (убыточные)"
                        showOnlyIngredients = false
                        currentFilter = "Убыточные"
                    }
                    which == filterOptions.size - 2 -> { // "Низкорентабельные (<10%)"
                        showOnlyIngredients = false
                        currentFilter = "Низкорентабельные"
                    }
                    which == filterOptions.size - 1 -> { // "Высокорентабельные (>30%)"
                        showOnlyIngredients = false
                        currentFilter = "Высокорентабельные"
                    }
                }

                applyFilterAndSort()

                val message = when {
                    showOnlyIngredients -> "Показаны только ингредиенты"
                    currentFilter == "Убыточные" -> "Показаны убыточные товары"
                    currentFilter == "Низкорентабельные" -> "Показаны низкорентабельные товары (<10%)"
                    currentFilter == "Высокорентабельные" -> "Показаны высокорентабельные товары (>30%)"
                    currentFilter != null && currentFilter != "Все" -> "Показана категория: $currentFilter"
                    else -> "Показаны все товары"
                }

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                loadProducts() // Перезагружаем товары с учетом фильтра
            }
            .setNegativeButton("Сбросить") { _, _ ->
                currentFilter = null
                showOnlyIngredients = false
                applyFilterAndSort()
                Toast.makeText(this, "Фильтр сброшен", Toast.LENGTH_SHORT).show()
                loadProducts()
            }
            .setNeutralButton("Отмена", null)
            .show()
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "По названию (А-Я)",
            "По названию (Я-А)",
            "По цене (возр.)",
            "По цене (убыв.)",
            "По количеству (мало → много)",
            "По количеству (много → мало)",
            "По категории (А-Я)",
            "По рентабельности (низкая → высокая)",
            "По рентабельности (высокая → низкая)"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Сортировка товаров")
            .setItems(sortOptions) { _, which ->
                currentSort = when (which) {
                    0 -> "name_asc"
                    1 -> "name_desc"
                    2 -> "price_asc"
                    3 -> "price_desc"
                    4 -> "stock_asc"
                    5 -> "stock_desc"
                    6 -> "category_asc"
                    7 -> "profitability_asc"
                    8 -> "profitability_desc"
                    else -> "name_asc"
                }
                applyFilterAndSort()
                Toast.makeText(this, "Товары отсортированы", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // Управление категориями
    private fun manageCategories() {
        val categories = dbHelper.getAllProducts()
            .map { it.category }
            .distinct()
            .sorted()
            .filter { it.isNotBlank() }

        val dialogView = layoutInflater.inflate(R.layout.dialog_manage_categories, null)

        val etNewCategory = dialogView.findViewById<TextInputEditText>(R.id.etNewCategory)
        val rvCategories = dialogView.findViewById<RecyclerView>(R.id.rvCategories)
        val tvEmpty = dialogView.findViewById<TextView>(R.id.tvEmpty)

        val adapter = CategoriesAdapter(categories.toMutableList()) { category ->
            showCategoryActionsDialog(category)
        }

        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = adapter

        MaterialAlertDialogBuilder(this)
            .setTitle("Управление категориями")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val newCategory = etNewCategory.text.toString().trim()
                if (newCategory.isNotEmpty()) {
                    addNewCategory(newCategory, adapter)
                    etNewCategory.text?.clear()
                }
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun showCategoryActionsDialog(category: String) {
        val actions = arrayOf(
            "Переименовать категорию",
            "Удалить категорию",
            "Показать товары в категории",
            "Объединить с другой категорией"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Категория: $category")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> renameCategory(category)
                    1 -> deleteCategory(category)
                    2 -> showProductsInCategory(category)
                    3 -> mergeCategory(category)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun renameCategory(oldCategory: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_category, null)

        val etNewName = dialogView.findViewById<TextInputEditText>(R.id.etCategoryName)
        etNewName.setText(oldCategory)

        MaterialAlertDialogBuilder(this)
            .setTitle("Переименование категории")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newCategory = etNewName.text.toString().trim()
                if (newCategory.isNotEmpty() && newCategory != oldCategory) {
                    renameCategoryInDatabase(oldCategory, newCategory)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun renameCategoryInDatabase(oldCategory: String, newCategory: String) {
        Thread {
            try {
                // Получаем все товары с этой категорией
                val allProducts = dbHelper.getAllProducts()
                val productsInCategory = allProducts.filter { it.category == oldCategory }

                // Обновляем каждый товар
                var updatedCount = 0
                productsInCategory.forEach { product ->
                    val updatedProduct = product.copy(category = newCategory)
                    if (dbHelper.updateProduct(updatedProduct)) {
                        updatedCount++
                    }
                }

                runOnUiThread {
                    loadProducts()
                    Toast.makeText(this,
                        "Категория переименована. Обновлено товаров: $updatedCount",
                        Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun deleteCategory(category: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Удаление категории")
            .setMessage("Удалить категорию '$category'?\n\n" +
                    "Товары из этой категории будут перемещены в категорию 'Без категории'.")
            .setPositiveButton("Удалить") { _, _ ->
                deleteCategoryFromDatabase(category)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteCategoryFromDatabase(category: String) {
        Thread {
            try {
                val allProducts = dbHelper.getAllProducts()
                val productsInCategory = allProducts.filter { it.category == category }

                var updatedCount = 0
                productsInCategory.forEach { product ->
                    val updatedProduct = product.copy(category = "Без категории")
                    if (dbHelper.updateProduct(updatedProduct)) {
                        updatedCount++
                    }
                }

                runOnUiThread {
                    loadProducts()
                    Toast.makeText(this,
                        "Категория удалена. Перемещено товаров: $updatedCount",
                        Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showProductsInCategory(category: String) {
        val products = dbHelper.getAllProducts()
            .filter { it.category == category }
            .sortedBy { it.name }

        if (products.isEmpty()) {
            Toast.makeText(this, "В этой категории нет товаров", Toast.LENGTH_SHORT).show()
            return
        }

        val productsText = buildString {
            appendLine("ТОВАРЫ В КАТЕГОРИИ: $category")
            appendLine("=".repeat(50))
            appendLine("Всего товаров: ${products.size}")
            appendLine()

            products.forEach { product ->
                appendLine("• ${product.name}")
                appendLine("  Цена: ${String.format("%.2f", product.price)} ₽")
                appendLine("  Остаток: ${product.stock} ${product.unit}")
                appendLine("  Тип: ${if (product.isIngredient) "Ингредиент" else "Товар"}")
                appendLine()
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Товары в категории: $category")
            .setMessage(productsText)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun mergeCategory(category: String) {
        val otherCategories = dbHelper.getAllProducts()
            .map { it.category }
            .distinct()
            .sorted()
            .filter { it.isNotBlank() && it != category }

        if (otherCategories.isEmpty()) {
            Toast.makeText(this, "Нет других категорий для объединения", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Объединить с категорией")
            .setItems(otherCategories.toTypedArray()) { _, which ->
                val targetCategory = otherCategories[which]
                mergeCategories(category, targetCategory)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun mergeCategories(sourceCategory: String, targetCategory: String) {
        Thread {
            try {
                val allProducts = dbHelper.getAllProducts()
                val productsInSource = allProducts.filter { it.category == sourceCategory }

                var mergedCount = 0
                productsInSource.forEach { product ->
                    val updatedProduct = product.copy(category = targetCategory)
                    if (dbHelper.updateProduct(updatedProduct)) {
                        mergedCount++
                    }
                }

                runOnUiThread {
                    loadProducts()
                    Toast.makeText(this,
                        "Категории объединены. Перемещено товаров: $mergedCount",
                        Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun addNewCategory(newCategory: String, adapter: CategoriesAdapter) {
        if (newCategory.isBlank()) {
            Toast.makeText(this, "Введите название категории", Toast.LENGTH_SHORT).show()
            return
        }

        adapter.addCategory(newCategory)
        Toast.makeText(this, "Категория добавлена: $newCategory", Toast.LENGTH_SHORT).show()
    }

    // Массовое редактирование
    private fun showBulkEditDialog() {
        val products = if (showOnlyIngredients) {
            dbHelper.getAllIngredients()
        } else {
            dbHelper.getAllProducts().filter { !it.isIngredient }
        }

        if (products.isEmpty()) {
            Toast.makeText(this, "Нет товаров для массового редактирования", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_bulk_edit, null)

        val spField = dialogView.findViewById<android.widget.Spinner>(R.id.spField)
        val etValue = dialogView.findViewById<TextInputEditText>(R.id.etValue)
        val tvSelectedCount = dialogView.findViewById<TextView>(R.id.tvSelectedCount)

        // Настройка адаптера для выбора поля
        val fields = if (showOnlyIngredients) {
            arrayOf("Себестоимость", "Единица измерения", "Категория")
        } else {
            arrayOf("Цена", "Себестоимость", "Категория", "Единица измерения")
        }

        val fieldAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fields)
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spField.adapter = fieldAdapter

        // Список товаров для выбора
        val rvProducts = dialogView.findViewById<RecyclerView>(R.id.rvProducts)
        val productAdapter = BulkEditProductsAdapter(products) { selectedCount ->
            tvSelectedCount.text = "Выбрано: $selectedCount"
        }

        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = productAdapter

        MaterialAlertDialogBuilder(this)
            .setTitle("Массовое редактирование")
            .setView(dialogView)
            .setPositiveButton("Применить") { _, _ ->
                val selectedField = spField.selectedItem.toString()
                val newValue = etValue.text.toString().trim()
                val selectedProducts = productAdapter.getSelectedProducts()

                if (selectedProducts.isEmpty()) {
                    Toast.makeText(this, "Выберите товары для редактирования", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newValue.isEmpty()) {
                    Toast.makeText(this, "Введите новое значение", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                applyBulkEdit(selectedField, newValue, selectedProducts)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun applyBulkEdit(field: String, newValue: String, products: List<Product>) {
        Thread {
            try {
                var updatedCount = 0

                products.forEach { product ->
                    val updatedProduct = when (field) {
                        "Цена" -> {
                            val price = newValue.toDoubleOrNull() ?: 0.0
                            product.copy(price = price)
                        }
                        "Себестоимость" -> {
                            val costPrice = newValue.toDoubleOrNull() ?: 0.0
                            product.copy(costPrice = costPrice)
                        }
                        "Категория" -> {
                            product.copy(category = newValue)
                        }
                        "Единица измерения" -> {
                            product.copy(unit = newValue)
                        }
                        else -> product
                    }

                    if (dbHelper.updateProduct(updatedProduct)) {
                        updatedCount++
                    }
                }

                runOnUiThread {
                    loadProducts()
                    Toast.makeText(this,
                        "Массовое редактирование завершено. Обновлено товаров: $updatedCount",
                        Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}

