package com.example.mad_lab_exam_3

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar

class Dashboard : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val allTransactions = TransactionStorage.loadTransactions(requireContext())

        val recentTransactions = allTransactions.takeLast(3).reversed()

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = TransactionAdapter(
            recentTransactions,
            onEdit = { transaction -> showEditTransactionDialog(transaction) },
            onDelete = { transaction -> confirmDeleteTransaction(transaction) }
        )

        val fab = view.findViewById<FloatingActionButton>(R.id.floatingActionButton)
        fab.setOnClickListener {
            showAddTransactionDialog()
        }

        val transactions = TransactionStorage.loadTransactions(requireContext())

        val totalIncome = transactions.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "expense" }.sumOf { it.amount }

        val prefs = requireContext().getSharedPreferences("finance_tracker_prefs", Context.MODE_PRIVATE)
        val budget = prefs.getFloat("monthly_budget", 0f)

        val remaining = budget - totalExpense

        view.findViewById<TextView>(R.id.totalIncomeValue)?.text = "Rs. %.2f".format(totalIncome)
        view.findViewById<TextView>(R.id.totalExpensesValue)?.text = "Rs. %.2f".format(totalExpense)
        view.findViewById<TextView>(R.id.remainingBudgetValue)?.text = "Rs. %.2f".format(remaining)

        if (remaining < 0) {
            Toast.makeText(requireContext(), "Warning: Budget exceeded!", Toast.LENGTH_LONG).show()
        } else if (remaining <= 1000) {
            Toast.makeText(requireContext(), "Warning: You have only Rs. 1000 budget left!", Toast.LENGTH_LONG).show()
        }

        val summaryLink = view.findViewById<TextView>(R.id.summaryLink)
        summaryLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ExpensesSummary())
                .addToBackStack(null)
                .commit()
        }

    }

    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgType)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        val etCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.etCategory)
        val categories = listOf("Food", "Transport", "Bills", "Entertainment", "Other")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        etCategory.setAdapter(categoryAdapter)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val category = etCategory.text.toString()
            val date = etDate.text.toString()

            if (title.isBlank()) {
                Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (amount == null) {
                Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (category.isBlank()) {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (date.isBlank()) {
                Toast.makeText(requireContext(), "Please choose a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rgType.checkedRadioButtonId == -1) {
                Toast.makeText(requireContext(), "Please select a transaction type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = if (rgType.checkedRadioButtonId == R.id.rbIncome) "income" else "expense"

            val transaction = Transaction(
                title = title,
                amount = amount,
                category = category,
                date = date,
                type = type
            )

            val transactions = TransactionStorage.loadTransactions(requireContext())
            transactions.add(transaction)
            TransactionStorage.saveTransactions(requireContext(), transactions)

            Toast.makeText(requireContext(), "Transaction Added", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()

            val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)
            val displayList = transactions.takeLast(3).reversed()
            recyclerView?.adapter = TransactionAdapter(
                displayList,
                onEdit = { transaction -> showEditTransactionDialog(transaction) },
                onDelete = { transaction -> confirmDeleteTransaction(transaction) }
            )

        }

        alertDialog.show()
    }

    private fun confirmDeleteTransaction(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                val transactions = TransactionStorage.loadTransactions(requireContext())
                transactions.removeIf { it.id == transaction.id }
                TransactionStorage.saveTransactions(requireContext(), transactions)
                Toast.makeText(requireContext(), "Transaction Deleted", Toast.LENGTH_SHORT).show()
                refreshTransactionList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshTransactionList() {
        val allTransactions = TransactionStorage.loadTransactions(requireContext())
        val displayList = allTransactions.takeLast(3).reversed()

        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView?.adapter = TransactionAdapter(
            displayList,
            onEdit = { transaction -> showEditTransactionDialog(transaction) },
            onDelete = { transaction -> confirmDeleteTransaction(transaction) }
        )

        val totalIncome = allTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpense = allTransactions.filter { it.type == "expense" }.sumOf { it.amount }
        val prefs = requireContext().getSharedPreferences("finance_tracker_prefs", Context.MODE_PRIVATE)
        val budget = prefs.getFloat("monthly_budget", 0f)
        val remaining = budget - totalExpense

        view?.findViewById<TextView>(R.id.totalIncomeValue)?.text = "Rs. %.2f".format(totalIncome)
        view?.findViewById<TextView>(R.id.totalExpensesValue)?.text = "Rs. %.2f".format(totalExpense)
        view?.findViewById<TextView>(R.id.remainingBudgetValue)?.text = "Rs. %.2f".format(remaining)
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgType)
        val rbIncome = dialogView.findViewById<RadioButton>(R.id.rbIncome)
        val rbExpense = dialogView.findViewById<RadioButton>(R.id.rbExpense)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        val etCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.etCategory)
        val categories = listOf("Food", "Transport", "Bills", "Entertainment", "Other")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        etCategory.setAdapter(categoryAdapter)

        etTitle.setText(transaction.title)
        etAmount.setText(transaction.amount.toString())
        etCategory.setText(transaction.category, false)
        etDate.setText(transaction.date)

        if (transaction.type == "income") rbIncome.isChecked = true else rbExpense.isChecked = true

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val category = etCategory.text.toString()
            val date = etDate.text.toString()

            if (title.isBlank()) {
                Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (amount == null) {
                Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (category.isBlank()) {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (date.isBlank()) {
                Toast.makeText(requireContext(), "Please choose a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rgType.checkedRadioButtonId == -1) {
                Toast.makeText(requireContext(), "Please select a transaction type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = if (rgType.checkedRadioButtonId == R.id.rbIncome) "income" else "expense"

            val updatedTransaction = Transaction(
                id = transaction.id,
                title = title,
                amount = amount,
                category = category,
                date = date,
                type = type
            )

            val transactions = TransactionStorage.loadTransactions(requireContext())
            val index = transactions.indexOfFirst { it.id == transaction.id }
            if (index != -1) {
                transactions[index] = updatedTransaction
                TransactionStorage.saveTransactions(requireContext(), transactions)
                Toast.makeText(requireContext(), "Transaction Updated", Toast.LENGTH_SHORT).show()
            }

            alertDialog.dismiss()
            refreshTransactionList()
        }

        alertDialog.show()
    }
}