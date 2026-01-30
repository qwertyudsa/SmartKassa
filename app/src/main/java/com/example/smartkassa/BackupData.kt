package com.example.smartkassa

data class BackupData(
    val users: List<User>? = null,
    val products: List<Product>? = null,
    val sales: List<Sale>? = null,
    val incomeRecords: List<IncomeRecord>? = null,
    val suppliers: List<Supplier>? = null,
    val activities: List<ActivityItem>? = null,
    val recipes: List<Recipe>? = null,
    val units: List<UnitDefinition>? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalRecords: Int
        get() = (users?.size ?: 0) +
                (products?.size ?: 0) +
                (sales?.size ?: 0) +
                (incomeRecords?.size ?: 0) +
                (suppliers?.size ?: 0) +
                (activities?.size ?: 0) +
                (recipes?.size ?: 0)
}