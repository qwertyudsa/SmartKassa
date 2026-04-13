package com.example.smartkassa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class SaleAdapter(
    private val items: List<SaleItem>,
    private val onQuantityChanged: (SaleItem, Int) -> Unit,
    private val onItemRemoved: (Int) -> Unit
) : RecyclerView.Adapter<SaleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val btnIncrease: MaterialButton = view.findViewById(R.id.btnIncrease)
        val btnDecrease: MaterialButton = view.findViewById(R.id.btnDecrease)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sale_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvProductName.text = item.productName
        holder.tvPrice.text = "%.2f ₽".format(item.price)
        holder.tvQuantity.text = item.quantity.toString()
        holder.tvTotal.text = "%.2f ₽".format(item.total)

        holder.btnIncrease.setOnClickListener {
            item.quantity++
            item.total = item.price * item.quantity
            notifyItemChanged(position)
            onQuantityChanged(item, position)
        }

        holder.btnDecrease.setOnClickListener {
            if (item.quantity > 1) {
                item.quantity--
                item.total = item.price * item.quantity
                notifyItemChanged(position)
                onQuantityChanged(item, position)
            } else {
                // При уменьшении до 0 удаляем товар
                onItemRemoved(position)
            }
        }

    }

    override fun getItemCount() = items.size
}

