package com.clearchain.app.presentation.grocery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.dto.ActivityItemData
import com.clearchain.app.data.remote.dto.DashboardStatsData
import com.clearchain.app.data.remote.dto.TodaySummaryData
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroceryDashboardState(
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

    private val _state = MutableStateFlow(
        GroceryDashboardState(userName = context.getString(R.string.label_grocery_store_name))
    )
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
     *  so pull-to-refresh keeps its spinner until the stats, activity, and today's summary
     *  are actually up to date.
     *
     *  Every section writes through [MutableStateFlow.update]: these loaders run in
     *  parallel and each one only owns a few fields, so a plain `_state.value = ... .copy()`
     *  would read a snapshot, then overwrite whatever a sibling wrote in the meantime. */
    private suspend fun loadAll(): Unit = coroutineScope {
        launch { loadStats() }
        launch { loadTodaySummary() }
        launch { loadActivity() }
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
            } catch (_: Exception) {}
        }
    }

    private suspend fun loadStats() {
        try {
            val stats = organizationApi.getMyStats().data
            _state.update { it.copy(stats = stats) }
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
}
