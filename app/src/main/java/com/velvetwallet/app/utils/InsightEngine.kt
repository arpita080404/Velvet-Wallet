package com.velvetwallet.app.utils

import com.velvetwallet.app.data.CategoryTotal

object InsightEngine {

    fun generate(
        currentTotals: List<CategoryTotal>,
        previousTotals: List<CategoryTotal>,
        income: Double,
        expense: Double,
        budgetCap: Double
    ): List<String> {
        val insights = mutableListOf<String>()
        val curMap = currentTotals.associate { it.category to it.total }
        val prevMap = previousTotals.associate { it.category to it.total }
        val curTotal = curMap.values.sum()
        val prevTotal = prevMap.values.sum()

        if (prevTotal > 0 && curTotal > 0) {
            val diff = ((curTotal - prevTotal) / prevTotal) * 100
            when {
                diff > 10 -> insights.add("Total spending is up ${diff.toInt()}% from last month")
                diff < -10 -> insights.add("You spent ${(-diff).toInt()}% less than last month — excellent!")
            }
        }

        for ((cat, amount) in curMap) {
            val last = prevMap[cat] ?: continue
            if (last <= 0) continue
            val diff = ((amount - last) / last) * 100
            when {
                diff > 30 -> insights.add("$cat spending increased by ${diff.toInt()}% this month")
                diff < -30 -> insights.add("$cat costs reduced by ${(-diff).toInt()}% vs last month")
            }
        }

        if (income > 0 && expense > 0) {
            val ratio = (expense / income) * 100
            when {
                ratio > 90 -> insights.add("High spend alert: ${ratio.toInt()}% of income used")
                ratio in 50.0..75.0 -> insights.add("On track — ${ratio.toInt()}% of income spent so far")
                ratio < 40 -> insights.add("Great discipline! Only ${ratio.toInt()}% of income spent")
            }
        }

        if (budgetCap > 0 && expense > 0) {
            val pct = (expense / budgetCap) * 100
            when {
                expense > budgetCap -> insights.add("Budget exceeded by ${(expense - budgetCap).format()} this month")
                pct > 80 -> insights.add("Budget warning: ${pct.toInt()}% of monthly cap used")
            }
        }

        val topCategory = curMap.maxByOrNull { it.value }
        if (topCategory != null && curTotal > 0) {
            val pct = (topCategory.value / curTotal * 100).toInt()
            if (pct > 35) {
                insights.add("${topCategory.key} is ${pct}% of total spending this month")
            }
        }

        return insights.take(4)
    }

    private fun Double.format(): String = "%.2f".format(this)
}
