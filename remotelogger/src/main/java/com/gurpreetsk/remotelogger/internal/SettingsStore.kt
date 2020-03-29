package com.gurpreetsk.remotelogger.internal

import android.content.Context

interface SettingsStore {
    fun putString(key: String, value: String)
    fun getString(key: String, default: String): String
    fun clear()
}

class SettingsStoreImpl(
        private val context: Context
) : SettingsStore {

    private val preferences by lazy {
        context.getSharedPreferences("remote_logger_preferences", Context.MODE_PRIVATE)
    }

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun getString(key: String, default: String): String {
        return preferences.getString(key, default) ?: ""
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }
}
