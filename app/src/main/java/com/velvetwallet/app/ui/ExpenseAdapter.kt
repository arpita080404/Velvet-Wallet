package com.velvetwallet.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.velvetwallet.app.R
import com.velvetwallet.app.data.Categories
import com.velvetwallet.app.data.Expense
import com.velvetwallet.app.databinding.ItemExpenseBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseAdapter(
    private val onEdit: (Expense) -> Unit,
    private val onDelete: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(DiffCallback()) {

    private val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())

    inner class ExpenseViewHolder(val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {
            binding.tvTitle.text = expense.title
            binding.tvCategory.text = expense.category
            binding.tvDate.text = dateFmt.format(Date(expense.date))

            val isIncome = expense.type.equals("income", ignoreCase = true)
            val amountText = if (isIncome) "+$%.2f".format(expense.amount)
            else "-$%.2f".format(expense.amount)
            binding.tvAmount.text = amountText
            binding.tvAmount.setTextColor(
                binding.root.context.getColor(
                    if (isIncome) R.color.income else R.color.expense
                )
            )

            val colorHex = Categories.COLORS[expense.category] ?: "#9CA3AF"
            binding.viewCategoryDot.setBackgroundColor(Color.parseColor(colorHex))

            if (expense.isRecurring) {
                binding.tvRecurring.visibility = android.view.View.VISIBLE
            } else {
                binding.tvRecurring.visibility = android.view.View.GONE
            }

            if (expense.notes.isNotBlank()) {
                binding.tvNotes.visibility = android.view.View.VISIBLE
                binding.tvNotes.text = expense.notes
            } else {
                binding.tvNotes.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onEdit(expense) }
            binding.root.setOnLongClickListener {
                onDelete(expense)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(a: Expense, b: Expense) = a.id == b.id
        override fun areContentsTheSame(a: Expense, b: Expense) = a == b
    }
}
