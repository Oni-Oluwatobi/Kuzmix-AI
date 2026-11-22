package com.kuzmix.ai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kuzmix.ai.domain.ai.QuizHistory
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
        val ASKED_QUIZ_STEMS = stringPreferencesKey("asked_quiz_stems")
        val SAVED_CLASS_KEY = stringPreferencesKey("saved_class_key")
        val SAVED_CLASS_NAME = stringPreferencesKey("saved_class_name")
        val SAVED_TERM = intPreferencesKey("saved_term")
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

    // ------------------------------------------------------ quiz history
    // Stems (questions) already shown to the student, stored as one
    // newline-joined column of 32-char SHA-256 prefixes. Insertion order
    // is preserved so the oldest asked question is the first candidate
    // when a tiny topic runs out of fresh material.

    suspend fun askedQuizStems(): Set<String> =
        context.dataStore.data.first()[Keys.ASKED_QUIZ_STEMS]
            ?.split('\n')
            ?.filter { it.length == QUIZ_STEM_HASH_LEN }
            ?.toCollection(linkedSetOf())
            ?: linkedSetOf()

    suspend fun rememberQuizStems(stems: Collection<String>) {
        if (stems.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.ASKED_QUIZ_STEMS] ?: "")
                .split('\n')
                .filter { it.length == QUIZ_STEM_HASH_LEN }
                .toMutableList()
            for (stem in stems) {
                current.remove(stem)          // re-asked -> move to the end
                current.add(stem)
            }
            val trimmed = if (current.size > MAX_QUIZ_STEMS) {
                current.takeLast(MAX_QUIZ_STEMS)
            } else {
                current
            }
            prefs[Keys.ASKED_QUIZ_STEMS] = trimmed.joinToString("\n")
        }
    }

    // ------------------------------------------------- saved class/term
    // The class the student picked in the quiz wizard. Cached locally so
    // the profile screen works offline, mirrored to Firestore (users/{uid})
    // on login/wizard completion so it follows the account across devices.

    /** Snapshot of the locally cached class/term, or null if never chosen. */
    suspend fun peekSavedClass(): SavedClass? {
        val prefs = context.dataStore.data.first()
        val key = prefs[Keys.SAVED_CLASS_KEY] ?: return null
        return SavedClass(key, prefs[Keys.SAVED_CLASS_NAME], prefs[Keys.SAVED_TERM])
    }

    suspend fun setSavedClass(classKey: String?, className: String?, term: Int?) {
        context.dataStore.edit { prefs ->
            if (classKey.isNullOrBlank()) {
                prefs.remove(Keys.SAVED_CLASS_KEY)
                prefs.remove(Keys.SAVED_CLASS_NAME)
                prefs.remove(Keys.SAVED_TERM)
            } else {
                prefs[Keys.SAVED_CLASS_KEY] = classKey
                className?.let { prefs[Keys.SAVED_CLASS_NAME] = it }
                if (term != null) prefs[Keys.SAVED_TERM] = term
                else prefs.remove(Keys.SAVED_TERM)
            }
        }
    }

    private companion object {
        const val QUIZ_STEM_HASH_LEN = 32
        const val MAX_QUIZ_STEMS = 1200
    }
}

/** Locally cached class/term choice (Firestore mirrors it per user). */
data class SavedClass(
    val classKey: String,
    val className: String?,
    val term: Int?
)

/**
 * [QuizHistory] over DataStore: "never ask me that question again"
 * survives process death. Data layer -> domain dependency is intentional —
 * the interface lives in the domain, this is its persistence adapter.
 */
class StoredQuizHistory(private val prefs: UserPreferences) : QuizHistory {

    override suspend fun askedStems(): Set<String> = prefs.askedQuizStems()

    override suspend fun remember(stems: Collection<String>) =
        prefs.rememberQuizStems(stems)
}
