package com.clearchain.app.presentation.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.NgoReputationData
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.dto.ActivityItemData
import com.clearchain.app.data.remote.dto.DashboardStatsData
import com.clearchain.app.data.remote.dto.RequestStatusCounts
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.ui.theme.StatusColors
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────────
data class AnalyticsState(
    val stats: DashboardStatsData? = null,
    val orgType: OrganizationType = OrganizationType.GROCERY,
    val activities: List<ActivityItemData> = emptyList(),
    val ngoReputation: NgoReputationData? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val organizationApi: OrganizationApi
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    init {
        loadAll()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            loadAll()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val user = getCurrentUserUseCase().first()
                val orgType = user?.type ?: OrganizationType.GROCERY
                val stats = organizationApi.getMyStats()

                // Supplementary sections degrade gracefully: if any of these fail, the core
                // stats above still render — only the extra section they feed goes missing.
                // 30 days to match the analytics activity-trend chart below.
                val activities = runCatching { organizationApi.getMyActivity(days = 30).data }
                    .getOrDefault(emptyList())
                val reputation = if (orgType == OrganizationType.NGO && user != null) {
                    runCatching { organizationApi.getNgoReputation(user.id).data }.getOrNull()
                } else {
                    null
                }
                _state.update {
                    it.copy(
                        stats = stats.data,
                        orgType = orgType,
                        activities = activities,
                        ngoReputation = reputation,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenTitleRow(
                title = stringResource(R.string.label_analytics),
                onBack = onNavigateBack,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.isLoading && state.stats == null) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    HapticPullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = viewModel::refresh
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(ScreenPadding),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val s = state.stats
                            if (s != null) {
                                // Each role orders its own sections, the activity trend
                                // included, so neither is stuck with the other's layout.
                                if (state.orgType == OrganizationType.GROCERY) {
                                    GroceryAnalytics(s, state.activities)
                                } else {
                                    NgoAnalytics(s, state.ngoReputation, state.activities)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroceryAnalytics(s: DashboardStatsData, activities: List<ActivityItemData>) {
    AnalyticsImpactSection(
        kgSaved = s.foodSaved,
        mealsEstimate = s.mealsEstimate,
        co2Estimate = s.co2EstimateKg
    )

    // Collected listings are deleted on pickup, so this ring covers what has *not* moved
    // rather than everything the store ever offered.
    val listingRing = s.listingStatus
    AnalyticsSectionCard(stringResource(R.string.analytics_listings_overview)) {
        DonutChart(
            slices = listOf(
                BarData(stringResource(R.string.status_active), listingRing.open, StatusColors.Available),
                BarData(stringResource(R.string.stat_reserved), listingRing.reserved, StatusColors.Reserved),
                BarData(stringResource(R.string.status_expired), listingRing.expired, StatusColors.Expired)
            ),
            centerValue = listingRing.total.toString(),
            centerLabel = stringResource(R.string.stat_total)
        )
    }

    ActivityTrendSection(activities)

    val pickupTotal = s.completed + s.pendingRequests
    val pickupCompletionRate = if (pickupTotal > 0) (s.completed.toFloat() / pickupTotal * 100).toInt() else 0
    AnalyticsSectionCard(
        title = stringResource(R.string.analytics_pickup_completion_rate),
        action = {
            Text(
                "$pickupCompletionRate%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (pickupCompletionRate >= 70) BrandGreen else MaterialTheme.colorScheme.error
            )
        },
        contentSpacing = RATE_CARD_SPACING
    ) {
        RateContent(
            percent = pickupCompletionRate,
            description = stringResource(R.string.analytics_completed_of_requests, s.completed, pickupTotal),
            spacing = RATE_CARD_SPACING
        )
    }

    RequestStatusSection(s.requestStatus)
}

/** The 30-day activity sparkline, shared by both roles. */
@Composable
private fun ActivityTrendSection(activities: List<ActivityItemData>) {
    AnalyticsSectionCard(title = "") {
        ActivitySparklineCard(
            title = stringResource(R.string.analytics_activity_trend),
            data = buildDailyActivityCounts(activities, days = 30, labelPattern = "MMM d"),
            periodLabel = stringResource(R.string.analytics_last_30_days),
            maxLabels = 6
        )
    }
}

@Composable
private fun NgoAnalytics(
    s: DashboardStatsData,
    reputation: NgoReputationData?,
    activities: List<ActivityItemData>
) {
    AnalyticsImpactSection(
        kgSaved = s.foodSaved,
        mealsEstimate = s.mealsEstimate,
        co2Estimate = s.co2EstimateKg
    )

    // A ring, not bars: these three are parts of one whole, and the old third bar was
    // not even part of it - "Available" counted every open listing on the platform, which
    // is a marketplace figure, not something this NGO holds.
    val inventory = s.inventoryStatus
    AnalyticsSectionCard(stringResource(R.string.analytics_inventory_status)) {
        DonutChart(
            slices = listOf(
                BarData(stringResource(R.string.status_in_stock), inventory.active, StatusColors.Available),
                BarData(stringResource(R.string.status_distributed), inventory.distributed, StatusColors.Distributed),
                BarData(stringResource(R.string.status_expired), inventory.expired, StatusColors.Expired)
            ),
            centerValue = inventory.total.toString(),
            centerLabel = stringResource(R.string.stat_total)
        )
    }

    ActivityTrendSection(activities)

    // Prefer the server-computed, all-time reputation numbers (also shown to groceries
    // reviewing this NGO); fall back to a local estimate if that call failed.
    val fallbackTotal = s.totalCompleted + s.activeRequests
    val fallbackRate = if (fallbackTotal > 0) (s.totalCompleted.toFloat() / fallbackTotal * 100).toInt() else 0
    val completionRate = reputation?.completionRate?.toInt() ?: fallbackRate
    val totalRequests = reputation?.totalRequests ?: fallbackTotal
    val completedRequests = reputation?.completedPickups ?: s.totalCompleted

    AnalyticsSectionCard(
        title = stringResource(R.string.analytics_pickup_completion_rate),
        action = {
            Text(
                "$completionRate%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (completionRate >= 70) BrandGreen else MaterialTheme.colorScheme.error
            )
        },
        contentSpacing = RATE_CARD_SPACING
    ) {
        RateContent(
            percent = completionRate,
            description = stringResource(R.string.analytics_completed_of_requests, completedRequests, totalRequests),
            spacing = RATE_CARD_SPACING
        )
    }

    RequestStatusSection(s.requestStatus)
}

// Request status breakdown — the same chart the admin report shows, scoped to the
// one organization looking at it.

@Composable
private fun RequestStatusSection(status: RequestStatusCounts) {
    if (status.total == 0) return

    AnalyticsSectionCard(stringResource(R.string.section_request_status_breakdown)) {
        ColumnBarChart(
            bars = requestStatusBars(
                pending = status.pending,
                approved = status.approved,
                ready = status.ready,
                completed = status.completed,
                cancelled = status.cancelled,
                rejected = status.rejected
            )
        )
    }
}

// Impact section — shared by GroceryAnalytics and NgoAnalytics: Food Saved,
// Meals Saved, and CO₂ Reduced as three ImpactStatCell columns.

@Composable
private fun AnalyticsImpactSection(kgSaved: Int, mealsEstimate: Int, co2Estimate: Int) {
    AnalyticsSectionCard(stringResource(R.string.analytics_impact)) {
        ImpactSummaryRow(
            kgSaved = kgSaved,
            mealsEstimate = mealsEstimate,
            co2EstimateKg = co2Estimate
        )
    }
}

// Shared section card — matches AccountDetailScreen / RequestDetailScreen.
// Card title uses the same labelSmall/Bold heading as every other section across the app.

/** Matches the rhythm of [WeeklyGoalCard], which the rate cards sit alongside. */
private val RATE_CARD_SPACING = 12.dp

@Composable
private fun AnalyticsSectionCard(
    title: String,
    action: (@Composable () -> Unit)? = null,
    contentSpacing: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing)
        ) {
            if (title.isNotBlank() || action != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title.isNotBlank()) {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    action?.invoke()
                }
            }
            content()
        }
    }
}
