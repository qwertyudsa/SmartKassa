package com.example.smartkassa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Адаптер для популярных товаров
class PopularProductsAdapter(
    private val products: List<PopularProduct>
) : RecyclerView.Adapter<PopularProductsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvSalesCount: TextView = view.findViewById(R.id.tvSalesCount)
        val tvRevenue: TextView = view.findViewById(R.id.tvRevenue)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_popular_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        // Находим максимальную выручку для расчета прогресса
        val maxRevenue = products.maxOfOrNull { it.revenue } ?: 1.0
        val progress = (product.revenue / maxRevenue * 100).toInt()

        holder.tvProductName.text = product.name
        holder.tvSalesCount.text = "${product.salesCount} продаж"
        holder.tvRevenue.text = "%.0f ₽".format(product.revenue)
        holder.progressBar.progress = progress
    }

    override fun getItemCount() = products.size
}