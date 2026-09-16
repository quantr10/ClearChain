package com.clearchain.app.presentation.admin.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.AdminStats
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adminApi: AdminApi,
    val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardState())
    val state: StateFlow<AdminDashboardState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadStats()
        setupSignalR()
    }

    private fun setupSignalR() {
        // Connection state is surfaced app-wide by ReconnectingBanner in MainActivity — this
        // screen only listens for the events themselves.
        viewModelScope.launch {
            signalRService.newOrganizationRegistered.collect { notification ->
                val activity = AdminActivity(
                    id          = UUID.randomUUID().toString(),
                    type        = AdminActivityType.NEW_ORGANIZATION,
                    title       = context.getString(R.string.activity_title_new_org, notification.type),
                    description = context.getString(R.string.activity_desc_new_org, notification.name, notification.location),
                    timestamp   = notification.registeredAt,
                    icon        = if (notification.type.equals("NGO", ignoreCase = true)) "🏢" else "🏪"
                )
                _state.update { it.copy(recentActivities = listOf(activity) + it.recentActivities.take(19)) }
                loadStats()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_new_admin_notification, notification.type, notification.name)))
            }
        }

        viewModelScope.launch {
            signalRService.transactionCompleted.collect { notification ->
                val activity = AdminActivity(
                    id          = UUID.randomUUID().toString(),
                    type        = AdminActivityType.TRANSACTION_COMPLETED,
                    title       = context.getString(R.string.activity_title_transaction),
                    description = context.getString(R.string.activity_desc_transaction, notification.ngoName, notification.quantity.toString(), notification.unit, notification.groceryName),
                    timestamp   = notification.completedAt,
                    icon        = "✅"
                )
                _state.update { it.copy(recentActivities = listOf(activity) + it.recentActivities.take(19)) }
                loadStats()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_new_transaction, notification.productName)))
            }
        }

        viewModelScope.launch {
            signalRService.statsUpdated.collect { stats ->
                val adminStats = AdminStats(
                    totalOrganizations    = stats.totalNGOs + stats.totalGroceries,
                    totalGroceries        = stats.totalGroceries,
                    totalNgos             = stats.totalNGOs,
                    verifiedOrganizations = 0,
                    unverifiedOrganizations = 0,
                    totalListings         = stats.activeListings,
                    activeListings        = stats.activeListings,
                    reservedListings      = 0,
                    expiredListings       = 0,
                    totalPickupRequests   = stats.totalDonations,
                    pendingRequests       = stats.pendingRequests,
                    approvedRequests      = 0,
                    readyRequests         = 0,
                    rejectedRequests      = 0,
                    completedRequests     = stats.completedToday,
                    cancelledRequests     = 0,
                    totalFoodSaved        = stats.totalDonations.toDouble()
                )
                val activity = AdminActivity(
                    id          = UUID.randomUUID().toString(),
                    type        = AdminActivityType.STATS_UPDATED,
                    title       = context.getString(R.string.activity_title_stats_updated),
                    description = context.getString(R.string.activity_desc_stats, stats.totalDonations.toString(), stats.completedToday.toString()),
                    timestamp   = stats.updatedAt,
                    icon        = "📊"
                )
                _state.update {
                    it.copy(
                        stats            = adminStats,
                        recentActivities = listOf(activity) + it.recentActivities.take(19)
                    )
                }
            }
        }

        viewModelScope.launch {
            signalRService.systemAlert.collect { alert ->
                val level = when (alert.level.lowercase()) {
                    "error"   -> AlertLevel.ERROR
                    "warning" -> AlertLevel.WARNING
                    else      -> AlertLevel.INFO
                }
                val systemAlert = SystemAlert(
                    id        = UUID.randomUUID().toString(),
                    level     = level,
                    message   = alert.message,
                    details   = alert.details,
                    timestamp = alert.timestamp
                )
                _state.update { it.copy(recentAlerts = listOf(systemAlert) + it.recentAlerts.take(9)) }
                _uiEvent.send(UiEvent.ShowSnackbar(alert.message))
            }
        }
    }

    fun onEvent(event: AdminDashboardEvent) {
        when (event) {
            AdminDashboardEvent.LoadStats    -> loadStats()
            AdminDashboardEvent.RefreshStats -> refreshStats()
            AdminDashboardEvent.ClearError   -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loadAll()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun refreshStats() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            val ok = loadAll()
            _state.update { it.copy(isRefreshing = false) }
            if (ok) _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_stats_refreshed)))
        }
    }

    /** Reloads every dashboard section concurrently and suspends until all have settled,
     *  so pull-to-refresh keeps its spinner until the statistics, alert feed and user
     *  growth chart are actually up to date. Returns whether the headline statistics
     *  loaded — the alert feed and growth chart are auxiliary and fail silently.
     *
     *  Every section writes through [MutableStateFlow.update]: these loaders run in
     *  parallel and each one only owns a few fields, so a plain `_state.value = ... .copy()`
     *  would read a snapshot, then overwrite whatever a sibling wrote in the meantime. */
    private suspend fun loadAll(): Boolean = coroutineScope {
        val statsOk = async { loadStatistics() }
        launch { loadAlertFeed() }
        statsOk.await()
    }

    private suspend fun loadStatistics(): Boolean =
        try {
            val stats = adminApi.getStatistics().data.toDomain()
            _state.update { it.copy(stats = stats) }
            true
        } catch (e: Exception) {
            val msg = e.message ?: context.getString(R.string.error_load_statistics)
            _state.update { it.copy(error = msg) }
            _uiEvent.send(UiEvent.ShowSnackbar(msg))
            false
        }

    private suspend fun loadAlertFeed() {
        try {
            val alerts = adminApi.getAlertFeed().data
            _state.update { it.copy(alertFeedItems = alerts) }
        } catch (_: Exception) {}
    }
}
