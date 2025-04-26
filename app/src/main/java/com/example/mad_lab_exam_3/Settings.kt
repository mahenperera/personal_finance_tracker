package com.example.mad_lab_exam_3

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText

class Settings : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val budgetInput = view.findViewById<TextInputEditText>(R.id.budgetEditText)
        val saveBtn = view.findViewById<Button>(R.id.saveSettingsBtn)

        // Load existing budget
        val prefs = requireContext().getSharedPreferences("finance_tracker_prefs", Context.MODE_PRIVATE)
        val savedBudget = prefs.getFloat("monthly_budget", 0f)
        if (savedBudget > 0f) {
            budgetInput?.setText(savedBudget.toString())
        }

        saveBtn.setOnClickListener {
            val budgetValue = budgetInput?.text.toString().toFloatOrNull()
            if (budgetValue == null || budgetValue < 0) {
                Toast.makeText(requireContext(), "Enter a valid budget", Toast.LENGTH_SHORT).show()
            } else {
                prefs.edit().putFloat("monthly_budget", budgetValue).apply()
                Toast.makeText(requireContext(), "Budget saved", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
