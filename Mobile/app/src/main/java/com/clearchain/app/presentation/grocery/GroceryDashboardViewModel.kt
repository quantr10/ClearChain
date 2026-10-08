package com.clearchain.app.presentation.grocery

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.dto.ActivityItemData
import com.clearchain.app.data.remote.dto.DashboardStatsData
import com.clearchain.app.data.remote.dto.TodaySummaryData
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroceryDashboardState(
    /** Until the first load settles; refreshes keep the sections on screen. */
    val isLoading: Boolean = true,
    val userName: String = "",
    val profilePictureUrl: String? = null,
    val stats: DashboardStatsData? = null,
    val todaySummary: TodaySummaryData? = null,
    val weeklyGoal: Int = 10,
    val activities: List<ActivityItemData> = emptyList(),
    val isRefreshing: Boolean = false
) {
    val weeklyCompleted: Int get() = stats?.completedThisWeek ?: 0

    val weeklyProgress: Float get() =
        if (weeklyGoal > 0) (weeklyCompleted.toFloat() / weeklyGoal).coerceIn(0f, 1f) else 0f
}

@HiltViewModel
class GroceryDashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val organizationApi: OrganizationApi
) : ViewModel() {

    private companion object {
        const val TAG = "GroceryDashboardVM"
    }

    private val _state = MutableStateFlow(
        GroceryDashboardState(userName = context.getString(R.string.label_grocery_store_name))
    )
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        observeUser()
        viewModelScope.launch {
            loadAll()
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val ok = loadAll()
            _state.update { it.copy(isRefreshing = false) }
            if (ok) _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_dashboard_refreshed)))
        }
    }

    /** Reloads every dashboard section concurrently and suspends until all have settled,
     *  so pull-to-refresh keeps its spinner until the stats, activity, and today's summary
     *  are actually up to date.
     *
     *  Every section writes through [MutableStateFlow.update]: these loaders run in
     *  parallel and each one only owns a few fields, so a plain `_state.value = ... .copy()`
     *  would read a snapshot, then overwrite whatever a sibling wrote in the meantime. */
    private suspend fun loadAll(): Boolean {
        val results = coroutineScope {
            listOf(
                async { loadStats() },
                async { loadTodaySummary() },
                async { loadActivity() }
            ).awaitAll()
        }
        // Sections fail independently and each keeps whatever it showed before;
        // one snackbar says something is missing instead of failing silently.
        if (false in results) {
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.error_dashboard_partial)))
        }
        return false !in results
    }

    // Collected rather than read once, so a new avatar or a renamed store shows
    // up here as soon as the cached user changes.
    private fun observeUser() {
        viewModelScope.launch {
            try {
                getCurrentUserUseCase().collect { user ->
                    if (user != null) {
                        _state.update {
                            it.copy(
                                userName = user.name,
                                profilePictureUrl = user.profilePictureUrl
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to observe current user", e)
            }
        }
    }

    private suspend fun loadStats(): Boolean {
        try {
            val stats = organizationApi.getMyStats().data
            _state.update { it.copy(stats = stats) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load stats", e)
            return false
        }
        return true
    }

    private suspend fun loadTodaySummary(): Boolean {
        try {
            val summary = organizationApi.getTodaySummary().data
            _state.update { it.copy(todaySummary = summary) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load today's summary", e)
            return false
        }
        return true
    }

    private suspend fun loadActivity(): Boolean {
        try {
            val activities = organizationApi.getMyActivity().data
            _state.update { it.copy(activities = activities) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load activity", e)
            return false
        }
        return true
    }
}
