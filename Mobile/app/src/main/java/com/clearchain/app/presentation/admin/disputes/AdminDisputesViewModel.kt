package com.clearchain.app.presentation.admin.disputes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.DisputeApi
import com.clearchain.app.data.remote.dto.ResolveDisputeRequest
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Disputes have no in-app negotiation between the NGO and the grocery — an admin calls or
 * emails both sides using the contact details this screen surfaces, then records the outcome
 * here with [resolveDispute] so the dispute drops out of the open queue.
 */
@HiltViewModel
class AdminDisputesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val disputeApi: DisputeApi
) : ViewModel() {

    private val _state = MutableStateFlow(AdminDisputesState())
    val state: StateFlow<AdminDisputesState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadDisputes()
    }

    fun onEvent(event: AdminDisputesEvent) {
        when (event) {
            AdminDisputesEvent.LoadDisputes -> loadDisputes()
            AdminDisputesEvent.RefreshDisputes -> refreshDisputes()
            AdminDisputesEvent.ClearError -> _state.update { it.copy(error = null) }

            is AdminDisputesEvent.StatusFilterChanged -> {
                _state.update { it.copy(selectedStatus = event.status) }
                loadDisputes()
            }

            is AdminDisputesEvent.ToggleExpanded -> _state.update {
                it.copy(expandedDisputeId = if (it.expandedDisputeId == event.disputeId) null else event.disputeId)
            }

            is AdminDisputesEvent.ShowResolveDialog -> _state.update {
                it.copy(
                    showResolveDialogForId = event.disputeId,
                    resolveOutcome = "resolved_ngo",
                    resolveGroceryStatement = "",
                    resolveNote = ""
                )
            }
            AdminDisputesEvent.DismissResolveDialog -> _state.update { it.copy(showResolveDialogForId = null) }
            is AdminDisputesEvent.OutcomeChanged -> _state.update { it.copy(resolveOutcome = event.outcome) }
            is AdminDisputesEvent.GroceryStatementChanged -> _state.update { it.copy(resolveGroceryStatement = event.text) }
            is AdminDisputesEvent.ResolveNoteChanged -> _state.update { it.copy(resolveNote = event.text) }
            AdminDisputesEvent.ConfirmResolve -> resolveDispute()
        }
    }

    private fun loadDisputes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = disputeApi.getDisputes(_state.value.selectedStatus)
                _state.update { it.copy(disputes = response.data, isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message ?: context.getString(R.string.error_load_disputes), isLoading = false)
                }
            }
        }
    }

    private fun refreshDisputes() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            try {
                val response = disputeApi.getDisputes(_state.value.selectedStatus)
                _state.update { it.copy(disputes = response.data, isRefreshing = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message ?: context.getString(R.string.error_load_disputes), isRefreshing = false)
                }
            }
        }
    }

    private fun resolveDispute() {
        val id = _state.value.showResolveDialogForId ?: return
        val note = _state.value.resolveNote.trim()
        if (note.isBlank()) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.error_resolution_note_required)))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isResolving = true) }
            try {
                disputeApi.resolveDispute(
                    id,
                    ResolveDisputeRequest(
                        status = _state.value.resolveOutcome,
                        groceryStatement = _state.value.resolveGroceryStatement.trim().ifBlank { null },
                        adminResolution = note
                    )
                )
                _state.update { it.copy(showResolveDialogForId = null, isResolving = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_dispute_resolved)))
                loadDisputes()
            } catch (e: Exception) {
                _state.update { it.copy(isResolving = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_resolve_dispute_failed)))
            }
        }
    }
}
