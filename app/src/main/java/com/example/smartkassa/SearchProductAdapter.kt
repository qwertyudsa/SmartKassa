package com.example.smartkassa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class SearchProductAdapter(
    private var products: List<Product>,
    private val onProductSelected: (Product) -> Unit
) : RecyclerView.Adapter<SearchProductAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view.findViewById(R.id.cardView)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvStock: TextView = view.findViewById(R.id.tvStock)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        holder.tvProductName.text = product.name
        holder.tvPrice.text = if (product.isIngredient) "Ингредиент" else "%.2f ₽".format(product.price)
        holder.tvStock.text = "Остаток: ${product.stock} ${product.unit}"
        holder.tvCategory.text = product.category

        // Подсветка товаров с низким запасом
        val context = holder.itemView.context
        when {
            product.stock == 0.0 -> {
                holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.accent_color))
                holder.cardView.alpha = 0.5f
            }
            product.stock < 5 -> {
                holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.warning_orange))
            }
            else -> {
                holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.success_green))
            }
        }

        // Не позволяем добавлять ингредиенты в продажу
        holder.cardView.setOnClickListener {
            if (product.isIngredient) {
                Toast.makeText(
                    holder.itemView.context,
                    "Ингредиенты не продаются",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (product.stock > 0) {
                onProductSelected(product)
            } else {
                Toast.makeText(
                    holder.itemView.context,
                    "Товара нет в наличии",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        this.products = newProducts
        notifyDataSetChanged()
    }
}