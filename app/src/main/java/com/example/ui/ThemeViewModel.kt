package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode = _isDarkMode.asStateFlow()

    fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean("dark_mode", isDark).apply()
        _isDarkMode.value = isDark
    }
}
