package com.velvetwallet.app.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.velvetwallet.app.data.Categories
import com.velvetwallet.app.data.Expense
import com.velvetwallet.app.data.ExpenseDatabase
import com.velvetwallet.app.databinding.ActivityAddExpenseBinding
import com.velvetwallet.app.repository.ExpenseRepository
import com.velvetwallet.app.viewmodel.ExpenseViewModel
import com.velvetwallet.app.viewmodel.ExpenseViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EXPENSE_ID = "extra_expense_id"
    }

    private lateinit var binding: ActivityAddExpenseBinding
    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory(ExpenseRepository(ExpenseDatabase.getInstance(this).expenseDao()))
    }

    private var selectedDate: Long = System.currentTimeMillis()
    private var editingExpense: Expense? = null
    private var currentType = "expense"
    private val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("velvet_prefs", MODE_PRIVATE)
        val currency = prefs.getString("currency", "$") ?: "$"
        binding.tilAmount.prefixText = currency

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val expenseId = intent.getIntExtra(EXTRA_EXPENSE_ID, -1)
        if (expenseId != -1) {
            supportActionBar?.title = "Edit Transaction"
            loadExpense(expenseId)
        } else {
            supportActionBar?.title = "Add Transaction"
        }

        setupTypeToggle()
        setupDatePicker()
        setupSaveButton()
        updateCategorySpinner()
    }

    private fun setupTypeToggle() {
        binding.btnGroupType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            currentType = if (checkedId == binding.btnIncome.id) "income" else "expense"
            updateCategorySpinner()
        }
    }

    private fun updateCategorySpinner() {

        val cats =
            if (currentType == "income")
                Categories.INCOME
            else
                Categories.EXPENSE

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            cats
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spinnerCategory.adapter = adapter

        binding.spinnerCategory.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {

                    val selected = cats[position]

                    if (selected == "➕ Custom Category") {

                        binding.tilCustomCategory.visibility =
                            android.view.View.VISIBLE

                    } else {

                        binding.tilCustomCategory.visibility =
                            android.view.View.GONE
                    }
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {
                }
            }
    }
    private fun setupDatePicker() {

        binding.tvDate.text = dateFmt.format(selectedDate)

        binding.btnPickDate.setOnClickListener {

            val cal = Calendar.getInstance().apply {
                timeInMillis = selectedDate
            }

            val dialog = DatePickerDialog(
                this,
                { _, y, m, d ->

                    val picked = Calendar.getInstance().apply {
                        set(y, m, d, 12, 0, 0)
                    }

                    selectedDate = picked.timeInMillis

                    binding.tvDate.text =
                        dateFmt.format(selectedDate)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )

            //  Prevent future dates
            dialog.datePicker.maxDate =
                System.currentTimeMillis()

            dialog.show()
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener { saveExpense() }
    }

    private fun loadExpense(id: Int) {
        lifecycleScope.launch {
            val expense = viewModel.allExpenses.value?.find { it.id == id } ?: return@launch
            editingExpense = expense
            binding.etTitle.setText(expense.title)
            binding.etAmount.setText(expense.amount.toString())
            binding.etNotes.setText(expense.notes)
            binding.switchRecurring.isChecked = expense.isRecurring
            selectedDate = expense.date
            binding.tvDate.text = dateFmt.format(selectedDate)
            currentType = expense.type

            if (expense.type == "income") {
                binding.btnGroupType.check(binding.btnIncome.id)
            } else {
                binding.btnGroupType.check(binding.btnExpense.id)
            }
            updateCategorySpinner()

            val cats = if (expense.type == "income") Categories.INCOME else Categories.EXPENSE
            val idx = cats.indexOf(expense.category)
            if (idx >= 0) binding.spinnerCategory.setSelection(idx)
        }
    }

    private fun saveExpense() {
        val title = binding.etTitle.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()
        val isRecurring = binding.switchRecurring.isChecked
        var category =
            binding.spinnerCategory.selectedItem?.toString()
                ?: "Other"

        if (category == "➕ Custom Category") {

            val custom =
                binding.etCustomCategory.text
                    .toString()
                    .trim()

            if (custom.isEmpty()) {

                binding.tilCustomCategory.error =
                    "Enter category"

                return
            }

            binding.tilCustomCategory.error = null

            category = custom
        }
        if (title.isEmpty()) {
            binding.tilTitle.error = "Title is required"
            return
        }
        binding.tilTitle.error = null

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.tilAmount.error = "Enter a valid amount"
            return
        }
        binding.tilAmount.error = null

        val expense = Expense(
            id = editingExpense?.id ?: 0,
            title = title,
            amount = amount,
            category = category,
            type = currentType,
            date = selectedDate,
            notes = notes,
            isRecurring = isRecurring
        )

        if (editingExpense != null) {
            viewModel.update(expense)
            Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insert(expense)
            Toast.makeText(this, "Transaction added", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
