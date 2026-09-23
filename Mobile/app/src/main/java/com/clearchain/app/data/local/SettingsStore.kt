package com.clearchain.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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

        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        const val LANG_EN = "en"
        const val LANG_VI = "vi"

        // DataStore only reads asynchronously, which is too late for the two settings that have
        // to be right on the very first frame: the locale is needed in attachBaseContext, before
        // any resource is resolved, and the theme is needed in the first composition or the app
        // paints the wrong palette and then flips. Both are mirrored here so they can be read
        // synchronously at startup; DataStore stays the source of truth for everything else.
        const val SYNC_PREFS = "settings_sync"
        private const val SYNC_KEY_LANGUAGE = "language"
        private const val SYNC_KEY_THEME = "theme"
    }

    val theme: Flow<String> = context.settingsDataStore.data.map { it[KEY_THEME] ?: THEME_SYSTEM }
    val language: Flow<String> = context.settingsDataStore.data.map { it[KEY_LANGUAGE] ?: LANG_EN }
    val notifNewListing: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_NOTIF_NEW_LISTING] ?: true }
    val notifRequestUpdate: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_NOTIF_REQUEST_UPDATE] ?: true }
    val notifExpiry: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_NOTIF_EXPIRY] ?: true }

    suspend fun setTheme(theme: String) = context.settingsDataStore.edit { it[KEY_THEME] = theme }
    suspend fun setLanguage(language: String) = context.settingsDataStore.edit { it[KEY_LANGUAGE] = language }
    suspend fun setNotifNewListing(enabled: Boolean) = context.settingsDataStore.edit { it[KEY_NOTIF_NEW_LISTING] = enabled }
    suspend fun setNotifRequestUpdate(enabled: Boolean) = context.settingsDataStore.edit { it[KEY_NOTIF_REQUEST_UPDATE] = enabled }
    suspend fun setNotifExpiry(enabled: Boolean) = context.settingsDataStore.edit { it[KEY_NOTIF_EXPIRY] = enabled }

    suspend fun setLanguageAndSync(language: String) {
        setLanguage(language)
        syncPrefs().edit().putString(SYNC_KEY_LANGUAGE, language).apply()
    }

    suspend fun setThemeAndSync(theme: String) {
        setTheme(theme)
        syncPrefs().edit().putString(SYNC_KEY_THEME, theme).apply()
    }

    /** The stored theme as of the last write, readable before DataStore has emitted. */
    fun syncedTheme(): String = syncPrefs().getString(SYNC_KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    /**
     * Repairs the synchronous mirror for an install that set its theme before the mirror
     * existed, so the stored choice survives the upgrade instead of silently resetting.
     */
    suspend fun primeSyncedTheme() {
        val prefs = syncPrefs()
        if (!prefs.contains(SYNC_KEY_THEME)) {
            prefs.edit().putString(SYNC_KEY_THEME, theme.first()).apply()
        }
    }

    /** True when this notification [type] should raise a system notification. */
    suspend fun allowsNotification(type: String): Boolean = when (type) {
        "new_listing" -> notifNewListing.first()

        "pickup_request_created", "pickup_approved", "pickup_ready",
        "pickup_completed", "pickup_rejected", "pickup_cancelled" -> notifRequestUpdate.first()

        "listing_expiring_soon", "listing_expired",
        "inventory_expiring_soon", "inventory_expired" -> notifExpiry.first()

        // Account-level messages — verification decisions, the welcome mail, admin alerts —
        // have no toggle of their own and must not be silenced by a content preference.
        else -> true
    }

    private fun syncPrefs() = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
}
