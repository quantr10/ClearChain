package com.clearchain.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_NOTIF_NEW_LISTING = booleanPreferencesKey("notif_new_listing")
        val KEY_NOTIF_REQUEST_UPDATE = booleanPreferencesKey("notif_request_update")
        val KEY_NOTIF_EXPIRY = booleanPreferencesKey("notif_expiry")
    }

    val theme: Flow<String> = context.settingsDataStore.data.map { it[KEY_THEME] ?: "system" }
    val language: Flow<String> = context.settingsDataStore.data.map { it[KEY_LANGUAGE] ?: "en" }
    val notifNewListing: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_NOTIF_NEW_LISTING] ?: true }
    val notifRequestUpdate: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_NOTIF_REQUEST_UPDATE] ?: true }
    val notifExpiry: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_NOTIF_EXPIRY] ?: true }

    suspend fun setTheme(theme: String) = context.settingsDataStore.edit { it[KEY_THEME] = theme }
    suspend fun setLanguage(language: String) = context.settingsDataStore.edit { it[KEY_LANGUAGE] = language }
    suspend fun setNotifNewListing(enabled: Boolean) = context.settingsDataStore.edit { it[KEY_NOTIF_NEW_LISTING] = enabled }
    suspend fun setNotifRequestUpdate(enabled: Boolean) = context.settingsDataStore.edit { it[KEY_NOTIF_REQUEST_UPDATE] = enabled }
    suspend fun setNotifExpiry(enabled: Boolean) = context.settingsDataStore.edit { it[KEY_NOTIF_EXPIRY] = enabled }

    /** Synchronous read for use in [android.app.Application.attachBaseContext]. */
    fun getLanguageSync(): String =
        context.getSharedPreferences("settings_sync", Context.MODE_PRIVATE)
            .getString("language", "en") ?: "en"

    suspend fun setLanguageAndSync(language: String) {
        setLanguage(language)
        context.getSharedPreferences("settings_sync", Context.MODE_PRIVATE)
            .edit().putString("language", language).apply()
    }
}
