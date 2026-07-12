package com.chroma.studio.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_usage_prefs", Context.MODE_PRIVATE)

    fun canGenerate(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val key = "generations_${uid}_$today"
        
        val count = prefs.getInt(key, 0)
        return count < 3
    }

    fun incrementGeneration() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val key = "generations_${uid}_$today"
        
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()
    }

    fun getGenerationsLeft(): Int {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return 0
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val key = "generations_${uid}_$today"
        
        val count = prefs.getInt(key, 0)
        return maxOf(0, 3 - count)
    }
}
