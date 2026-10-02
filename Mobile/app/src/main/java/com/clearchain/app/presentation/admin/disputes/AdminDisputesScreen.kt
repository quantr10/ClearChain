package com.clearchain.app.presentation.admin.disputes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.DisputeListItemData
import com.clearchain.app.data.remote.dto.DisputePartyContact
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.ui.theme.StatusColors
import com.clearchain.app.util.UiEvent
import com.clearchain.app.util.dialPhone
import com.clearchain.app.util.sendEmail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDisputesScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminDisputesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                else -> {}
            }
        }
    }

    state.showResolveDialogForId?.let { disputeId ->
        state.disputes.firstOrNull { it.id == disputeId }?.let { dispute ->
            ResolveDisputeDialog(state = state, dispute = dispute, onEvent = viewModel::onEvent)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ListScreenHeader {
                Text(
                    stringResource(R.string.disputes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                FilterChipsRow(
                    filters = state.availableStatusFilters,
                    selectedFilter = state.selectedStatus,
                    onFilterSelected = { viewModel.onEvent(AdminDisputesEvent.StatusFilterChanged(it)) }
                )
            }

            HapticPullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.onEvent(AdminDisputesEvent.RefreshDisputes) }
            ) {
                LazyColumn(
                    contentPadding = ScreenPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isLoading && state.disputes.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (state.disputes.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Gavel,
                                title = stringResource(R.string.empty_no_disputes),
                                subtitle = stringResource(R.string.empty_no_disputes_subtitle),
                                modifier = Modifier.fillParentMaxSize()
                            )
                        }
                    } else {
                        items(state.disputes, key = { it.id }) { dispute ->
                            DisputeCard(
                                dispute = dispute,
                                isExpanded = state.expandedDisputeId == dispute.id,
                                onToggleExpand = { viewModel.onEvent(AdminDisputesEvent.ToggleExpanded(dispute.id)) },
                                onResolve = { viewModel.onEvent(AdminDisputesEvent.ShowResolveDialog(dispute.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class DisputeBadgeStyle(val bg: Color, val onBg: Color, val labelResId: Int)

private fun disputeBadgeStyle(status: String): DisputeBadgeStyle = when (status) {
    "open" -> DisputeBadgeStyle(StatusColors.PendingBg, StatusColors.PendingOnBg, R.string.dispute_status_open)
    "under_review" -> DisputeBadgeStyle(StatusColors.ReservedBg, StatusColors.ReservedOnBg, R.string.dispute_status_under_review)
    "dismissed" -> DisputeBadgeStyle(StatusColors.ExpiredBg, StatusColors.ExpiredOnBg, R.string.dispute_status_dismissed)
    else -> DisputeBadgeStyle(StatusColors.AvailableBg, StatusColors.AvailableOnBg, R.string.dispute_status_resolved)
}

@Composable
private fun DisputeCard(
    dispute: DisputeListItemData,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onResolve: () -> Unit
) {
    val isOpen = dispute.status == "open" || dispute.status == "under_review"
    val badge = disputeBadgeStyle(dispute.status)

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.clickable(onClick = onToggleExpand).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(dispute.listingTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.dispute_parties, dispute.ngo.name, dispute.grocery.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(stringResource(badge.labelResId), badge.bg, badge.onBg)
            }

            Spacer(Modifier.height(8.dp))
            Text(dispute.reason, style = MaterialTheme.typography.bodyMedium)

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    DisputeContactRow(label = stringResource(R.string.dispute_ngo_contact), contact = dispute.ngo)
                    Spacer(Modifier.height(8.dp))
                    DisputeContactRow(label = stringResource(R.string.dispute_grocery_contact), contact = dispute.grocery)

                    dispute.ngoStatement?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(12.dp))
                        DisputeDetailLine(stringResource(R.string.dispute_ngo_statement), it)
                    }
                    dispute.groceryStatement?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(8.dp))
                        DisputeDetailLine(stringResource(R.string.dispute_grocery_statement), it)
                    }
                    dispute.adminResolution?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(8.dp))
                        DisputeDetailLine(stringResource(R.string.dispute_admin_resolution), it)
                    }

                    dispute.photoEvidenceUrl?.let { url ->
                        Spacer(Modifier.height(12.dp))
                        AsyncImage(
                            model = url,
                            contentDescription = stringResource(R.string.dispute_photo_evidence),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    if (isOpen) {
                        Spacer(Modifier.height(12.dp))
                        ClearChainOutlinedButton(
                            text = stringResource(R.string.dispute_mark_resolved),
                            icon = Icons.Default.CheckCircle,
                            onClick = onResolve,
                            fillMaxWidth = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisputeContactRow(label: String, contact: DisputePartyContact) {
    val context = LocalContext.current
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(contact.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            IconButton(onClick = { sendEmail(context, contact.email) }) {
                Icon(Icons.Default.Email, contentDescription = stringResource(R.string.action_send_email), modifier = Modifier.size(20.dp))
            }
            contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                IconButton(onClick = { dialPhone(context, phone) }) {
                    Icon(Icons.Default.Phone, contentDescription = stringResource(R.string.action_call), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun DisputeDetailLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ResolveDisputeDialog(
    state: AdminDisputesState,
    dispute: DisputeListItemData,
    onEvent: (AdminDisputesEvent) -> Unit
) {
    val outcomes = listOf(
        "resolved_ngo" to stringResource(R.string.dispute_outcome_ngo),
        "resolved_grocery" to stringResource(R.string.dispute_outcome_grocery),
        "dismissed" to stringResource(R.string.dispute_outcome_dismissed)
    )

    ConfirmDialog(
        onDismiss = { onEvent(AdminDisputesEvent.DismissResolveDialog) },
        icon = Icons.Default.Gavel,
        title = stringResource(R.string.dispute_resolve_title),
        confirmLabel = stringResource(R.string.dispute_mark_resolved),
        dismissLabel = stringResource(R.string.cancel),
        confirmEnabled = !state.isResolving,
        confirmLoading = state.isResolving,
        onConfirm = { onEvent(AdminDisputesEvent.ConfirmResolve) }
    ) {
        Text(stringResource(R.string.dispute_resolve_outcome_label), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            outcomes.forEach { (value, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEvent(AdminDisputesEvent.OutcomeChanged(value)) }
                ) {
                    RadioButton(selected = state.resolveOutcome == value, onClick = { onEvent(AdminDisputesEvent.OutcomeChanged(value)) })
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        ClearChainTextField(
            value = state.resolveGroceryStatement,
            onValueChange = { onEvent(AdminDisputesEvent.GroceryStatementChanged(it)) },
            label = stringResource(R.string.dispute_grocery_statement),
            isOptional = true,
            placeholder = stringResource(R.string.dispute_grocery_statement_placeholder),
            singleLine = false,
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        ClearChainTextField(
            value = state.resolveNote,
            onValueChange = { onEvent(AdminDisputesEvent.ResolveNoteChanged(it)) },
            label = stringResource(R.string.dispute_resolution_note),
            placeholder = stringResource(R.string.dispute_resolution_note_placeholder),
            singleLine = false,
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
