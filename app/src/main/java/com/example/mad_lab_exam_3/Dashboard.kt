package com.example.mad_lab_exam_3

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
        recyclerView.adapter = TransactionAdapter(recentTransactions)

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
        }

    }

    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategory)
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
            val displayList = transactions.takeLast(3).reversed()
            recyclerView?.adapter = TransactionAdapter(displayList)
        }

        alertDialog.show()
    }

}

