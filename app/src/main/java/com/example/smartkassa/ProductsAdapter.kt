package com.example.smartkassa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ProductsAdapter(
    private var products: List<Product>,
    private val onProductClick: (Product) -> Unit,
    private val onProductLongClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvStock: TextView = view.findViewById(R.id.tvStock)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvUnit: TextView = view.findViewById(R.id.tvUnit)
        val cardView: MaterialCardView = view.findViewById(R.id.cardView)
        val ivIngredientIcon: android.widget.ImageView = view.findViewById(R.id.ivIngredientIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        holder.tvProductName.text = product.name
        holder.tvPrice.text = if (product.isIngredient) "Ингредиент" else "%.2f ₽".format(product.price)
        holder.tvStock.text = "${product.stock} ${product.unit}"
        holder.tvCategory.text = product.category
        holder.tvUnit.text = product.unit

        // Показываем иконку для ингредиентов
        if (product.isIngredient) {
            holder.ivIngredientIcon.visibility = View.VISIBLE
            holder.tvPrice.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.gray_dark))
        } else {
            holder.ivIngredientIcon.visibility = View.GONE
            holder.tvPrice.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.primary_color))
        }

        // Цвет остатка в зависимости от количества
        val context = holder.itemView.context
        when {
            product.stock == 0.0 -> {
                holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.accent_color))
                holder.tvStock.text = "НЕТ В НАЛИЧИИ"
            }
            product.stock < 5 -> {
                holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.warning_orange))
                holder.tvStock.text = "Мало: ${product.stock} ${product.unit}"
            }
            else -> {
                holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.success_green))
            }
        }

        // Проверка на невыгодность товара (только для товаров, не для ингредиентов)
        if (!product.isIngredient && product.price > 0 && product.costPrice > 0) {
            val margin = product.price - product.costPrice
            val marginPercent = (margin / product.costPrice * 100)

            // Если маржа отрицательная (убыток) или очень низкая (меньше 10%)
            if (margin < 0) {
                // Красный фон для убыточных товаров
                holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.loss_red_light)
                )
                // Показываем убыток в tvUnit
                holder.tvUnit.text = "УБЫТОК: ${"%.1f".format(Math.abs(marginPercent))}%"
                holder.tvUnit.setTextColor(ContextCompat.getColor(context, R.color.accent_color))

            } else if (marginPercent < 10) {
                // Оранжевый фон для низкорентабельных товаров
                holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.warning_orange_light)
                )
                // Показываем низкую маржу
                holder.tvUnit.text = "МАРЖА: ${"%.1f".format(marginPercent)}%"
                holder.tvUnit.setTextColor(ContextCompat.getColor(context, R.color.warning_orange))

            } else {
                // Нормальный фон для рентабельных товаров
                holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface)
                )
                // Для рентабельных показываем просто единицу измерения
                holder.tvUnit.text = product.unit
                holder.tvUnit.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        } else {
            // Сброс цвета для ингредиентов или товаров без себестоимости
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.surface)
            )
            holder.tvUnit.text = product.unit
            holder.tvUnit.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        // Клик на товар
        holder.cardView.setOnClickListener {
            onProductClick(product)
        }

        // Долгий клик
        holder.cardView.setOnLongClickListener {
            onProductLongClick(product)
            true
        }
    }

    override fun getItemCount() = products.size

    // Обновление данных
    fun updateProducts(newProducts: List<Product>) {
        this.products = newProducts
        notifyDataSetChanged()
    }

}