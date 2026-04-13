package com.example.smartkassa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class RecipesAdapter(
    private val recipes: List<Recipe>,
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIngredient: TextView = view.findViewById(R.id.tvIngredient)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val tvStock: TextView = view.findViewById(R.id.tvStock)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.tvIngredient.text = recipe.ingredientName
        holder.tvQuantity.text = "${recipe.quantityNeeded} ${recipe.ingredientUnit}"
        holder.tvStock.text = "На складе: ${String.format("%.2f", recipe.ingredientStock)} ${recipe.ingredientUnit}"

        // Определяем статус доступности
        val context = holder.itemView.context
        if (recipe.ingredientStock >= recipe.quantityNeeded) {
            holder.tvStatus.text = "✓ Достаточно"
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success_green))
        } else {
            val needed = recipe.quantityNeeded - recipe.ingredientStock
            holder.tvStatus.text = "⚠ Нужно еще: ${String.format("%.2f", needed)}"
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.accent_color))
        }

        holder.itemView.setOnClickListener {
            onRecipeClick(recipe)
        }
    }

    override fun getItemCount() = recipes.size
}