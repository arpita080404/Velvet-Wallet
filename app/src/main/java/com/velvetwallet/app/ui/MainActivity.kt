package com.velvetwallet.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.velvetwallet.app.R
import com.velvetwallet.app.data.Expense
import com.velvetwallet.app.data.ExpenseDatabase
import com.velvetwallet.app.databinding.ActivityMainBinding
import com.velvetwallet.app.repository.ExpenseRepository
import com.velvetwallet.app.viewmodel.ExpenseViewModel
import com.velvetwallet.app.viewmodel.ExpenseViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory(
            ExpenseRepository(
                ExpenseDatabase.getInstance(this).expenseDao()
            )
        )
    }

    private lateinit var adapter: ExpenseAdapter

    private val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Velvet Wallet"

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        val cal = Calendar.getInstance()
        binding.tvMonth.text = monthFmt.format(cal.time)
    }

    // -----------------------------------------------------------------------------------------
    // RecyclerView
    // -----------------------------------------------------------------------------------------
    private fun setupRecyclerView() {

        adapter = ExpenseAdapter(
            onEdit = { expense ->
                openAddExpense(expense)
            },
            onDelete = { expense ->
                confirmDelete(expense)
            }
        )

        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            isNestedScrollingEnabled = false
        }
    }

    // -----------------------------------------------------------------------------------------
    // LiveData Observers
    // -----------------------------------------------------------------------------------------
    private fun setupObservers() {

        // Recent Transactions
        viewModel.recentExpenses.observe(this) { list ->

            adapter.submitList(list)

            binding.tvNoRecent.visibility =
                if (list.isEmpty()) View.VISIBLE
                else View.GONE
        }

        // Income
        viewModel.monthIncome.observe(this) { income ->
            binding.tvIncome.text = formatAmount(income)
        }

        // Expense
        viewModel.monthExpenseTotal.observe(this) { expense ->

            binding.tvExpense.text = formatAmount(expense)

            updateBudgetProgress(expense)
        }

        // Balance
        var latestIncome = 0.0
        var latestExpense = 0.0

        viewModel.monthIncome.observe(this) { income ->
            latestIncome = income
            updateBalance(latestIncome, latestExpense)
        }

        viewModel.monthExpenseTotal.observe(this) { expense ->
            latestExpense = expense
            updateBalance(latestIncome, latestExpense)
        }
    }

    // -----------------------------------------------------------------------------------------
    // Balance
    // -----------------------------------------------------------------------------------------
    private fun updateBalance(income: Double, expense: Double) {

        val balance = income - expense

        binding.tvBalance.text = formatAmount(balance)

        binding.tvBalance.setTextColor(
            getColor(
                if (balance >= 0)
                    R.color.income
                else
                    R.color.expense
            )
        )
    }

    // -----------------------------------------------------------------------------------------
    // Budget Progress
    // -----------------------------------------------------------------------------------------
    private fun updateBudgetProgress(expense: Double) {

        val prefs = getSharedPreferences("velvet_prefs", MODE_PRIVATE)

        val cap = prefs.getFloat("budget_cap", 0f).toDouble()

        if (cap <= 0) {
            binding.cardBudget.visibility = View.GONE
            return
        }

        binding.cardBudget.visibility = View.VISIBLE

        val progress =
            ((expense / cap) * 100).toInt().coerceIn(0, 100)

        binding.progressBudget.progress = progress

        binding.tvBudgetStatus.text =
            if (expense > cap) {

                binding.tvBudgetStatus.setTextColor(
                    getColor(R.color.expense)
                )

                "Over budget by ${formatAmount(expense - cap)}"

            } else {

                binding.tvBudgetStatus.setTextColor(
                    getColor(R.color.income)
                )

                "${formatAmount(cap - expense)} remaining"
            }

        binding.tvBudgetLabel.text =
            "${formatAmount(expense)} / ${formatAmount(cap)}"
    }

    // -----------------------------------------------------------------------------------------
    // Click Listeners
    // -----------------------------------------------------------------------------------------
    private fun setupClickListeners() {

        // Floating Add Button
        binding.fabAdd.setOnClickListener {
            openAddExpense(null)
        }
/*
        // Add Expense Button
        binding.btnAddExpense.setOnClickListener {
            openAddExpense(null)
        }
*/
        // View Expenses
        binding.btnViewExpenses.setOnClickListener {
            startActivity(
                Intent(this, ExpenseListActivity::class.java)
            )
        }

        // Analytics
        binding.btnAnalytics.setOnClickListener {
            startActivity(
                Intent(this, AnalyticsActivity::class.java)
            )
        }

        // Settings
        binding.btnSettings.setOnClickListener {
            startActivity(
                Intent(this, SettingsActivity::class.java)
            )
        }
    }

    // -----------------------------------------------------------------------------------------
    // Open Add Expense
    // -----------------------------------------------------------------------------------------
    private fun openAddExpense(expense: Expense?) {

        val intent =
            Intent(this, AddExpenseActivity::class.java)

        expense?.let {
            intent.putExtra(
                AddExpenseActivity.EXTRA_EXPENSE_ID,
                it.id
            )
        }

        startActivity(intent)
    }

    // -----------------------------------------------------------------------------------------
    // Delete Confirmation
    // -----------------------------------------------------------------------------------------
    private fun confirmDelete(expense: Expense) {

        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Transaction")
            .setMessage("Remove \"${expense.title}\"?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.delete(expense)
            }
            .show()
    }

    // -----------------------------------------------------------------------------------------
    // Toolbar Menu
    // -----------------------------------------------------------------------------------------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {

            R.id.action_settings -> {

                startActivity(
                    Intent(this, SettingsActivity::class.java)
                )

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // -----------------------------------------------------------------------------------------
    // Format Currency
    // -----------------------------------------------------------------------------------------
    private fun formatAmount(amount: Double): String {

        val prefs =
            getSharedPreferences("velvet_prefs", MODE_PRIVATE)

        val currency =
            prefs.getString("currency", "₹") ?: "₹"

        return "$currency%.2f".format(amount)
    }

    // -----------------------------------------------------------------------------------------
    // Refresh on Resume
    // -----------------------------------------------------------------------------------------
    override fun onResume() {

        super.onResume()

        viewModel.monthExpenseTotal.value?.let {
            updateBudgetProgress(it)
        }
    }
}