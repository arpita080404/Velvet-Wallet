package com.velvetwallet.app.data

import androidx.room.ColumnInfo

data class CategoryTotal(
    val category: String,
    @ColumnInfo(name = "total") val total: Double
)

data class MonthlyStats(
    val year: Int,
    val month: Int,
    val totalIncome: Double,
    val totalExpense: Double
) {
    val savings: Double get() = totalIncome - totalExpense
    val savingsRate: Double get() = if (totalIncome > 0) (savings / totalIncome) * 100 else 0.0
}
