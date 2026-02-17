package com.clearchain.app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.domain.model.AdminStats
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminApi: AdminApi,
    val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardState())
    val state: StateFlow<AdminDashboardState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadStats()
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