package com.clearchain.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.listingDraftStore by preferencesDataStore(name = "listing_draft")

data class ListingDraft(
    val title: String = "",
    val description: String = "",
    val category: String = "FRUITS",
    val quantity: String = "",
    val unit: String = "kg",
    val expiryDate: String = "",
    val savedAt: Long = 0L
)

@Singleton
class ListingDraftStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_TITLE = stringPreferencesKey("draft_title")
        val KEY_DESCRIPTION = stringPreferencesKey("draft_description")
        val KEY_CATEGORY = stringPreferencesKey("draft_category")
        val KEY_QUANTITY = stringPreferencesKey("draft_quantity")
        val KEY_UNIT = stringPreferencesKey("draft_unit")
        val KEY_EXPIRY_DATE = stringPreferencesKey("draft_expiry_date")
        val KEY_PICKUP_START = stringPreferencesKey("draft_pickup_start")
        val KEY_PICKUP_END = stringPreferencesKey("draft_pickup_end")
        val KEY_SAVED_AT = longPreferencesKey("draft_saved_at")
    }

    val draft: Flow<ListingDraft?> = context.listingDraftStore.data.map { prefs ->
        val savedAt = prefs[KEY_SAVED_AT] ?: return@map null
        if (savedAt == 0L) return@map null
        ListingDraft(
            title = prefs[KEY_TITLE] ?: "",
            description = prefs[KEY_DESCRIPTION] ?: "",
            category = prefs[KEY_CATEGORY] ?: "FRUITS",
            quantity = prefs[KEY_QUANTITY] ?: "",
            unit = prefs[KEY_UNIT] ?: "kg",
            expiryDate = prefs[KEY_EXPIRY_DATE] ?: "",
            savedAt = savedAt
        )
    }

    suspend fun save(draft: ListingDraft) {
        context.listingDraftStore.edit { prefs ->
            prefs[KEY_TITLE] = draft.title
            prefs[KEY_DESCRIPTION] = draft.description
            prefs[KEY_CATEGORY] = draft.category
            prefs[KEY_QUANTITY] = draft.quantity
            prefs[KEY_UNIT] = draft.unit
            prefs[KEY_EXPIRY_DATE] = draft.expiryDate
            prefs[KEY_SAVED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun clear() {
        context.listingDraftStore.edit { it.clear() }
    }
}
