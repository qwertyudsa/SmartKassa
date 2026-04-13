package com.example.smartkassa

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты для экрана товаров
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProductsActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ProductsActivity::class.java)

    // ТЕСТ 1: Проверка, что экран товаров отображается
    @Test
    fun testProductsActivityIsDisplayed() {
        // Проверяем заголовок
        onView(withText("Товары")).check(matches(isDisplayed()))

        // Проверяем поле поиска
        onView(withId(R.id.etSearch)).check(matches(isDisplayed()))

        // Проверяем кнопки фильтра и сортировки
        onView(withId(R.id.btnFilter)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSort)).check(matches(isDisplayed()))

        // Проверяем кнопку добавления товара
        onView(withId(R.id.fabAddProduct)).check(matches(isDisplayed()))
    }

    // ТЕСТ 2: Проверка поиска товаров
    @Test
    fun testSearchProducts() {
        // Вводим текст в поле поиска
        onView(withId(R.id.etSearch)).perform(typeText("хлеб"), closeSoftKeyboard())

        // Проверяем, что поиск выполнился (просто проверяем, что нет краша)
        // Реальный результат зависит от наличия товаров в БД
    }

    // ТЕСТ 3: Проверка открытия диалога добавления товара
    @Test
    fun testOpenAddProductDialog() {
        // Нажимаем на кнопку добавления
        onView(withId(R.id.fabAddProduct)).perform(click())

        // Проверяем, что диалог открылся
        onView(withText("Добавление товара")).check(matches(isDisplayed()))

        // Закрываем диалог
        onView(withText("Отмена")).perform(click())
    }

    // ТЕСТ 4: Проверка открытия диалога фильтрации
    @Test
    fun testOpenFilterDialog() {
        // Нажимаем на кнопку фильтра
        onView(withId(R.id.btnFilter)).perform(click())

        // Проверяем, что диалог открылся
        onView(withText("Фильтр товаров")).check(matches(isDisplayed()))

        // Закрываем диалог
        onView(withText("Отмена")).perform(click())
    }

    // ТЕСТ 5: Проверка открытия диалога сортировки
    @Test
    fun testOpenSortDialog() {
        // Нажимаем на кнопку сортировки
        onView(withId(R.id.btnSort)).perform(click())

        // Проверяем, что диалог открылся
        onView(withText("Сортировка товаров")).check(matches(isDisplayed()))

        // Закрываем диалог
        onView(withText("Отмена")).perform(click())
    }
}