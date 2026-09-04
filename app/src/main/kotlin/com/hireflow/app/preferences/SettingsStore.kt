package com.hireflow.app.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("hireflow_settings")

class SettingsStore(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")

    val darkMode: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[darkModeKey] ?: false
    }

    val notificationsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[notificationsEnabledKey] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[darkModeKey] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[notificationsEnabledKey] = enabled }
    }
}
