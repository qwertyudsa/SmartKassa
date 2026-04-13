package com.example.smartkassa

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты для главного экрана приложения SmartKassa
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    // Правило для запуска MainActivity перед каждым тестом
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // ТЕСТ 1: Проверка, что главный экран отображается
    @Test
    fun testMainActivityIsDisplayed() {
        // Проверяем, что тулбар с названием бизнеса отображается
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()))

        // Проверяем, что карточка "Продажа" отображается
        onView(withId(R.id.cardSale)).check(matches(isDisplayed()))

        // Проверяем, что карточка "Поступление" отображается
        onView(withId(R.id.cardIncome)).check(matches(isDisplayed()))

        // Проверяем, что карточка "Товары" отображается
        onView(withId(R.id.cardProducts)).check(matches(isDisplayed()))

        // Проверяем, что карточка "Рецепты" отображается
        onView(withId(R.id.cardRecipes)).check(matches(isDisplayed()))

        // Проверяем, что статистика отображается
        onView(withId(R.id.tvTodaySales)).check(matches(isDisplayed()))
        onView(withId(R.id.tvTodayRevenue)).check(matches(isDisplayed()))
        onView(withId(R.id.tvProductsCount)).check(matches(isDisplayed()))
    }

    // ТЕСТ 2: Проверка перехода на экран товаров
    @Test
    fun testNavigateToProductsActivity() {
        // Нажимаем на карточку "Товары"
        onView(withId(R.id.cardProducts)).perform(click())

        // Проверяем, что открылся экран товаров (проверяем заголовок)
        onView(withText("Товары")).check(matches(isDisplayed()))
    }

    // ТЕСТ 3: Проверка перехода на экран продажи
    @Test
    fun testNavigateToSaleActivity() {
        // Нажимаем на карточку "Продажа"
        onView(withId(R.id.cardSale)).perform(click())

        // Проверяем, что открылся экран продажи
        onView(withText("Новая продажа")).check(matches(isDisplayed()))
    }

    // ТЕСТ 4: Проверка перехода на экран поступления
    @Test
    fun testNavigateToIncomeActivity() {
        // Нажимаем на карточку "Поступление"
        onView(withId(R.id.cardIncome)).perform(click())

        // Проверяем, что открылся экран поступления
        onView(withText("Поступление товаров")).check(matches(isDisplayed()))
    }

    // ТЕСТ 5: Проверка перехода на экран рецептов
    @Test
    fun testNavigateToRecipesActivity() {
        // Нажимаем на карточку "Рецепты"
        onView(withId(R.id.cardRecipes)).perform(click())

        // Проверяем, что открылся экран рецептов
        onView(withText("Рецепты")).check(matches(isDisplayed()))
    }

    // ТЕСТ 6: Проверка кнопки "Все операции"
    @Test
    fun testViewAllActivitiesButton() {
        // Нажимаем на кнопку "Все операции"
        onView(withId(R.id.btnViewAll)).perform(click())

        // Проверяем, что открылся экран всех операций
        onView(withText("Все операции")).check(matches(isDisplayed()))
    }
}