package com.example.smartkassa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class IncomeAdapter(
    private var items: MutableList<IncomeItem>,
    private val onItemRemoved: (Int) -> Unit,
    private val onQuantityChanged: (IncomeItem, Int) -> Unit
) : RecyclerView.Adapter<IncomeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvCost: TextView = view.findViewById(R.id.tvCost)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val btnIncrease: MaterialButton = view.findViewById(R.id.btnIncrease)
        val btnDecrease: MaterialButton = view.findViewById(R.id.btnDecrease)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_income_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvProductName.text = item.productName
        holder.tvCost.text = "%.2f ₽/${item.unit}".format(item.costPerUnit)
        holder.tvQuantity.text = item.quantity.toString()
        holder.tvTotal.text = "%.2f ₽".format(item.total)

        holder.btnIncrease.setOnClickListener {
            item.quantity++
            item.total = item.costPerUnit * item.quantity
            notifyItemChanged(position)
            onQuantityChanged(item, position)
        }

        holder.btnDecrease.setOnClickListener {
            if (item.quantity > 1) {
                item.quantity--
                item.total = item.costPerUnit * item.quantity
                notifyItemChanged(position)
                onQuantityChanged(item, position)
            } else {
                // При уменьшении до 0 удаляем товар с подтверждением
                showRemoveConfirmation(holder.itemView.context, position)
            }
        }

        // Длинный клик для быстрого удаления
        holder.itemView.setOnLongClickListener {
            showRemoveConfirmation(holder.itemView.context, position)
            true
        }
    }

    override fun getItemCount() = items.size

    fun removeItem(position: Int) {
        if (position in 0 until items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
            onItemRemoved(position)
        }
    }

    private fun showRemoveConfirmation(context: android.content.Context, position: Int) {
        val item = items[position]

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Удаление товара")
            .setMessage("Удалить товар \"${item.productName}\" из поставки?")
            .setPositiveButton("Удалить") { dialog, _ ->
                removeItem(position)
                android.widget.Toast.makeText(
                    context,
                    "Товар \"${item.productName}\" удален",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}