package com.clearchain.app.presentation.admin.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.*
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.dispute.NgoDisputeReason
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.ui.theme.StatusColors
import com.clearchain.app.util.UiEvent
import kotlinx.coroutines.delay

// Admin Stats & Analytics.
//
// The screen is a fixed ladder of sections, in the order of AnalyticsSection, each
// one a card. Nothing is ever dropped for being empty: a section that has no data
// says so, which keeps the position of every other section - and therefore the
// deep links the admin home uses to jump into one - stable.
//
// Every figure covers the platform's whole history; the organization register is the
// platform as it stands now.

/** Item 0 is the header, so the first section sits one below it. */
private const val FIRST_SECTION_INDEX = 1

@Composable
fun AdminAnalyticsScreen(
    onOpenOrganization: (String) -> Unit,
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
                                    isExporting = state.isExporting,
                                    onExportPdf = { viewModel.onEvent(AdminAnalyticsEvent.ExportPdf) }
                                )
                            }

                            // One item per section, in enum order: see FIRST_SECTION_INDEX.
                            item { RequestStatusCard(data, highlighted) }
                            item { LeaderboardCard(data, highlighted, onOpenOrganization) }
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
private fun AnalyticsHeader(isExporting: Boolean, onExportPdf: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.stats_analytics_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        ClearChainActionIconButton(
            icon = Icons.Default.FileDownload,
            contentDescription = stringResource(R.string.cd_export_pdf),
            onClick = onExportPdf,
            enabled = !isExporting
        )
    }
}

// ── Request status ───────────────────────────────────────────────────────────

/** Where every request ever raised stands now. */
@Composable
private fun RequestStatusCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val f = data.funnel

    AnalyticsCard(AnalyticsSection.REQUESTS, stringResource(R.string.section_request_status_breakdown), highlighted) {
        if (f.requests == 0) {
            EmptyChartNote(stringResource(R.string.chart_no_requests))
        } else {
            ColumnBarChart(
                bars = requestStatusBars(
                    pending = f.pending,
                    approved = f.approved,
                    ready = f.ready,
                    completed = f.completed,
                    cancelled = f.cancelled,
                    rejected = f.rejected
                )
            )
        }
    }
}

// ── Leaderboards ─────────────────────────────────────────────────────────────

private enum class LeaderboardSide { STORES, NGOS }

private enum class LeaderMetric { PICKUPS, RATING }

/**
 * The top ten stores or NGOs by pickups, then by rating. Each row backs its score up with
 * the other measures, so a place earned on volume alone is visible as such.
 */
@Composable
private fun LeaderboardCard(
    data: AdminDetailedStatsData,
    highlighted: AnalyticsSection?,
    onOpenOrganization: (String) -> Unit
) {
    var side by rememberSaveable { mutableStateOf(LeaderboardSide.STORES) }
    val boards = data.leaderboards
    val board = if (side == LeaderboardSide.STORES) boards.groceries else boards.ngos

    AnalyticsCard(AnalyticsSection.LEADERBOARDS, stringResource(R.string.section_leaderboards), highlighted) {
        // The side on show is the filled button; the other is the outlined one beside it.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LeaderboardSide.entries.forEach { option ->
                val label = stringResource(if (option == LeaderboardSide.STORES) R.string.org_type_groceries else R.string.org_type_ngos)
                val icon = if (option == LeaderboardSide.STORES) Icons.Default.Store else Icons.Default.VolunteerActivism
                val modifier = Modifier.weight(1f).semantics { selected = side == option }
                if (side == option) {
                    ClearChainButton(text = label, onClick = {}, icon = icon, modifier = modifier)
                } else {
                    ClearChainOutlinedButton(text = label, onClick = { side = option }, icon = icon, modifier = modifier)
                }
            }
        }

        SubHeading(stringResource(R.string.leaderboard_metric_pickups))
        LeaderList(board.byPickups, LeaderMetric.PICKUPS, side, stringResource(R.string.chart_no_activity), onOpenOrganization)

        SubHeading(stringResource(R.string.leaderboard_metric_rating))
        LeaderList(board.byRating, LeaderMetric.RATING, side, stringResource(R.string.leaderboard_no_ratings), onOpenOrganization)
    }
}

/** One ranking, best first, or [emptyText] when nobody qualifies for it. */
@Composable
private fun LeaderList(
    leaders: List<StatsLeader>,
    metric: LeaderMetric,
    side: LeaderboardSide,
    emptyText: String,
    onOpenOrganization: (String) -> Unit
) {
    if (leaders.isEmpty()) {
        EmptyChartNote(emptyText)
        return
    }
    val unknown = stringResource(R.string.label_unknown_item)
    val top = leaders.first()
    val color = if (side == LeaderboardSide.STORES) BrandGreen else StatusColors.Approved
    Column {
        leaders.forEach { leader ->
            LeaderRow(
                rank = leader.rank,
                name = leader.name.ifBlank { unknown },
                pictureUrl = leader.profilePictureUrl,
                value = leaderValue(leader, metric),
                fraction = when (metric) {
                    LeaderMetric.PICKUPS -> leader.completedPickups.toFloat() / top.completedPickups.coerceAtLeast(1)
                    // Against the whole scale, not the leader: 4.2 is not "84% of first place".
                    LeaderMetric.RATING -> ((leader.averageRating ?: 0.0) / 5).toFloat()
                },
                color = color,
                onClick = { onOpenOrganization(leader.id) }
            )
        }
    }
}

@Composable
private fun leaderValue(leader: StatsLeader, metric: LeaderMetric): String = when (metric) {
    LeaderMetric.PICKUPS -> leader.completedPickups.toString()
    LeaderMetric.RATING -> "${formatRating(leader.averageRating ?: 0.0)} ★"
}

// ── Disputes ─────────────────────────────────────────────────────────────────

/**
 * Every dispute opened, twice over: where they stand now, and what they were
 * about. Empty parts are left out, since a zero row says nothing.
 */
@Composable
private fun QualityCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val q = data.quality
    val o = q.disputeOutcomes

    AnalyticsCard(AnalyticsSection.QUALITY, stringResource(R.string.section_quality), highlighted) {
        if (q.disputesOpened == 0) {
            EmptyChartNote(stringResource(R.string.quality_no_data))
            return@AnalyticsCard
        }

        SubHeading(stringResource(R.string.quality_dispute_status_title))
        ShareBar(
            segments = listOf(
                BarData(stringResource(R.string.dispute_status_open), o.open, StatusColors.Pending),
                BarData(stringResource(R.string.quality_outcome_ngo_short), o.resolvedForNgo, StatusColors.Approved),
                BarData(stringResource(R.string.quality_outcome_grocery_short), o.resolvedForGrocery, StatusColors.Available),
                BarData(stringResource(R.string.dispute_status_dismissed), o.dismissed, StatusColors.Expired)
            )
        )

        if (q.disputeReasons.isNotEmpty()) {
            SubHeading(stringResource(R.string.quality_dispute_reasons_title))
            // Against every dispute, not the top reason: three reasons at one each are a
            // third apiece, not three full bars.
            val total = q.disputeReasons.sumOf { it.count }.coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                q.disputeReasons.forEach { reason ->
                    ReasonRow(
                        label = NgoDisputeReason.fromKey(reason.key)?.let { stringResource(it.labelRes) } ?: reason.key,
                        count = reason.count,
                        fraction = reason.count.toFloat() / total
                    )
                }
            }
        }
    }
}

/** One bar split by [segments], then a legend naming the non-empty ones with their counts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareBar(segments: List<BarData>) {
    val visible = segments.filter { it.value > 0 }
    if (visible.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            // The legend below reads the same figures out.
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        visible.forEach { segment ->
            Box(
                Modifier
                    .weight(segment.value.toFloat())
                    .fillMaxHeight()
                    .background(segment.color)
            )
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        visible.forEach { segment ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(segment.color)
                )
                Text(
                    segment.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    segment.value.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** A reason on one line: its name, a bar for its share of all disputes, and its count. */
@Composable
private fun ReasonRow(label: String, count: Int, fraction: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clearAndSetSemantics {}
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.error)
            )
        }
        Text(
            count.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 20.dp)
        )
    }
}

// ── Organizations ────────────────────────────────────────────────────────────

/** Who is on the platform now: the register as it stands. */
@Composable
private fun OrganizationsCard(data: AdminDetailedStatsData, highlighted: AnalyticsSection?) {
    val o = data.organizations

    AnalyticsCard(AnalyticsSection.ORGANIZATIONS, stringResource(R.string.section_organizations), highlighted) {
        if (o.total == 0) {
            EmptyChartNote(stringResource(R.string.chart_no_organizations))
            return@AnalyticsCard
        }

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
