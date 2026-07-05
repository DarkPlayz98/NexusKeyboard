package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("minimalist_keyboard_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_THEME = "keyboard_theme"
        const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        const val KEY_HAPTIC_STRENGTH = "haptic_strength"
        const val KEY_LANGUAGE = "keyboard_language"
        const val KEY_CLOUD_SYNC = "cloud_sync_enabled"
        const val KEY_LAST_SYNCED = "last_synced_time"
        const val KEY_ONE_HANDED = "one_handed_mode"
    }

    var oneHandedMode: String
        get() = prefs.getString(KEY_ONE_HANDED, "Standard") ?: "Standard"
        set(value) = prefs.edit().putString(KEY_ONE_HANDED, value).apply()

    var selectedTheme: String
        get() = prefs.getString(KEY_THEME, "Midnight OLED") ?: "Midnight OLED"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var isHapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, value).apply()

    var hapticStrength: Int
        get() = prefs.getInt(KEY_HAPTIC_STRENGTH, 50)
        set(value) = prefs.edit().putInt(KEY_HAPTIC_STRENGTH, value).apply()

    var selectedLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "EN") ?: "EN"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var isCloudSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_CLOUD_SYNC, value).apply()

    var lastSyncedTime: Long
        get() = prefs.getLong(KEY_LAST_SYNCED, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNCED, value).apply()
}
