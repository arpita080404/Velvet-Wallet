package com.velvetwallet.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val type: String,       // "income" | "expense"
    val date: Long,         // epoch millis
    val notes: String = "",
    val isRecurring: Boolean = false
)

object Categories {

    val EXPENSE = listOf(
        "🍔 Food",
        "🚕 Transport",
        "🛍 Shopping",
        "🎬 Entertainment",
        "🏥 Health",
        "🏠 Housing",
        "📚 Education",
        "💡 Utilities",
        "✈️ Travel",
        "🛒 Groceries",
        "💄 Personal Care",
        "📱 Recharge",
        "🐶 Pets",
        "🎁 Gifts",
        "👕 Clothing",
        "📦 Other",
        "➕ Custom Category"
    )

    val INCOME = listOf(
        "💼 Salary",
        "💻 Freelance",
        "📈 Investment",
        "🎁 Gift",
        "🏆 Bonus",
        "💸 Refund",
        "🪙 Side Income",
        "📦 Other",
        "➕ Custom Category"
    )

    val ALL = (EXPENSE + INCOME).distinct()

    val COLORS = mapOf(

        "🍔 Food" to "#F59E0B",
        "🚕 Transport" to "#3B82F6",
        "🛍 Shopping" to "#EC4899",
        "🎬 Entertainment" to "#8B5CF6",
        "🏥 Health" to "#EF4444",
        "🏠 Housing" to "#10B981",
        "📚 Education" to "#06B6D4",
        "💡 Utilities" to "#F97316",
        "✈️ Travel" to "#14B8A6",
        "🛒 Groceries" to "#84CC16",
        "💄 Personal Care" to "#F472B6",
        "📱 Recharge" to "#6366F1",
        "🐶 Pets" to "#A855F7",
        "🎁 Gifts" to "#E879F9",
        "👕 Clothing" to "#F43F5E",

        "💼 Salary" to "#10B981",
        "💻 Freelance" to "#6366F1",
        "📈 Investment" to "#0EA5E9",
        "🎁 Gift" to "#E879F9",
        "🏆 Bonus" to "#22C55E",
        "💸 Refund" to "#06B6D4",
        "🪙 Side Income" to "#FACC15",

        "📦 Other" to "#9CA3AF",
        "➕ Custom Category" to "#9CA3AF"
    )
}