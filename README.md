# Документация приложения SmartKassa для пекарни

## Обзор

SmartKassa - это полнофункциональное приложение для управления пекарней/кондитерской, включающее:
- Управление товарами и ингредиентами
- Учет продаж и поставок
- Рецепты и производство
- Отчетность и аналитика
- Резервное копирование и экспорт данных

## Архитектура

### Основные компоненты

1. **База данных** (SQLite) - `DatabaseHelper`
2. **Activity (Экраны)** - Основные UI компоненты
3. **Адаптеры** - Для отображения списков
4. **Утилиты** - Вспомогательные классы

# Содержание

- [Обзор](#обзор)
- [Архитектура](#архитектура)
  - [Основные компоненты](#основные-компоненты)
- [1. База данных](#1-база-данных)
  - [DatabaseHelper](#databasehelper)
    - [Таблицы](#таблицы)
    - [Ключевые методы](#ключевые-методы)
- [2. Activity (Экраны)](#2-activity-экраны)
  - [MainActivity](#mainactivity)
  - [LoginActivity](#loginactivity)
  - [ProductsActivity](#productsactivity)
  - [RecipesActivity](#recipesactivity)
  - [SaleActivity](#saleactivity)
  - [IncomeActivity](#incomeactivity)
  - [AllActivitiesActivity](#allactivitiesactivity)
  - [ReportsActivity](#reportsactivity)
  - [SettingsActivity](#settingsactivity)
- [3. Модели данных](#3-модели-данных)
  - [Product](#product)
  - [Sale](#sale)
  - [IncomeRecord](#incomerecord)
  - [Recipe](#recipe)
  - [User](#user)
  - [UnitDefinition](#unitdefinition)
- [4. Адаптеры](#4-адаптеры)
  - [ProductsAdapter](#productsadapter)
  - [ActivityAdapter](#activityadapter)
  - [RecipesAdapter](#recipesadapter)
  - [SaleAdapter / IncomeAdapter](#saleadapter--incomeadapter)
- [5. Утилиты](#5-утилиты)
  - [FileUtils](#fileutils)
  - [IdGenerator](#idgenerator)
  - [TimeUtils](#timeutils)
- [6. Особенности реализации](#6-особенности-реализации)
  - [Безопасность](#безопасность)
  - [Производительность](#производительность)
  - [Масштабируемость](#масштабируемость)
- [7. Расширение функциональности](#7-расширение-функциональности)
- [8. Установка](#8-установка)
  - [Требования](#требования)
  - [Шаги установки](#шаги-установки)
  - [Альтернативная установка](#альтернативная-установка)
- [9. Рекомендации по улучшению](#9-рекомендации-по-улучшению)
  - [Безопасность](#безопасность-1)
  - [Функциональность](#функциональность)
  - [UI/UX](#uiux)
- [10. Структура проекта](#10-структура-проекта)
- [11. Разработка](#11-разработка)
  - [Технологический стек](#технологический-стек)
  - [Сборка и запуск](#сборка-и-запуск)

---

## 1. База данных

### DatabaseHelper
Основной класс для работы с SQLite базой данных.

#### Таблицы:
- `users` - Пользователи системы
- `products` - Товары и ингредиенты
- `recipes` - Рецепты (связи товар-ингредиент)
- `suppliers` - Поставщики
- `sales` - Продажи
- `sale_items` - Позиции продаж
- `income_records` - Поставки
- `income_items` - Позиции поставок
- `activities` - История действий
- `units` - Единицы измерения

#### Ключевые методы:

```kotlin
// Пользователи
addUser(user: User): Long
getUserByEmail(email: String): User?
getUserById(userId: String): User?
updateUser(user: User): Boolean

// Товары
addProduct(product: Product): Long
getProductById(productId: String): Product?
getAllProducts(): List<Product>
searchProducts(query: String): List<Product>
updateProduct(product: Product): Boolean
deleteProduct(productId: String): Boolean

// Продажи
addSale(sale: Sale): Long
getSalesByDateRange(startDate: Long, endDate: Long): List<Sale>
getAllSales(): List<Sale>

// Поставки
addIncomeRecord(incomeRecord: IncomeRecord): Long
getIncomeRecordsByDateRange(startDate: Long, endDate: Long): List<IncomeRecord>
getAllIncomeRecords(): List<IncomeRecord>

// Рецепты
addRecipe(recipe: Recipe): Long
getRecipesForProduct(productId: String): List<Recipe>
getAllRecipes(): List<Recipe>
deleteRecipe(recipeId: String): Boolean
produceProduct(productId: String, quantity: Int): Boolean

// Активности
addActivity(activity: ActivityItem): Long
getRecentActivities(limit: Int = 10): List<ActivityItem>
getAllActivities(): List<ActivityItem>

// Единицы измерения
addUnit(unit: UnitDefinition): Long
getAllUnits(): List<UnitDefinition>
getUnitByName(name: String): UnitDefinition?
updateUnit(unit: UnitDefinition): Boolean
deleteUnit(unitId: String): Boolean
```

---

## 2. Activity (Экраны)

### MainActivity
Главный экран с панелью управления и статистикой.

**Функции:**
- Отображение статистики за день
- Быстрый доступ к основным функциям
- Список последних активностей

### LoginActivity
Экран авторизации.

**Функции:**
- Вход по email/password
- Быстрый гостевой вход
- Создание тестовых данных при первом запуске

### ProductsActivity
Управление товарами и ингредиентами.

**Функции:**
- Просмотр списка товаров/ингредиентов
- Добавление/редактирование товаров
- Фильтрация и сортировка
- Управление категориями
- Массовое редактирование
- Управление единицами измерения

### RecipesActivity
Управление рецептами.

**Функции:**
- Создание рецептов для товаров
- Добавление ингредиентов в рецепты
- Расчет себестоимости
- Приготовление товаров
- Контроль запасов ингредиентов

### SaleActivity
Оформление продаж.

**Функции:**
- Добавление товаров в чек
- Изменение количества
- Завершение продажи
- Формирование чека

### IncomeActivity
Учет поставок товаров.

**Функции:**
- Выбор поставщика
- Добавление товаров в поставку
- Ввод стоимости товаров
- Контроль себестоимости
- Сохранение поставки

### AllActivitiesActivity
Просмотр всех операций.

**Функции:**
- Фильтрация по типу операции
- Сортировка по разным параметрам
- Поиск операций
- Статистика доходов/расходов

### ReportsActivity
Отчеты и аналитика.

**Функции:**
- Статистика продаж за период
- Популярные товары
- Финансовые показатели
- Экспорт отчетов (JSON, Excel)

### SettingsActivity
Настройки и управление данными.

**Функции:**
- Редактирование информации о бизнесе
- Резервное копирование
- Восстановление данных
- Экспорт данных
- Обратная связь
- Выход из аккаунта

---

## 3. Модели данных

### Product
```kotlin
data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val stock: Double = 0.0,
    val category: String = "",
    val barcode: String? = null,
    val costPrice: Double = 0.0,
    val isIngredient: Boolean = false,
    val unit: String = "шт"
)
```

### Sale
```kotlin
data class Sale(
    val id: String,
    val date: Date,
    val items: List<SaleItem>,
    val totalAmount: Double,
    val paymentMethod: String = "Наличные",
    val userId: String
)
```

### IncomeRecord
```kotlin
data class IncomeRecord(
    val id: String,
    val date: Date,
    val supplierId: String,
    val invoiceNumber: String = "",
    val items: List<IncomeItem>,
    val totalCost: Double
)
```

### Recipe
```kotlin
data class Recipe(
    val id: String,
    val productId: String,
    val ingredientId: String,
    val quantityNeeded: Double,
    val productName: String = "",
    val ingredientName: String = "",
    val productUnit: String = "шт",
    val ingredientUnit: String = "кг",
    val ingredientStock: Double = 0.0,
    val ingredientCost: Double = 0.0
)
```

### User
```kotlin
data class User(
    val id: String,
    val email: String,
    val password: String,
    val businessName: String = "Мой бизнес",
    val currency: String = "RUB",
    val taxRate: Double = 20.0
)
```

### UnitDefinition
```kotlin
data class UnitDefinition(
    val id: String = "",
    val name: String,
    val category: String,
    val baseUnit: String,
    val conversionRate: Double = 1.0
)
```

---

## 4. Адаптеры

### ProductsAdapter
Отображение списка товаров.

**Особенности:**
- Цветовая индикация остатков
- Подсветка убыточных товаров
- Отображение маржи

### ActivityAdapter
Отображение истории операций.

**Особенности:**
- Разный цвет для разных типов операций
- Форматирование даты и времени
- Подсветка доходов/расходов

### RecipesAdapter
Отображение рецептов.

**Особенности:**
- Индикация наличия ингредиентов
- Отображение необходимого количества
- Статус доступности

### SaleAdapter / IncomeAdapter
Отображение товаров в чеке/поставке.

**Особенности:**
- Управление количеством
- Расчет итоговой суммы
- Удаление позиций

---

## 5. Утилиты

### FileUtils
Работа с файлами и экспортом.

**Функции:**
- Создание резервных копий
- Восстановление из бэкапа
- Экспорт в различные форматы
- Управление файлами экспорта

```kotlin
saveBackup(context: Context, backupData: BackupData): Pair<Boolean, String>
restoreFromBackup(context: Context, fileUri: Uri): RestoreResult
exportToCSV(context: Context, data: List<Any>, fileName: String, headers: List<String>): Pair<Boolean, String>
exportToJSON(context: Context, data: Map<String, Any>, exportName: String): Pair<Boolean, String>
getBackupFiles(context: Context): List<BackupFileInfo>
```

### IdGenerator
Генерация уникальных ID.

```kotlin
generateId(): String = UUID.randomUUID().toString()
generateSaleId(): String = "SALE_${System.currentTimeMillis()}_${(1000..9999).random()}"
generateIncomeId(): String = "INC_${System.currentTimeMillis()}_${(1000..9999).random()}"
```

### TimeUtils
Работа с датой и временем.

```kotlin
getCurrentTime(): String
getCurrentDate(): String
formatTime(date: Date): String
formatDate(date: Date): String
```

---

## 6. Особенности реализации

### Безопасность
- Пароли хранятся в открытом виде (нужно добавить хэширование)
- Нет шифрования базы данных
- Бэкапы сохраняются в открытом формате JSON

### Производительность
- Использование SQLite для локального хранения
- Оптимизированные запросы к базе данных
- Фоновая обработка длительных операций

### Масштабируемость
- Модульная архитектура
- Легко добавлять новые типы данных
- Поддержка различных единиц измерения

---

## 7. Расширение функциональности

### Для добавления новых функций:

1. **Новый тип отчета:**
   - Создать метод в DatabaseHelper
   - Добавить UI в ReportsActivity
   - Реализовать экспорт в FileUtils

2. **Новый тип операции:**
   - Добавить тип в ActivityItem
   - Расширить ActivityAdapter
   - Обновить AllActivitiesActivity

3. **Интеграция с внешними системами:**
   - Добавить API клиент
   - Создать сервис синхронизации
   - Реализовать обработку ошибок

---

## 8. Установка

### Требования:

- Android Studio 
- Android SDK 21+
- Kotlin 1.8+


### Шаги установки:


1. Клонируйте репозиторий:
```
git clone https://gl.guap.ru/user43301/smartkassa.git
```
2. Откройте проект в Android Studio
3. Дождитесь завершения синхронизации Gradle
4. Соберите проект:
```
./gradlew build
```
5. Запустите на эмуляторе или устройстве

### Альтернативная установка:
```
cd existing_repo
git remote add origin https://gl.guap.ru/user43301/smartkassa.git
git branch -M main
git push -uf origin main
```

---


## 9. Рекомендации по улучшению

### Безопасность:
1. Добавить хэширование паролей
2. Шифрование базы данных
3. Шифрование бэкапов
4. Добавить аутентификацию по отпечатку

### Функциональность:
1. Добавить онлайн-синхронизацию
2. Реализовать облачное резервное копирование
3. Добавить интеграцию с кассовыми аппаратами
4. Реализовать печать чеков

### UI/UX:
1. Добавить темную тему
2. Улучшить анимации
3. Добавить виджеты для главного экрана
4. Реализовать поиск по штрих-коду

---

## 10. Структура проекта

```
com.example.smartkassa/
├── DatabaseHelper.kt
├── models/
│   ├── Product.kt
│   ├── Sale.kt
│   ├── SaleItem.kt
│   ├── IncomeRecord.kt
│   ├── IncomeItem.kt
│   ├── Recipe.kt
│   ├── User.kt
│   ├── Supplier.kt
│   ├── BackupFileInfo.kt
│   ├── ExportFileInfo.kt
│   ├── RestoreResult.kt
│   ├── PopularProduct.kt
│   ├── ProductSales.kt
│   └── UnitDefinition.kt
├── activities/
│   ├── MainActivity.kt
│   ├── LoginActivity.kt
│   ├── ProductsActivity.kt
│   ├── SaleActivity.kt
│   ├── IncomeActivity.kt
│   ├── RecipesActivity.kt
│   ├── AllActivitiesActivity.kt
│   ├── ReportsActivity.kt
│   ├── SettingsActivity.kt
│   └── BackupData.kt
├── adapters/
│   ├── ProductsAdapter.kt
│   ├── BulkEditProductsAdapter.kt
│   ├── SearchProductAdapter.kt
│   ├── SearchProductForIncomeAdapter.kt
│   ├── SaleAdapter.kt
│   ├── ActivityAdapter.kt
│   ├── ActivityItem.kt
│   ├── IncomeAdapter.kt
│   ├── CategoriesAdapter.kt
│   ├── RecipesAdapter.kt
│   ├── PopularProductsAdapter.kt
│   └── UnitsAdapter.kt
├── utils/
│   ├── FileUtils.kt
│   ├── IdGenerator.kt
│   └── TimeUtils.kt
└── di/
    └── AppModule.kt
```


---
## 11. Разработка

#### Технологический стек:

- Язык: Kotlin
- Минимальная версия: Android 10 (API 29)
- Архитектура: MVVM
- UI: Material Design 3


#### Сборка и запуск:

1. Откройте проект в Android Studio
2. Выберите целевое устройство
3. Запустите с помощью Shift + F10

---

УмнаяКасса v1.0.0
