package com.clearchain.app.presentation.admin.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.signalr.ConnectionState
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerificationQueueViewModel @Inject constructor(
    private val adminApi: AdminApi,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationQueueState())
    val state: StateFlow<VerificationQueueState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadOrganizations()
        setupSignalR()
    }

    private fun setupSignalR() {
        viewModelScope.launch {
            try {
                signalRService.connect()
            } catch (e: Exception) {
                return@launch
            }

            launch {
                signalRService.connectionState.collect { state ->
                    when (state) {
                        is ConnectionState.Connected -> {
                            _uiEvent.send(UiEvent.ShowSnackbar("✅ Real-time monitoring enabled"))
                        }
                        else -> {}
                    }
                }
            }

            launch {
                signalRService.newOrganizationRegistered.collect { notification ->
                    loadOrganizations()
                    _uiEvent.send(
                        UiEvent.ShowSnackbar("📢 New ${notification.type} registered: ${notification.name}")
                    )
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

    fun onEvent(event: VerificationQueueEvent) {
        when (event) {
            VerificationQueueEvent.LoadOrganizations -> loadOrganizations()
            VerificationQueueEvent.RefreshOrganizations -> refreshOrganizations()
            VerificationQueueEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun loadOrganizations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val orgsResponse = adminApi.getAllOrganizations()
                val organizations = orgsResponse.data.map { dto ->
                    Organization(
                        id = dto.id,
                        name = dto.name,
                        type = when (dto.type.uppercase()) {
                            "GROCERY" -> OrganizationType.GROCERY
                            "NGO" -> OrganizationType.NGO
                            "ADMIN" -> OrganizationType.ADMIN
                            else -> OrganizationType.NGO
                        },
                        email = dto.email,
                        phone = dto.phone,
                        address = dto.address,
                        location = dto.location,
                        verified = dto.verified,
                        verificationStatus = when (dto.verificationStatus.uppercase()) {
                            "APPROVED", "VERIFIED" -> VerificationStatus.APPROVED
                            "REJECTED" -> VerificationStatus.REJECTED
                            else -> VerificationStatus.PENDING
                        },
                        hours = null,
                        profilePictureUrl = null,
                        createdAt = dto.createdAt
                    )
                }

                _state.update {
                    it.copy(
                        organizations = organizations,
                        // ✅ REMOVED: unverifiedOrganizations field
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to load organizations",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun refreshOrganizations() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

            try {
                val orgsResponse = adminApi.getAllOrganizations()
                val organizations = orgsResponse.data.map { dto ->
                    Organization(
                        id = dto.id,
                        name = dto.name,
                        type = when (dto.type.uppercase()) {
                            "GROCERY" -> OrganizationType.GROCERY
                            "NGO" -> OrganizationType.NGO
                            "ADMIN" -> OrganizationType.ADMIN
                            else -> OrganizationType.NGO
                        },
                        email = dto.email,
                        phone = dto.phone,
                        address = dto.address,
                        location = dto.location,
                        verified = dto.verified,
                        verificationStatus = when (dto.verificationStatus.uppercase()) {
                            "APPROVED", "VERIFIED" -> VerificationStatus.APPROVED
                            "REJECTED" -> VerificationStatus.REJECTED
                            else -> VerificationStatus.PENDING
                        },
                        hours = null,
                        profilePictureUrl = null,
                        createdAt = dto.createdAt
                    )
                }

                _state.update {
                    it.copy(
                        organizations = organizations,
                        // ✅ REMOVED: unverifiedOrganizations field
                        isRefreshing = false
                    )
                }
                _uiEvent.send(UiEvent.ShowSnackbar("Organizations refreshed"))
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to refresh organizations",
                        isRefreshing = false
                    )
                }
            }
        }
    }
}