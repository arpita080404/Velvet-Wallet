package com.velvetwallet.app.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.velvetwallet.app.data.ExpenseDatabase
import com.velvetwallet.app.databinding.ActivitySettingsBinding
import com.velvetwallet.app.repository.ExpenseRepository
import com.velvetwallet.app.utils.CsvExporter
import com.velvetwallet.app.viewmodel.ExpenseViewModel
import com.velvetwallet.app.viewmodel.ExpenseViewModelFactory

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory(
            ExpenseRepository(
                ExpenseDatabase.getInstance(this).expenseDao()
            )
        )
    }

    private val currencies = listOf(
        "$",
        "€",
        "£",
        "¥",
        "₹",
        "₩"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        loadCurrentSettings()
        setupCurrencySpinner()
        setupClickListeners()
    }

    private fun loadCurrentSettings() {

        val prefs = getSharedPreferences(
            "velvet_prefs",
            MODE_PRIVATE
        )

        val cap = prefs.getFloat(
            "budget_cap",
            0f
        )

        binding.switchBudget.isChecked = cap > 0

        if (cap > 0) {
            binding.etBudgetCap.setText(
                cap.toInt().toString()
            )
        }

        binding.switchPin.isChecked =
            prefs.getBoolean(
                "pin_enabled",
                false
            )

        binding.switchBiometric.isChecked =
            prefs.getBoolean(
                "biometric_enabled",
                false
            )

        toggleBudgetInput(cap > 0)

        binding.switchBudget.setOnCheckedChangeListener { _, checked ->
            toggleBudgetInput(checked)
        }
    }

    private fun toggleBudgetInput(show: Boolean) {

        binding.tilBudgetCap.isEnabled = show

        binding.tilBudgetCap.alpha =
            if (show) 1f else 0.5f
    }

    private fun setupCurrencySpinner() {

        val prefs = getSharedPreferences(
            "velvet_prefs",
            MODE_PRIVATE
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            currencies
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spinnerCurrency.adapter = adapter

        val cur =
            prefs.getString(
                "currency",
                "$"
            ) ?: "$"

        binding.spinnerCurrency.setSelection(
            currencies.indexOf(cur).coerceAtLeast(0)
        )

        //  Set current currency symbol
        binding.tilBudgetCap.prefixText = cur

        //  Update instantly when changed
        binding.spinnerCurrency.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    binding.tilBudgetCap.prefixText =
                        currencies[position]
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }
    }

    private fun setupClickListeners() {

        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        binding.btnExportCsv.setOnClickListener {

            viewModel.allExpenses.observe(
                this@SettingsActivity
            ) { list ->

                viewModel.allExpenses.removeObservers(
                    this@SettingsActivity
                )

                val result =
                    CsvExporter.export(
                        this,
                        list
                    )

                Toast.makeText(
                    this,
                    result.getOrElse {
                        it.message ?: "Export failed"
                    },
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.btnClearAll.setOnClickListener {

            MaterialAlertDialogBuilder(this)
                .setTitle("Clear All Data")
                .setMessage(
                    "This will permanently delete all transactions and cannot be undone."
                )
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Clear All"
                ) { _, _ ->

                    viewModel.deleteAll()

                    Toast.makeText(
                        this,
                        "All data cleared",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .show()
        }
    }

    private fun saveSettings() {

        val prefs =
            getSharedPreferences(
                "velvet_prefs",
                MODE_PRIVATE
            ).edit()

        val budgetEnabled =
            binding.switchBudget.isChecked

        val capStr =
            binding.etBudgetCap.text
                .toString()
                .trim()

        val cap =
            if (
                budgetEnabled &&
                capStr.isNotEmpty()
            ) {
                capStr.toFloatOrNull() ?: 0f
            } else {
                0f
            }

        prefs.putFloat(
            "budget_cap",
            cap
        )

        val currency =
            currencies[
                binding.spinnerCurrency.selectedItemPosition
            ]

        prefs.putString(
            "currency",
            currency
        )

        val pinEnabled =
            binding.switchPin.isChecked

        prefs.putBoolean(
            "pin_enabled",
            pinEnabled
        )

        prefs.putBoolean(
            "biometric_enabled",
            binding.switchBiometric.isChecked
        )

        // Remove saved PIN if disabled
        if (!pinEnabled) {
            prefs.remove("user_pin")
        }

        prefs.apply()

        Toast.makeText(
            this,
            "Settings saved",
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    override fun onSupportNavigateUp(): Boolean {

        onBackPressedDispatcher.onBackPressed()

        return true
    }
}