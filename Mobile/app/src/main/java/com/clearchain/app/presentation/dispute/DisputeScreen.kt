package com.clearchain.app.presentation.dispute

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.DisputeApi
import com.clearchain.app.presentation.components.DetailTopBar
import com.clearchain.app.util.HapticUtils
import com.clearchain.app.util.UiEvent
import okhttp3.RequestBody.Companion.toRequestBody
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══ State ═══
data class DisputeState(
    val reason: String = "",
    val statement: String = "",
    val reasonError: String? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null
)

// ═══ ViewModel ═══
@HiltViewModel
class DisputeViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    savedStateHandle: SavedStateHandle,
    private val disputeApi: DisputeApi
) : ViewModel() {

    val pickupRequestId: String = savedStateHandle["pickupRequestId"] ?: ""

    private val _state = MutableStateFlow(DisputeState())
    val state: StateFlow<DisputeState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onReasonChanged(reason: String) = _state.update { it.copy(reason = reason, reasonError = null) }
    fun onStatementChanged(stmt: String) = _state.update { it.copy(statement = stmt) }

    fun submit() {
        val s = _state.value
        if (s.reason.isBlank()) {
            _state.update { it.copy(reasonError = context.getString(R.string.error_select_reason)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            try {
                disputeApi.openDispute(
                    pickupRequestId = pickupRequestId.toRequestBody(),
                    reason = s.reason.toRequestBody(),
                    statement = s.statement.ifBlank { null }?.toRequestBody(),
                    photo = null
                )
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_dispute_submitted)))
                _uiEvent.send(UiEvent.NavigateUp)
            } catch (e: Exception) {
                _state.update { it.copy(isSubmitting = false, error = e.message ?: "Failed to submit dispute") }
            }
        }
    }
}


// ═══ Screen ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisputeScreen(
    onNavigateBack: () -> Unit,
    viewModel: DisputeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val disputeReasons = listOf(
        stringResource(R.string.dispute_reason_poor_condition),
        stringResource(R.string.dispute_reason_wrong_items),
        stringResource(R.string.dispute_reason_quantity),
        stringResource(R.string.dispute_reason_expired),
        stringResource(R.string.dispute_reason_not_available),
        stringResource(R.string.dispute_reason_other)
    )

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                UiEvent.NavigateUp -> onNavigateBack()
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        stringResource(R.string.dispute_info_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Reason selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.dispute_reason_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                disputeReasons.forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.reason == reason,
                            onClick = { HapticUtils.tick(context); viewModel.onReasonChanged(reason) }
                        )
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                state.reasonError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            // Statement
            OutlinedTextField(
                value = state.statement,
                onValueChange = viewModel::onStatementChanged,
                label = { Text(stringResource(R.string.dispute_statement_label)) },
                placeholder = { Text(stringResource(R.string.dispute_statement_hint)) },
                singleLine = false,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { HapticUtils.confirm(context); viewModel.submit() },
                enabled = !state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Flag, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.dispute_submit), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
