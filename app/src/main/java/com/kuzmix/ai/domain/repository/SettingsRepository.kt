package com.kuzmix.ai.domain.repository

import com.kuzmix.ai.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val prefs: UserPreferences) {

    val isFirstLaunchDone: Flow<Boolean> = prefs.isFirstLaunchDone
    val userName: Flow<String> = prefs.userName
    val userEmail: Flow<String> = prefs.userEmail
    val appearance: Flow<String> = prefs.appearance
    val language: Flow<String> = prefs.language
    val notificationsEnabled: Flow<Boolean> = prefs.notificationsEnabled

    suspend fun setFirstLaunchDone(done: Boolean = true) = prefs.setFirstLaunchDone(done)
    suspend fun setUserName(name: String) = prefs.setUserName(name)
    suspend fun setUserEmail(email: String) = prefs.setUserEmail(email)
    suspend fun setAppearance(value: String) = prefs.setAppearance(value)
    suspend fun setLanguage(value: String) = prefs.setLanguage(value)
    suspend fun setNotificationsEnabled(enabled: Boolean) = prefs.setNotificationsEnabled(enabled)
}
