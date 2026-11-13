package com.kuzmix.ai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kuzmix_prefs")

class UserPreferences(private val context: Context) {

    private object Keys {
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch_done")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val APPEARANCE = stringPreferencesKey("appearance")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SEED_VERSION = intPreferencesKey("knowledge_seed_version")
        val KNOWLEDGE_DB_VERSION = intPreferencesKey("knowledge_db_version")
        val MODEL_PATH = stringPreferencesKey("local_model_path")
    }

    val isFirstLaunchDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.FIRST_LAUNCH] ?: false }

    val userName: Flow<String> = context.dataStore.data.map { it[Keys.USER_NAME] ?: "Oni Oluwatobi" }

    val userEmail: Flow<String> = context.dataStore.data.map { it[Keys.USER_EMAIL] ?: "thekreadivegalaxy@gmail.com" }

    val appearance: Flow<String> = context.dataStore.data.map { it[Keys.APPEARANCE] ?: "Dark" }

    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "English" }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setFirstLaunchDone(done: Boolean = true) {
        context.dataStore.edit { it[Keys.FIRST_LAUNCH] = done }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name }
    }

    suspend fun setUserEmail(email: String) {
        context.dataStore.edit { it[Keys.USER_EMAIL] = email }
    }

    suspend fun setAppearance(value: String) {
        context.dataStore.edit { it[Keys.APPEARANCE] = value }
    }

    suspend fun setLanguage(value: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = value }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun getSeedVersion(): Int =
        context.dataStore.data.first()[Keys.SEED_VERSION] ?: 0

    suspend fun setSeedVersion(version: Int) {
        context.dataStore.edit { it[Keys.SEED_VERSION] = version }
    }

    suspend fun getKnowledgeDbVersion(): Int =
        context.dataStore.data.first()[Keys.KNOWLEDGE_DB_VERSION] ?: 0

    suspend fun setKnowledgeDbVersion(version: Int) {
        context.dataStore.edit { it[Keys.KNOWLEDGE_DB_VERSION] = version }
    }

    suspend fun getModelPath(): String? =
        context.dataStore.data.first()[Keys.MODEL_PATH]

    suspend fun setModelPath(path: String?) {
        context.dataStore.edit { prefs ->
            if (path == null) prefs.remove(Keys.MODEL_PATH)
            else prefs[Keys.MODEL_PATH] = path
        }
    }
}
