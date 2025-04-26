package com.example.mad_lab_exam_3

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar

class Transactions : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val fab = view.findViewById<FloatingActionButton>(R.id.floatingActionButton)
        val spinner = view.findViewById<Spinner>(R.id.categoryFilterSpinner)

        val allTransactions = TransactionStorage.loadTransactions(requireContext())
        val categories = listOf("All", "Food", "Transport", "Bills", "Entertainment", "Other")

        // Setup Spinner
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set initial adapter with all transactions
        var filteredTransactions = allTransactions
        recyclerView.adapter = TransactionAdapter(
            filteredTransactions,
            onEdit = { transaction -> showEditTransactionDialog(transaction) },
            onDelete = { transaction -> confirmDeleteTransaction(transaction) }
        )

        // Handle category selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                filteredTransactions = if (selectedCategory == "All") {
                    allTransactions
                } else {
                    allTransactions.filter {
                        it.category.equals(selectedCategory, ignoreCase = true)
                    }.toMutableList()
                }


                recyclerView.adapter = TransactionAdapter(
                    filteredTransactions,
                    onEdit = { transaction -> showEditTransactionDialog(transaction) },
                    onDelete = { transaction -> confirmDeleteTransaction(transaction) }
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // FAB for adding new transactions
        fab.setOnClickListener {
            showAddTransactionDialog()
        }
    }


    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)

        val etCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.etCategory)

        val categories = listOf("Food", "Transport", "Bills", "Entertainment", "Other")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        etCategory.setAdapter(categoryAdapter)

        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgType)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Date picker
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
            val type = if (rgType.checkedRadioButtonId == R.id.rbIncome) "income" else "expense"

            if (title.isBlank() || amount == null || category.isBlank() || date.isBlank()) {
                Toast.makeText(requireContext(), "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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

            Toast.makeText(requireContext(), "Transaction added", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()

            val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)
            recyclerView?.adapter = TransactionAdapter(
                transactions,
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
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                refreshTransactionList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshTransactionList() {
        val allTransactions = TransactionStorage.loadTransactions(requireContext())

        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView?.adapter = TransactionAdapter(
            allTransactions,
            onEdit = { transaction -> showEditTransactionDialog(transaction) },
            onDelete = { transaction -> confirmDeleteTransaction(transaction) }
        )
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

        // Pre-fill fields with existing transaction data
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
            val type = if (rgType.checkedRadioButtonId == R.id.rbIncome) "income" else "expense"

            if (title.isBlank() || amount == null || category.isBlank() || date.isBlank()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedTransaction = Transaction(
                id = transaction.id, // keep same ID
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
                Toast.makeText(requireContext(), "Transaction updated", Toast.LENGTH_SHORT).show()
            }

            alertDialog.dismiss()
            refreshTransactionList()
        }

        alertDialog.show()
    }


}
