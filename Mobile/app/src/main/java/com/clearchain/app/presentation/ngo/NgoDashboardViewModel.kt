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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// Derived impact metrics computed from stats
data class ImpactMetrics(
    val kgSaved: Int,
    val mealsProvided: Int,
    val co2AvoidedKg: Int
)

data class NgoDashboardState(
    val userName: String = "",
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
        val distributed = stats?.distributed ?: 0
        return ImpactMetrics(
            kgSaved       = distributed * 3,
            mealsProvided = distributed * 8,
            co2AvoidedKg  = distributed * 7
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

    // Keep legacy for any existing collectors
    val userName     = MutableStateFlow("")
    val stats        = MutableStateFlow<DashboardStatsData?>(null)

    init {
        loadAll()
    }

    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        loadAll(onDone = { _state.value = _state.value.copy(isRefreshing = false) })
    }

    private fun loadAll(onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            getCurrentUserUseCase().first()?.let { user ->
                _state.value = _state.value.copy(
                    userName = user.name,
                    userLatitude = user.latitude,
                    userLongitude = user.longitude
                )
                userName.value = user.name
            }
        }
        viewModelScope.launch {
            try {
                val response = organizationApi.getMyStats()
                val s = response.data
                val weeklyCompleted = if (s.totalCompleted > 0) minOf(s.totalCompleted % 20, 10) else 0
                _state.value = _state.value.copy(stats = s, weeklyCompleted = weeklyCompleted)
                stats.value = s
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                val summary = organizationApi.getTodaySummary()
                _state.value = _state.value.copy(todaySummary = summary.data)
            } catch (_: Exception) {}
            onDone?.invoke()
        }
        viewModelScope.launch {
            try {
                val response = organizationApi.getMyActivity()
                _state.value = _state.value.copy(activities = response.data)
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                val result = listingRepository.getAllListings(status = "open", pageSize = 50)
                result.onSuccess { listings ->
                    val today = LocalDate.now()
                    val cutoff = today.plusDays(3)
                    val expiring = listings.filter { listing ->
                        runCatching {
                            val date = LocalDate.parse(listing.expiryDate.take(10))
                            !date.isBefore(today) && !date.isAfter(cutoff)
                        }.getOrDefault(false)
                    }
                    _state.value = _state.value.copy(
                        nearbyExpiringListings = expiring,
                        availableListings = listings
                    )
                }
            } catch (_: Exception) {}
        }
    }

    // Legacy functions kept for compatibility
    fun loadUser() { viewModelScope.launch { getCurrentUserUseCase().first()?.let { userName.value = it.name } } }
    fun loadStats() { viewModelScope.launch { try { stats.value = organizationApi.getMyStats().data } catch (_: Exception) {} } }
}
