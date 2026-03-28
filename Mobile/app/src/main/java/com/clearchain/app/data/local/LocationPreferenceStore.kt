package com.clearchain.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.clearchain.app.domain.model.LocationPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "location_prefs")

@Singleton
class LocationPreferenceStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_LAT = doublePreferencesKey("lat")
        val KEY_LNG = doublePreferencesKey("lng")
        val KEY_RADIUS = intPreferencesKey("radius_km")
        val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        val KEY_SAVED_AT = longPreferencesKey("saved_at")
    }

    val locationPreference: Flow<LocationPreference?> = context.dataStore.data.map { prefs ->
        val lat = prefs[KEY_LAT] ?: return@map null
        val lng = prefs[KEY_LNG] ?: return@map null
        LocationPreference(
            latitude = lat,
            longitude = lng,
            radiusKm = prefs[KEY_RADIUS] ?: 10,
            displayName = prefs[KEY_DISPLAY_NAME] ?: "Unknown",
            savedAt = prefs[KEY_SAVED_AT] ?: 0
        )
    }

    suspend fun save(preference: LocationPreference) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAT] = preference.latitude
            prefs[KEY_LNG] = preference.longitude
            prefs[KEY_RADIUS] = preference.radiusKm
            prefs[KEY_DISPLAY_NAME] = preference.displayName
            prefs[KEY_SAVED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}