package com.clearchain.app.presentation.admin.verification

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.ScreenPadding
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
                            "${state.selectedOrgIds.size} ${if (state.selectedOrgIds.size == 1) "org" else "orgs"} selected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        ClearChainButton(
                            text = stringResource(R.string.action_reject_all_batch),
                            onClick  = { viewModel.onEvent(VerificationQueueEvent.BatchReject) },
                            enabled  = !state.isProcessing,
                            fillMaxWidth = false,
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            icon = Icons.Default.Cancel
                        )
                        ClearChainButton(
                            text = stringResource(R.string.action_approve_all_batch),
                            onClick  = { viewModel.onEvent(VerificationQueueEvent.BatchApprove) },
                            enabled  = !state.isProcessing,
                            fillMaxWidth = false,
                            icon = Icons.Default.CheckCircle
                        )
                    }
                }
            }
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                        ListScreenHeader {
                        // Search + filter
                        ListHeaderSearchRow(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.onEvent(VerificationQueueEvent.SearchQueryChanged(it)) },
                            placeholder = stringResource(R.string.hint_search_organizations)
                        ) {
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
                        FilterChipsRow(
                            tabs = listOf(
                                null       to stringResource(R.string.filter_all),
                                "PENDING"  to stringResource(R.string.status_pending),
                                "APPROVED" to stringResource(R.string.status_approved),
                                "REJECTED" to stringResource(R.string.status_rejected)
                            ),
                            selectedTab = state.selectedStatus,
                            onTabSelected = { viewModel.onEvent(VerificationQueueEvent.StatusFilterChanged(it)) }
                        )

                        ResultsCountAndSort(
                            count          = state.filteredOrgs.size,
                            itemName       = "organization",
                            selectedSort   = state.selectedSort,
                            onSortSelected = { viewModel.onEvent(VerificationQueueEvent.SortOptionChanged(it)) },
                            sortOptions    = state.availableSortOptions,
                            countText      = if (state.isBatchMode) {
                                "${state.selectedOrgIds.size} ${if (state.selectedOrgIds.size == 1) "org" else "orgs"} selected"
                            } else null,
                            leadingContent = if (state.isBatchMode) {
                                {
                                    SelectionCircleButton(
                                        checked = state.allSelected,
                                        onCheckedChange = {
                                            if (state.allSelected) viewModel.onEvent(VerificationQueueEvent.ClearSelection)
                                            else viewModel.onEvent(VerificationQueueEvent.SelectAllVisible)
                                        }
                                    )
                                }
                            } else null
                        )
                        }

                        HapticPullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh    = { viewModel.onEvent(VerificationQueueEvent.RefreshOrganizations) }
                        ) {
                            LazyColumn(
                                contentPadding      = ScreenPadding,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (state.isLoading && state.organizations.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillParentMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                } else if (state.organizations.isEmpty()) {
                                    item {
                                        EmptyState(
                                            icon     = Icons.Default.Business,
                                            title    = stringResource(R.string.empty_no_organizations),
                                            subtitle = stringResource(R.string.empty_no_organizations_subtitle),
                                            modifier = Modifier.fillParentMaxSize()
                                        )
                                    }
                                } else if (state.filteredOrgs.isEmpty()) {
                                    item {
                                        EmptyState(
                                            icon     = Icons.Default.FilterAlt,
                                            title    = stringResource(R.string.empty_no_org_category),
                                            subtitle = stringResource(R.string.empty_no_org_category_subtitle),
                                            modifier = Modifier.fillParentMaxSize()
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
// Advanced filter sheet
// -----------------------------------------------------------------------------

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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.advanced_filters),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (state.activeFilterCount > 0) {
                    ClearChainOutlinedButton(
                        text = stringResource(R.string.action_clear_all),
                        onClick = { onEvent(VerificationQueueEvent.ClearAdvancedFilters) }
                    )
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
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                }
            }

            ClearChainButton(
                text = stringResource(R.string.action_apply_filters),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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

    // Same container theme as ListingCard (ClearChainCard: surface fill, 12dp corners, 1dp
    // shadow). A tap opens the profile (or toggles selection in batch mode) with the standard
    // Material ripple — clipped to the card shape because the click target sits inside the
    // card. A long-press starts multi-select.
    ClearChainCard(
        containerColor = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surface,
        elevation = if (isSelected) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick     = { if (isBatchMode) onToggleSelect() else onViewProfile() },
                    onLongClick = onLongClick
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Identity header ──────────────────────────────────────────────
            // The logo (or a type-tinted monogram) anchors the card on the left; the
            // name shares its line with the verification status wash, and the org type
            // sits underneath as a filled-tonal pill — the same soft chip the profile
            // screens use for a role, so admin/NGO/grocery reads the same everywhere.
            // The two chips form a deliberate pair: identity = solid tonal, state =
            // light 15%-alpha wash. City is no longer up here; it moves to the contact
            // block below, after the phone.
            val typeAccent = when (organization.type) {
                OrganizationType.GROCERY -> MaterialTheme.colorScheme.onSecondaryContainer
                OrganizationType.NGO     -> MaterialTheme.colorScheme.onTertiaryContainer
                OrganizationType.ADMIN   -> MaterialTheme.colorScheme.onPrimaryContainer
            }
            val typeContainer = when (organization.type) {
                OrganizationType.GROCERY -> MaterialTheme.colorScheme.secondaryContainer
                OrganizationType.NGO     -> MaterialTheme.colorScheme.tertiaryContainer
                OrganizationType.ADMIN   -> MaterialTheme.colorScheme.primaryContainer
            }
            val roleLabel = when (organization.type) {
                OrganizationType.GROCERY -> stringResource(R.string.role_grocery)
                OrganizationType.NGO     -> stringResource(R.string.role_ngo)
                OrganizationType.ADMIN   -> stringResource(R.string.role_admin)
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (isBatchMode) {
                    SelectionCircleButton(
                        checked         = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier        = Modifier.size(24.dp)
                    )
                }

                AvatarImage(
                    imageUrl        = organization.profilePictureUrl,
                    name            = organization.name,
                    size            = 44,
                    backgroundColor = typeContainer,
                    textColor       = typeAccent
                )

                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val (statusLabel, statusColor, statusIcon) = when (organization.verificationStatus) {
                        VerificationStatus.APPROVED -> Triple(stringResource(R.string.status_approved), BrandGreen, Icons.Default.CheckCircle)
                        VerificationStatus.REJECTED -> Triple(stringResource(R.string.status_rejected), MaterialTheme.colorScheme.error, Icons.Default.Cancel)
                        VerificationStatus.PENDING  -> Triple(stringResource(R.string.status_pending),  MaterialTheme.colorScheme.secondary, Icons.Default.Schedule)
                    }
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = organization.name,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f)
                        )
                        StatusBadge(
                            label           = statusLabel,
                            backgroundColor = statusColor.copy(alpha = 0.15f),
                            contentColor    = statusColor,
                            icon            = statusIcon
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeContainer,
                        contentColor = typeAccent
                    ) {
                        Text(
                            text       = roleLabel,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Contact block ────────────────────────────────────────────────
            // Email, then phone (with the named contact beside it), then the city —
            // location reads last here because it's the least actionable of the four
            // and it keeps the header uncluttered.
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                CompactInfoRow(
                    icon     = Icons.Default.Email,
                    value    = organization.email,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    CompactInfoRow(
                        icon  = Icons.Default.Phone,
                        value = organization.phone.ifBlank { stringResource(R.string.msg_not_provided) }
                    )
                    organization.contactPerson?.takeIf { it.isNotBlank() }?.let { person ->
                        CompactInfoRow(
                            icon     = Icons.Default.Person,
                            value    = person,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                CompactInfoRow(
                    icon     = Icons.Default.Place,
                    value    = organization.location.ifBlank { stringResource(R.string.msg_not_provided) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Verification document ────────────────────────────────────────
            organization.documentUrl?.let { docUrl ->
                val isPdf = docUrl.endsWith(".pdf", ignoreCase = true) ||
                    organization.documentMimeType?.contains("pdf") == true
                ClearChainOutlinedButton(
                    text = stringResource(R.string.label_view_document),
                    onClick = {
                        if (isPdf) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(docUrl))
                            context.startActivity(intent)
                        } else {
                            fullPhotoUrl = docUrl
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image
                )
            }

            // ── Actions (PENDING only) ───────────────────────────────────────
            if (organization.verificationStatus == VerificationStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ClearChainOutlinedButton(
                        text = stringResource(R.string.reject),
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing,
                        icon = Icons.Default.Cancel,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                    ClearChainButton(
                        text = stringResource(R.string.approve),
                        onClick  = onApprove,
                        modifier = Modifier.weight(1f),
                        enabled  = !isProcessing,
                        icon = Icons.Default.CheckCircle
                    )
                }
            }
        }
    }
}

// Matches CompactAccountDetailRow in AccountDetailScreen.kt exactly (icon 14dp, 6dp gap,
// labelSmall/SemiBold value, no separate label — the icon alone reads as Email/Phone/
// Location, same as that screen's Contact/Location & Hours rows) so this list card reads at
// the same compact scale as the rest of the app instead of the larger shared InfoRow
// (icon 18dp, bodyMedium value) meant for full detail-screen layouts.
@Composable
private fun CompactInfoRow(icon: ImageVector, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint     = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SelectionCircleButton(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onCheckedChange,
        modifier = modifier.size(24.dp),
        shape = RoundedCornerShape(50),
        color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = if (checked) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (checked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Approval checklist dialog
// -----------------------------------------------------------------------------

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
    ConfirmDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        icon = Icons.Default.Checklist,
        title = stringResource(R.string.verification_checklist_title),
        message = stringResource(R.string.verification_confirm_items),
        confirmLabel = stringResource(R.string.verification_approve_org),
        dismissLabel = stringResource(R.string.cancel),
        confirmEnabled = checklistComplete
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            checklistItems.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Same shrunk touch target as the "Remember me" checkbox on Login —
                    // the default Checkbox reserves ~40dp for its touch target, which is
                    // what made these rows look far apart despite the 4dp Column spacing.
                    Checkbox(
                        checked = idx in checkedItems,
                        onCheckedChange = { onToggle(idx) },
                        modifier = Modifier.size(24.dp)
                    )
                    Text(item, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Rejection dialog with template reasons
// -----------------------------------------------------------------------------

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
    ConfirmDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        icon = Icons.Default.Cancel,
        title = stringResource(R.string.verification_reject_org),
        message = stringResource(R.string.verification_select_reason),
        confirmLabel = stringResource(R.string.reject),
        dismissLabel = stringResource(R.string.cancel),
        isDestructive = true,
        confirmEnabled = reason.isNotBlank()
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
    }
}
