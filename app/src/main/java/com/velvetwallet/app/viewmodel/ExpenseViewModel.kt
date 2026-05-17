package com.velvetwallet.app.viewmodel

import androidx.lifecycle.*
import com.velvetwallet.app.data.*
import com.velvetwallet.app.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseViewModel(private val repo: ExpenseRepository) : ViewModel() {

    // ── Selected month ──────────────────────────────────────────────────────
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    fun setMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
    }

    fun prevMonth() {
        if (_selectedMonth.value == 0) { _selectedMonth.value = 11; _selectedYear.value-- }
        else _selectedMonth.value--
    }

    fun nextMonth() {
        if (_selectedMonth.value == 11) { _selectedMonth.value = 0; _selectedYear.value++ }
        else _selectedMonth.value++
    }

    // ── Month-scoped data ───────────────────────────────────────────────────
    val monthExpenses: LiveData<List<Expense>> = combine(_selectedYear, _selectedMonth) { y, m -> y to m }
        .flatMapLatest { (y, m) -> repo.getMonthExpenses(y, m) }
        .asLiveData()

    val monthIncome: LiveData<Double> = combine(_selectedYear, _selectedMonth) { y, m -> y to m }
        .flatMapLatest { (y, m) -> repo.getMonthIncome(y, m) }
        .asLiveData()

    val monthExpenseTotal: LiveData<Double> = combine(_selectedYear, _selectedMonth) { y, m -> y to m }
        .flatMapLatest { (y, m) -> repo.getMonthExpense(y, m) }
        .asLiveData()

    val categoryTotals: LiveData<List<CategoryTotal>> = combine(_selectedYear, _selectedMonth) { y, m -> y to m }
        .flatMapLatest { (y, m) -> repo.getCategoryTotals(y, m) }
        .asLiveData()

    // ── All expenses ────────────────────────────────────────────────────────
    val allExpenses: LiveData<List<Expense>> = repo.getAllExpenses().asLiveData()

    val recentExpenses: LiveData<List<Expense>> = repo.getAllExpenses()
        .map { it.take(5) }
        .asLiveData()

    val recurringExpenses: LiveData<List<Expense>> = repo.getRecurring().asLiveData()

    // ── Search ──────────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchResults: LiveData<List<Expense>> = _searchQuery
        .debounce(300)
        .flatMapLatest { q -> if (q.isBlank()) flowOf(emptyList()) else repo.search(q) }
        .asLiveData()

    fun search(query: String) { _searchQuery.value = query }

    // ── Smart insights ──────────────────────────────────────────────────────
    val insights: LiveData<List<String>> = combine(_selectedYear, _selectedMonth) { y, m -> y to m }
        .flatMapLatest { (y, m) ->
            val prevY = if (m == 0) y - 1 else y
            val prevM = if (m == 0) 11 else m - 1
            combine(
                repo.getCategoryTotals(y, m),
                repo.getCategoryTotals(prevY, prevM),
                repo.getMonthIncome(y, m),
                repo.getMonthExpense(y, m)
            ) { cur, prev, income, expense ->
                buildInsights(cur, prev, income, expense)
            }
        }
        .asLiveData()

    // ── CRUD ────────────────────────────────────────────────────────────────
    fun insert(expense: Expense) = viewModelScope.launch { repo.insert(expense) }
    fun update(expense: Expense) = viewModelScope.launch { repo.update(expense) }
    fun delete(expense: Expense) = viewModelScope.launch { repo.delete(expense) }
    fun deleteAll() = viewModelScope.launch { repo.deleteAll() }

    // ── Six-month data for charts ───────────────────────────────────────────
    suspend fun getSixMonthStats(): List<MonthlyStats> {
        val result = mutableListOf<MonthlyStats>()
        val now = Calendar.getInstance()
        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance().apply { time = now.time; add(Calendar.MONTH, -i) }
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val income = repo.getMonthIncome(y, m).first()
            val expense = repo.getMonthExpense(y, m).first()
            result.add(MonthlyStats(y, m, income, expense))
        }
        return result
    }

    private fun buildInsights(
        cur: List<CategoryTotal>,
        prev: List<CategoryTotal>,
        income: Double,
        expense: Double
    ): List<String> {
        val insights = mutableListOf<String>()
        val curMap = cur.associate { it.category to it.total }
        val prevMap = prev.associate { it.category to it.total }

        val curTotal = curMap.values.sum()
        val prevTotal = prevMap.values.sum()

        if (prevTotal > 0 && curTotal > 0) {
            val diff = ((curTotal - prevTotal) / prevTotal) * 100
            when {
                diff > 10 -> insights.add("Total spending is up ${diff.toInt()}% from last month")
                diff < -10 -> insights.add("You spent ${(-diff).toInt()}% less than last month")
            }
        }

        for ((cat, amount) in curMap) {
            val last = prevMap[cat] ?: continue
            if (last <= 0) continue
            val diff = ((amount - last) / last) * 100
            when {
                diff > 25 -> insights.add("$cat spending up ${diff.toInt()}% this month")
                diff < -25 -> insights.add("$cat costs down ${(-diff).toInt()}% — great job!")
            }
        }

        if (income > 0 && expense > 0) {
            val ratio = (expense / income) * 100
            when {
                ratio > 85 -> insights.add("Alert: ${ratio.toInt()}% of income spent this month")
                ratio < 50 -> insights.add("On track — only ${ratio.toInt()}% of income spent")
            }
        }

        return insights.take(3)
    }
}
