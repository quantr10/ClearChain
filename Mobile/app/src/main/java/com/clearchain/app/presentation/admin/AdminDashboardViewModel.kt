package com.clearchain.app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.signalr.ConnectionState
import com.clearchain.app.data.remote.signalr.SignalRService  // ✅ ADD
import com.clearchain.app.domain.model.AdminStats
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminApi: AdminApi,
    val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val signalRService: SignalRService  // ✅ ADD
) : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardState())
    val state: StateFlow<AdminDashboardState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadStats()
        setupSignalR()  // ✅ ADD
    }

    // ✅ NEW: Setup SignalR real-time updates
    private fun setupSignalR() {
        // Connect to SignalR (reuse existing connection)
        viewModelScope.launch {
            signalRService.connect()
        }

        // Listen for connection state
        viewModelScope.launch {
            signalRService.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        _uiEvent.send(UiEvent.ShowSnackbar("✅ Real-time monitoring enabled"))
                    }
                    is ConnectionState.Error -> {
                        // Silent fail - dashboard works without real-time
                    }
                    else -> {}
                }
            }
        }

        // ✅ Listen for new organization registrations
        viewModelScope.launch {
            signalRService.newOrganizationRegistered.collect { notification ->
                // Add to recent activities
                val activity = AdminActivity(
                    id = UUID.randomUUID().toString(),
                    type = AdminActivityType.NEW_ORGANIZATION,
                    title = "New ${notification.type} registered",
                    description = "${notification.name} from ${notification.location}",
                    timestamp = notification.registeredAt,
                    icon = if (notification.type.equals("NGO", ignoreCase = true)) "🏢" else "🏪"
                )

                _state.update { currentState ->
                    currentState.copy(
                        recentActivities = listOf(activity) + currentState.recentActivities.take(19) // Keep last 20
                    )
                }

                // Refresh stats to show new organization
                loadStats()

                _uiEvent.send(
                    UiEvent.ShowSnackbar("📢 New ${notification.type}: ${notification.name}")
                )
            }
        }

        // ✅ Listen for completed transactions
        viewModelScope.launch {
            signalRService.transactionCompleted.collect { notification ->
                // Add to recent activities
                val activity = AdminActivity(
                    id = UUID.randomUUID().toString(),
                    type = AdminActivityType.TRANSACTION_COMPLETED,
                    title = "Transaction completed",
                    description = "${notification.ngoName} received ${notification.quantity} ${notification.unit} ${notification.productName} from ${notification.groceryName}",
                    timestamp = notification.completedAt,
                    icon = "✅"
                )

                _state.update { currentState ->
                    currentState.copy(
                        recentActivities = listOf(activity) + currentState.recentActivities.take(19)
                    )
                }

                // Refresh stats to show new completion
                loadStats()

                _uiEvent.send(
                    UiEvent.ShowSnackbar("✅ Transaction: ${notification.productName}")
                )
            }
        }

        // ✅ Listen for stats updates
        viewModelScope.launch {
            signalRService.statsUpdated.collect { stats ->
                // Update stats in state
                val adminStats = AdminStats(
                    totalOrganizations = stats.totalNGOs + stats.totalGroceries,
                    totalGroceries = stats.totalGroceries,
                    totalNgos = stats.totalNGOs,
                    verifiedOrganizations = 0, // Not provided in notification
                    unverifiedOrganizations = 0,
                    totalListings = stats.activeListings,
                    activeListings = stats.activeListings,
                    reservedListings = 0,
                    totalPickupRequests = stats.totalDonations,
                    pendingRequests = stats.pendingRequests,
                    approvedRequests = 0,
                    readyRequests = 0,
                    rejectedRequests = 0,
                    completedRequests = stats.completedToday,
                    cancelledRequests = 0,
                    totalFoodSaved = stats.totalDonations.toDouble()
                )

                _state.update { currentState ->
                    currentState.copy(stats = adminStats)
                }

                // Add to activities
                val activity = AdminActivity(
                    id = UUID.randomUUID().toString(),
                    type = AdminActivityType.STATS_UPDATED,
                    title = "Platform stats updated",
                    description = "${stats.totalDonations} total donations, ${stats.completedToday} completed today",
                    timestamp = stats.updatedAt,
                    icon = "📊"
                )

                _state.update { currentState ->
                    currentState.copy(
                        recentActivities = listOf(activity) + currentState.recentActivities.take(19)
                    )
                }
            }
        }

        // ✅ Listen for system alerts
        viewModelScope.launch {
            signalRService.systemAlert.collect { alert ->
                val alertLevel = when (alert.level.lowercase()) {
                    "error" -> AlertLevel.ERROR
                    "warning" -> AlertLevel.WARNING
                    else -> AlertLevel.INFO
                }

                val systemAlert = SystemAlert(
                    id = UUID.randomUUID().toString(),
                    level = alertLevel,
                    message = alert.message,
                    details = alert.details,
                    timestamp = alert.timestamp
                )

                _state.update { currentState ->
                    currentState.copy(
                        recentAlerts = listOf(systemAlert) + currentState.recentAlerts.take(9) // Keep last 10
                    )
                }

                val icon = when (alertLevel) {
                    AlertLevel.ERROR -> "🚨"
                    AlertLevel.WARNING -> "⚠️"
                    AlertLevel.INFO -> "ℹ️"
                }

                _uiEvent.send(
                    UiEvent.ShowSnackbar("$icon ${alert.message}")
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            signalRService.disconnect()
        }
    }

    fun onEvent(event: AdminDashboardEvent) {
        when (event) {
            AdminDashboardEvent.LoadStats -> loadStats()
            AdminDashboardEvent.RefreshStats -> refreshStats()
            AdminDashboardEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val statsResponse = adminApi.getStatistics()
                val stats = statsResponse.data.let {
                    AdminStats(
                        totalOrganizations = it.totalOrganizations,
                        totalGroceries = it.totalGroceries,
                        totalNgos = it.totalNgos,
                        verifiedOrganizations = it.verifiedOrganizations,
                        unverifiedOrganizations = it.unverifiedOrganizations,
                        totalListings = it.totalListings,
                        activeListings = it.activeListings,
                        reservedListings = it.reservedListings,
                        totalPickupRequests = it.totalPickupRequests,
                        pendingRequests = it.pendingRequests,
                        approvedRequests = it.approvedRequests,
                        readyRequests = it.readyRequests,
                        rejectedRequests = it.rejectedRequests,
                        completedRequests = it.completedRequests,
                        cancelledRequests = it.cancelledRequests,
                        totalFoodSaved = it.totalFoodSaved
                    )
                }

                _state.update {
                    it.copy(
                        stats = stats,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to load statistics",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun refreshStats() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

            try {
                val statsResponse = adminApi.getStatistics()
                val stats = statsResponse.data.let {
                    AdminStats(
                        totalOrganizations = it.totalOrganizations,
                        totalGroceries = it.totalGroceries,
                        totalNgos = it.totalNgos,
                        verifiedOrganizations = it.verifiedOrganizations,
                        unverifiedOrganizations = it.unverifiedOrganizations,
                        totalListings = it.totalListings,
                        activeListings = it.activeListings,
                        reservedListings = it.reservedListings,
                        totalPickupRequests = it.totalPickupRequests,
                        pendingRequests = it.pendingRequests,
                        approvedRequests = it.approvedRequests,
                        readyRequests = it.readyRequests,
                        rejectedRequests = it.rejectedRequests,
                        completedRequests = it.completedRequests,
                        cancelledRequests = it.cancelledRequests,
                        totalFoodSaved = it.totalFoodSaved
                    )
                }

                _state.update {
                    it.copy(
                        stats = stats,
                        isRefreshing = false
                    )
                }
                _uiEvent.send(UiEvent.ShowSnackbar("Statistics refreshed"))
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to refresh statistics",
                        isRefreshing = false
                    )
                }
            }
        }
    }
}