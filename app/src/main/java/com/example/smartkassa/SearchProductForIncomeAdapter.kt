package com.example.smartkassa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class SearchProductForIncomeAdapter(
    private var products: List<Product>,
    private val onProductSelected: (Product) -> Unit
) : RecyclerView.Adapter<SearchProductForIncomeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardView)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvStock: TextView = view.findViewById(R.id.tvStock)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvUnit: TextView = view.findViewById(R.id.tvUnit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        holder.tvProductName.text = product.name
        holder.tvPrice.text = if (product.isIngredient) "Ингредиент" else "Товар"
        holder.tvStock.text = "На складе: ${product.stock} ${product.unit}"
        holder.tvCategory.text = product.category
        holder.tvUnit.text = product.unit

        // Подсветка типа товара
        val context = holder.itemView.context
        if (product.isIngredient) {
            holder.tvCategory.setTextColor(ContextCompat.getColor(context, R.color.warning_orange))
            holder.tvCategory.text = "⚗ ${product.category}"
        } else {
            holder.tvCategory.setTextColor(ContextCompat.getColor(context, R.color.primary_color))
        }

        holder.cardView.setOnClickListener {
            onProductSelected(product)
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        this.products = newProducts
        notifyDataSetChanged()
    }
}