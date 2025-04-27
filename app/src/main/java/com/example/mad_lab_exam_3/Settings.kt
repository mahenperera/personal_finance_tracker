package com.example.mad_lab_exam_3

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader

class Settings : Fragment() {

    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val budgetInput = view.findViewById<TextInputEditText>(R.id.budgetEditText)
        val saveBtn = view.findViewById<Button>(R.id.saveSettingsBtn)
        val backupBtn = view.findViewById<Button>(R.id.btn_export_data)
        val restoreBtn = view.findViewById<Button>(R.id.btn_import_data)

        prefs = requireContext().getSharedPreferences("finance_tracker_prefs", Context.MODE_PRIVATE)

        // Load existing budget
        val savedBudget = prefs.getFloat("monthly_budget", 0f)
        if (savedBudget > 0f) {
            budgetInput.setText(savedBudget.toString())
        }

        saveBtn.setOnClickListener {
            val budgetValue = budgetInput.text.toString().toFloatOrNull()
            if (budgetValue == null || budgetValue < 0) {
                Toast.makeText(requireContext(), "Enter a valid budget", Toast.LENGTH_SHORT).show()
            } else {
                prefs.edit().putFloat("monthly_budget", budgetValue).apply()
                Toast.makeText(requireContext(), "Budget saved", Toast.LENGTH_SHORT).show()
            }
        }

        backupBtn.setOnClickListener {
            val budgetValue = prefs.getFloat("monthly_budget", 0f)
            val transactionsJson = prefs.getString("transactions", "[]")

            val json = JSONObject()
            json.put("monthly_budget", budgetValue)
            json.put("transactions", JSONArray(transactionsJson))

            val file = File(requireContext().filesDir, "transaction_backup.json")
            file.writeText(json.toString())

            Toast.makeText(requireContext(), "Backup saved!", Toast.LENGTH_SHORT).show()
        }

        restoreBtn.setOnClickListener {
            try {
                val file = File(requireContext().filesDir, "transaction_backup.json")
                if (file.exists()) {
                    val jsonString = file.readText()

                    val json = JSONObject(jsonString)
                    val restoredBudget = json.getDouble("monthly_budget").toFloat()
                    val restoredTransactions = json.getJSONArray("transactions").toString()

                    prefs.edit()
                        .putFloat("monthly_budget", restoredBudget)
                        .putString("transactions", restoredTransactions)
                        .apply()

                    budgetInput?.setText(restoredBudget.toString())

                    Toast.makeText(requireContext(), "Backup restored!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "No backup file found.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Failed to restore backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        return view
    }
}
