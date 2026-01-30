package com.example.smartkassa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UnitsAdapter(
    private var units: List<UnitDefinition>,
    private val onUnitClick: (UnitDefinition) -> Unit
) : RecyclerView.Adapter<UnitsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUnitName: TextView = view.findViewById(R.id.tvUnitName)
        val tvUnitCategory: TextView = view.findViewById(R.id.tvUnitCategory)
        val tvConversionRate: TextView = view.findViewById(R.id.tvConversionRate)
        val tvBaseUnit: TextView = view.findViewById(R.id.tvBaseUnit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_unit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val unit = units[position]

        holder.tvUnitName.text = unit.name
        holder.tvUnitCategory.text = when (unit.category) {
            UnitDefinition.CATEGORY_WEIGHT -> "Вес"
            UnitDefinition.CATEGORY_VOLUME -> "Объем"
            UnitDefinition.CATEGORY_PIECE -> "Штучный"
            else -> unit.category
        }
        holder.tvBaseUnit.text = "Базовая: ${unit.baseUnit}"

        // Форматируем коэффициент
        val rateText = if (unit.conversionRate >= 1) {
            "1 ${unit.name} = ${"%.0f".format(unit.conversionRate)} ${unit.baseUnit}"
        } else {
            "1 ${unit.name} = ${unit.conversionRate} ${unit.baseUnit}"
        }
        holder.tvConversionRate.text = rateText

        holder.itemView.setOnClickListener {
            onUnitClick(unit)
        }
    }

    override fun getItemCount() = units.size

    fun updateUnits(newUnits: List<UnitDefinition>) {
        this.units = newUnits
        notifyDataSetChanged()
    }
}