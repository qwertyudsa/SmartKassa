package com.example.smartkassa

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.Date

class RecipesActivity : AppCompatActivity() {

    private lateinit var rvRecipes: RecyclerView
    private lateinit var btnAddRecipe: MaterialButton
    private lateinit var btnProduce: MaterialButton
    private lateinit var tvRecipeSummary: TextView
    private lateinit var tvEmptyRecipes: TextView

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: RecipesAdapter

    private var selectedProduct: Product? = null
    private val recipes = mutableListOf<Recipe>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipes)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupClickListeners()

        // Проверяем, был ли передан продукт из intent
        val productId = intent.getStringExtra("product_id")
        if (productId != null) {
            selectedProduct = dbHelper.getProductById(productId)
            if (selectedProduct != null) {
                supportActionBar?.title = "Рецепт: ${selectedProduct!!.name}"
                loadRecipes()
            }
        } else {
            loadProductsForSelection()
        }
    }

    private fun initViews() {
        rvRecipes = findViewById(R.id.rvRecipes)
        btnAddRecipe = findViewById(R.id.btnAddRecipe)
        btnProduce = findViewById(R.id.btnProduce)
        tvRecipeSummary = findViewById(R.id.tvRecipeSummary)
        tvEmptyRecipes = findViewById(R.id.tvEmptyRecipes)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = RecipesAdapter(recipes) { recipe ->
            showRecipeActionsDialog(recipe)
        }

        rvRecipes.layoutManager = LinearLayoutManager(this)
        rvRecipes.adapter = adapter
    }

    private fun setupClickListeners() {
        btnAddRecipe.setOnClickListener {
            if (selectedProduct == null) {
                Toast.makeText(this, "Сначала выберите товар", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showAddRecipeDialog()
        }

        btnProduce.setOnClickListener {
            if (selectedProduct == null) {
                Toast.makeText(this, "Сначала выберите товар", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showProduceDialog()
        }
    }

    private fun loadProductsForSelection() {
        val products = dbHelper.getAllProducts().filter { !it.isIngredient }

        if (products.isEmpty()) {
            Toast.makeText(this, "Сначала добавьте товары (готовые изделия)", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val productNames = products.map { "${it.name} (остаток: ${it.stock} ${it.unit})" }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Выберите товар для рецепта")
            .setItems(productNames) { _, which ->
                selectedProduct = products[which]
                supportActionBar?.title = "Рецепт: ${selectedProduct!!.name}"
                loadRecipes()
                updateRecipeSummary()
            }
            .setCancelable(false)
            .show()
    }

    private fun loadRecipes() {
        selectedProduct?.let { product ->
            recipes.clear()
            val dbRecipes = dbHelper.getRecipesForProduct(product.id)
            recipes.addAll(dbRecipes)
            adapter.notifyDataSetChanged()

            btnAddRecipe.isEnabled = true
            btnProduce.isEnabled = recipes.isNotEmpty()

            // Показываем/скрываем сообщение о пустом списке
            if (recipes.isEmpty()) {
                tvEmptyRecipes.visibility = View.VISIBLE
                rvRecipes.visibility = View.GONE
            } else {
                tvEmptyRecipes.visibility = View.GONE
                rvRecipes.visibility = View.VISIBLE
            }

            updateRecipeSummary()
        }
    }

    private fun updateRecipeSummary() {
        selectedProduct?.let { product ->
            val totalIngredients = recipes.size

            if (totalIngredients == 0) {
                tvRecipeSummary.text = "Товар: ${product.name}\nНа складе: ${"%.2f".format(product.stock)} ${product.unit}\n\nДобавьте ингредиенты для рецепта"
                return
            }

            val maxProduction = calculateMaxProduction()
            val missingIngredients = getMissingIngredientsMessage()
            val productCost = dbHelper.calculateProductCost(product.id)

            val summary = buildString {
                appendLine("Товар: ${product.name}")
                appendLine("На складе: ${"%.2f".format(product.stock)} ${product.unit}")
                appendLine("Ингредиентов в рецепте: $totalIngredients")
                appendLine("Себестоимость: ${"%.2f".format(productCost)} ₽")

                if (product.costPrice > 0) {
                    val margin = product.price - productCost
                    val marginPercent = if (productCost > 0) (margin / productCost * 100) else 0.0
                    appendLine("Маржа: ${"%.2f".format(margin)} ₽ (${"%.1f".format(marginPercent)}%)")

                    // Проверка на превышение себестоимости
                    if (productCost > product.price * 0.9) { // Если себестоимость > 90% цены
                        appendLine()
                        appendLine("⚠️ ВНИМАНИЕ: Себестоимость превышает 90% от цены!")
                    }
                }

                if (maxProduction > 0) {
                    appendLine("Можно приготовить: ${"%.0f".format(maxProduction)} ${product.unit}")
                }

                if (missingIngredients.isNotEmpty()) {
                    appendLine()
                    appendLine(missingIngredients)
                }
            }

            tvRecipeSummary.text = summary
        }
    }

    private fun calculateMaxProduction(): Double {
        if (selectedProduct == null || recipes.isEmpty()) return 0.0

        var maxProduction = Double.MAX_VALUE

        for (recipe in recipes) {
            val ingredient = dbHelper.getProductById(recipe.ingredientId)
            if (ingredient != null) {
                val canProduceFromThisIngredient = ingredient.stock / recipe.quantityNeeded
                if (canProduceFromThisIngredient < maxProduction) {
                    maxProduction = canProduceFromThisIngredient
                }
            }
        }

        return if (maxProduction == Double.MAX_VALUE) 0.0 else maxProduction
    }

    private fun getMissingIngredientsMessage(): String {
        val missingIngredients = mutableListOf<String>()

        for (recipe in recipes) {
            val ingredient = dbHelper.getProductById(recipe.ingredientId)
            if (ingredient != null && ingredient.stock < recipe.quantityNeeded) {
                val needed = recipe.quantityNeeded - ingredient.stock
                missingIngredients.add("${recipe.ingredientName}: нужно ${"%.3f".format(needed)} ${ingredient.unit}")
            }
        }

        return if (missingIngredients.isNotEmpty()) {
            "Не хватает:\n" + missingIngredients.joinToString("\n")
        } else {
            "Все ингредиенты в наличии ✓"
        }
    }

    private fun showAddRecipeDialog() {
        val ingredients = dbHelper.getAllProducts().filter { it.isIngredient }

        if (ingredients.isEmpty()) {
            Toast.makeText(this, "Сначала добавьте ингредиенты", Toast.LENGTH_SHORT).show()
            return
        }

        val ingredientNames = ingredients.map {
            "${it.name} (остаток: ${"%.3f".format(it.stock)} ${it.unit}, себестоимость: ${"%.2f".format(it.costPrice)} ₽/${it.unit})"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Выберите ингредиент")
            .setItems(ingredientNames) { _, which ->
                val selectedIngredient = ingredients[which]

                // Проверяем, не добавлен ли уже этот ингредиент в рецепт
                if (recipes.any { it.ingredientId == selectedIngredient.id }) {
                    Toast.makeText(this, "Этот ингредиент уже есть в рецепте", Toast.LENGTH_SHORT).show()
                    return@setItems
                }

                showQuantityDialog(selectedIngredient)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showQuantityDialog(ingredient: Product) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recipe_quantity, null)

        val tvIngredient = dialogView.findViewById<TextView>(R.id.tvIngredient)
        val etQuantity = dialogView.findViewById<TextInputEditText>(R.id.etQuantity)
        val spUnit = dialogView.findViewById<Spinner>(R.id.spUnit)
        val tvUnit = dialogView.findViewById<TextView>(R.id.tvUnit)
        val tvCostInfo = dialogView.findViewById<TextView>(R.id.tvCostInfo)

        tvIngredient.text = "Ингредиент: ${ingredient.name}"
        tvUnit.text = "на 1 ${selectedProduct?.unit ?: "шт"}"

        // Настройка Spinner для единиц измерения
        val units = arrayOf(ingredient.unit, "кг", "г", "л", "мл", "шт", "уп", "пак").distinct()
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units.toTypedArray())
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spUnit.adapter = unitAdapter
        spUnit.setSelection(0)

        // Функция для обновления информации о стоимости (объявлена внутри метода)
        val updateCostInfo = {
            val quantity = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
            val selectedUnit = spUnit.selectedItem.toString()

            // Конвертируем стоимость в выбранную единицу
            val costPerUnit = getCostPerUnitInSelectedUnit(ingredient, selectedUnit)

            if (quantity > 0) {
                val cost = quantity * costPerUnit
                tvCostInfo.text = "Стоимость: ${"%.2f".format(cost)} ₽ (${"%.2f".format(costPerUnit)} ₽/$selectedUnit)"
                tvCostInfo.visibility = View.VISIBLE
            } else {
                tvCostInfo.text = "Себестоимость: ${"%.2f".format(costPerUnit)} ₽/$selectedUnit"
                tvCostInfo.visibility = View.VISIBLE
            }
        }

        // Автоматическая конвертация при смене единицы измерения
        spUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCostInfo()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateCostInfo() }
        })

        MaterialAlertDialogBuilder(this)
            .setTitle("Количество ингредиента")
            .setView(dialogView)
            .setPositiveButton("Добавить") { dialog, _ ->
                val quantityStr = etQuantity.text.toString().trim()
                val selectedUnit = spUnit.selectedItem.toString()

                // Конвертируем количество в единицы измерения ингредиента
                val quantity = quantityStr.toDoubleOrNull() ?: 0.0
                val convertedQuantity = convertToIngredientUnit(quantity, selectedUnit, ingredient.unit)

                if (convertedQuantity > 0) {
                    addRecipe(ingredient, convertedQuantity)
                } else {
                    Toast.makeText(this, "Введите корректное количество", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()

        // Показываем начальную информацию
        updateCostInfo()
    }

    // Метод для получения себестоимости в выбранной единице
    private fun getCostPerUnitInSelectedUnit(ingredient: Product, selectedUnit: String): Double {
        return when {
            ingredient.unit == selectedUnit -> ingredient.costPrice
            ingredient.unit == "кг" && selectedUnit == "г" -> ingredient.costPrice / 1000.0
            ingredient.unit == "г" && selectedUnit == "кг" -> ingredient.costPrice * 1000.0
            ingredient.unit == "л" && selectedUnit == "мл" -> ingredient.costPrice / 1000.0
            ingredient.unit == "мл" && selectedUnit == "л" -> ingredient.costPrice * 1000.0
            else -> ingredient.costPrice // Если нет коэффициента, оставляем как есть
        }
    }

    // Метод для конвертации в единицы измерения ингредиента
    private fun convertToIngredientUnit(quantity: Double, fromUnit: String, toUnit: String): Double {
        return when {
            fromUnit == toUnit -> quantity
            fromUnit == "кг" && toUnit == "г" -> quantity * 1000.0
            fromUnit == "г" && toUnit == "кг" -> quantity / 1000.0
            fromUnit == "л" && toUnit == "мл" -> quantity * 1000.0
            fromUnit == "мл" && toUnit == "л" -> quantity / 1000.0
            else -> quantity
        }
    }



    private fun addRecipe(ingredient: Product, quantity: Double) {
        val recipe = Recipe(
            id = IdGenerator.generateId(),
            productId = selectedProduct!!.id,
            ingredientId = ingredient.id,
            quantityNeeded = quantity,
            productName = selectedProduct!!.name,
            ingredientName = ingredient.name,
            productUnit = selectedProduct!!.unit,
            ingredientUnit = ingredient.unit,
            ingredientStock = ingredient.stock.toDouble()
        )

        val result = dbHelper.addRecipe(recipe)
        if (result != -1L) {
            recipes.add(recipe)
            adapter.notifyItemInserted(recipes.size - 1)
            btnProduce.isEnabled = true
            tvEmptyRecipes.visibility = View.GONE
            rvRecipes.visibility = View.VISIBLE
            updateRecipeSummary()
            Toast.makeText(this, "Ингредиент добавлен в рецепт", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Ошибка добавления рецепта. Возможно, этот ингредиент уже есть в рецепте", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRecipeActionsDialog(recipe: Recipe) {
        val actions = arrayOf("Изменить количество", "Удалить из рецепта", "Показать детали")

        MaterialAlertDialogBuilder(this)
            .setTitle(recipe.ingredientName)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showEditQuantityDialog(recipe)
                    1 -> showDeleteRecipeDialog(recipe)
                    2 -> showRecipeDetails(recipe)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditQuantityDialog(recipe: Recipe) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recipe_quantity, null)

        val tvIngredient = dialogView.findViewById<TextView>(R.id.tvIngredient)
        val etQuantity = dialogView.findViewById<TextInputEditText>(R.id.etQuantity)
        val tvUnit = dialogView.findViewById<TextView>(R.id.tvUnit)

        tvIngredient.text = "Ингредиент: ${recipe.ingredientName}"
        tvUnit.text = "на 1 ${selectedProduct?.unit ?: "шт"}"
        etQuantity.setText(recipe.quantityNeeded.toString())
        etQuantity.hint = "Количество ${recipe.ingredientUnit}"

        MaterialAlertDialogBuilder(this)
            .setTitle("Изменение количества")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val quantityStr = etQuantity.text.toString().trim()
                val quantity = quantityStr.toDoubleOrNull()

                if (quantity != null && quantity > 0) {
                    updateRecipeQuantity(recipe, quantity)
                } else {
                    Toast.makeText(this, "Введите корректное количество", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateRecipeQuantity(recipe: Recipe, newQuantity: Double) {
        val updatedRecipe = recipe.copy(quantityNeeded = newQuantity)

        if (dbHelper.updateRecipe(updatedRecipe)) {
            val index = recipes.indexOfFirst { it.id == recipe.id }
            if (index != -1) {
                recipes[index] = updatedRecipe
                adapter.notifyItemChanged(index)
                updateRecipeSummary()
                Toast.makeText(this, "Количество обновлено", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Ошибка обновления", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteRecipeDialog(recipe: Recipe) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Удаление из рецепта")
            .setMessage("Удалить ${recipe.ingredientName} из рецепта ${selectedProduct?.name}?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteRecipe(recipe)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteRecipe(recipe: Recipe) {
        if (dbHelper.deleteRecipe(recipe.id)) {
            val index = recipes.indexOfFirst { it.id == recipe.id }
            if (index != -1) {
                recipes.removeAt(index)
                adapter.notifyItemRemoved(index)
                updateRecipeSummary()
                Toast.makeText(this, "Ингредиент удален из рецепта", Toast.LENGTH_SHORT).show()

                if (recipes.isEmpty()) {
                    btnProduce.isEnabled = false
                    tvEmptyRecipes.visibility = View.VISIBLE
                    rvRecipes.visibility = View.GONE
                }
            }
        } else {
            Toast.makeText(this, "Ошибка удаления рецепта", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRecipeDetails(recipe: Recipe) {
        val ingredient = dbHelper.getProductById(recipe.ingredientId)
        val maxFromThisIngredient = if (ingredient != null && recipe.quantityNeeded > 0) {
            (ingredient.stock / recipe.quantityNeeded).toInt()
        } else {
            0
        }

        val details = buildString {
            appendLine("Ингредиент: ${recipe.ingredientName}")
            appendLine("Требуется на 1 ${selectedProduct?.unit ?: "шт"}: ${recipe.quantityNeeded} ${recipe.ingredientUnit}")
            appendLine("На складе: ${ingredient?.stock ?: 0} ${recipe.ingredientUnit}")
            appendLine("Достаточно для: $maxFromThisIngredient ${selectedProduct?.unit ?: "шт"}")

            if ((ingredient?.stock ?: 0.0) < recipe.quantityNeeded) {
                val needed = recipe.quantityNeeded - (ingredient?.stock?.toDouble() ?: 0.0)
                appendLine()
                appendLine("⚠️ Недостаточно для приготовления 1 шт")
                appendLine("Нужно докупить: ${String.format("%.2f", needed)} ${recipe.ingredientUnit}")
            } else {
                appendLine()
                appendLine("✓ Достаточно")
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Детали ингредиента")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showProduceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_produce_quantity, null)

        val tvProduct = dialogView.findViewById<TextView>(R.id.tvProduct)
        val etQuantity = dialogView.findViewById<TextInputEditText>(R.id.etQuantity)

        val maxProduction = calculateMaxProduction()

        tvProduct.text = "Товар: ${selectedProduct!!.name}\nМожно приготовить: $maxProduction ${selectedProduct!!.unit}"
        etQuantity.hint = "Количество для приготовления"

        MaterialAlertDialogBuilder(this)
            .setTitle("Приготовление товара")
            .setView(dialogView)
            .setPositiveButton("Приготовить") { dialog, _ ->
                val quantityStr = etQuantity.text.toString().trim()
                val quantity = quantityStr.toIntOrNull()

                if (quantity != null && quantity > 0) {
                    if (quantity > maxProduction) {
                        Toast.makeText(this, "Недостаточно ингредиентов. Максимум: $maxProduction", Toast.LENGTH_LONG).show()
                    } else {
                        produceProduct(quantity)
                    }
                } else {
                    Toast.makeText(this, "Введите корректное количество", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun produceProduct(quantity: Int) {
        // Сначала рассчитываем новую себестоимость
        val productCost = dbHelper.calculateProductCost(selectedProduct!!.id)

        // Проверяем, хватает ли ингредиентов (только проверка, без списания)
        if (!dbHelper.canProduceProduct(selectedProduct!!.id, quantity)) {
            Toast.makeText(this, "Недостаточно ингредиентов", Toast.LENGTH_LONG).show()
            return
        }

        // Проверяем, не превышает ли себестоимость цену
        if (productCost > selectedProduct!!.price * 0.95) {
            showCostWarningDialog(productCost, quantity)
            return
        }

        // Пытаемся обновить себестоимость ПЕРЕД списанием
        val canUpdateCost = dbHelper.updateProductCostWithCheck(
            selectedProduct!!.id,
            productCost
        )

        if (!canUpdateCost) {
            // Предлагаем создать новый продукт с новой себестоимостью
            showNewProductDialog(productCost)
            return
        }

        // Только после успешного обновления себестоимости списываем ингредиенты
        if (dbHelper.produceProduct(selectedProduct!!.id, quantity)) {
            // Обновляем выбранный продукт в памяти
            selectedProduct = dbHelper.getProductById(selectedProduct!!.id)

            // Добавляем активность
            val activity = ActivityItem(
                id = IdGenerator.generateId(),
                type = "Приготовление",
                description = "${selectedProduct!!.name} - $quantity ${selectedProduct!!.unit}",
                amount = "+${quantity} ${selectedProduct!!.unit}",
                time = TimeUtils.getCurrentTime(),
                date = Date()
            )

            dbHelper.addActivity(activity)

            // Обновляем данные и текст описания
            loadRecipes() // Это обновит список рецептов и вызовет updateRecipeSummary()

            Toast.makeText(this, "Приготовлено $quantity ${selectedProduct!!.unit}", Toast.LENGTH_LONG).show()

            // Показываем детали производства
            showProductionDetails(quantity, productCost)
        } else {
            Toast.makeText(this, "Ошибка приготовления", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showCostWarningDialog(productCost: Double, quantity: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Внимание: Высокая себестоимость")
            .setMessage("Себестоимость приготовления (${"%.2f".format(productCost)} ₽) составляет ${"%.1f".format((productCost / selectedProduct!!.price) * 100)}% от цены продажи (${selectedProduct!!.price} ₽).\n\n" +
                    "Маржа составит всего ${"%.2f".format(selectedProduct!!.price - productCost)} ₽.\n\n" +
                    "Продолжить приготовление?")
            .setPositiveButton("Продолжить") { _, _ ->
                // Обновляем себестоимость даже если она высокая
                val success = dbHelper.updateProductCostForce(selectedProduct!!.id, productCost)
                if (success) {
                    // Обновляем продукт в памяти и продолжаем производство
                    selectedProduct = dbHelper.getProductById(selectedProduct!!.id)
                    proceedWithProduction(quantity, productCost)
                } else {
                    Toast.makeText(this, "Ошибка обновления себестоимости", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена") { _, _ ->
                // Ничего не делаем, просто закрываем диалог
                Toast.makeText(this, "Приготовление отменено", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    private fun showNewProductDialog(newCostPrice: Double) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Новая себестоимость")
            .setMessage("Себестоимость изменилась на ${"%.1f".format(Math.abs((newCostPrice - selectedProduct!!.costPrice) / selectedProduct!!.costPrice * 100))}%.\n\n" +
                    "Рекомендуется создать новый вариант товара с новой себестоимостью.\n\n" +
                    "Что вы хотите сделать?")
            .setPositiveButton("Создать новый") { _, _ ->
                createNewProductVariant(newCostPrice)
            }
            .setNegativeButton("Обновить текущий") { _, _ ->
                // Принудительно обновляем себестоимость и продолжаем
                dbHelper.updateProductCostForce(selectedProduct!!.id, newCostPrice)
                // Обновляем продукт в памяти
                selectedProduct = dbHelper.getProductById(selectedProduct!!.id)
                Toast.makeText(this, "Себестоимость обновлена", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Отмена") { _, _ ->
                Toast.makeText(this, "Приготовление отменено", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    private fun createNewProductVariant(newCostPrice: Double) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_product_variant, null)

        val etNewName = dialogView.findViewById<TextInputEditText>(R.id.etNewName)
        val etNewPrice = dialogView.findViewById<TextInputEditText>(R.id.etNewPrice)
        val tvOldInfo = dialogView.findViewById<TextView>(R.id.tvOldInfo)

        etNewName.setText("${selectedProduct!!.name} (новая себестоимость)")
        etNewPrice.setText(selectedProduct!!.price.toString())

        tvOldInfo.text = "Старая цена: ${selectedProduct!!.price} ₽\n" +
                "Новая себестоимость: ${"%.2f".format(newCostPrice)} ₽\n" +
                "Старая маржа: ${"%.2f".format(selectedProduct!!.price - selectedProduct!!.costPrice)} ₽\n" +
                "Новая маржа: ${"%.2f".format(selectedProduct!!.price - newCostPrice)} ₽"

        MaterialAlertDialogBuilder(this)
            .setTitle("Создание нового варианта")
            .setView(dialogView)
            .setPositiveButton("Создать") { _, _ ->
                val newName = etNewName.text.toString().trim()
                val newPrice = etNewPrice.text.toString().toDoubleOrNull() ?: selectedProduct!!.price

                val newProduct = selectedProduct!!.copy(
                    id = IdGenerator.generateId(),
                    name = newName,
                    price = newPrice,
                    costPrice = newCostPrice,
                    stock = 0.0
                )

                dbHelper.addProduct(newProduct)
                Toast.makeText(this, "Создан новый вариант товара", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun proceedWithProduction(quantity: Int, productCost: Double? = null) {
        if (dbHelper.produceProduct(selectedProduct!!.id, quantity)) {
            // Обновляем выбранный продукт в памяти
            selectedProduct = dbHelper.getProductById(selectedProduct!!.id)

            // Добавляем активность
            val activity = ActivityItem(
                id = IdGenerator.generateId(),
                type = "Приготовление",
                description = "${selectedProduct!!.name} - $quantity ${selectedProduct!!.unit}",
                amount = "+${quantity} ${selectedProduct!!.unit}",
                time = TimeUtils.getCurrentTime(),
                date = Date()
            )

            dbHelper.addActivity(activity)

            // Обновляем данные и текст описания
            loadRecipes()

            // Показываем детали производства
            showProductionDetails(quantity, productCost)

            Toast.makeText(this, "Приготовлено $quantity ${selectedProduct!!.unit}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Ошибка приготовления", Toast.LENGTH_SHORT).show()
        }
    }



    private fun showProductionDetails(quantity: Int, productCost: Double? = null) {
        val details = buildString {
            appendLine("✅ ПРИГОТОВЛЕНИЕ ЗАВЕРШЕНО")
            appendLine("Товар: ${selectedProduct!!.name}")
            appendLine("Количество: $quantity ${selectedProduct!!.unit}")

            productCost?.let {
                appendLine("Себестоимость: ${"%.2f".format(it)} ₽")
                appendLine("Цена продажи: ${"%.2f".format(selectedProduct!!.price)} ₽")
                val margin = selectedProduct!!.price - it
                val marginPercent = if (it > 0) (margin / it * 100) else 0.0
                appendLine("Маржа: ${"%.2f".format(margin)} ₽ (${"%.1f".format(marginPercent)}%)")
            }

            appendLine()
            appendLine("Использовано ингредиентов:")

            for (recipe in recipes) {
                val totalNeeded = recipe.quantityNeeded * quantity
                val ingredient = dbHelper.getProductById(recipe.ingredientId)
                val newStock = (ingredient?.stock ?: 0.0) - totalNeeded
                appendLine("• ${recipe.ingredientName}: ${"%.3f".format(totalNeeded)} ${recipe.ingredientUnit} (осталось: ${"%.3f".format(newStock)})")
            }

            val newProductStock = (selectedProduct?.stock ?: 0.0) + quantity
            appendLine()
            appendLine("Новый остаток товара: ${"%.0f".format(newProductStock)} ${selectedProduct!!.unit}")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Детали производства")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.recipes_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_change_product -> {
                loadProductsForSelection()
                true
            }
            R.id.action_view_all_recipes -> {
                showAllRecipesDialog()
                true
            }
            R.id.action_export_recipes -> {
                exportRecipes()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAllRecipesDialog() {
        val allRecipes = dbHelper.getAllRecipes()

        if (allRecipes.isEmpty()) {
            Toast.makeText(this, "Рецепты не найдены", Toast.LENGTH_SHORT).show()
            return
        }

        val recipesByProduct = allRecipes.groupBy { it.productName }
        val message = buildString {
            appendLine("ВСЕ РЕЦЕПТЫ В СИСТЕМЕ")
            appendLine("=".repeat(40))
            appendLine()

            for ((productName, productRecipes) in recipesByProduct) {
                appendLine("$productName:")
                appendLine("-".repeat(30))
                productRecipes.forEach { recipe ->
                    appendLine("  • ${recipe.ingredientName}: ${recipe.quantityNeeded} ${recipe.ingredientUnit}")
                }
                appendLine()
            }

            appendLine("Всего рецептов: ${allRecipes.size}")
            appendLine("Всего товаров с рецептами: ${recipesByProduct.size}")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Все рецепты")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun exportRecipes() {
        val recipes = dbHelper.getAllRecipes()

        if (recipes.isEmpty()) {
            Toast.makeText(this, "Нет рецептов для экспорта", Toast.LENGTH_SHORT).show()
            return
        }

        val exportText = buildString {
            appendLine("ЭКСПОРТ РЕЦЕПТОВ ПЕКАРНИ")
            appendLine("=".repeat(50))
            appendLine("Дата экспорта: ${TimeUtils.getCurrentDate()} ${TimeUtils.getCurrentTime()}")
            appendLine()

            val groupedRecipes = recipes.groupBy { it.productName }

            for ((productName, productRecipes) in groupedRecipes) {
                appendLine("ТОВАР: $productName")
                appendLine("-".repeat(40))

                productRecipes.forEach { recipe ->
                    appendLine("${recipe.ingredientName}: ${recipe.quantityNeeded} ${recipe.ingredientUnit} на 1 ${recipe.productUnit}")
                }

                appendLine()
                val totalIngredients = productRecipes.size
                val ingredientList = productRecipes.joinToString(", ") { it.ingredientName }
                appendLine("Итого: $totalIngredients ингредиентов ($ingredientList)")
                appendLine()
            }

            appendLine("=".repeat(50))
            appendLine("Всего рецептов: ${recipes.size}")
            appendLine("Всего товаров: ${groupedRecipes.size}")
        }

        // Показываем предпросмотр
        MaterialAlertDialogBuilder(this)
            .setTitle("Экспорт рецептов")
            .setMessage(exportText)
            .setPositiveButton("OK", null)
            .setNeutralButton("Поделиться") { _, _ ->
                shareRecipes(exportText)
            }
            .show()
    }

    private fun shareRecipes(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Рецепты пекарни - Экспорт")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Поделиться рецептами"))
    }
}


