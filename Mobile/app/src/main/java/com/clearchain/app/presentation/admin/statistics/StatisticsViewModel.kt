package com.clearchain.app.presentation.admin.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.signalr.ConnectionState
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.AdminStats
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val adminApi: AdminApi,
    private val signalRService: SignalRService,  // ✅ ADD
    private val getCurrentUserUseCase: GetCurrentUserUseCase  // ✅ ADD
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadStatistics()
        setupSignalR()  // ✅ ADD
    }

    // ✅ NEW: Setup SignalR real-time updates
    private fun setupSignalR() {
        viewModelScope.launch {
            try {
                signalRService.connect()  // ✅ Use regular connect()
            } catch (e: Exception) {
                return@launch
            }

            // Listen for connection state
            launch {
                signalRService.connectionState.collect { state ->
                    when (state) {
                        is ConnectionState.Connected -> {
                            _uiEvent.send(UiEvent.ShowSnackbar("✅ Real-time stats enabled"))
                        }
                        else -> {}
                    }
                }
            }

            // ✅ Listen for stats updates (from backend)
            launch {
                signalRService.statsUpdated.collect { stats ->
                    val adminStats = AdminStats(
                        totalOrganizations = stats.totalNGOs + stats.totalGroceries,
                        totalGroceries = stats.totalGroceries,
                        totalNgos = stats.totalNGOs,
                        verifiedOrganizations = 0,
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

                    _state.update { it.copy(stats = adminStats) }
                    _uiEvent.send(UiEvent.ShowSnackbar("📊 Stats updated"))
                }
            }

            // ✅ Listen for events that affect stats
            launch {
                signalRService.pickupRequestCreated.collect {
                    loadStatistics()
                }
            }

            launch {
                signalRService.transactionCompleted.collect {
                    loadStatistics()
                    _uiEvent.send(UiEvent.ShowSnackbar("✅ Transaction completed"))
                }
            }

            launch {
                signalRService.listingCreated.collect {
                    loadStatistics()
                }
            }

            launch {
                signalRService.newOrganizationRegistered.collect {
                    loadStatistics()
                    _uiEvent.send(UiEvent.ShowSnackbar("📢 New organization registered"))
                }
            }

            launch {
                signalRService.pickupRequestCancelled.collect {
                    loadStatistics()
                }
            }

            launch {
                signalRService.listingDeleted.collect {
                    loadStatistics()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            signalRService.disconnect()
        }
    }

    fun onEvent(event: StatisticsEvent) {
        when (event) {
            StatisticsEvent.LoadStatistics -> loadStatistics()
            StatisticsEvent.RefreshStatistics -> refreshStatistics()
            StatisticsEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun loadStatistics() {
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

    private fun refreshStatistics() {
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