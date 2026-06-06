package com.clearchain.app.presentation.admin.verification

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VerificationQueueScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPublicProfile: (String) -> Unit = {},
    viewModel: VerificationQueueViewModel = hiltViewModel()
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

    // Approval checklist dialog
    state.showChecklistForId?.let {
        ApprovalChecklistDialog(
            checkedItems    = state.checkedItems,
            onToggle        = { idx -> viewModel.onEvent(VerificationQueueEvent.ToggleChecklistItem(idx)) },
            onConfirm       = { viewModel.onEvent(VerificationQueueEvent.ConfirmApprove) },
            onDismiss       = { viewModel.onEvent(VerificationQueueEvent.DismissChecklist) },
            checklistComplete = state.checklistComplete
        )
    }

    // Rejection dialog
    state.showRejectDialogForId?.let {
        RejectOrgDialog(
            reason          = state.rejectionReason,
            onReasonChange  = { viewModel.onEvent(VerificationQueueEvent.RejectionReasonChanged(it)) },
            onSelectTemplate = { viewModel.onEvent(VerificationQueueEvent.SelectRejectionTemplate(it)) },
            onConfirm       = { viewModel.onEvent(VerificationQueueEvent.ConfirmReject) },
            onDismiss       = { viewModel.onEvent(VerificationQueueEvent.DismissRejectDialog) }
        )
    }

    BackHandler(state.isBatchMode) { viewModel.onEvent(VerificationQueueEvent.ToggleBatchMode) }

    if (state.showFilterSheet) {
        VerificationFilterSheet(
            state     = state,
            onEvent   = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(VerificationQueueEvent.HideFilterSheet) }
        )
    }

    Scaffold(
        bottomBar = {
            if (state.isBatchMode && state.selectedOrgIds.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.selected_count, state.selectedOrgIds.size),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick  = { viewModel.onEvent(VerificationQueueEvent.BatchReject) },
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            enabled  = !state.isProcessing
                        ) {
                            Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_reject_all_batch))
                        }
                        Button(
                            onClick  = { viewModel.onEvent(VerificationQueueEvent.BatchApprove) },
                            enabled  = !state.isProcessing
                        ) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_approve_all_batch))
                        }
                    }
                }
            }
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.organizations.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.organizations.isEmpty() -> {
                    EmptyState(
                        icon     = Icons.Default.Business,
                        title    = stringResource(R.string.empty_no_organizations),
                        subtitle = stringResource(R.string.empty_no_organizations_subtitle)
                    )
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search + filter
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchBar(
                                query         = state.searchQuery,
                                onQueryChange = { viewModel.onEvent(VerificationQueueEvent.SearchQueryChanged(it)) },
                                placeholder   = stringResource(R.string.hint_search_organizations),
                                modifier      = Modifier.weight(1f)
                            )
                            BadgedBox(
                                badge = {
                                    if (state.activeFilterCount > 0) Badge { Text(state.activeFilterCount.toString()) }
                                }
                            ) {
                                ClearChainActionIconButton(
                                    icon               = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.advanced_filters),
                                    onClick            = { viewModel.onEvent(VerificationQueueEvent.ShowFilterSheet) }
                                )
                            }
                        }

                        // Status filter tabs (no counts — counts shown below)
                        val statusOptions = listOf(
                            null       to stringResource(R.string.filter_all),
                            "PENDING"  to stringResource(R.string.status_pending),
                            "APPROVED" to stringResource(R.string.status_approved),
                            "REJECTED" to stringResource(R.string.status_rejected)
                        )
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(statusOptions) { (value, label) ->
                                FilterChip(
                                    selected = state.selectedStatus == value,
                                    onClick  = { viewModel.onEvent(VerificationQueueEvent.StatusFilterChanged(value)) },
                                    label    = { Text(label) }
                                )
                            }
                        }

                        // Count summary pills below tabs
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            listOf(
                                state.organizations.size to MaterialTheme.colorScheme.onSurfaceVariant,
                                state.pendingOrgs.size   to MaterialTheme.colorScheme.secondary,
                                state.approvedOrgs.size  to BrandGreen,
                                state.rejectedOrgs.size  to MaterialTheme.colorScheme.error
                            ).zip(listOf(
                                stringResource(R.string.filter_all_count, state.organizations.size),
                                stringResource(R.string.filter_pending_count, state.pendingOrgs.size),
                                stringResource(R.string.filter_approved_count, state.approvedOrgs.size),
                                stringResource(R.string.filter_rejected_count, state.rejectedOrgs.size)
                            )).forEach { (countColor, label) ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = countColor.second.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text     = label,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        style    = MaterialTheme.typography.labelSmall,
                                        color    = countColor.second,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Batch mode selection toolbar
                        if (state.isBatchMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.verification_batch_mode_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.onEvent(VerificationQueueEvent.SelectAllVisible) }
                                ) { Text(stringResource(R.string.select_all)) }
                                TextButton(
                                    onClick = { viewModel.onEvent(VerificationQueueEvent.ClearSelection) }
                                ) { Text(stringResource(R.string.cancel)) }
                            }
                        }

                        HapticPullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh    = { viewModel.onEvent(VerificationQueueEvent.RefreshOrganizations) }
                        ) {
                            LazyColumn(
                                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (state.filteredOrgs.isEmpty()) {
                                    item {
                                        EmptyState(
                                            icon     = Icons.Default.FilterAlt,
                                            title    = stringResource(R.string.empty_no_org_category),
                                            subtitle = stringResource(R.string.empty_no_org_category_subtitle)
                                        )
                                    }
                                } else {
                                    items(state.filteredOrgs, key = { it.id }) { org ->
                                        OrganizationCard(
                                            organization = org,
                                            isProcessing = state.isProcessing,
                                            onApprove    = { viewModel.onEvent(VerificationQueueEvent.ShowChecklist(org.id)) },
                                            onReject     = { viewModel.onEvent(VerificationQueueEvent.ShowRejectDialog(org.id)) },
                                            onViewProfile = { onNavigateToPublicProfile(org.id) },
                                            isBatchMode  = state.isBatchMode,
                                            isSelected   = org.id in state.selectedOrgIds,
                                            onToggleSelect = { viewModel.onEvent(VerificationQueueEvent.ToggleOrgSelection(org.id)) },
                                            onLongClick  = {
                                                if (!state.isBatchMode) {
                                                    viewModel.onEvent(VerificationQueueEvent.ToggleBatchMode)
                                                    viewModel.onEvent(VerificationQueueEvent.ToggleOrgSelection(org.id))
                                                }
                                            }
                                        )
                                    }
                                }

                                item { Spacer(Modifier.height(16.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Advanced filter sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerificationFilterSheet(
    state: VerificationQueueState,
    onEvent: (VerificationQueueEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.advanced_filters),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (state.activeFilterCount > 0) {
                    TextButton(onClick = { onEvent(VerificationQueueEvent.ClearAdvancedFilters) }) {
                        Text(stringResource(R.string.action_clear_all))
                    }
                }
            }

            // Organization type
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.filter_org_type), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to stringResource(R.string.filter_all), "GROCERY" to "Grocery", "NGO" to "NGO")
                        .forEach { (type, label) ->
                            FilterChip(
                                selected = state.filterOrgType == type,
                                onClick  = { onEvent(VerificationQueueEvent.FilterOrgTypeChanged(type)) },
                                label    = { Text(label) }
                            )
                        }
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_apply_filters))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OrganizationCard(
    organization: Organization,
    isProcessing:  Boolean,
    onApprove:     () -> Unit,
    onReject:      () -> Unit,
    onViewProfile: () -> Unit = {},
    isBatchMode:   Boolean = false,
    isSelected:    Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick:   () -> Unit = {}
) {
    var fullPhotoUrl by remember { mutableStateOf<String?>(null) }
    if (fullPhotoUrl != null) {
        FullPhotoDialog(photoUrl = fullPhotoUrl!!, onDismiss = { fullPhotoUrl = null })
    }
    val context = LocalContext.current
    val cardClickModifier = if (isBatchMode)
        Modifier.fillMaxWidth().clickable { onToggleSelect() }
    else
        Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongClick)

    Card(
        modifier  = cardClickModifier,
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        colors    = if (isSelected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        else CardDefaults.cardColors()
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                if (isBatchMode) {
                    Checkbox(
                        checked   = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier  = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(organization.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    StatusBadge(
                        label = organization.type.name,
                        backgroundColor = when (organization.type) {
                            OrganizationType.GROCERY -> MaterialTheme.colorScheme.secondaryContainer
                            OrganizationType.NGO     -> MaterialTheme.colorScheme.tertiaryContainer
                            OrganizationType.ADMIN   -> MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = when (organization.type) {
                            OrganizationType.GROCERY -> MaterialTheme.colorScheme.onSecondaryContainer
                            OrganizationType.NGO     -> MaterialTheme.colorScheme.onTertiaryContainer
                            OrganizationType.ADMIN   -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }

                val (statusLabel, statusColor, statusIcon) = when (organization.verificationStatus) {
                    VerificationStatus.APPROVED -> Triple(stringResource(R.string.status_approved), BrandGreen, Icons.Default.CheckCircle)
                    VerificationStatus.REJECTED -> Triple(stringResource(R.string.status_rejected), MaterialTheme.colorScheme.error, Icons.Default.Cancel)
                    VerificationStatus.PENDING  -> Triple(stringResource(R.string.status_pending),  MaterialTheme.colorScheme.secondary, Icons.Default.Schedule)
                }
                StatusBadge(label = statusLabel, backgroundColor = statusColor.copy(alpha = 0.15f),
                    contentColor = statusColor, icon = statusIcon)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow(Icons.Default.Email, stringResource(R.string.label_email), organization.email)
            InfoRow(Icons.Default.Phone, stringResource(R.string.label_phone), organization.phone.ifBlank { stringResource(R.string.msg_not_provided) })
            InfoRow(Icons.Default.Place, stringResource(R.string.label_location), organization.location.ifBlank { stringResource(R.string.msg_not_provided) })

            // Document viewer
            val docs = listOfNotNull(
                organization.documentUrl?.let { stringResource(R.string.label_doc_n, 1) to it },
                organization.documentUrl2?.let { stringResource(R.string.label_doc_n, 2) to it }
            )
            if (docs.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Verification Documents",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    docs.forEach { (label, url) ->
                        val isPdf = url.endsWith(".pdf", ignoreCase = true) ||
                            organization.documentMimeType?.contains("pdf") == true
                        OutlinedButton(
                            onClick = {
                                if (isPdf) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } else {
                                    fullPhotoUrl = url
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // View Profile button (always visible)
            OutlinedButton(
                onClick  = onViewProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.cd_view_profile))
            }

            // Action buttons for PENDING orgs
            if (organization.verificationStatus == VerificationStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = onReject,
                        modifier = Modifier.weight(1f),
                        enabled  = !isProcessing,
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.reject))
                    }
                    Button(
                        onClick  = onApprove,
                        modifier = Modifier.weight(1f),
                        enabled  = !isProcessing
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.approve))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Approval checklist dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ApprovalChecklistDialog(
    checkedItems:      Set<Int>,
    onToggle:          (Int) -> Unit,
    onConfirm:         () -> Unit,
    onDismiss:         () -> Unit,
    checklistComplete: Boolean
) {
    val checklistItems = listOf(
        stringResource(R.string.verify_check_1),
        stringResource(R.string.verify_check_2),
        stringResource(R.string.verify_check_3),
        stringResource(R.string.verify_check_4),
        stringResource(R.string.verify_check_5),
        stringResource(R.string.verify_check_6)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.Checklist, null) },
        title = { Text(stringResource(R.string.verification_checklist_title)) },
        text  = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(stringResource(R.string.verification_confirm_items),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                checklistItems.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = idx in checkedItems,
                            onCheckedChange = { onToggle(idx) }
                        )
                        Text(item, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = checklistComplete) {
                Text(stringResource(R.string.verification_approve_org))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Rejection dialog with template reasons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RejectOrgDialog(
    reason:          String,
    onReasonChange:  (String) -> Unit,
    onSelectTemplate: (String) -> Unit,
    onConfirm:       () -> Unit,
    onDismiss:       () -> Unit
) {
    val rejectionTemplates = listOf(
        stringResource(R.string.reject_template_1),
        stringResource(R.string.reject_template_2),
        stringResource(R.string.reject_template_3),
        stringResource(R.string.reject_template_4),
        stringResource(R.string.reject_template_5),
        stringResource(R.string.reject_template_6)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.verification_reject_org)) },
        text  = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.verification_select_reason))

                // Template chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rejectionTemplates.forEach { template ->
                        SuggestionChip(
                            onClick = { onSelectTemplate(template) },
                            label   = { Text(template, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(
                    value         = reason,
                    onValueChange = onReasonChange,
                    label         = { Text(stringResource(R.string.label_rejection_reason)) },
                    placeholder   = { Text(stringResource(R.string.hint_rejection_reason)) },
                    singleLine    = false,
                    maxLines      = 4,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = onConfirm,
                enabled  = reason.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.reject)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
