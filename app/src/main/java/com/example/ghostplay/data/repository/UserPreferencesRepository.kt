package com.example.ghostplay.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
    }

    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_NAME]
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            if (preferences[PreferencesKeys.USER_NAME] == null) {
                preferences[PreferencesKeys.USER_NAME] = name
            }
        }
    }
}
