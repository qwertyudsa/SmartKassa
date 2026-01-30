package com.example.smartkassa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ActivityAdapter(
    private val activities: List<ActivityItem>,
    private val onItemClick: (ActivityItem) -> Unit
) : RecyclerView.Adapter<ActivityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvDate: TextView = view.findViewById(R.id.tvDate) // Новая TextView для даты
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        val context = holder.itemView.context

        // Форматируем дату
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val formattedDate = dateFormat.format(activity.date)
        val formattedTime = timeFormat.format(activity.date)

        // Устанавливаем значения
        holder.tvType.text = activity.type
        holder.tvDescription.text = activity.description
        holder.tvAmount.text = activity.amount
        holder.tvTime.text = formattedTime
        holder.tvDate.text = formattedDate

        // Разный цвет для разных типов операций
        val colorRes = when (activity.type) {
            "Продажа" -> R.color.success_green
            "Поступление" -> R.color.accent_color
            "Приготовление" -> R.color.warning_orange
            "Обратная связь" -> R.color.info_blue
            else -> R.color.black
        }
        holder.tvType.setTextColor(ContextCompat.getColor(context, colorRes))

        // Цвет для суммы (доход/расход)
        val amountColor = if (activity.amount.startsWith("+")) {
            ContextCompat.getColor(context, R.color.success_green)
        } else {
            ContextCompat.getColor(context, R.color.accent_color)
        }
        holder.tvAmount.setTextColor(amountColor)

        // Обработчик клика
        holder.itemView.setOnClickListener {
            onItemClick(activity)
        }
    }

    override fun getItemCount() = activities.size
}