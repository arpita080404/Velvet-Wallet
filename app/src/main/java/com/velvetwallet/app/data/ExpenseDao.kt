package com.velvetwallet.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getExpensesByMonth(start: Long, end: Long): Flow<List<Expense>>

    @Query("""
        SELECT * FROM expenses
        WHERE date BETWEEN :start AND :end
          AND (:type = '' OR type = :type)
        ORDER BY date DESC
    """)
    fun getFilteredExpenses(start: Long, end: Long, type: String): Flow<List<Expense>>

    @Query("""
        SELECT category, SUM(amount) AS total
        FROM expenses
        WHERE type = 'expense' AND date BETWEEN :start AND :end
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryTotals(start: Long, end: Long): Flow<List<CategoryTotal>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE type = 'income' AND date BETWEEN :start AND :end")
    fun getTotalIncome(start: Long, end: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE type = 'expense' AND date BETWEEN :start AND :end")
    fun getTotalExpense(start: Long, end: Long): Flow<Double>

    @Query("""
        SELECT * FROM expenses
        WHERE (title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        ORDER BY date DESC
    """)
    fun searchExpenses(query: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE isRecurring = 1 ORDER BY date DESC")
    fun getRecurringExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY amount DESC")
    fun getAllSortedByAmountDesc(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date ASC")
    fun getAllSortedByDateAsc(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Int): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
