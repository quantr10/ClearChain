package com.clearchain.app.presentation.admin.analytics

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.*
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.ui.theme.StatusColors
import com.clearchain.app.util.UiEvent
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// Admin Stats & Analytics.
//
// The screen is a fixed ladder of sections, in the order of AnalyticsSection, each
// one a card. Nothing is ever dropped for being empty: a section that has no data
// says so, which keeps the position of every other section - and therefore the
// deep links the admin home uses to jump into one - stable.
//
// Two kinds of figure appear and the cards say which is which: everything above
// Backlog is counted over the selected period, Backlog is the live queue as it
// stands right now.

/** Item 0 is the header, so the first section sits one below it. */
private const val FIRST_SECTION_INDEX = 1

@Composable
fun AdminAnalyticsScreen(
    viewModel: AdminAnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var highlighted by remember { mutableStateOf<AnalyticsSection?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                else -> {}
            }
        }
    }

    // Opened from an admin home tile: jump to the matching card and mark it, so the
    // number tapped on the previous screen is visibly the one being explained here.
    LaunchedEffect(state.focusedSection, state.data != null) {
        val section = state.focusedSection ?: return@LaunchedEffect
        if (state.data == null) return@LaunchedEffect
        listState.animateScrollToItem(FIRST_SECTION_INDEX + section.ordinal)
        highlighted = section
        viewModel.onEvent(AdminAnalyticsEvent.FocusConsumed)
    }

    // Kept out of the effect above: that one is cancelled the moment the focus is
    // consumed, which would leave the marker on the card for good.
    LaunchedEffect(highlighted) {
        if (highlighted != null) {
            delay(2400)
            highlighted = null
        }
    }

    if (state.showFilterSheet) {
        AnalyticsFilterSheet(
            state = state,
            onEvent = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(AdminAnalyticsEvent.HideFilterSheet) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.data == null -> LoadingState()

                state.data == null ->
                    EmptyState(
                        icon = Icons.Default.BarChart,
                        title = stringResource(R.string.stats_unavailable),
                        subtitle = state.error ?: stringResource(R.string.stats_unavailable_subtitle),
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = { viewModel.onEvent(AdminAnalyticsEvent.Load) }
                    )

                else -> {
                    val data = state.data!!
                    HapticPullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.onEvent(AdminAnalyticsEvent.Refresh) }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = ScreenPadding,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                AnalyticsHeader(
                                    state = state,
                                    onShowFilters = { viewModel.onEvent(AdminAnalyticsEvent.ShowFilterSheet) },
                                    onExportPdf = { viewModel.onEvent(AdminAnalyticsEvent.ExportPdf) }
                                )
                            }

                            // One item per section, in enum order: see FIRST_SECTION_INDEX.
                            item { RequestStatusCard(data, highlighted) }
                            item { BacklogCard(data, highlighted) }
                            item { LeaderboardCard(data, highlighted) }
                            item { QualityCard(data, highlighted) }
                            item { OrganizationsCard(data, highlighted) }
                        }
                    }
                }
            }
        }
    }
}

// ── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun AnalyticsHeader(
    state: AdminAnalyticsState,
    onShowFilters: () -> Unit,
    onExportPdf: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.stats_analytics_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.period.description(LocalContext.current.resources),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClearChainActionIconButton(
                    icon = Icons.Default.FileDownload,
                    contentDescription = stringResource(R.string.cd_export_pdf),
                    onClick = onExportPdf,
                    enabled = !state.isExporting
                )
                BadgedBox(
                    badge = {
                        if (state.activeFilterCount > 0) Badge { Text(state.activeFilterCount.toString()) }
                    }
                ) {
                    ClearChainActionIconButton(
                        icon = Icons.Default.Tune,
                        contentDescription = stringResource(R.string.advanced_filters),
                        onClick = onShowFilters
                    )
                }
            }
        }

        // Changing the range keeps the old figures on screen while the new ones load.
        UpdatingBar(visible = state.isLoading && state.data != null)
    }
}

// ── Advanced filter sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AnalyticsFilterSheet(
    state: AdminAnalyticsState,
    onEvent: (AdminAnalyticsEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        FilterSheetContent {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.advanced_filters),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (state.activeFilterCount > 0) {
                    ClearChainOutlinedButton(
                        text = stringResource(R.string.action_clear_all),
                        onClick = { onEvent(AdminAnalyticsEvent.ClearFilters) }
                    )
                }
            }

            FilterSection(title = stringResource(R.string.filter_time_period)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatsPeriodType.entries.forEach { type ->
                        FilterChip(
                            selected = state.period.type == type,
                            onClick = { onEvent(AdminAnalyticsEvent.PeriodTypeSelected(type)) },
                            label = { Text(stringResource(type.labelRes), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                if (state.period.type != StatsPeriodType.ALL) {
                    PeriodStepper(
                        period = state.period,
                        canGoBack = state.period.canGoBack(state.firstDataDate),
                        onPrevious = { onEvent(AdminAnalyticsEvent.PreviousPeriod) },
                        onNext = { onEvent(AdminAnalyticsEvent.NextPeriod) }
                    )
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

/** ‹ September 2026 › with the days it covers underneath. */
@Composable
private fun PeriodStepper(
    period: AnalyticsPeriod,
    canGoBack: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val res = LocalContext.current.resources
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClearChainActionIconButton(
            icon = Icons.Default.ChevronLeft,
            contentDescription = stringResource(R.string.cd_previous_period),
            onClick = onPrevious,
            enabled = canGoBack
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                period.title(res),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            period.rangeText(res)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ClearChainActionIconButton(
            icon = Icons.Default.ChevronRight,
            contentDescription = stringResource(R.string.cd_next_period),
            onClick = onNext,
            enabled = period.canGoForward
        )
    }
}

// ── Request status ───────────────────────────────────────────────────────────

@Composable
private fun RequestStatusCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val f = data.funnel
    val bars = requestStatusBars(
        pending = f.pending,
        approved = f.approved,
        ready = f.ready,
        completed = f.completed,
        cancelled = f.cancelled,
        rejected = f.rejected
    )

    AnalyticsCard(AnalyticsSection.REQUESTS, stringResource(R.string.section_request_status_breakdown), highlighted) {
        if (f.requests == 0) {
            EmptyChartNote(stringResource(R.string.chart_no_requests))
        } else {
            ColumnBarChart(bars = bars)
        }
    }
}

// ── Backlog ──────────────────────────────────────────────────────────────────

@Composable
private fun BacklogCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val b = data.backlog

    AnalyticsCard(
        section = AnalyticsSection.BACKLOG,
        title = stringResource(R.string.section_live_backlog),
        highlighted = highlighted,
        action = {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = StatusColors.Pending,
                modifier = Modifier.size(16.dp)
            )
        }
    ) {
        FootNote(stringResource(R.string.backlog_note))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BacklogTile(
                icon = Icons.Default.Inventory2,
                label = stringResource(R.string.backlog_open_listings),
                value = b.openListings.toString(),
                modifier = Modifier.weight(1f)
            )
            BacklogTile(
                icon = Icons.Default.Timelapse,
                label = stringResource(R.string.backlog_expiring_24h),
                value = b.expiringWithin24h.toString(),
                urgent = b.expiringWithin24h > 0,
                modifier = Modifier.weight(1f)
            )
            BacklogTile(
                icon = Icons.Default.MarkEmailUnread,
                label = stringResource(R.string.backlog_pending_requests),
                value = b.pendingRequests.toString(),
                detail = b.oldestPendingRequestHours
                    ?.takeIf { b.pendingRequests > 0 }
                    ?.let { stringResource(R.string.backlog_oldest, formatDuration(it)) },
                urgent = (b.oldestPendingRequestHours ?: 0.0) >= 48,
                modifier = Modifier.weight(1f)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BacklogTile(
                icon = Icons.Default.LocalShipping,
                label = stringResource(R.string.backlog_ready_requests),
                value = b.readyRequests.toString(),
                modifier = Modifier.weight(1f)
            )
            BacklogTile(
                icon = Icons.Default.VerifiedUser,
                label = stringResource(R.string.backlog_pending_verifications),
                value = b.pendingVerifications.toString(),
                detail = b.oldestPendingVerificationDays
                    ?.takeIf { b.pendingVerifications > 0 }
                    ?.let { stringResource(R.string.backlog_oldest_days, it) },
                urgent = (b.oldestPendingVerificationDays ?: 0) >= 7,
                modifier = Modifier.weight(1f)
            )
            BacklogTile(
                icon = Icons.Default.ReportProblem,
                label = stringResource(R.string.backlog_open_cases),
                value = b.openDisputes.toString(),
                urgent = b.openDisputes > 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Leaderboards ─────────────────────────────────────────────────────────────

@Composable
private fun LeaderboardCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val groceries = data.leaderboards.topGroceries
    val ngos = data.leaderboards.topNgos
    val unknown = stringResource(R.string.label_unknown_item)

    AnalyticsCard(AnalyticsSection.LEADERBOARDS, stringResource(R.string.section_leaderboards), highlighted) {
        if (groceries.isEmpty() && ngos.isEmpty()) {
            EmptyChartNote(stringResource(R.string.chart_no_activity))
        } else {
            if (groceries.isNotEmpty()) {
                SubHeading(stringResource(R.string.chart_top_groceries))
                BarChartContent(
                    bars = groceries.map {
                        BarData(it.name.ifBlank { unknown }, it.completedPickups, BrandGreen)
                    }
                )
                FootNote(stringResource(R.string.leaderboard_grocery_note))
            }
            if (ngos.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                SubHeading(stringResource(R.string.chart_top_ngos))
                BarChartContent(
                    bars = ngos.map { BarData(it.name.ifBlank { unknown }, it.completedPickups, StatusColors.Approved) }
                )
                FootNote(stringResource(R.string.leaderboard_ngo_note))
            }
        }
    }
}

// ── Quality ──────────────────────────────────────────────────────────────────

@Composable
private fun QualityCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val q = data.quality

    AnalyticsCard(
        section = AnalyticsSection.QUALITY,
        title = stringResource(R.string.section_quality),
        highlighted = highlighted,
        action = {
            q.averageRating?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = StatusColors.Pending,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        String.format(LocalConfiguration.current.locales[0], "%.1f", it),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) {
        if (q.reviewCount == 0 && q.disputesOpened == 0) {
            EmptyChartNote(stringResource(R.string.quality_no_data))
        } else {
            MetricRow(stringResource(R.string.quality_reviews), q.reviewCount.toString())
            MetricRow(
                stringResource(R.string.quality_review_coverage_label),
                "${(q.reviewCoverage * 100).roundToInt()}%"
            )
            MetricRow(
                label = stringResource(R.string.quality_disputes),
                value = q.disputesOpened.toString(),
                valueColor = if (q.disputesOpened > 0) MaterialTheme.colorScheme.error else null
            )
            if (data.headline.completedPickups > 0) {
                FootNote(stringResource(R.string.quality_dispute_rate, (q.disputeRate * 100).roundToInt()))
            }
        }
    }
}

// ── Organizations ────────────────────────────────────────────────────────────

@Composable
private fun OrganizationsCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val o = data.organizations

    AnalyticsCard(AnalyticsSection.ORGANIZATIONS, stringResource(R.string.section_organizations), highlighted) {
        if (o.total == 0) {
            EmptyChartNote(stringResource(R.string.chart_no_organizations))
        } else {
            // The slices split every org exactly once: an applicant still in the queue
            // is counted as awaiting, not under its type as well, so the ring adds up
            // to the total in the middle.
            DonutChart(
                slices = listOf(
                    BarData(stringResource(R.string.org_type_groceries), o.groceries - o.pendingGroceries, StatusColors.Available),
                    BarData(stringResource(R.string.org_type_ngos), o.ngos - o.pendingNgos, StatusColors.Approved),
                    BarData(stringResource(R.string.stat_awaiting_verification), o.pendingGroceries + o.pendingNgos, StatusColors.Pending)
                ),
                centerValue = o.total.toString(),
                centerLabel = stringResource(R.string.stat_total),
                legendFirst = true
            )
        }
    }
}

// ── Building blocks ──────────────────────────────────────────────────────────

/** A section card that can be marked when the admin home linked straight to it. */
@Composable
private fun AnalyticsCard(
    section: AnalyticsSection,
    title: String,
    highlighted: AnalyticsSection?,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isHighlighted = highlighted == section
    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "sectionHighlight"
    )
    DashboardSection(
        title = title,
        modifier = Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        action = action,
        content = content
    )
}

@Composable
private fun BacklogTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    urgent: Boolean = false
) {
    val tint = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        color = tint.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SubHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun FootNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    )
}

@Composable
private fun EmptyChartNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

// ── Formatting ───────────────────────────────────────────────────────────────

private fun formatDuration(hours: Double?): String = when {
    hours == null -> "–"
    hours < 1 -> "${(hours * 60).roundToInt()}m"
    hours < 48 -> "${hours.roundToInt()}h"
    else -> "${(hours / 24).roundToInt()}d"
}
