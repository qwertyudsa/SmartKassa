package com.example.smartkassa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BulkEditProductsAdapter(
    private val products: List<Product>,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<BulkEditProductsAdapter.ViewHolder>() {

    private val selectedProducts = mutableSetOf<String>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvProductInfo: TextView = view.findViewById(R.id.tvProductInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bulk_edit_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        holder.tvProductName.text = product.name
        holder.tvProductInfo.text = "${product.category} | ${product.stock} ${product.unit} | ${String.format("%.2f", product.price)} ₽"

        holder.cbSelect.isChecked = selectedProducts.contains(product.id)

        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedProducts.add(product.id)
            } else {
                selectedProducts.remove(product.id)
            }
            onSelectionChanged(selectedProducts.size)
        }
    }

    override fun getItemCount() = products.size

    fun getSelectedProducts(): List<Product> {
        return products.filter { selectedProducts.contains(it.id) }
    }
}