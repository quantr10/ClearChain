package com.clearchain.app.presentation.ngo

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
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Derived impact metrics computed from stats
data class ImpactMetrics(
    val kgSaved: Int,
    val mealsProvided: Int,
    val co2AvoidedKg: Int
)

data class NgoDashboardState(
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
    val availableListings: List<Listing> = emptyList(),
    val nearbyExpiringListings: List<Listing> = emptyList()
) {
    val impact: ImpactMetrics get() {
        return ImpactMetrics(
            kgSaved       = stats?.foodSaved ?: 0,
            mealsProvided = stats?.mealsEstimate ?: 0,
            co2AvoidedKg  = stats?.co2EstimateKg ?: 0
        )
    }
    val weeklyProgress: Float get() =
        if (weeklyGoal > 0) (weeklyCompleted.toFloat() / weeklyGoal).coerceIn(0f, 1f) else 0f
}

@HiltViewModel
class NgoDashboardViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val organizationApi: OrganizationApi,
    private val listingRepository: ListingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NgoDashboardState())
    val state = _state.asStateFlow()

    init {
        observeUser()
        viewModelScope.launch { loadAll() }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            loadAll()
            _state.update { it.copy(isRefreshing = false) }
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
    private suspend fun loadAll(): Unit = coroutineScope {
        launch { loadStats() }
        launch { loadTodaySummary() }
        launch { loadActivity() }
        launch { loadNearbyListings() }
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
            } catch (_: Exception) {}
        }
    }

    private suspend fun loadStats() {
        try {
            val s = organizationApi.getMyStats().data
            // The API counts the last seven days off the hand-over date. This used to be
            // `totalCompleted % 20`, which moved with the all-time total and told the
            // reader nothing about their week.
            _state.update { it.copy(stats = s, weeklyCompleted = s.completedThisWeek) }
        } catch (_: Exception) {}
    }

    private suspend fun loadTodaySummary() {
        try {
            val summary = organizationApi.getTodaySummary().data
            _state.update { it.copy(todaySummary = summary) }
        } catch (_: Exception) {}
    }

    private suspend fun loadActivity() {
        try {
            val activities = organizationApi.getMyActivity().data
            _state.update { it.copy(activities = activities) }
        } catch (_: Exception) {}
    }

    private suspend fun loadNearbyListings() {
        try {
            listingRepository.getAllListings(status = "open", pageSize = 50).onSuccess { listings ->
                val today = LocalDate.now()
                val cutoff = today.plusDays(3)
                val expiring = listings.filter { listing ->
                    runCatching {
                        val date = LocalDate.parse(listing.expiryDate.take(10))
                        !date.isBefore(today) && !date.isAfter(cutoff)
                    }.getOrDefault(false)
                }
                _state.update {
                    it.copy(
                        nearbyExpiringListings = expiring,
                        availableListings = listings
                    )
                }
            }
        } catch (_: Exception) {}
    }
}
