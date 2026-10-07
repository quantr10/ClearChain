package com.clearchain.app.presentation.ngo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.dto.ActivityItemData
import com.clearchain.app.data.remote.dto.DashboardStatsData
import com.clearchain.app.data.remote.dto.TodaySummaryData
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.repository.ListingRepository
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.clearchain.app.util.UiEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import android.content.Context
import com.clearchain.app.R
import dagger.hilt.android.qualifiers.ApplicationContext

// Derived impact metrics computed from stats
data class ImpactMetrics(
    val kgSaved: Int,
    val mealsProvided: Int,
    val co2AvoidedKg: Int
)

data class NgoDashboardState(
    /** Until the first load settles; refreshes keep the sections on screen. */
    val isLoading: Boolean = true,
    val userName: String = "",
    val profilePictureUrl: String? = null,
    val stats: DashboardStatsData? = null,
    val todaySummary: TodaySummaryData? = null,
    val activities: List<ActivityItemData> = emptyList(),
    val isRefreshing: Boolean = false,
    val weeklyGoal: Int = 10,
    val weeklyCompleted: Int = 0,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val availableListings: List<Listing> = emptyList()
) {
    val impact: ImpactMetrics get() {
        return ImpactMetrics(
            kgSaved = stats?.foodSaved ?: 0,
            mealsProvided = stats?.mealsEstimate ?: 0,
            co2AvoidedKg = stats?.co2EstimateKg ?: 0
        )
    }
    val weeklyProgress: Float get() =
        if (weeklyGoal > 0) (weeklyCompleted.toFloat() / weeklyGoal).coerceIn(0f, 1f) else 0f
}

@HiltViewModel
class NgoDashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val organizationApi: OrganizationApi,
    private val listingRepository: ListingRepository
) : ViewModel() {

    private companion object {
        const val TAG = "NgoDashboardVM"
    }

    private val _state = MutableStateFlow(NgoDashboardState())
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
     *  so pull-to-refresh keeps its spinner until the impact tracker, weekly goal, stats,
     *  activity, and nearby listings are actually up to date.
     *
     *  Every section writes through [MutableStateFlow.update]: these loaders run in
     *  parallel and each one only owns a few fields, so a plain `_state.value = ... .copy()`
     *  would read a snapshot, then overwrite whatever a sibling wrote in the meantime —
     *  which is how the freshly loaded user name and coordinates (and with them the nearby
     *  map) used to blink in and then vanish again. */
    private suspend fun loadAll(): Boolean {
        val results = coroutineScope {
            listOf(
            async { loadStats() },
            async { loadTodaySummary() },
            async { loadActivity() },
            async { loadNearbyListings() },
            ).awaitAll()
        }
        // Sections fail independently and each keeps whatever it showed before;
        // one snackbar says something is missing instead of failing silently.
        if (false in results) {
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.error_dashboard_partial)))
        }
        return false !in results
    }

    // Collected rather than read once, so a new avatar or a renamed organization
    // shows up here as soon as the cached user changes.
    private fun observeUser() {
        viewModelScope.launch {
            try {
                getCurrentUserUseCase().collect { user ->
                    if (user != null) {
                        _state.update {
                            it.copy(
                                userName = user.name,
                                profilePictureUrl = user.profilePictureUrl,
                                userLatitude = user.latitude,
                                userLongitude = user.longitude
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
            val s = organizationApi.getMyStats().data
            // The API counts the last seven days off the hand-over date. This used to be
            // `totalCompleted % 20`, which moved with the all-time total and told the
            // reader nothing about their week.
            _state.update { it.copy(stats = s, weeklyCompleted = s.completedThisWeek) }
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

    private suspend fun loadNearbyListings(): Boolean {
        try {
            return listingRepository.getAllListings(status = "open", pageSize = 50).onSuccess { listings ->
                _state.update { it.copy(availableListings = listings) }
            }.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load nearby listings", e)
            return false
        }
    }
}
