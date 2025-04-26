package com.example.mad_lab_exam_3

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TransactionStorage {
    private const val PREF_NAME = "finance_tracker_prefs"
    private const val KEY_TRANSACTIONS = "transactions"

    fun saveTransactions(context: Context, transactions: List<Transaction>) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val json = Gson().toJson(transactions)
        editor.putString(KEY_TRANSACTIONS, json)
        editor.apply()
    }

    fun loadTransactions(context: Context): MutableList<Transaction> {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = sharedPrefs.getString(KEY_TRANSACTIONS, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            Gson().fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}
