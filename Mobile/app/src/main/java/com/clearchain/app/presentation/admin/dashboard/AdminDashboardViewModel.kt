package com.clearchain.app.presentation.admin.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.signalr.ConnectionState
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.AdminStats
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
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
        viewModelScope.launch { signalRService.connect() }

        viewModelScope.launch {
            signalRService.connectionState.collect { connectionState ->
                if (connectionState is ConnectionState.Error) {
                    // Silent fail — dashboard works without real-time
                }
            }
        }

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

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { signalRService.disconnect() }
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
            try {
                val statsResponse = adminApi.getStatistics()
                val stats = statsResponse.data.let {
                    AdminStats(
                        totalOrganizations    = it.totalOrganizations,
                        totalGroceries        = it.totalGroceries,
                        totalNgos             = it.totalNgos,
                        verifiedOrganizations = it.verifiedOrganizations,
                        unverifiedOrganizations = it.unverifiedOrganizations,
                        totalListings         = it.totalListings,
                        activeListings        = it.activeListings,
                        reservedListings      = it.reservedListings,
                        totalPickupRequests   = it.totalPickupRequests,
                        pendingRequests       = it.pendingRequests,
                        approvedRequests      = it.approvedRequests,
                        readyRequests         = it.readyRequests,
                        rejectedRequests      = it.rejectedRequests,
                        completedRequests     = it.completedRequests,
                        cancelledRequests     = it.cancelledRequests,
                        totalFoodSaved        = it.totalFoodSaved
                    )
                }
                _state.update { it.copy(stats = stats, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: context.getString(R.string.error_load_statistics), isLoading = false) }
            }
        }
        // Load auxiliary data concurrently (silently ignore failures)
        viewModelScope.launch {
            try {
                val health = adminApi.getSystemHealth()
                _state.update { it.copy(healthData = health.data) }
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                val alerts = adminApi.getAlertFeed()
                _state.update { it.copy(alertFeedItems = alerts.data) }
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                val growth = adminApi.getUserGrowth(days = 30)
                _state.update { it.copy(userGrowthData = growth.data) }
            } catch (_: Exception) {}
        }
    }

    private fun refreshStats() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            try {
                val statsResponse = adminApi.getStatistics()
                val stats = statsResponse.data.let {
                    AdminStats(
                        totalOrganizations    = it.totalOrganizations,
                        totalGroceries        = it.totalGroceries,
                        totalNgos             = it.totalNgos,
                        verifiedOrganizations = it.verifiedOrganizations,
                        unverifiedOrganizations = it.unverifiedOrganizations,
                        totalListings         = it.totalListings,
                        activeListings        = it.activeListings,
                        reservedListings      = it.reservedListings,
                        totalPickupRequests   = it.totalPickupRequests,
                        pendingRequests       = it.pendingRequests,
                        approvedRequests      = it.approvedRequests,
                        readyRequests         = it.readyRequests,
                        rejectedRequests      = it.rejectedRequests,
                        completedRequests     = it.completedRequests,
                        cancelledRequests     = it.cancelledRequests,
                        totalFoodSaved        = it.totalFoodSaved
                    )
                }
                _state.update { it.copy(stats = stats, isRefreshing = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_stats_refreshed)))
                // Also refresh aux data
                try { val h = adminApi.getSystemHealth(); _state.update { it.copy(healthData = h.data) } } catch (_: Exception) {}
                try { val a = adminApi.getAlertFeed(); _state.update { it.copy(alertFeedItems = a.data) } } catch (_: Exception) {}
                try { val g = adminApi.getUserGrowth(30); _state.update { it.copy(userGrowthData = g.data) } } catch (_: Exception) {}
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: context.getString(R.string.error_refresh_failed), isRefreshing = false) }
            }
        }
    }
}
