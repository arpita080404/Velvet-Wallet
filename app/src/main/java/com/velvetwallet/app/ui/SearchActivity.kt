package com.velvetwallet.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.velvetwallet.app.data.ExpenseDatabase
import com.velvetwallet.app.databinding.ActivitySearchBinding
import com.velvetwallet.app.repository.ExpenseRepository
import com.velvetwallet.app.viewmodel.ExpenseViewModel
import com.velvetwallet.app.viewmodel.ExpenseViewModelFactory

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory(ExpenseRepository(ExpenseDatabase.getInstance(this).expenseDao()))
    }
    private lateinit var adapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Search"

        adapter = ExpenseAdapter(
            onEdit = { expense ->
                val intent = Intent(this, AddExpenseActivity::class.java)
                intent.putExtra(AddExpenseActivity.EXTRA_EXPENSE_ID, expense.id)
                startActivity(intent)
            },
            onDelete = { viewModel.delete(it) }
        )

        binding.rvResults.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            this.adapter = this@SearchActivity.adapter
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.search(newText ?: "")
                return true
            }
        })

        viewModel.searchResults.observe(this) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty() && !binding.searchView.query.isNullOrBlank())
                View.VISIBLE else View.GONE
        }

        binding.searchView.requestFocus()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}
