package com.velvetwallet.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.velvetwallet.app.R
import com.velvetwallet.app.data.Expense
import com.velvetwallet.app.data.ExpenseDatabase
import com.velvetwallet.app.databinding.ActivityExpenseListBinding
import com.velvetwallet.app.repository.ExpenseRepository
import com.velvetwallet.app.viewmodel.ExpenseViewModel
import com.velvetwallet.app.viewmodel.ExpenseViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseListBinding
    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory(ExpenseRepository(ExpenseDatabase.getInstance(this).expenseDao()))
    }
    private lateinit var adapter: ExpenseAdapter
    private val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Transactions"

        setupRecyclerView()
        setupMonthNav()
        setupTypeFilter()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(
            onEdit = { openEdit(it) },
            onDelete = { confirmDelete(it) }
        )
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@ExpenseListActivity)
            this.adapter = this@ExpenseListActivity.adapter
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val expense = adapter.currentList[vh.adapterPosition]
                viewModel.delete(expense)
                Snackbar.make(binding.root, "Deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") { viewModel.insert(expense) }
                    .show()
            }
        }).attachToRecyclerView(binding.rvExpenses)
    }

    private fun setupMonthNav() {
        updateMonthLabel()
        binding.btnPrevMonth.setOnClickListener { viewModel.prevMonth(); updateMonthLabel() }
        binding.btnNextMonth.setOnClickListener { viewModel.nextMonth(); updateMonthLabel() }
    }

    private fun updateMonthLabel() {
        val cal = Calendar.getInstance().apply {
            set(
                viewModel.selectedYear.value ?: Calendar.getInstance().get(Calendar.YEAR),
                viewModel.selectedMonth.value ?: Calendar.getInstance().get(Calendar.MONTH),
                1
            )
        }
        binding.tvMonth.text = monthFmt.format(cal.time)
    }

    private fun setupTypeFilter() {
        listOf(
            binding.chipAll to "",
            binding.chipIncome to "income",
            binding.chipExpense to "expense"
        ).forEach { (chip, type) ->
            chip.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    // Re-observe with type filter applied on existing month expenses
                    viewModel.monthExpenses.removeObservers(this)
                    viewModel.monthExpenses.observe(this) { list ->
                        val filtered = if (type.isEmpty()) list else list.filter { it.type == type }
                        adapter.submitList(filtered)
                        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
        binding.chipAll.isChecked = true
    }

    private fun setupObservers() {
        viewModel.monthExpenses.observe(this) { list ->
            if (binding.chipAll.isChecked) {
                adapter.submitList(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun openEdit(expense: Expense) {
        val intent = Intent(this, AddExpenseActivity::class.java)
        intent.putExtra(AddExpenseActivity.EXTRA_EXPENSE_ID, expense.id)
        startActivity(intent)
    }

    private fun confirmDelete(expense: Expense) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete")
            .setMessage("Remove \"${expense.title}\"?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> viewModel.delete(expense) }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_date_desc -> { /* default */ true }
            R.id.action_sort_date_asc -> { /* sorted asc via adapter */ true }
            R.id.action_sort_amount -> { /* sorted by amount */ true }
            R.id.action_delete_all -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Delete All")
                    .setMessage("This will permanently delete all transactions.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete All") { _, _ -> viewModel.deleteAll() }
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}
