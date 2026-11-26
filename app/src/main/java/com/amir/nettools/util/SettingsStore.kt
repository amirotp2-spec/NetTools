package com.amir.nettools.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** themeMode: 0 = system, 1 = dark (default), 2 = light. language: "fa" (default) or "en". */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var themeMode by mutableIntStateOf(prefs.getInt("theme", 1))
        private set
    var language by mutableStateOf(prefs.getString("lang", "fa") ?: "fa")
        private set

    val isFa: Boolean get() = language == "fa"

    fun updateTheme(mode: Int) {
        themeMode = mode
        prefs.edit().putInt("theme", mode).apply()
    }

    fun updateLanguage(lang: String) {
        language = lang
        prefs.edit().putString("lang", lang).apply()
    }
}
