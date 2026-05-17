package com.velvetwallet.app.utils

import com.velvetwallet.app.data.MonthlyStats

object PredictionEngine {

    /**
     * Predicts next month's expense using simple linear regression over
     * the provided monthly stats (ordered oldest → newest).
     *
     * Returns null if fewer than 2 data points are available.
     */
    fun predictNextMonthExpense(history: List<MonthlyStats>): Double? {
        val data = history.filter { it.totalExpense > 0 }
        if (data.size < 2) return null

        val n = data.size
        val xMean = (n - 1) / 2.0
        val yMean = data.sumOf { it.totalExpense } / n

        var numerator = 0.0
        var denominator = 0.0
        data.forEachIndexed { i, stats ->
            val xi = i.toDouble()
            numerator += (xi - xMean) * (stats.totalExpense - yMean)
            denominator += (xi - xMean) * (xi - xMean)
        }

        val slope = if (denominator != 0.0) numerator / denominator else 0.0
        val intercept = yMean - slope * xMean

        val nextX = n.toDouble()
        val prediction = intercept + slope * nextX
        return prediction.coerceAtLeast(0.0)
    }

    /**
     * Predicts per-category spend based on a 3-month rolling average.
     */
    fun predictCategorySpend(monthlyValues: List<Double>): Double {
        if (monthlyValues.isEmpty()) return 0.0
        val window = monthlyValues.takeLast(3)
        return window.sum() / window.size
    }

    fun trend(history: List<MonthlyStats>): String {
        val prediction = predictNextMonthExpense(history) ?: return "Insufficient data"
        val last = history.lastOrNull()?.totalExpense ?: return "No data"
        val diff = prediction - last
        val pct = if (last > 0) (diff / last * 100).toInt() else 0
        return when {
            pct > 5 -> "Expenses predicted to rise ~$pct% next month"
            pct < -5 -> "Expenses predicted to drop ~${-pct}% next month"
            else -> "Expenses predicted to stay stable next month"
        }
    }
}
