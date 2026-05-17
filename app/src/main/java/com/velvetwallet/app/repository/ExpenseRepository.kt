package com.velvetwallet.app.repository

import com.velvetwallet.app.data.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ExpenseRepository(private val dao: ExpenseDao) {

    fun getAllExpenses(): Flow<List<Expense>> = dao.getAllExpenses()

    fun getMonthExpenses(year: Int, month: Int): Flow<List<Expense>> {
        val (start, end) = monthRange(year, month)
        return dao.getExpensesByMonth(start, end)
    }

    fun getFilteredMonthExpenses(year: Int, month: Int, type: String): Flow<List<Expense>> {
        val (start, end) = monthRange(year, month)
        return dao.getFilteredExpenses(start, end, type)
    }

    fun getCategoryTotals(year: Int, month: Int): Flow<List<CategoryTotal>> {
        val (start, end) = monthRange(year, month)
        return dao.getCategoryTotals(start, end)
    }

    fun getMonthIncome(year: Int, month: Int): Flow<Double> {
        val (start, end) = monthRange(year, month)
        return dao.getTotalIncome(start, end)
    }

    fun getMonthExpense(year: Int, month: Int): Flow<Double> {
        val (start, end) = monthRange(year, month)
        return dao.getTotalExpense(start, end)
    }

    fun search(query: String): Flow<List<Expense>> = dao.searchExpenses(query)

    fun getRecurring(): Flow<List<Expense>> = dao.getRecurringExpenses()

    suspend fun insert(expense: Expense) = dao.insert(expense)
    suspend fun update(expense: Expense) = dao.update(expense)
    suspend fun delete(expense: Expense) = dao.delete(expense)
    suspend fun deleteAll() = dao.deleteAll()

    /** Returns epoch millis for the start (inclusive) and end (exclusive) of a month. */
    fun monthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }
}
