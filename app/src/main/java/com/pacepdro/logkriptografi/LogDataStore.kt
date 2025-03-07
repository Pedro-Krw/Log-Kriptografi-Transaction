package com.pacepdro.logkriptografi


import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "log_persistence")

class LogDataStore(private val context: Context) {
    private val LOGS_KEY = stringSetPreferencesKey("logs")

    val logFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[LOGS_KEY]?.toList() ?: emptyList()
    }

    suspend fun saveLog(newLog: String) {
        context.dataStore.edit { preferences ->
            val currentLogs = preferences[LOGS_KEY]?.toMutableSet() ?: mutableSetOf()
            currentLogs.add(newLog)
            preferences[LOGS_KEY] = currentLogs
        }
    }
}
