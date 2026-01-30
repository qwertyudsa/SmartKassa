package com.example.smartkassa

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.*

class AllActivitiesActivity : AppCompatActivity() {

    private lateinit var rvAllActivities: RecyclerView
    private lateinit var btnLoadAll: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvTotalCount: TextView

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ActivityAdapter

    private var allActivities: List<ActivityItem> = emptyList()
    private var displayedActivities: List<ActivityItem> = emptyList()
    private var isLoadingAll = false

    // Параметры сортировки и фильтрации
    private var currentSortType: SortType = SortType.DATE_DESC
    private var currentFilterOptions = FilterOptions()
    private var currentSearchQuery: String = ""

    enum class SortType {
        DATE_DESC,    // Новые сверху
        DATE_ASC,     // Старые сверху
        AMOUNT_DESC,  // Большие суммы сверху
        AMOUNT_ASC,   // Маленькие суммы сверху
        TYPE_ASC      // По типу (А-Я)
    }

    data class FilterOptions(
        val types: Set<String> = emptySet(),
        val showIncome: Boolean = true,   // Доходы (+)
        val showExpense: Boolean = true   // Расходы (-)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_activities)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupRecyclerView()
        loadInitialData()
        setupClickListeners()
    }

    private fun initViews() {
        rvAllActivities = findViewById(R.id.rvAllActivities)
        btnLoadAll = findViewById(R.id.btnLoadAll)
        progressBar = findViewById(R.id.progressBar)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvTotalCount = findViewById(R.id.tvTotalCount)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Все операции"
    }

    private fun setupRecyclerView() {
        adapter = ActivityAdapter(emptyList()) { activity ->
            showActivityDetails(activity)
        }
        rvAllActivities.layoutManager = LinearLayoutManager(this)
        rvAllActivities.adapter = adapter
    }

    private fun setupClickListeners() {
        btnLoadAll.setOnClickListener {
            if (!isLoadingAll) {
                loadAllActivities()
            }
        }
    }

    private fun loadInitialData() {
        // Загружаем первые 20 операций
        val initialActivities = dbHelper.getRecentActivities(20)
        allActivities = initialActivities
        displayedActivities = applyFiltersAndSorting(initialActivities)
        adapter = ActivityAdapter(displayedActivities) { activity ->
            showActivityDetails(activity)
        }
        rvAllActivities.adapter = adapter

        updateStatistics(displayedActivities)
    }

    private fun loadAllActivities() {
        isLoadingAll = true
        progressBar.visibility = View.VISIBLE
        btnLoadAll.isEnabled = false
        btnLoadAll.text = "Загрузка..."
        Thread {
            Thread.sleep(500)

            runOnUiThread {
                val allActivitiesFromDb = dbHelper.getAllActivities()
                allActivities = allActivitiesFromDb
                displayedActivities = applyFiltersAndSorting(allActivitiesFromDb)

                adapter = ActivityAdapter(displayedActivities) { activity ->
                    showActivityDetails(activity)
                }
                rvAllActivities.adapter = adapter

                updateStatistics(displayedActivities)

                progressBar.visibility = View.GONE
                btnLoadAll.isEnabled = true
                btnLoadAll.text = "Все операции загружены"
                btnLoadAll.isClickable = false
                isLoadingAll = false

                Toast.makeText(this, "Загружено ${allActivitiesFromDb.size} операций", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun updateStatistics(activities: List<ActivityItem>) {
        var totalIncome = 0.0
        var totalExpense = 0.0

        activities.forEach { activity ->
            val amount = activity.amount.replace("[^\\d.-]".toRegex(), "").toDoubleOrNull() ?: 0.0
            if (activity.amount.startsWith("+")) {
                totalIncome += amount
            } else if (activity.amount.startsWith("-")) {
                totalExpense += amount
            }
        }

        tvTotalIncome.text = "%,.0f ₽".format(totalIncome)
        tvTotalExpense.text = "%,.0f ₽".format(totalExpense)
        tvTotalCount.text = activities.size.toString()
    }

    private fun applyFiltersAndSorting(activities: List<ActivityItem>): List<ActivityItem> {
        var result = activities

        // Применяем фильтрацию по типу
        if (currentFilterOptions.types.isNotEmpty()) {
            result = result.filter { currentFilterOptions.types.contains(it.type) }
        }

        // Фильтрация доходов/расходов
        val filteredByAmountType = mutableListOf<ActivityItem>()
        if (currentFilterOptions.showIncome) {
            filteredByAmountType.addAll(result.filter { it.amount.startsWith("+") })
        }
        if (currentFilterOptions.showExpense) {
            filteredByAmountType.addAll(result.filter { it.amount.startsWith("-") })
        }
        result = filteredByAmountType

        // Применяем поиск
        if (currentSearchQuery.isNotEmpty()) {
            val query = currentSearchQuery.lowercase(Locale.getDefault())
            result = result.filter {
                it.description.lowercase(Locale.getDefault()).contains(query) ||
                        it.type.lowercase(Locale.getDefault()).contains(query)
            }
        }

        // Применяем сортировку
        result = when (currentSortType) {
            SortType.DATE_DESC -> result.sortedByDescending { it.date }
            SortType.DATE_ASC -> result.sortedBy { it.date }
            SortType.AMOUNT_DESC -> result.sortedByDescending {
                it.amount.replace("[^\\d.-]".toRegex(), "").toDoubleOrNull() ?: 0.0
            }
            SortType.AMOUNT_ASC -> result.sortedBy {
                it.amount.replace("[^\\d.-]".toRegex(), "").toDoubleOrNull() ?: 0.0
            }
            SortType.TYPE_ASC -> result.sortedBy { it.type }
        }

        return result
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

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "Новые сверху",
            "Старые сверху",
            "Сумма (по убыванию)",
            "Сумма (по возрастанию)",
            "По типу (А-Я)"
        )

        var checkedItem = when (currentSortType) {
            SortType.DATE_DESC -> 0
            SortType.DATE_ASC -> 1
            SortType.AMOUNT_DESC -> 2
            SortType.AMOUNT_ASC -> 3
            SortType.TYPE_ASC -> 4
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Сортировка")
            .setSingleChoiceItems(sortOptions, checkedItem) { dialog, which ->
                currentSortType = when (which) {
                    0 -> SortType.DATE_DESC
                    1 -> SortType.DATE_ASC
                    2 -> SortType.AMOUNT_DESC
                    3 -> SortType.AMOUNT_ASC
                    4 -> SortType.TYPE_ASC
                    else -> SortType.DATE_DESC
                }

                displayedActivities = applyFiltersAndSorting(allActivities)
                adapter = ActivityAdapter(displayedActivities) { activity ->
                    showActivityDetails(activity)
                }
                rvAllActivities.adapter = adapter
                updateStatistics(displayedActivities)

                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showFilterDialog() {
        val activityTypes = arrayOf("Продажа", "Поступление", "Приготовление", "Обратная связь")
        val checkedItems = booleanArrayOf(
            currentFilterOptions.types.contains("Продажа"),
            currentFilterOptions.types.contains("Поступление"),
            currentFilterOptions.types.contains("Приготовление"),
            currentFilterOptions.types.contains("Обратная связь")
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Фильтр по типу")
            .setMultiChoiceItems(activityTypes, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Применить") { _, _ ->
                val selectedTypes = mutableSetOf<String>()
                checkedItems.forEachIndexed { index, isChecked ->
                    if (isChecked) {
                        selectedTypes.add(activityTypes[index])
                    }
                }

                currentFilterOptions = currentFilterOptions.copy(types = selectedTypes)

                displayedActivities = applyFiltersAndSorting(allActivities)
                adapter = ActivityAdapter(displayedActivities) { activity ->
                    showActivityDetails(activity)
                }
                rvAllActivities.adapter = adapter
                updateStatistics(displayedActivities)

                Toast.makeText(this, "Фильтр применен", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Сбросить") { _, _ ->
                currentFilterOptions = FilterOptions()
                displayedActivities = applyFiltersAndSorting(allActivities)
                adapter = ActivityAdapter(displayedActivities) { activity ->
                    showActivityDetails(activity)
                }
                rvAllActivities.adapter = adapter
                updateStatistics(displayedActivities)
                Toast.makeText(this, "Фильтр сброшен", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Отмена", null)
            .show()
    }

    private fun showStatisticsDialog() {
        val incomeActivities = displayedActivities.filter { it.amount.startsWith("+") }
        val expenseActivities = displayedActivities.filter { it.amount.startsWith("-") }

        var totalIncome = 0.0
        var totalExpense = 0.0

        incomeActivities.forEach { activity ->
            totalIncome += activity.amount.replace("[^\\d.-]".toRegex(), "").toDoubleOrNull() ?: 0.0
        }

        expenseActivities.forEach { activity ->
            totalExpense += activity.amount.replace("[^\\d.-]".toRegex(), "").toDoubleOrNull() ?: 0.0
        }

        val statistics = """
            Всего операций: ${displayedActivities.size}
            
            Доходы:
            • Количество: ${incomeActivities.size}
            • Сумма: %,.0f ₽
            
            Расходы:
            • Количество: ${expenseActivities.size}
            • Сумма: %,.0f ₽
            
            Итого: %,.0f ₽
        """.trimIndent().format(totalIncome, totalExpense, totalIncome + totalExpense)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Статистика")
            .setMessage(statistics)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.all_activities_menu, menu)

        // Настройка SearchView
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = "Поиск операций..."

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                currentSearchQuery = newText
                displayedActivities = applyFiltersAndSorting(allActivities)
                adapter = ActivityAdapter(displayedActivities) { activity ->
                    showActivityDetails(activity)
                }
                rvAllActivities.adapter = adapter
                updateStatistics(displayedActivities)
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            R.id.action_stats -> {
                showStatisticsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}