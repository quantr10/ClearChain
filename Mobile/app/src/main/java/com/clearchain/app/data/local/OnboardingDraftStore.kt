package com.clearchain.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_draft")

data class OnboardingDraft(
    val phone: String = "",
    val description: String = "",
    val contactPerson: String = "",
    val address: String = "",
    val city: String = "",
    val openTime: String = "",
    val closeTime: String = "",
    val pickupInstructions: String = "",
    val verificationDocumentUri: String = "",
    val savedAt: Long = 0L
)

@Singleton
class OnboardingDraftStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_PHONE = stringPreferencesKey("phone")
        val KEY_DESCRIPTION = stringPreferencesKey("description")
        val KEY_CONTACT_PERSON = stringPreferencesKey("contact_person")
        val KEY_ADDRESS = stringPreferencesKey("address")
        val KEY_CITY = stringPreferencesKey("city")
        val KEY_OPEN_TIME = stringPreferencesKey("open_time")
        val KEY_CLOSE_TIME = stringPreferencesKey("close_time")
        val KEY_PICKUP_INSTRUCTIONS = stringPreferencesKey("pickup_instructions")
        val KEY_DOC_URI = stringPreferencesKey("verification_doc_uri")
        val KEY_SAVED_AT = longPreferencesKey("saved_at")
    }

    val draft: Flow<OnboardingDraft?> = context.onboardingDataStore.data.map { prefs ->
        val savedAt = prefs[KEY_SAVED_AT] ?: return@map null
        if (savedAt == 0L) return@map null
        OnboardingDraft(
            phone = prefs[KEY_PHONE] ?: "",
            description = prefs[KEY_DESCRIPTION] ?: "",
            contactPerson = prefs[KEY_CONTACT_PERSON] ?: "",
            address = prefs[KEY_ADDRESS] ?: "",
            city = prefs[KEY_CITY] ?: "",
            openTime = prefs[KEY_OPEN_TIME] ?: "",
            closeTime = prefs[KEY_CLOSE_TIME] ?: "",
            pickupInstructions = prefs[KEY_PICKUP_INSTRUCTIONS] ?: "",
            verificationDocumentUri = prefs[KEY_DOC_URI] ?: "",
            savedAt = savedAt
        )
    }

    suspend fun save(draft: OnboardingDraft) {
        context.onboardingDataStore.edit { prefs ->
            prefs[KEY_PHONE] = draft.phone
            prefs[KEY_DESCRIPTION] = draft.description
            prefs[KEY_CONTACT_PERSON] = draft.contactPerson
            prefs[KEY_ADDRESS] = draft.address
            prefs[KEY_CITY] = draft.city
            prefs[KEY_OPEN_TIME] = draft.openTime
            prefs[KEY_CLOSE_TIME] = draft.closeTime
            prefs[KEY_PICKUP_INSTRUCTIONS] = draft.pickupInstructions
            prefs[KEY_DOC_URI] = draft.verificationDocumentUri
            prefs[KEY_SAVED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun clear() {
        context.onboardingDataStore.edit { it.clear() }
    }
}
