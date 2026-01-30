package com.example.smartkassa

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.util.Calendar
import java.util.Date

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "smartkassa.db"

        // Таблица пользователей
        const val TABLE_USERS = "users"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_BUSINESS_NAME = "business_name"
        const val COLUMN_CURRENCY = "currency"
        const val COLUMN_TAX_RATE = "tax_rate"

        // Таблица товаров (готовой продукции)
        const val TABLE_PRODUCTS = "products"
        const val COLUMN_PRODUCT_ID = "product_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_PRICE = "price"
        const val COLUMN_COST_PRICE = "cost_price"
        const val COLUMN_STOCK = "stock"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_BARCODE = "barcode"
        const val COLUMN_IS_INGREDIENT = "is_ingredient" // 0 - товар, 1 - ингредиент
        const val COLUMN_UNIT = "unit" // единица измерения (кг, шт, л и т.д.)

        // Таблица рецептов (для пекарни)
        const val TABLE_RECIPES = "recipes"
        const val COLUMN_RECIPE_ID = "recipe_id"
        const val COLUMN_PRODUCT_ID_FK = "product_id_fk"
        const val COLUMN_INGREDIENT_ID_FK = "ingredient_id_fk"
        const val COLUMN_QUANTITY_NEEDED = "quantity_needed"

        // Таблица поставщиков
        const val TABLE_SUPPLIERS = "suppliers"
        const val COLUMN_SUPPLIER_ID = "supplier_id"
        const val COLUMN_SUPPLIER_NAME = "supplier_name"
        const val COLUMN_CONTACT_PERSON = "contact_person"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_EMAIL_SUPPLIER = "email_supplier"

        // Таблица продаж
        const val TABLE_SALES = "sales"
        const val COLUMN_SALE_ID = "sale_id"
        const val COLUMN_SALE_DATE = "sale_date"
        const val COLUMN_TOTAL_AMOUNT = "total_amount"
        const val COLUMN_PAYMENT_METHOD = "payment_method"
        const val COLUMN_USER_ID_FK = "user_id_fk"

        // Таблица позиций продажи
        const val TABLE_SALE_ITEMS = "sale_items"
        const val COLUMN_SALE_ITEM_ID = "sale_item_id"
        const val COLUMN_SALE_ID_FK = "sale_id_fk"
        const val COLUMN_QUANTITY = "quantity"
        const val COLUMN_SUBTOTAL = "subtotal"

        // Таблица поставок
        const val TABLE_INCOME_RECORDS = "income_records"
        const val COLUMN_INCOME_ID = "income_id"
        const val COLUMN_INCOME_DATE = "income_date"
        const val COLUMN_SUPPLIER_ID_FK = "supplier_id_fk"
        const val COLUMN_INVOICE_NUMBER = "invoice_number"
        const val COLUMN_TOTAL_COST = "total_cost"

        // Таблица позиций поставки
        const val TABLE_INCOME_ITEMS = "income_items"
        const val COLUMN_INCOME_ITEM_ID = "income_item_id"
        const val COLUMN_INCOME_ID_FK = "income_id_fk"
        const val COLUMN_COST_PER_UNIT= "cost_per_unit"

        // Таблица активностей
        const val TABLE_ACTIVITIES = "activities"
        const val COLUMN_ACTIVITY_ID = "activity_id"
        const val COLUMN_ACTIVITY_TYPE = "activity_type"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_AMOUNT = "amount"
        const val COLUMN_ACTIVITY_TIME = "activity_time"
        const val COLUMN_ACTIVITY_DATE = "activity_date"

        // Таблица единиц измерения
        const val TABLE_UNITS = "units"
        const val COLUMN_UNIT_ID = "unit_id"
        const val COLUMN_UNIT_NAME = "unit_name"
        const val COLUMN_UNIT_CATEGORY = "unit_category"
        const val COLUMN_BASE_UNIT = "base_unit"
        const val COLUMN_CONVERSION_RATE = "conversion_rate"

    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("DatabaseHelper", "Создание базы данных")

        // Таблица пользователей
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID TEXT PRIMARY KEY,
                $COLUMN_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_BUSINESS_NAME TEXT DEFAULT 'Мой бизнес',
                $COLUMN_CURRENCY TEXT DEFAULT 'RUB',
                $COLUMN_TAX_RATE REAL DEFAULT 20.0
            )
        """.trimIndent()

        // Таблица товаров/ингредиентов
        val createProductsTable = """
            CREATE TABLE $TABLE_PRODUCTS (
                $COLUMN_PRODUCT_ID TEXT PRIMARY KEY,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_PRICE REAL DEFAULT 0.0,
                $COLUMN_COST_PRICE REAL DEFAULT 0.0,
                $COLUMN_STOCK INTEGER DEFAULT 0,
                $COLUMN_CATEGORY TEXT DEFAULT 'Без категории',
                $COLUMN_BARCODE TEXT,
                $COLUMN_IS_INGREDIENT INTEGER DEFAULT 0,
                $COLUMN_UNIT TEXT DEFAULT 'шт',
                UNIQUE($COLUMN_NAME)
            )
        """.trimIndent()

        // Таблица рецептов
        val createRecipesTable = """
            CREATE TABLE $TABLE_RECIPES (
                $COLUMN_RECIPE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PRODUCT_ID_FK TEXT NOT NULL,
                $COLUMN_INGREDIENT_ID_FK TEXT NOT NULL,
                $COLUMN_QUANTITY_NEEDED REAL NOT NULL,
                FOREIGN KEY ($COLUMN_PRODUCT_ID_FK) 
                    REFERENCES $TABLE_PRODUCTS($COLUMN_PRODUCT_ID),
                FOREIGN KEY ($COLUMN_INGREDIENT_ID_FK) 
                    REFERENCES $TABLE_PRODUCTS($COLUMN_PRODUCT_ID),
                UNIQUE($COLUMN_PRODUCT_ID_FK, $COLUMN_INGREDIENT_ID_FK)
            )
        """.trimIndent()

        // Таблица поставщиков
        val createSuppliersTable = """
            CREATE TABLE $TABLE_SUPPLIERS (
                $COLUMN_SUPPLIER_ID TEXT PRIMARY KEY,
                $COLUMN_SUPPLIER_NAME TEXT NOT NULL,
                $COLUMN_CONTACT_PERSON TEXT,
                $COLUMN_PHONE TEXT,
                $COLUMN_EMAIL_SUPPLIER TEXT
            )
        """.trimIndent()

        // Таблица продаж
        val createSalesTable = """
            CREATE TABLE $TABLE_SALES (
                $COLUMN_SALE_ID TEXT PRIMARY KEY,
                $COLUMN_SALE_DATE INTEGER NOT NULL,
                $COLUMN_TOTAL_AMOUNT REAL NOT NULL,
                $COLUMN_PAYMENT_METHOD TEXT DEFAULT 'Наличные',
                $COLUMN_USER_ID_FK TEXT NOT NULL,
                FOREIGN KEY ($COLUMN_USER_ID_FK) 
                    REFERENCES $TABLE_USERS($COLUMN_USER_ID)
            )
        """.trimIndent()

        // Таблица позиций продажи
        val createSaleItemsTable = """
            CREATE TABLE $TABLE_SALE_ITEMS (
                $COLUMN_SALE_ITEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SALE_ID_FK TEXT NOT NULL,
                $COLUMN_PRODUCT_ID TEXT NOT NULL,
                $COLUMN_QUANTITY INTEGER NOT NULL,
                $COLUMN_SUBTOTAL REAL NOT NULL,
                FOREIGN KEY ($COLUMN_SALE_ID_FK) 
                    REFERENCES $TABLE_SALES($COLUMN_SALE_ID),
                FOREIGN KEY ($COLUMN_PRODUCT_ID) 
                    REFERENCES $TABLE_PRODUCTS($COLUMN_PRODUCT_ID)
            )
        """.trimIndent()

        // Таблица поставок
        val createIncomeRecordsTable = """
            CREATE TABLE $TABLE_INCOME_RECORDS (
                $COLUMN_INCOME_ID TEXT PRIMARY KEY,
                $COLUMN_INCOME_DATE INTEGER NOT NULL,
                $COLUMN_SUPPLIER_ID_FK TEXT NOT NULL,
                $COLUMN_INVOICE_NUMBER TEXT,
                $COLUMN_TOTAL_COST REAL NOT NULL,
                FOREIGN KEY ($COLUMN_SUPPLIER_ID_FK) 
                    REFERENCES $TABLE_SUPPLIERS($COLUMN_SUPPLIER_ID)
            )
        """.trimIndent()

        // Таблица позиций поставки
        val createIncomeItemsTable = """
            CREATE TABLE $TABLE_INCOME_ITEMS (
                $COLUMN_INCOME_ITEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_INCOME_ID_FK TEXT NOT NULL,
                $COLUMN_PRODUCT_ID TEXT NOT NULL,
                $COLUMN_QUANTITY INTEGER NOT NULL,
                $COLUMN_COST_PER_UNIT REAL NOT NULL,
                $COLUMN_SUBTOTAL REAL NOT NULL,
                FOREIGN KEY ($COLUMN_INCOME_ID_FK) 
                    REFERENCES $TABLE_INCOME_RECORDS($COLUMN_INCOME_ID),
                FOREIGN KEY ($COLUMN_PRODUCT_ID) 
                    REFERENCES $TABLE_PRODUCTS($COLUMN_PRODUCT_ID)
            )
        """.trimIndent()

        // Таблица активностей
        val createActivitiesTable = """
            CREATE TABLE $TABLE_ACTIVITIES (
                $COLUMN_ACTIVITY_ID TEXT PRIMARY KEY,
                $COLUMN_ACTIVITY_TYPE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT NOT NULL,
                $COLUMN_AMOUNT TEXT NOT NULL,
                $COLUMN_ACTIVITY_TIME TEXT NOT NULL,
                $COLUMN_ACTIVITY_DATE INTEGER NOT NULL
            )
        """.trimIndent()
        val createUnitsTable = """
            CREATE TABLE $TABLE_UNITS (
                $COLUMN_UNIT_ID TEXT PRIMARY KEY,
                $COLUMN_UNIT_NAME TEXT UNIQUE NOT NULL,
                $COLUMN_UNIT_CATEGORY TEXT NOT NULL,
                $COLUMN_BASE_UNIT TEXT NOT NULL,
                $COLUMN_CONVERSION_RATE REAL DEFAULT 1.0
            )
        """.trimIndent()
        db.execSQL(createUnitsTable)

        // Создаем все таблицы
        db.execSQL(createUsersTable)
        db.execSQL(createProductsTable)
        db.execSQL(createRecipesTable)
        db.execSQL(createSuppliersTable)
        db.execSQL(createSalesTable)
        db.execSQL(createSaleItemsTable)
        db.execSQL(createIncomeRecordsTable)
        db.execSQL(createIncomeItemsTable)
        db.execSQL(createActivitiesTable)

        Log.d("DatabaseHelper", "Все таблицы созданы успешно")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DatabaseHelper", "Обновление базы данных с версии $oldVersion на $newVersion")
        try {
            if (oldVersion < 2) {
                // Создаем таблицу units для версии 2
                val createUnitsTable = """
                CREATE TABLE $TABLE_UNITS (
                    $COLUMN_UNIT_ID TEXT PRIMARY KEY,
                    $COLUMN_UNIT_NAME TEXT UNIQUE NOT NULL,
                    $COLUMN_UNIT_CATEGORY TEXT NOT NULL,
                    $COLUMN_BASE_UNIT TEXT NOT NULL,
                    $COLUMN_CONVERSION_RATE REAL DEFAULT 1.0
                )
            """.trimIndent()

                db.execSQL(createUnitsTable)
                Log.d("DatabaseHelper", "Таблица units создана")

                // Добавляем стандартные единицы
                addDefaultUnitsOnUpgrade(db)
            }

            // Если будут другие версии, добавьте условия
            if (oldVersion < 3) {
                // Код для версии 3
            }

        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Ошибка обновления базы: ${e.message}")
            // Если произошла ошибка, пересоздаем всю базу
            recreateDatabase(db)
        }
    }
    private fun addDefaultUnitsOnUpgrade(db: SQLiteDatabase) {
        val defaultUnits = listOf(
            ContentValues().apply {
                put(COLUMN_UNIT_ID, IdGenerator.generateId())
                put(COLUMN_UNIT_NAME, "кг")
                put(COLUMN_UNIT_CATEGORY, UnitDefinition.CATEGORY_WEIGHT)
                put(COLUMN_BASE_UNIT, UnitDefinition.BASE_KG)
                put(COLUMN_CONVERSION_RATE, 1.0)
            },
            ContentValues().apply {
                put(COLUMN_UNIT_ID, IdGenerator.generateId())
                put(COLUMN_UNIT_NAME, "г")
                put(COLUMN_UNIT_CATEGORY, UnitDefinition.CATEGORY_WEIGHT)
                put(COLUMN_BASE_UNIT, UnitDefinition.BASE_KG)
                put(COLUMN_CONVERSION_RATE, 0.001) // 1 г = 0.001 кг
            },

            ContentValues().apply {
                put(COLUMN_UNIT_ID, IdGenerator.generateId())
                put(COLUMN_UNIT_NAME, "л")
                put(COLUMN_UNIT_CATEGORY, UnitDefinition.CATEGORY_VOLUME)
                put(COLUMN_BASE_UNIT, UnitDefinition.BASE_LITER)
                put(COLUMN_CONVERSION_RATE, 1.0)
            },
            ContentValues().apply {
                put(COLUMN_UNIT_ID, IdGenerator.generateId())
                put(COLUMN_UNIT_NAME, "мл")
                put(COLUMN_UNIT_CATEGORY, UnitDefinition.CATEGORY_VOLUME)
                put(COLUMN_BASE_UNIT, UnitDefinition.BASE_LITER)
                put(COLUMN_CONVERSION_RATE, 0.001) // 1 мл = 0.001 л
            },
            ContentValues().apply {
                put(COLUMN_UNIT_ID, IdGenerator.generateId())
                put(COLUMN_UNIT_NAME, "ст")
                put(COLUMN_UNIT_CATEGORY, UnitDefinition.CATEGORY_VOLUME)
                put(COLUMN_BASE_UNIT, UnitDefinition.BASE_LITER)
                put(COLUMN_CONVERSION_RATE, 0.2) // 1 стакан ≈ 200 мл = 0.2 л
            },
            ContentValues().apply {
                put(COLUMN_UNIT_ID, IdGenerator.generateId())
                put(COLUMN_UNIT_NAME, "ч.л.")
                put(COLUMN_UNIT_CATEGORY, UnitDefinition.CATEGORY_VOLUME)
                put(COLUMN_BASE_UNIT, UnitDefinition.BASE_LITER)
                put(COLUMN_CONVERSION_RATE, 0.005) // 1 ч.л. ≈ 5 мл = 0.005 л
            },
            ContentValues().apply {
                put(COLUMN_UNIT_ID, IdGenerator.generateId())
                put(COLUMN_UNIT_NAME, "ст.л.")
                put(COLUMN_UNIT_CATEGORY, UnitDefinition.CATEGORY_VOLUME)
                put(COLUMN_BASE_UNIT, UnitDefinition.BASE_LITER)
                put(COLUMN_CONVERSION_RATE, 0.015) // 1 ст.л. ≈ 15 мл = 0.015 л
            },
            ContentValues().apply {
                put(COLUMN_UNIT_ID, IdGenerator.generateId())
                put(COLUMN_UNIT_NAME, "шт")
                put(COLUMN_UNIT_CATEGORY, UnitDefinition.CATEGORY_PIECE)
                put(COLUMN_BASE_UNIT, UnitDefinition.BASE_PIECE)
                put(COLUMN_CONVERSION_RATE, 1.0)
            },
            ContentValues().apply {
                put(COLUMN_UNIT_ID, IdGenerator.generateId())
                put(COLUMN_UNIT_NAME, "уп")
                put(COLUMN_UNIT_CATEGORY, UnitDefinition.CATEGORY_PIECE)
                put(COLUMN_BASE_UNIT, UnitDefinition.BASE_PIECE)
                put(COLUMN_CONVERSION_RATE, 10.0) // 1 упаковка = 10 штук
            },
            ContentValues().apply {
                put(COLUMN_UNIT_ID, IdGenerator.generateId())
                put(COLUMN_UNIT_NAME, "пак")
                put(COLUMN_UNIT_CATEGORY, UnitDefinition.CATEGORY_PIECE)
                put(COLUMN_BASE_UNIT, UnitDefinition.BASE_PIECE)
                put(COLUMN_CONVERSION_RATE, 5.0) // 1 пакет = 5 штук
            }
        )

        defaultUnits.forEach { values ->
            db.insert(TABLE_UNITS, null, values)
        }
        Log.d("DatabaseHelper", "Добавлены стандартные единицы измерения")
    }

    private fun recreateDatabase(db: SQLiteDatabase) {
        // Удаляем все таблицы
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ACTIVITIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INCOME_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INCOME_RECORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SALE_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SALES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SUPPLIERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECIPES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_UNITS") // Добавляем удаление units

        // Создаем заново
        onCreate(db)
    }

    fun addUser(user: User): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, user.id)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_PASSWORD, user.password)
            put(COLUMN_BUSINESS_NAME, user.businessName)
            put(COLUMN_CURRENCY, user.currency)
            put(COLUMN_TAX_RATE, user.taxRate)
        }

        val result = db.insert(TABLE_USERS, null, values)
        db.close()

        if (result == -1L) {
            Log.e("DatabaseHelper", "Ошибка добавления пользователя: ${user.email}")
        } else {
            Log.d("DatabaseHelper", "Пользователь добавлен: ${user.email}")
        }

        return result
    }

    fun getUserByEmail(email: String): User? {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(email))

        return if (cursor.moveToFirst()) {
            val user = User(
                id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                businessName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BUSINESS_NAME)),
                currency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)),
                taxRate = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TAX_RATE))
            )
            cursor.close()
            db.close()
            user
        } else {
            cursor.close()
            db.close()
            null
        }
    }

    fun getAllUsers(): List<User> {
        val users = mutableListOf<User>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val user = User(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                    password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                    businessName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BUSINESS_NAME)),
                    currency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)),
                    taxRate = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TAX_RATE))
                )
                users.add(user)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return users
    }

    fun addProduct(product: Product): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PRODUCT_ID, product.id)
            put(COLUMN_NAME, product.name)
            put(COLUMN_PRICE, product.price)
            put(COLUMN_COST_PRICE, product.costPrice)
            put(COLUMN_STOCK, product.stock)
            put(COLUMN_CATEGORY, product.category)
            put(COLUMN_BARCODE, product.barcode)
            put(COLUMN_IS_INGREDIENT, if (product.isIngredient) 1 else 0)
            put(COLUMN_UNIT, product.unit)
        }

        val result = db.insert(TABLE_PRODUCTS, null, values)
        db.close()

        Log.d("DatabaseHelper", "Товар ${product.name} добавлен с остатком: ${product.stock} ${product.unit}")
        return result
    }

    fun getProductById(productId: String): Product? {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_PRODUCTS WHERE $COLUMN_PRODUCT_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(productId))

        return if (cursor.moveToFirst()) {
            val product = Product(
                id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)),
                stock = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_STOCK)), // Читаем как Double
                category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                barcode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARCODE)),
                costPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_COST_PRICE)),
                isIngredient = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_INGREDIENT)) == 1,
                unit = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT))
            )
            cursor.close()
            db.close()
            product
        } else {
            cursor.close()
            db.close()
            null
        }
    }

    fun getAllProducts(): List<Product> {
        val products = mutableListOf<Product>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_PRODUCTS ORDER BY $COLUMN_NAME"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val product = Product(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)),
                    stock = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_STOCK)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    barcode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARCODE)),
                    costPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_COST_PRICE)),
                    isIngredient = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_INGREDIENT)) == 1,
                    unit = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT))
                )
                products.add(product)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return products
    }

    fun convertProductUnit(productId: String, newUnit: String, conversionRate: Double = 1.0): Boolean {
        val product = getProductById(productId) ?: return false

        if (product.unit == newUnit) return true

        val convertedProduct = product.convertTo(newUnit, conversionRate)

        return updateProduct(convertedProduct)
    }

    fun searchProducts(query: String): List<Product> {
        val products = mutableListOf<Product>()
        val db = this.readableDatabase
        val searchQuery = "%$query%"
        val sql = """
            SELECT * FROM $TABLE_PRODUCTS 
            WHERE $COLUMN_NAME LIKE ? 
            OR $COLUMN_CATEGORY LIKE ? 
            OR $COLUMN_BARCODE LIKE ?
            ORDER BY $COLUMN_NAME
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(searchQuery, searchQuery, searchQuery))

        if (cursor.moveToFirst()) {
            do {
                val product = Product(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)),
                    stock = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_STOCK)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    barcode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARCODE)),
                    costPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_COST_PRICE)),
                    isIngredient = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_INGREDIENT)) == 1,
                    unit = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT))
                )
                products.add(product)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return products
    }

    fun deleteProduct(productId: String): Boolean {
        val db = this.writableDatabase

        // Проверяем, есть ли товар в продажах или поставках
        val checkSales = "SELECT COUNT(*) FROM $TABLE_SALE_ITEMS WHERE $COLUMN_PRODUCT_ID = ?"
        val cursorSales = db.rawQuery(checkSales, arrayOf(productId))
        cursorSales.moveToFirst()
        val salesCount = cursorSales.getInt(0)
        cursorSales.close()

        val checkIncome = "SELECT COUNT(*) FROM $TABLE_INCOME_ITEMS WHERE $COLUMN_PRODUCT_ID = ?"
        val cursorIncome = db.rawQuery(checkIncome, arrayOf(productId))
        cursorIncome.moveToFirst()
        val incomeCount = cursorIncome.getInt(0)
        cursorIncome.close()

        if (salesCount > 0 || incomeCount > 0) {
            Log.e("DatabaseHelper", "Нельзя удалить товар, который есть в продажах или поставках")
            db.close()
            return false
        }

        // Удаляем из рецептов
        db.delete(TABLE_RECIPES, "$COLUMN_PRODUCT_ID_FK = ? OR $COLUMN_INGREDIENT_ID_FK = ?",
            arrayOf(productId, productId))

        // Удаляем товар
        val result = db.delete(TABLE_PRODUCTS, "$COLUMN_PRODUCT_ID = ?", arrayOf(productId))
        db.close()

        return result > 0
    }

    fun addActivity(activity: ActivityItem): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ACTIVITY_ID, activity.id)
            put(COLUMN_ACTIVITY_TYPE, activity.type)
            put(COLUMN_DESCRIPTION, activity.description)
            put(COLUMN_AMOUNT, activity.amount)
            put(COLUMN_ACTIVITY_TIME, activity.time)
            put(COLUMN_ACTIVITY_DATE, activity.date.time)
        }

        val result = db.insert(TABLE_ACTIVITIES, null, values)
        db.close()

        if (result != -1L) {
            Log.d("DatabaseHelper", "Активность добавлена: ${activity.type}")
        }

        return result
    }

    fun getRecentActivities(limit: Int = 10): List<ActivityItem> {
        val activities = mutableListOf<ActivityItem>()
        val db = this.readableDatabase
        val query = """
            SELECT * FROM $TABLE_ACTIVITIES 
            ORDER BY $COLUMN_ACTIVITY_DATE DESC 
            LIMIT $limit
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val activity = ActivityItem(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTIVITY_ID)),
                    type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTIVITY_TYPE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    amount = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                    time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTIVITY_TIME)),
                    date = Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ACTIVITY_DATE)))
                )
                activities.add(activity)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return activities
    }
    fun getUserById(userId: String): User? {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USER_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(userId))

        return if (cursor.moveToFirst()) {
            val user = User(
                id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                businessName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BUSINESS_NAME)),
                currency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)),
                taxRate = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TAX_RATE))
            )
            cursor.close()
            db.close()
            user
        } else {
            cursor.close()
            db.close()
            null
        }
    }

    fun getTodaySalesCount(): Int {
        val db = this.readableDatabase
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        val query = """
        SELECT COUNT(*) FROM $TABLE_SALES 
        WHERE $COLUMN_SALE_DATE >= ? AND $COLUMN_SALE_DATE < ?
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(startOfDay.toString(), endOfDay.toString()))
        cursor.moveToFirst()
        val count = cursor.getInt(0)

        cursor.close()
        db.close()
        return count
    }

    fun getTodayRevenue(): Double {
        val db = this.readableDatabase
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        val query = """
        SELECT COALESCE(SUM($COLUMN_TOTAL_AMOUNT), 0) FROM $TABLE_SALES 
        WHERE $COLUMN_SALE_DATE >= ? AND $COLUMN_SALE_DATE < ?
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(startOfDay.toString(), endOfDay.toString()))
        cursor.moveToFirst()
        val revenue = cursor.getDouble(0)

        cursor.close()
        db.close()
        return revenue
    }

    fun getProductsCount(): Int {
        val db = this.readableDatabase
        val query = "SELECT COUNT(*) FROM $TABLE_PRODUCTS WHERE $COLUMN_IS_INGREDIENT = 0"
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)

        cursor.close()
        db.close()
        return count
    }
    fun addSale(sale: Sale): Long {
        val db = this.writableDatabase
        db.beginTransaction()

        try {
            // 1. Добавляем запись о продаже
            val saleValues = ContentValues().apply {
                put(COLUMN_SALE_ID, sale.id)
                put(COLUMN_SALE_DATE, sale.date.time)
                put(COLUMN_TOTAL_AMOUNT, sale.totalAmount)
                put(COLUMN_PAYMENT_METHOD, sale.paymentMethod)
                put(COLUMN_USER_ID_FK, sale.userId)
            }

            val saleResult = db.insert(TABLE_SALES, null, saleValues)
            if (saleResult == -1L) {
                throw Exception("Ошибка сохранения продажи")
            }

            // 2. Добавляем позиции продажи
            for (item in sale.items) {
                val itemValues = ContentValues().apply {
                    put(COLUMN_SALE_ID_FK, sale.id)
                    put(COLUMN_PRODUCT_ID, item.productId)
                    put(COLUMN_QUANTITY, item.quantity)
                    put(COLUMN_SUBTOTAL, item.total)
                }

                val itemResult = db.insert(TABLE_SALE_ITEMS, null, itemValues)
                if (itemResult == -1L) {
                    throw Exception("Ошибка сохранения позиции продажи: ${item.productName}")
                }

                // 3. Обновляем остаток товара
                val updateStockSql = """
                UPDATE $TABLE_PRODUCTS 
                SET $COLUMN_STOCK = $COLUMN_STOCK - ? 
                WHERE $COLUMN_PRODUCT_ID = ? AND $COLUMN_STOCK >= ?
            """.trimIndent()

                val args = arrayOf(item.quantity.toString(), item.productId, item.quantity.toString())
                db.execSQL(updateStockSql, args)

                // Проверяем, что остаток обновился
                val checkSql = "SELECT $COLUMN_STOCK FROM $TABLE_PRODUCTS WHERE $COLUMN_PRODUCT_ID = ?"
                val cursor = db.rawQuery(checkSql, arrayOf(item.productId))
                cursor.moveToFirst()
                val newStock = cursor.getInt(0)
                cursor.close()

                if (newStock < 0) {
                    throw Exception("Недостаточно товара: ${item.productName}")
                }
            }

            db.setTransactionSuccessful()
            return saleResult

        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Ошибка при добавлении продажи: ${e.message}")
            throw e
        } finally {
            db.endTransaction()
        }
    }

    fun getAllIngredients(): List<Product> {
        val ingredients = mutableListOf<Product>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_PRODUCTS WHERE $COLUMN_IS_INGREDIENT = 1 ORDER BY $COLUMN_NAME"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val product = Product(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)),
                    stock = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_STOCK)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    barcode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARCODE)),
                    costPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_COST_PRICE)),
                    isIngredient = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_INGREDIENT)) == 1,
                    unit = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT))
                )
                ingredients.add(product)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return ingredients
    }

    fun updateProduct(product: Product): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, product.name)
            put(COLUMN_PRICE, product.price)
            put(COLUMN_COST_PRICE, product.costPrice)
            put(COLUMN_STOCK, product.stock)
            put(COLUMN_CATEGORY, product.category)
            put(COLUMN_BARCODE, product.barcode)
            put(COLUMN_IS_INGREDIENT, if (product.isIngredient) 1 else 0)
            put(COLUMN_UNIT, product.unit)
        }

        val result = db.update(
            TABLE_PRODUCTS,
            values,
            "$COLUMN_PRODUCT_ID = ?",
            arrayOf(product.id)
        )

        db.close()
        return result > 0
    }
    // Метод для расчета себестоимости готового продукта
    fun calculateProductCost(productId: String): Double {
        val recipes = getRecipesForProduct(productId)
        var totalCost = 0.0

        for (recipe in recipes) {
            val ingredient = getProductById(recipe.ingredientId)
            if (ingredient != null) {
                totalCost += recipe.quantityNeeded * ingredient.costPrice
            }
        }

        return totalCost
    }

    // Метод для проверки и обновления себестоимости
    fun updateProductCostWithCheck(productId: String, newCostPrice: Double): Boolean {
        val product = getProductById(productId) ?: return false

        // Проверяем, если новая себестоимость сильно отличается от текущей
        val difference = if (product.costPrice > 0) {
            Math.abs((newCostPrice - product.costPrice) / product.costPrice) * 100
        } else {
            100.0 // Если текущая себестоимость 0, любая новая - это 100% изменение
        }

        // Если разница больше 20%, предлагаем создать новый продукт
        if (difference > 20.0) {
            Log.w("DatabaseHelper", "Себестоимость изменилась на ${"%.1f".format(difference)}%")
            return false // Возвращаем false, чтобы вызывающий код мог предложить создать новый продукт
        }

        // Обновляем себестоимость
        val updatedProduct = product.copy(costPrice = newCostPrice)
        return updateProduct(updatedProduct)
    }

    private fun getSaleItems(saleId: String): List<SaleItem> {
        val items = mutableListOf<SaleItem>()
        val db = this.readableDatabase
        val query = """
        SELECT si.*, p.$COLUMN_NAME, p.$COLUMN_PRICE 
        FROM $TABLE_SALE_ITEMS si
        JOIN $TABLE_PRODUCTS p ON si.$COLUMN_PRODUCT_ID = p.$COLUMN_PRODUCT_ID
        WHERE si.$COLUMN_SALE_ID_FK = ?
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(saleId))

        if (cursor.moveToFirst()) {
            do {
                val item = SaleItem(
                    productId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_ID)),
                    productName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)),
                    quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY)),
                    total = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SUBTOTAL))
                )
                items.add(item)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return items
    }
    // Добавление поставщика
    fun addSupplier(supplier: Supplier): Long {
        val db = this.writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_SUPPLIER_ID, supplier.id)
            put(COLUMN_SUPPLIER_NAME, supplier.name)
            put(COLUMN_CONTACT_PERSON, supplier.contactPerson)
            put(COLUMN_PHONE, supplier.phone)
            put(COLUMN_EMAIL_SUPPLIER, supplier.email)
        }

        val result = db.insert(TABLE_SUPPLIERS, null, values)
        db.close()

        if (result != -1L) {
            Log.d("DatabaseHelper", "Поставщик добавлен: ${supplier.name}")
        }

        return result
    }
    // Получение всех поставщиков
    fun getAllSuppliers(): List<Supplier> {
        val suppliers = mutableListOf<Supplier>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_SUPPLIERS ORDER BY $COLUMN_SUPPLIER_NAME"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val supplier = Supplier(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUPPLIER_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUPPLIER_NAME)),
                    contactPerson = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_PERSON)),
                    phone = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                    email = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COLUMN_EMAIL_SUPPLIER))
                )
                suppliers.add(supplier)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return suppliers
    }
    // Получение поставщика по ID
    fun getSupplierById(supplierId: String): Supplier? {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_SUPPLIERS WHERE $COLUMN_SUPPLIER_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(supplierId))

        return if (cursor.moveToFirst()) {
            val supplier = Supplier(
                id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUPPLIER_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUPPLIER_NAME)),
                contactPerson = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_PERSON)),
                phone = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                email = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COLUMN_EMAIL_SUPPLIER))
            )
            cursor.close()
            db.close()
            supplier
        } else {
            cursor.close()
            db.close()
            null
        }
    }
    // Вспомогательное расширение для безопасного получения строк
    private fun Cursor.getStringOrNull(columnIndex: Int): String? {
        return if (isNull(columnIndex)) null else getString(columnIndex)
    }

    private fun getIncomeItems(incomeId: String): List<IncomeItem> {
        val items = mutableListOf<IncomeItem>()
        val db = this.readableDatabase
        val query = """
        SELECT ii.*, p.$COLUMN_NAME, p.$COLUMN_UNIT 
        FROM $TABLE_INCOME_ITEMS ii
        JOIN $TABLE_PRODUCTS p ON ii.$COLUMN_PRODUCT_ID = p.$COLUMN_PRODUCT_ID
        WHERE ii.$COLUMN_INCOME_ID_FK = ?
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(incomeId))

        if (cursor.moveToFirst()) {
            do {
                val item = IncomeItem(
                    productId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_ID)),
                    productName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    costPerUnit = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_COST_PER_UNIT)),
                    quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY)),
                    total = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SUBTOTAL)),
                    unit = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT))
                )
                items.add(item)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return items
    }
    // Получение поставок за период
    fun getIncomeRecordsByDateRange(startDate: Long, endDate: Long): List<IncomeRecord> {
        val incomeRecords = mutableListOf<IncomeRecord>()
        val db = this.readableDatabase
        val query = """
        SELECT ir.*, s.$COLUMN_SUPPLIER_NAME 
        FROM $TABLE_INCOME_RECORDS ir
        LEFT JOIN $TABLE_SUPPLIERS s ON ir.$COLUMN_SUPPLIER_ID_FK = s.$COLUMN_SUPPLIER_ID
        WHERE ir.$COLUMN_INCOME_DATE >= ? AND ir.$COLUMN_INCOME_DATE <= ?
        ORDER BY ir.$COLUMN_INCOME_DATE DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(startDate.toString(), endDate.toString()))

        if (cursor.moveToFirst()) {
            do {
                val incomeId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INCOME_ID))
                val items = getIncomeItems(incomeId)

                val incomeRecord = IncomeRecord(
                    id = incomeId,
                    date = Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_INCOME_DATE))),
                    supplierId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUPPLIER_ID_FK)),
                    invoiceNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INVOICE_NUMBER)),
                    items = items,
                    totalCost = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_COST))
                )
                incomeRecords.add(incomeRecord)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return incomeRecords
    }
    // Получение продаж за период
    fun getSalesByDateRange(startDate: Long, endDate: Long): List<Sale> {
        val sales = mutableListOf<Sale>()
        val db = this.readableDatabase
        val query = """
        SELECT * FROM $TABLE_SALES 
        WHERE $COLUMN_SALE_DATE >= ? AND $COLUMN_SALE_DATE <= ?
        ORDER BY $COLUMN_SALE_DATE DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(startDate.toString(), endDate.toString()))

        if (cursor.moveToFirst()) {
            do {
                val saleId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SALE_ID))
                val saleItems = getSaleItems(saleId)

                val sale = Sale(
                    id = saleId,
                    date = Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SALE_DATE))),
                    items = saleItems,
                    totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_AMOUNT)),
                    paymentMethod = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAYMENT_METHOD)),
                    userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID_FK))
                )
                sales.add(sale)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return sales
    }

    // Обновление пользователя
    fun updateUser(user: User): Boolean {
        val db = this.writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_PASSWORD, user.password)
            put(COLUMN_BUSINESS_NAME, user.businessName)
            put(COLUMN_CURRENCY, user.currency)
            put(COLUMN_TAX_RATE, user.taxRate)
        }

        val result = db.update(
            TABLE_USERS,
            values,
            "$COLUMN_USER_ID = ?",
            arrayOf(user.id)
        )

        db.close()
        return result > 0
    }

    // Получение всех активностей
    fun getAllActivities(): List<ActivityItem> {
        val activities = mutableListOf<ActivityItem>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_ACTIVITIES ORDER BY $COLUMN_ACTIVITY_DATE DESC"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val activity = ActivityItem(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTIVITY_ID)),
                    type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTIVITY_TYPE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    amount = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                    time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTIVITY_TIME)),
                    date = Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ACTIVITY_DATE)))
                )
                activities.add(activity)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return activities
    }

    // Получение всех продаж
    fun getAllSales(): List<Sale> {
        val sales = mutableListOf<Sale>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_SALES ORDER BY $COLUMN_SALE_DATE DESC"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val saleId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SALE_ID))
                val saleItems = getSaleItems(saleId)

                val sale = Sale(
                    id = saleId,
                    date = Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SALE_DATE))),
                    items = saleItems,
                    totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_AMOUNT)),
                    paymentMethod = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAYMENT_METHOD)),
                    userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID_FK))
                )
                sales.add(sale)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return sales
    }

    // Получение всех поставок
    fun getAllIncomeRecords(): List<IncomeRecord> {
        val incomeRecords = mutableListOf<IncomeRecord>()
        val db = this.readableDatabase
        val query = """
        SELECT ir.*, s.$COLUMN_SUPPLIER_NAME 
        FROM $TABLE_INCOME_RECORDS ir
        LEFT JOIN $TABLE_SUPPLIERS s ON ir.$COLUMN_SUPPLIER_ID_FK = s.$COLUMN_SUPPLIER_ID
        ORDER BY ir.$COLUMN_INCOME_DATE DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val incomeId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INCOME_ID))
                val items = getIncomeItems(incomeId)

                val incomeRecord = IncomeRecord(
                    id = incomeId,
                    date = Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_INCOME_DATE))),
                    supplierId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUPPLIER_ID_FK)),
                    invoiceNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INVOICE_NUMBER)),
                    items = items,
                    totalCost = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_COST))
                )
                incomeRecords.add(incomeRecord)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return incomeRecords
    }
    fun addIncomeRecord(incomeRecord: IncomeRecord): Long {
        val db = this.writableDatabase
        db.beginTransaction()

        try {
            // 1. Добавляем запись о поставке
            val incomeValues = ContentValues().apply {
                put(COLUMN_INCOME_ID, incomeRecord.id)
                put(COLUMN_INCOME_DATE, incomeRecord.date.time)
                put(COLUMN_SUPPLIER_ID_FK, incomeRecord.supplierId)
                put(COLUMN_INVOICE_NUMBER, incomeRecord.invoiceNumber)
                put(COLUMN_TOTAL_COST, incomeRecord.totalCost)
            }

            val incomeResult = db.insert(TABLE_INCOME_RECORDS, null, incomeValues)
            if (incomeResult == -1L) {
                throw Exception("Ошибка сохранения поставки")
            }

            // 2. Добавляем позиции поставки
            for (item in incomeRecord.items) {
                val itemValues = ContentValues().apply {
                    put(COLUMN_INCOME_ID_FK, incomeRecord.id)
                    put(COLUMN_PRODUCT_ID, item.productId)
                    put(COLUMN_QUANTITY, item.quantity)
                    put(COLUMN_COST_PER_UNIT, item.costPerUnit)
                    put(COLUMN_SUBTOTAL, item.total)
                }

                val itemResult = db.insert(TABLE_INCOME_ITEMS, null, itemValues)
                if (itemResult == -1L) {
                    throw Exception("Ошибка сохранения позиции поставки: ${item.productName}")
                }

                // 3. Обновляем остаток товара
                val updateStockSql = """
                UPDATE $TABLE_PRODUCTS 
                SET $COLUMN_STOCK = $COLUMN_STOCK + ? 
                WHERE $COLUMN_PRODUCT_ID = ?
            """.trimIndent()

                db.execSQL(updateStockSql, arrayOf(item.quantity.toString(), item.productId))
            }

            db.setTransactionSuccessful()
            return incomeResult

        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Ошибка при добавлении поставки: ${e.message}")
            throw e
        } finally {
            db.endTransaction()
        }
    }
    // Добавление рецепта
    fun addRecipe(recipe: Recipe): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PRODUCT_ID_FK, recipe.productId)
            put(COLUMN_INGREDIENT_ID_FK, recipe.ingredientId)
            put(COLUMN_QUANTITY_NEEDED, recipe.quantityNeeded)
        }

        // Проверяем, не существует ли уже такой комбинации
        val checkQuery = "SELECT * FROM $TABLE_RECIPES WHERE $COLUMN_PRODUCT_ID_FK = ? AND $COLUMN_INGREDIENT_ID_FK = ?"
        val cursor = db.rawQuery(checkQuery, arrayOf(recipe.productId, recipe.ingredientId))
        val exists = cursor.moveToFirst()
        cursor.close()

        if (exists) {
            Log.e("DatabaseHelper", "Рецепт уже существует для этого товара и ингредиента")
            db.close()
            return -1L
        }

        val result = db.insert(TABLE_RECIPES, null, values)
        db.close()

        if (result != -1L) {
            Log.d("DatabaseHelper", "Рецепт добавлен: ${recipe.productName} -> ${recipe.ingredientName}")
        } else {
            Log.e("DatabaseHelper", "Ошибка добавления рецепта")
        }

        return result
    }

    // Получение рецептов для товара
    fun getRecipesForProduct(productId: String): List<Recipe> {
        val recipes = mutableListOf<Recipe>()
        val db = this.readableDatabase
        val query = """
        SELECT r.*, p.$COLUMN_NAME as product_name, i.$COLUMN_NAME as ingredient_name,
               i.$COLUMN_UNIT as ingredient_unit, i.$COLUMN_STOCK as ingredient_stock
        FROM $TABLE_RECIPES r
        JOIN $TABLE_PRODUCTS p ON r.$COLUMN_PRODUCT_ID_FK = p.$COLUMN_PRODUCT_ID
        JOIN $TABLE_PRODUCTS i ON r.$COLUMN_INGREDIENT_ID_FK = i.$COLUMN_PRODUCT_ID
        WHERE r.$COLUMN_PRODUCT_ID_FK = ?
        ORDER BY i.$COLUMN_NAME
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(productId))

        if (cursor.moveToFirst()) {
            do {
                val recipe = Recipe(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECIPE_ID)),
                    productId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_ID_FK)),
                    ingredientId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INGREDIENT_ID_FK)),
                    quantityNeeded = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY_NEEDED)),
                    productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name")),
                    ingredientName = cursor.getString(cursor.getColumnIndexOrThrow("ingredient_name")),
                    ingredientUnit = cursor.getString(cursor.getColumnIndexOrThrow("ingredient_unit")),
                    ingredientStock = cursor.getDouble(cursor.getColumnIndexOrThrow("ingredient_stock"))
                )
                recipes.add(recipe)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return recipes
    }
    // Удаление рецепта
    fun deleteRecipe(recipeId: String): Boolean {
        val db = this.writableDatabase
        val result = db.delete(
            TABLE_RECIPES,
            "$COLUMN_RECIPE_ID = ?",
            arrayOf(recipeId)
        )
        db.close()

        return result > 0
    }
    // Получение всех рецептов
    fun getAllRecipes(): List<Recipe> {
        val recipes = mutableListOf<Recipe>()
        val db = this.readableDatabase
        val query = """
        SELECT r.*, p.$COLUMN_NAME as product_name, i.$COLUMN_NAME as ingredient_name,
               i.$COLUMN_UNIT as ingredient_unit, p.$COLUMN_UNIT as product_unit
        FROM $TABLE_RECIPES r
        JOIN $TABLE_PRODUCTS p ON r.$COLUMN_PRODUCT_ID_FK = p.$COLUMN_PRODUCT_ID
        JOIN $TABLE_PRODUCTS i ON r.$COLUMN_INGREDIENT_ID_FK = i.$COLUMN_PRODUCT_ID
        ORDER BY p.$COLUMN_NAME, i.$COLUMN_NAME
    """.trimIndent()

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val recipe = Recipe(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECIPE_ID)),
                    productId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_ID_FK)),
                    ingredientId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INGREDIENT_ID_FK)),
                    quantityNeeded = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY_NEEDED)),
                    productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name")),
                    ingredientName = cursor.getString(cursor.getColumnIndexOrThrow("ingredient_name")),
                    productUnit = cursor.getString(cursor.getColumnIndexOrThrow("product_unit")),
                    ingredientUnit = cursor.getString(cursor.getColumnIndexOrThrow("ingredient_unit"))
                )
                recipes.add(recipe)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return recipes
    }
    // Обновление рецепта
    fun updateRecipe(recipe: Recipe): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_QUANTITY_NEEDED, recipe.quantityNeeded)
        }

        val result = db.update(
            TABLE_RECIPES,
            values,
            "$COLUMN_RECIPE_ID = ?",
            arrayOf(recipe.id)
        )

        db.close()
        return result > 0
    }
    // Проверка возможности приготовления товара
    fun canProduceProduct(productId: String, quantity: Int): Boolean {
        val recipes = getRecipesForProduct(productId)
        val db = this.readableDatabase

        for (recipe in recipes) {
            val query = "SELECT $COLUMN_STOCK FROM $TABLE_PRODUCTS WHERE $COLUMN_PRODUCT_ID = ?"
            val cursor = db.rawQuery(query, arrayOf(recipe.ingredientId))
            cursor.moveToFirst()
            val stock = cursor.getDouble(0)
            cursor.close()

            val needed = recipe.quantityNeeded * quantity
            if (stock < needed) {
                return false
            }
        }

        db.close()
        return true
    }

    // Приготовление товара (списание ингредиентов)
    fun produceProduct(productId: String, quantity: Int): Boolean {
        val recipes = getRecipesForProduct(productId)
        val db = this.writableDatabase
        db.beginTransaction()

        try {
            for (recipe in recipes) {
                val needed = recipe.quantityNeeded * quantity
                val updateSql = """
                UPDATE $TABLE_PRODUCTS 
                SET $COLUMN_STOCK = $COLUMN_STOCK - ? 
                WHERE $COLUMN_PRODUCT_ID = ? AND $COLUMN_STOCK >= ?
            """.trimIndent()

                db.execSQL(updateSql, arrayOf(needed.toString(), recipe.ingredientId, needed.toString()))
            }

            // Увеличиваем остаток готового товара
            val updateProductSql = """
            UPDATE $TABLE_PRODUCTS 
            SET $COLUMN_STOCK = $COLUMN_STOCK + ? 
            WHERE $COLUMN_PRODUCT_ID = ?
        """.trimIndent()

            db.execSQL(updateProductSql, arrayOf(quantity.toString(), productId))

            db.setTransactionSuccessful()
            return true

        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Ошибка приготовления товара: ${e.message}")
            return false
        } finally {
            db.endTransaction()
            db.close()
        }
    }
    // Метод для добавления единицы
    fun addUnit(unit: UnitDefinition): Long {
        val db = this.writableDatabase
        var result = -1L

        try {
            val values = ContentValues().apply {
                put(COLUMN_UNIT_ID, unit.id)
                put(COLUMN_UNIT_NAME, unit.name)
                put(COLUMN_UNIT_CATEGORY, unit.category)
                put(COLUMN_BASE_UNIT, unit.baseUnit)
                put(COLUMN_CONVERSION_RATE, unit.conversionRate)
            }

            result = db.insert(TABLE_UNITS, null, values)
            Log.d("DatabaseHelper", "Единица добавлена: ${unit.name}")

        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Ошибка добавления единицы: ${e.message}")
        } finally {
            db.close()
        }

        return result
    }

    // Метод для получения всех единиц измерения
    fun getAllUnits(): List<UnitDefinition> {
        val units = mutableListOf<UnitDefinition>()
        val db = this.readableDatabase

        try {
            val query = "SELECT * FROM $TABLE_UNITS ORDER BY $COLUMN_UNIT_CATEGORY, $COLUMN_UNIT_NAME"
            val cursor = db.rawQuery(query, null)

            if (cursor.moveToFirst()) {
                do {
                    val unit = UnitDefinition(
                        id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT_NAME)),
                        category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT_CATEGORY)),
                        baseUnit = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BASE_UNIT)),
                        conversionRate = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_CONVERSION_RATE))
                    )
                    units.add(unit)
                } while (cursor.moveToNext())
            }

            cursor.close()
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Ошибка получения единиц: ${e.message}")
        } finally {
            db.close()
        }

        return units
    }

    // Метод для получения единицы по имени
    fun getUnitByName(name: String): UnitDefinition? {
        val db = this.readableDatabase
        var unit: UnitDefinition? = null

        try {
            val query = "SELECT * FROM $TABLE_UNITS WHERE $COLUMN_UNIT_NAME = ?"
            val cursor = db.rawQuery(query, arrayOf(name))

            if (cursor.moveToFirst()) {
                unit = UnitDefinition(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT_NAME)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT_CATEGORY)),
                    baseUnit = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BASE_UNIT)),
                    conversionRate = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_CONVERSION_RATE))
                )
            }

            cursor.close()
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Ошибка получения единицы по имени: ${e.message}")
        } finally {
            db.close()
        }

        return unit
    }

    fun updateUnit(unit: UnitDefinition): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_UNIT_NAME, unit.name)
            put(COLUMN_UNIT_CATEGORY, unit.category)
            put(COLUMN_BASE_UNIT, unit.baseUnit)
            put(COLUMN_CONVERSION_RATE, unit.conversionRate)
        }

        val result = db.update(
            TABLE_UNITS,
            values,
            "$COLUMN_UNIT_ID = ?",
            arrayOf(unit.id)
        )

        db.close()
        return result > 0
    }

    fun deleteUnit(unitId: String): Boolean {
        val db = this.writableDatabase

        // Проверяем, используется ли единица в товарах
        val checkQuery = "SELECT COUNT(*) FROM $TABLE_PRODUCTS WHERE $COLUMN_UNIT = ?"
        val cursor = db.rawQuery(checkQuery, arrayOf(
            getUnitById(unitId)?.name ?: ""
        ))
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()

        if (count > 0) {
            db.close()
            return false // нельзя удалить, если используется в товарах
        }

        val result = db.delete(
            TABLE_UNITS,
            "$COLUMN_UNIT_ID = ?",
            arrayOf(unitId)
        )

        db.close()
        return result > 0
    }

    fun getUnitById(unitId: String): UnitDefinition? {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_UNITS WHERE $COLUMN_UNIT_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(unitId))

        return if (cursor.moveToFirst()) {
            val unit = UnitDefinition(
                id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT_NAME)),
                category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT_CATEGORY)),
                baseUnit = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BASE_UNIT)),
                conversionRate = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_CONVERSION_RATE))
            )
            cursor.close()
            db.close()
            unit
        } else {
            cursor.close()
            db.close()
            null
        }
    }
    fun updateProductCostForce(productId: String, newCostPrice: Double): Boolean {
        val product = getProductById(productId) ?: return false

        // Обновляем себестоимость без проверок
        val updatedProduct = product.copy(costPrice = newCostPrice)
        return updateProduct(updatedProduct)
    }

}



