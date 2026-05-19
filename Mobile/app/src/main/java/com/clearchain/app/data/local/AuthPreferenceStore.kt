package com.clearchain.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthPreferenceStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
        val KEY_SAVED_EMAIL = stringPreferencesKey("saved_email")
    }

    val rememberMe: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        prefs[KEY_REMEMBER_ME] ?: false
    }

    val savedEmail: Flow<String> = context.authDataStore.data.map { prefs ->
        prefs[KEY_SAVED_EMAIL] ?: ""
    }

    suspend fun saveRememberMe(enabled: Boolean, email: String = "") {
        context.authDataStore.edit { prefs ->
            prefs[KEY_REMEMBER_ME] = enabled
            prefs[KEY_SAVED_EMAIL] = if (enabled) email else ""
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { prefs ->
            prefs[KEY_REMEMBER_ME] = false
            prefs[KEY_SAVED_EMAIL] = ""
        }
    }
}
