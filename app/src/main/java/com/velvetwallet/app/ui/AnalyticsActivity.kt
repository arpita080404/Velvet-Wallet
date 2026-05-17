package com.velvetwallet.app.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.velvetwallet.app.data.Categories
import com.velvetwallet.app.data.ExpenseDatabase
import com.velvetwallet.app.databinding.ActivityAnalyticsBinding
import com.velvetwallet.app.repository.ExpenseRepository
import com.velvetwallet.app.viewmodel.ExpenseViewModel
import com.velvetwallet.app.viewmodel.ExpenseViewModelFactory
import com.velvetwallet.app.utils.PredictionEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory(ExpenseRepository(ExpenseDatabase.getInstance(this).expenseDao()))
    }
    private val monthShort = SimpleDateFormat("MMM", Locale.getDefault())
    private val monthFull = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Analytics"

        setupPieChart()
        setupBarChart()
        setupMonthNav()
        setupObservers()
        loadSixMonthData()
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            isDrawHoleEnabled = true
            holeRadius = 52f
            setHoleColor(Color.TRANSPARENT)
            setUsePercentValues(true)
            description.isEnabled = false
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(11f)
            legend.apply {
                isEnabled = true
                orientation = Legend.LegendOrientation.VERTICAL
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                verticalAlignment = Legend.LegendVerticalAlignment.CENTER
                textColor = if (resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES)
                    Color.WHITE else Color.BLACK
                textSize = 12f
            }
        }
    }

    private fun setupBarChart() {
        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setFitBars(true)
            xAxis.setDrawGridLines(false)
            axisRight.isEnabled = false
            axisLeft.setDrawGridLines(true)
            legend.isEnabled = true
        }
    }

    private fun setupMonthNav() {
        updateMonthLabel()
        binding.btnPrevMonth.setOnClickListener { viewModel.prevMonth(); updateMonthLabel() }
        binding.btnNextMonth.setOnClickListener { viewModel.nextMonth(); updateMonthLabel() }
    }

    private fun updateMonthLabel() {

        val year = viewModel.selectedYear.value ?: Calendar.getInstance().get(Calendar.YEAR)

        val month =  viewModel.selectedMonth.value ?: Calendar.getInstance().get(Calendar.MONTH)

        val cal = Calendar.getInstance().apply {
            set(year, month, 1)
        }

        binding.tvMonth.text = monthFull.format(cal.time)
    }

    private fun setupObservers() {
        viewModel.monthIncome.observe(this) { income ->
            binding.tvTotalIncome.text = formatAmount(income)
        }

        viewModel.monthExpenseTotal.observe(this) { expense ->
            binding.tvTotalExpense.text = formatAmount(expense)
        }

        viewModel.categoryTotals.observe(this) { totals ->
            if (totals.isEmpty()) {
                binding.pieChart.visibility = View.GONE
                binding.tvNoPieData.visibility = View.VISIBLE
                return@observe
            }
            binding.pieChart.visibility = View.VISIBLE
            binding.tvNoPieData.visibility = View.GONE

            val entries = totals.map { PieEntry(it.total.toFloat(), it.category) }
            val colors = totals.map {
                Color.parseColor(Categories.COLORS[it.category] ?: "#9CA3AF")
            }

            val dataSet = PieDataSet(entries, "").apply {
                this.colors = colors
                sliceSpace = 2f
                selectionShift = 6f
                valueFormatter = PercentFormatter(binding.pieChart)
                valueTextSize = 11f
                valueTextColor = Color.WHITE
            }

            binding.pieChart.data = PieData(dataSet)
            binding.pieChart.invalidate()
        }

        viewModel.insights.observe(this) { list ->
            binding.tvPrediction.text = list.joinToString("\n• ", prefix = "• ")
        }
    }

    private fun loadSixMonthData() {
        lifecycleScope.launch {
            val stats = viewModel.getSixMonthStats()
            val labels = stats.map {
                val cal = Calendar.getInstance().apply { set(it.year ?: 2026, it.month ?: 0, 1) }
                monthShort.format(cal.time)
            }

            val incomeEntries = stats.mapIndexed { i, s -> BarEntry(i.toFloat(), s.totalIncome.toFloat()) }
            val expenseEntries = stats.mapIndexed { i, s -> BarEntry(i.toFloat(), s.totalExpense.toFloat()) }

            val incomeDs = BarDataSet(incomeEntries, "Income").apply {
                color = Color.parseColor("#10B981")
                valueTextSize = 9f
            }
            val expenseDs = BarDataSet(expenseEntries, "Expense").apply {
                color = Color.parseColor("#F43F5E")
                valueTextSize = 9f
            }

            val groupSpace = 0.2f
            val barSpace = 0.05f
            val barWidth = 0.35f

            val barData = BarData(incomeDs, expenseDs).apply { this.barWidth = barWidth }
            binding.barChart.data = barData
            binding.barChart.xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                setCenterAxisLabels(true)
                axisMinimum = 0f
                axisMaximum = stats.size.toFloat()
                granularity = 1f
            }
            binding.barChart.groupBars(0f, groupSpace, barSpace)
            binding.barChart.invalidate()

            val trend = PredictionEngine.trend(stats)
            binding.tvTrend.text = trend
        }
    }

    private fun formatAmount(amount: Double): String {
        val prefs = getSharedPreferences("velvet_prefs", MODE_PRIVATE)
        val currency = prefs.getString("currency", "$") ?: "$"
        return "$currency%.2f".format(amount)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}
