package com.clearchain.app.presentation.admin.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.ActivityItemData
import com.clearchain.app.data.remote.dto.AdminAlertItem
import com.clearchain.app.presentation.admin.analytics.AnalyticsSection
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.dispute.NgoDisputeReason
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.presentation.ngo.ActivityFeedItem
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.ui.theme.StatusColors
import com.clearchain.app.util.UiEvent
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val adminDefault = stringResource(R.string.label_admin)
    var userName by remember { mutableStateOf(adminDefault) }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var showAlertSheet by remember { mutableStateOf(false) }
    val openRequest: (String) -> Unit = { requestId ->
        showAlertSheet = false
        navController.navigate(Screen.RequestDetail.createRoute(requestId))
    }

    // Collected rather than read once, so a freshly uploaded avatar shows up here
    // without a re-login.
    LaunchedEffect(true) {
        scope.launch {
            viewModel.getCurrentUserUseCase().collect { user ->
                if (user != null) {
                    userName = user.name
                    profilePictureUrl = user.profilePictureUrl
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                else -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        HapticPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.onEvent(AdminDashboardEvent.RefreshStats) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Gradient admin header ────────────────────────────────────────
                DashboardWelcomeHeader(
                    userName = userName,
                    subtitle = stringResource(R.string.system_health),
                    roleLabel = stringResource(R.string.role_admin),
                    profilePictureUrl = profilePictureUrl,
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.primary,
                        BrandTeal
                    ),
                    onProfileClick = { navController.navigate(Screen.AccountDetail.route) },
                    onNotificationsClick = { navController.navigate(Screen.NotificationInbox.route) }
                )

                Column(
                    modifier = Modifier.padding(ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isLoading && state.stats == null) {
                        LoadingState(Modifier.fillMaxWidth().padding(vertical = 64.dp))
                    } else {
                        // ── Platform stats ───────────────────────────────────────
                        state.stats?.let { stats ->
                            // Listing supply only. Request counts live in the status breakdown
                            // and the completion meter below, so they are not repeated here.
                            // These are live counts, so the tile opens the analytics backlog card.
                            DrillDownSection(
                                title = stringResource(R.string.section_listings),
                                section = AnalyticsSection.BACKLOG,
                                navController = navController
                            ) {
                                StatOverview(
                                    hero = StatTile(Icons.Default.Inventory2, stringResource(R.string.status_active), stats.activeListings),
                                    secondary = listOf(
                                        StatTile(Icons.Default.Bookmark, stringResource(R.string.stat_reserved), stats.reservedListings),
                                        StatTile(Icons.Default.EventBusy, stringResource(R.string.status_expired), stats.expiredListings)
                                    )
                                )
                            }

                            // Completion rate meter
                            if (stats.totalPickupRequests > 0) {
                                val rate = (stats.completedRequests.toFloat() / stats.totalPickupRequests * 100).toInt()
                                DrillDownSection(
                                    // The meter inside already names itself, next to the figure
                                    // it belongs to; a section title above would say it twice.
                                    title = "",
                                    section = AnalyticsSection.REQUESTS,
                                    navController = navController
                                ) {
                                    // Same layout as the NGO dashboard's weekly goal card.
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = stringResource(R.string.stat_completion_rate),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Text(
                                                text = "$rate%",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { rate / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            color = if (rate >= 100) BrandGreen else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = pluralStringResource(R.plurals.admin_completion_detail, stats.totalPickupRequests, stats.completedRequests, stats.totalPickupRequests),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // ── Request Status Bar Chart ─────────────────────────────
                        state.stats?.let { stats ->
                            val barData = listOf(
                                BarData(stringResource(R.string.status_pending), stats.pendingRequests, StatusColors.Pending),
                                BarData(stringResource(R.string.status_approved), stats.approvedRequests, StatusColors.Approved),
                                BarData(stringResource(R.string.status_ready), stats.readyRequests, StatusColors.Ready),
                                BarData(stringResource(R.string.status_completed), stats.completedRequests, StatusColors.Completed),
                                BarData(stringResource(R.string.status_cancelled), stats.cancelledRequests, StatusColors.Expired)
                            )
                            if (barData.any { it.value > 0 }) {
                                DrillDownSection(
                                    title = stringResource(R.string.section_request_status_breakdown),
                                    section = AnalyticsSection.REQUESTS,
                                    navController = navController
                                ) {
                                    ColumnBarChart(bars = barData)
                                }
                            }

                            // ── Org Type Donut Chart ─────────────────────────────
                            if (stats.totalOrganizations > 0) {
                                DrillDownSection(
                                    title = stringResource(R.string.section_organizations),
                                    section = AnalyticsSection.ORGANIZATIONS,
                                    navController = navController
                                ) {
                                    // Same split as the stats screen: an applicant still in the
                                    // queue is counted as awaiting, not under its type as well.
                                    DonutChart(
                                        slices = listOf(
                                            BarData(stringResource(R.string.org_type_groceries), stats.totalGroceries - stats.pendingGroceries, StatusColors.Available),
                                            BarData(stringResource(R.string.org_type_ngos), stats.totalNgos - stats.pendingNgos, StatusColors.Approved),
                                            BarData(stringResource(R.string.stat_awaiting_verification), stats.pendingGroceries + stats.pendingNgos, StatusColors.Pending)
                                        ),
                                        centerValue = stats.totalOrganizations.toString(),
                                        centerLabel = stringResource(R.string.stat_total),
                                        legendFirst = true
                                    )
                                }
                            }
                        }
                    }

                    // ── Alert feed ───────────────────────────────────────────────
                    // Above the quick actions: open disputes are the one thing here that
                    // waits on the admin.
                    if (state.alertFeedItems.isNotEmpty()) {
                        DashboardSection(
                            title = stringResource(R.string.section_alert_feed),
                            action = { AlertCountPill(state.alertFeedItems.size) }
                        ) {
                            // Same pattern as the NGO and grocery activity feeds: five here, the
                            // rest in a sheet.
                            AlertList(alerts = state.alertFeedItems.take(5), onOpenRequest = openRequest)
                            if (state.alertFeedItems.size > 5) {
                                Spacer(Modifier.height(8.dp))
                                ClearChainOutlinedButton(
                                    text = stringResource(R.string.action_view_more),
                                    onClick = { showAlertSheet = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // ── Quick actions ────────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardActionCard(
                            icon = Icons.Default.VerifiedUser,
                            title = stringResource(R.string.verification_queue),
                            subtitle = stringResource(R.string.admin_review_organizations),
                            onClick = { navController.navigate(Screen.Verification.route) }
                        )
                        DashboardActionCard(
                            icon = Icons.Default.History,
                            title = stringResource(R.string.transactions),
                            subtitle = stringResource(R.string.admin_view_transactions),
                            onClick = { navController.navigate(Screen.Transactions.route) }
                        )
                        DashboardActionCard(
                            icon = Icons.Default.BarChart,
                            title = stringResource(R.string.statistics),
                            subtitle = stringResource(R.string.system_health),
                            onClick = { navController.navigate(Screen.AdminStatistics.route) }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAlertSheet) {
        AlertFeedSheet(
            alerts = state.alertFeedItems,
            onOpenRequest = openRequest,
            onDismiss = { showAlertSheet = false }
        )
    }
}

// ── Drill-down sections ──────────────────────────────────────────────────────

/**
 * A dashboard card whose figures are a summary of one analytics section, and which opens
 * that section when tapped. The home screen shows the headline; the number keeps its
 * meaning on the next screen because it lands on the card that explains it rather than at
 * the top of a long report.
 */
@Composable
private fun DrillDownSection(
    title: String,
    section: AnalyticsSection,
    navController: NavController,
    content: @Composable ColumnScope.() -> Unit
) {
    DashboardSection(
        title = title,
        modifier = Modifier.clickable {
            navController.navigate(Screen.AdminStatistics.createRoute(section.key))
        },
        content = content
    )
}

// ── Alert feed ───────────────────────────────────────────────────────────────

/** Same layout as the NGO and grocery [ActivityFeedList], one row per dispute. */
@Composable
private fun AlertList(alerts: List<AdminAlertItem>, onOpenRequest: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        alerts.forEachIndexed { index, alert ->
            AlertRowWithDivider(alert, onOpenRequest, showDivider = index < alerts.lastIndex)
        }
    }
}

/** Every open or under-review dispute, laid out like the NGO and grocery activity history sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertFeedSheet(
    alerts: List<AdminAlertItem>,
    onOpenRequest: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.section_alert_feed),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    AlertCountPill(alerts.size)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        stringResource(R.string.close),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                itemsIndexed(alerts, key = { _, alert -> alert.id }) { index, alert ->
                    AlertRowWithDivider(alert, onOpenRequest, showDivider = index < alerts.lastIndex)
                }
            }
        }
    }
}

/** Opens the pickup request the dispute belongs to; null when the feed didn't send one. */
private fun AdminAlertItem.openAction(onOpenRequest: (String) -> Unit): (() -> Unit)? =
    pickupRequestId?.takeIf { it.isNotBlank() }?.let { requestId -> { onOpenRequest(requestId) } }

@Composable
private fun AlertCountPill(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = CircleShape
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * One open dispute, drawn by the same [ActivityFeedItem] as the NGO and grocery feeds: the
 * reason as the title, its status and the NGO's own words underneath. Tapping opens the
 * pickup request, where the admin resolves it.
 */
@Composable
private fun AlertRowWithDivider(
    alert: AdminAlertItem,
    onOpenRequest: (String) -> Unit,
    showDivider: Boolean
) {
    val title = alert.reason?.let(NgoDisputeReason::fromKey)?.let { stringResource(it.labelRes) }
        ?: alert.title
    val status = when (alert.status) {
        "open" -> stringResource(R.string.dispute_status_open)
        else -> alert.status
    }
    val item = ActivityItemData(
        id = alert.id,
        type = "dispute_opened",
        title = title,
        subtitle = if (alert.body.isBlank()) status else "$status · ${alert.body}",
        timestamp = alert.createdAt,
        relatedId = alert.pickupRequestId
    )
    val onClick = alert.openAction(onOpenRequest)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            ActivityFeedItem(item = item)
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 38.dp, top = 8.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            )
        }
    }
}
