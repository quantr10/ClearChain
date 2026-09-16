package com.clearchain.app.presentation.admin.analytics

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.*
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.ui.theme.StatusColors
import com.clearchain.app.util.UiEvent
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// ═════════════════════════════════════════════════════════════════════════════
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
// ═════════════════════════════════════════════════════════════════════════════

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

    Scaffold(
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick        = { viewModel.onEvent(AdminAnalyticsEvent.ExportPdf) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            ) {
                if (state.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.cd_export_pdf))
                }
            }
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.data == null ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                state.data == null ->
                    EmptyState(
                        icon        = Icons.Default.BarChart,
                        title       = stringResource(R.string.stats_unavailable),
                        subtitle    = state.error ?: stringResource(R.string.stats_unavailable_subtitle),
                        actionLabel = stringResource(R.string.action_retry),
                        onAction    = { viewModel.onEvent(AdminAnalyticsEvent.Load) }
                    )

                else -> {
                    val data = state.data!!
                    HapticPullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh    = { viewModel.onEvent(AdminAnalyticsEvent.Refresh) }
                    ) {
                        LazyColumn(
                            state               = listState,
                            modifier            = Modifier.fillMaxSize(),
                            contentPadding      = ScreenPadding,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                AnalyticsHeader(
                                    state    = state,
                                    onSelect = { viewModel.onEvent(AdminAnalyticsEvent.SelectPeriod(it)) }
                                )
                            }

                            // One item per section, in enum order: see FIRST_SECTION_INDEX.
                            item { RequestStatusCard(data, highlighted) }
                            item { TimingCard(data, highlighted) }
                            item { BacklogCard(data, highlighted) }
                            item { LeaderboardCard(data, highlighted) }
                            item { QualityCard(data, highlighted) }
                            item { OrganizationsCard(data, highlighted) }

                            item { Spacer(Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun AnalyticsHeader(
    state: AdminAnalyticsState,
    onSelect: (StatsPeriodOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.stats_analytics_title),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = periodSubtitle(state.data?.period),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.isLoading && state.data != null) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }

        Row(
            modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsPeriodOption.entries.forEach { option ->
                FilterChip(
                    selected = option == state.period,
                    onClick  = { onSelect(option) },
                    label    = { Text(stringResource(option.labelRes)) }
                )
            }
        }
    }
}

@Composable
private fun periodSubtitle(period: StatsPeriod?): String {
    if (period == null || period.isAllTime) return stringResource(R.string.stats_all_time_subtitle)
    val from = formatIsoDate(period.from)
    val to   = formatIsoDate(period.to)
    return if (from != null && to != null) {
        stringResource(R.string.stats_period_range, from, to)
    } else {
        stringResource(R.string.stats_period_days, period.days)
    }
}

// ── Request status ──────────────────────────────────────────────────────────

@Composable
private fun RequestStatusCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val f = data.funnel
    val bars = requestStatusBars(
        pending   = f.pending,
        approved  = f.approved,
        ready     = f.ready,
        completed = f.completed,
        cancelled = f.cancelled,
        rejected  = f.rejected
    )

    AnalyticsCard(AnalyticsSection.REQUESTS, stringResource(R.string.section_request_status_breakdown), highlighted) {
        if (f.requests == 0) {
            EmptyChartNote(stringResource(R.string.chart_no_requests))
        } else {
            ColumnBarChart(bars = bars)
        }
    }
}

// ── Timing ──────────────────────────────────────────────────────────────────

@Composable
private fun TimingCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val t = data.timing

    AnalyticsCard(AnalyticsSection.TIMING, stringResource(R.string.section_timing), highlighted) {
        if (t.sampleSize == 0) {
            EmptyChartNote(stringResource(R.string.timing_no_data))
        } else {
            MetricRow(stringResource(R.string.timing_to_ready), formatDuration(t.medianHoursToReady))
            MetricRow(stringResource(R.string.timing_to_pickup), formatDuration(t.medianHoursToPickup))
            MetricRow(stringResource(R.string.timing_to_confirm), formatDuration(t.medianHoursToConfirm))
            MetricRow(stringResource(R.string.timing_p90), formatDuration(t.p90HoursToPickup))
            Spacer(Modifier.height(4.dp))
            RateContent(
                percent     = (t.completedWithin24hRate * 100).roundToInt(),
                description = stringResource(R.string.timing_within_24h_detail, (t.completedWithin24hRate * 100).roundToInt())
            )
            FootNote(stringResource(R.string.timing_sample, t.sampleSize))
        }
    }
}

// ── Backlog ─────────────────────────────────────────────────────────────────

@Composable
private fun BacklogCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val b = data.backlog

    AnalyticsCard(
        section     = AnalyticsSection.BACKLOG,
        title       = stringResource(R.string.section_live_backlog),
        highlighted = highlighted,
        action      = {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint     = StatusColors.Pending,
                modifier = Modifier.size(16.dp)
            )
        }
    ) {
        FootNote(stringResource(R.string.backlog_note))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BacklogTile(
                icon     = Icons.Default.Inventory2,
                label    = stringResource(R.string.backlog_open_listings),
                value    = b.openListings.toString(),
                modifier = Modifier.weight(1f)
            )
            BacklogTile(
                icon     = Icons.Default.Timelapse,
                label    = stringResource(R.string.backlog_expiring_24h),
                value    = b.expiringWithin24h.toString(),
                urgent   = b.expiringWithin24h > 0,
                modifier = Modifier.weight(1f)
            )
            BacklogTile(
                icon     = Icons.Default.MarkEmailUnread,
                label    = stringResource(R.string.backlog_pending_requests),
                value    = b.pendingRequests.toString(),
                detail   = b.oldestPendingRequestHours
                    ?.takeIf { b.pendingRequests > 0 }
                    ?.let { stringResource(R.string.backlog_oldest, formatDuration(it)) },
                urgent   = (b.oldestPendingRequestHours ?: 0.0) >= 48,
                modifier = Modifier.weight(1f)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BacklogTile(
                icon     = Icons.Default.LocalShipping,
                label    = stringResource(R.string.backlog_ready_requests),
                value    = b.readyRequests.toString(),
                modifier = Modifier.weight(1f)
            )
            BacklogTile(
                icon     = Icons.Default.VerifiedUser,
                label    = stringResource(R.string.backlog_pending_verifications),
                value    = b.pendingVerifications.toString(),
                detail   = b.oldestPendingVerificationDays
                    ?.takeIf { b.pendingVerifications > 0 }
                    ?.let { stringResource(R.string.backlog_oldest_days, it) },
                urgent   = (b.oldestPendingVerificationDays ?: 0) >= 7,
                modifier = Modifier.weight(1f)
            )
            BacklogTile(
                icon     = Icons.Default.ReportProblem,
                label    = stringResource(R.string.backlog_open_cases),
                value    = (b.openDisputes + b.pendingReports).toString(),
                detail   = stringResource(R.string.backlog_cases_detail, b.openDisputes, b.pendingReports),
                urgent   = b.openDisputes > 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Leaderboards ────────────────────────────────────────────────────────────

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

// ── Quality ─────────────────────────────────────────────────────────────────

@Composable
private fun QualityCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val q = data.quality

    AnalyticsCard(
        section     = AnalyticsSection.QUALITY,
        title       = stringResource(R.string.section_quality),
        highlighted = highlighted,
        action      = {
            q.averageRating?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint     = StatusColors.Pending,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        String.format(Locale.getDefault(), "%.1f", it),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) {
        if (q.reviewCount == 0 && q.disputesOpened == 0 && q.reportsFiled == 0) {
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
            MetricRow(
                label = stringResource(R.string.quality_reports),
                value = q.reportsFiled.toString(),
                valueColor = if (q.reportsFiled > 0) MaterialTheme.colorScheme.error else null
            )
            if (data.headline.completedPickups > 0) {
                FootNote(stringResource(R.string.quality_dispute_rate, (q.disputeRate * 100).roundToInt()))
            }
        }
    }
}

// ── Organizations ───────────────────────────────────────────────────────────

@Composable
private fun OrganizationsCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val o = data.organizations

    AnalyticsCard(AnalyticsSection.ORGANIZATIONS, stringResource(R.string.section_organizations), highlighted) {
        if (o.total == 0) {
            EmptyChartNote(stringResource(R.string.chart_no_organizations))
        } else {
            // One dimension only: an unverified NGO is still an NGO, so verification is
            // not a third slice here.
            DonutChart(
                slices = listOf(
                    BarData(stringResource(R.string.org_type_groceries), o.groceries, StatusColors.Available),
                    BarData(stringResource(R.string.org_type_ngos), o.ngos, StatusColors.Approved)
                ),
                centerValue = o.total.toString(),
                centerLabel = stringResource(R.string.stat_total),
                footnote    = stringResource(R.string.admin_org_total_verified, o.total, o.verified)
            )

            if (o.pendingVerification > 0) {
                val stale = (o.oldestPendingDays ?: 0) >= 7
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        if (stale) Icons.Default.Warning else Icons.Default.HourglassTop,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint     = if (stale) MaterialTheme.colorScheme.error else StatusColors.Pending
                    )
                    Text(
                        text     = o.oldestPendingDays?.let {
                            stringResource(R.string.stat_pending_verification_aged, o.pendingVerification, it)
                        } ?: stringResource(R.string.stat_pending_verification, o.pendingVerification),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = if (stale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Building blocks ─────────────────────────────────────────────────────────

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
        label       = "sectionHighlight"
    )
    DashboardSection(
        title    = title,
        modifier = Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        action   = action,
        content  = content
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
        color    = tint.copy(alpha = 0.10f),
        shape    = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = tint
            )
            Text(
                text      = label,
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis
            )
            detail?.let {
                Text(
                    text      = it,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = tint.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SubHeading(text: String) {
    Text(
        text,
        style      = MaterialTheme.typography.labelLarge,
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
        style    = MaterialTheme.typography.bodySmall,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

// ── Formatting ──────────────────────────────────────────────────────────────

private fun formatDuration(hours: Double?): String = when {
    hours == null -> "–"
    hours < 1     -> "${(hours * 60).roundToInt()}m"
    hours < 48    -> "${hours.roundToInt()}h"
    else          -> "${(hours / 24).roundToInt()}d"
}

private fun formatIsoDate(iso: String?): String? = iso?.let {
    runCatching {
        LocalDate.parse(it.take(10)).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
    }.getOrNull()
}
