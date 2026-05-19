package com.clearchain.app.presentation.admin.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.data.remote.dto.AdminDetailedStatsData
import com.clearchain.app.data.remote.dto.DailyTrendItem
import com.clearchain.app.data.remote.dto.LeaderboardEntry
import com.clearchain.app.data.remote.dto.NgoLeaderboardEntry
import com.clearchain.app.domain.model.AdminStats
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.util.UiEvent
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.abs
import kotlin.math.roundToInt

// ── Date preset options ───────────────────────────────────────────────────────


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val datePresets = listOf(
        "all"   to stringResource(R.string.preset_all),
        "month" to stringResource(R.string.preset_month),
        "week"  to stringResource(R.string.preset_week),
        "today" to stringResource(R.string.preset_today)
    )

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                else -> {}
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(StatisticsEvent.ExportPdf) }) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.cd_export_pdf))
            }
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading && state.stats == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                state.stats?.let { stats ->
                    HapticPullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh    = { viewModel.onEvent(StatisticsEvent.RefreshStatistics) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // ── Date range presets ────────────────────────────
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    stringResource(R.string.label_date_range),
                                    style      = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier   = Modifier.padding(bottom = 8.dp)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(datePresets) { (key, label) ->
                                        FilterChip(
                                            selected = state.selectedPreset == key,
                                            onClick  = { viewModel.onEvent(StatisticsEvent.PresetChanged(key)) },
                                            label    = { Text(label) },
                                            leadingIcon = if (state.selectedPreset == key) ({
                                                Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                            }) else null
                                        )
                                    }
                                }
                            }

                            // ── Overview stats (all-time from /overview) ──────
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SectionHeader(stringResource(R.string.section_organizations))
                            }
                            StatsGroup(
                                icon           = Icons.Default.Business,
                                title          = stringResource(R.string.stat_org_overview),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
                                items          = listOf(
                                    stringResource(R.string.stat_total)        to "${stats.totalOrganizations}",
                                    stringResource(R.string.org_type_groceries) to "${stats.totalGroceries}",
                                    stringResource(R.string.org_type_ngos)     to "${stats.totalNgos}",
                                    stringResource(R.string.stat_verified)     to "${stats.verifiedOrganizations}",
                                    stringResource(R.string.stat_unverified)   to "${stats.unverifiedOrganizations}"
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SectionHeader(stringResource(R.string.section_listings))
                            }
                            StatsGroup(
                                icon           = Icons.Default.Inventory,
                                title          = stringResource(R.string.stat_food_listings),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
                                items          = listOf(
                                    stringResource(R.string.stat_total)    to "${stats.totalListings}",
                                    stringResource(R.string.status_active) to "${stats.activeListings}",
                                    stringResource(R.string.stat_reserved) to "${stats.reservedListings}"
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SectionHeader(stringResource(R.string.section_pickup_requests))
                            }
                            StatsGroup(
                                icon           = Icons.Default.LocalShipping,
                                title          = stringResource(R.string.stat_request_status),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor   = MaterialTheme.colorScheme.onTertiaryContainer,
                                items          = listOf(
                                    stringResource(R.string.stat_total)        to "${stats.totalPickupRequests}",
                                    stringResource(R.string.status_pending)    to "${stats.pendingRequests}",
                                    stringResource(R.string.status_approved)   to "${stats.approvedRequests}",
                                    stringResource(R.string.status_ready)      to "${stats.readyRequests}",
                                    stringResource(R.string.status_completed)  to "${stats.completedRequests}",
                                    stringResource(R.string.status_cancelled)  to "${stats.cancelledRequests}"
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // ── Completion rate ───────────────────────────────
                            if (stats.totalPickupRequests > 0) {
                                val rate = (stats.completedRequests.toFloat() / stats.totalPickupRequests * 100).toInt()
                                CompletionRateCard(
                                    rate     = rate,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }

                            // ── Detailed section (date-range dependent) ───────
                            state.detailedStats?.let { detailed ->
                                // Impact metrics
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    SectionHeader(stringResource(R.string.section_platform_impact))
                                }
                                ImpactCard(
                                    detailed = detailed,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                // Daily activity heatmap
                                if (detailed.dailyTrend.isNotEmpty()) {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        SectionHeader(stringResource(R.string.section_daily_activity))
                                    }
                                    DailyHeatmapCard(
                                        trend    = detailed.dailyTrend,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                // Period-over-period comparison
                                state.previousDetailedStats?.let { prev ->
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        SectionHeader(stringResource(R.string.section_period_comparison))
                                    }
                                    PeriodComparisonCard(
                                        current  = detailed,
                                        previous = prev,
                                        preset   = state.selectedPreset,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                // Category breakdown
                                if (detailed.categoryBreakdown.isNotEmpty()) {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        SectionHeader(stringResource(R.string.section_category_breakdown))
                                    }
                                    CategoryBreakdownCard(
                                        items    = detailed.categoryBreakdown,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                // Leaderboards
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    SectionHeader(stringResource(R.string.section_leaderboards))
                                }
                                if (detailed.leaderboards.topGroceries.isNotEmpty()) {
                                    GroceryLeaderboardCard(
                                        entries  = detailed.leaderboards.topGroceries,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                                if (detailed.leaderboards.topNgos.isNotEmpty()) {
                                    NgoLeaderboardCard(
                                        entries  = detailed.leaderboards.topNgos,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                                if (detailed.leaderboards.topGroceries.isEmpty() && detailed.leaderboards.topNgos.isEmpty()) {
                                    EmptyState(
                                        icon     = Icons.Default.EmojiEvents,
                                        title    = stringResource(R.string.leaderboard_no_data),
                                        subtitle = stringResource(R.string.leaderboard_no_data_subtitle)
                                    )
                                }
                            }

                            if (state.isLoadingDetailed) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }

                            // ── Geographic heatmap ────────────────────────────
                            if (state.orgLocations.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.section_geographic_distribution),
                                        style      = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier   = Modifier.padding(bottom = 4.dp)
                                    )
                                    OrgGeographicMap(orgs = state.orgLocations)
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Stats Group Card ──────────────────────────────────────────────────────────

@Composable
private fun StatsGroup(
    icon:           ImageVector,
    title:          String,
    containerColor: Color,
    contentColor:   Color,
    items:          List<Pair<String, String>>,
    modifier:       Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, null, Modifier.size(28.dp), tint = contentColor)
                Text(
                    title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = contentColor
                )
            }
            HorizontalDivider(color = contentColor.copy(alpha = 0.2f))
            items.forEach { (label, value) ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f))
                    Text(value, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold, color = contentColor)
                }
            }
        }
    }
}

// ── Completion Rate Card ──────────────────────────────────────────────────────

@Composable
private fun CompletionRateCard(rate: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.TrendingUp, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    stringResource(R.string.stat_completion_rate),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                "$rate%",
                style      = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer
            )
            LinearProgressIndicator(
                progress   = { rate / 100f },
                modifier   = Modifier.fillMaxWidth(),
                color      = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
    }
}

// ── Impact Card ───────────────────────────────────────────────────────────────

@Composable
private fun ImpactCard(detailed: AdminDetailedStatsData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.12f))
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Eco, null, tint = BrandGreen)
                Text(
                    stringResource(R.string.section_impact),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = BrandGreen
                )
            }
            HorizontalDivider(color = BrandGreen.copy(alpha = 0.2f))
            Row(modifier = Modifier.fillMaxWidth()) {
                ImpactMetricColumn(
                    value    = "${detailed.impact.kgSaved.toInt()} kg",
                    label    = stringResource(R.string.stat_food_saved),
                    modifier = Modifier.weight(1f)
                )
                ImpactMetricColumn(
                    value    = "${detailed.impact.mealsEquivalent}",
                    label    = stringResource(R.string.stat_meals),
                    modifier = Modifier.weight(1f)
                )
                ImpactMetricColumn(
                    value    = "${detailed.impact.co2Saved.toInt()} kg",
                    label    = stringResource(R.string.stat_co2_avoided),
                    modifier = Modifier.weight(1f)
                )
                ImpactMetricColumn(
                    value    = "${detailed.impact.totalBeneficiaries}",
                    label    = stringResource(R.string.stat_beneficiaries),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ImpactMetricColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier          = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandGreen)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Category Breakdown Card ───────────────────────────────────────────────────

private val CATEGORY_COLORS = listOf(
    Color(0xFF6750A4), Color(0xFF0284C7), Color(0xFF059669),
    Color(0xFFD97706), Color(0xFFDC2626)
)

@Composable
private fun CategoryBreakdownCard(
    items: List<com.clearchain.app.data.remote.dto.CategoryBreakdownItem>,
    modifier: Modifier = Modifier
) {
    val maxCount = items.maxOfOrNull { it.count } ?: 1
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.PieChart, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "By Category",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items.take(5).forEachIndexed { idx, item ->
                val color = CATEGORY_COLORS.getOrElse(idx) { MaterialTheme.colorScheme.primary }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.category, style = MaterialTheme.typography.bodySmall)
                        Text("${item.count}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress   = { item.count.toFloat() / maxCount },
                        modifier   = Modifier.fillMaxWidth(),
                        color      = color,
                        trackColor = color.copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

// ── Grocery Leaderboard Card ──────────────────────────────────────────────────

@Composable
private fun GroceryLeaderboardCard(
    entries:  List<LeaderboardEntry>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.StoreMallDirectory, null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    "Top Grocery Stores",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
            entries.take(5).forEachIndexed { idx, entry ->
                LeaderboardRow(
                    rank         = idx + 1,
                    name         = entry.name,
                    primaryStat  = "${entry.completedPickups} pickups",
                    secondaryStat = "${entry.totalKg} kg",
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ── NGO Leaderboard Card ──────────────────────────────────────────────────────

@Composable
private fun NgoLeaderboardCard(
    entries:  List<NgoLeaderboardEntry>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.VolunteerActivism, null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    "Top NGOs",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f))
            entries.take(5).forEachIndexed { idx, entry ->
                LeaderboardRow(
                    rank         = idx + 1,
                    name         = entry.name,
                    primaryStat  = "${entry.completedPickups} pickups",
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

// ── Shared leaderboard row ────────────────────────────────────────────────────

@Composable
private fun LeaderboardRow(
    rank:          Int,
    name:          String,
    primaryStat:   String,
    secondaryStat: String? = null,
    contentColor:  Color
) {
    val rankIcon = when (rank) {
        1 -> Icons.Default.EmojiEvents
        2 -> Icons.Default.WorkspacePremium
        3 -> Icons.Default.MilitaryTech
        else -> null
    }
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // gold
        2 -> Color(0xFFC0C0C0) // silver
        3 -> Color(0xFFCD7F32) // bronze
        else -> contentColor.copy(alpha = 0.6f)
    }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (rankIcon != null) {
            Icon(rankIcon, null, modifier = Modifier.size(20.dp), tint = rankColor)
        } else {
            Text(
                "#$rank",
                style      = MaterialTheme.typography.labelMedium,
                color      = rankColor,
                modifier   = Modifier.width(20.dp),
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            name,
            modifier = Modifier.weight(1f),
            style    = MaterialTheme.typography.bodyMedium,
            color    = contentColor
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(primaryStat, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = contentColor)
            if (secondaryStat != null) {
                Text(secondaryStat, style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f))
            }
        }
    }
}

// ── Geographic Distribution Map ───────────────────────────────────────────────

@Composable
private fun OrgGeographicMap(orgs: List<Organization>) {
    val orgsWithCoords = orgs.filter { it.latitude != null && it.longitude != null }
    if (orgsWithCoords.isEmpty()) return

    val center = LatLng(
        orgsWithCoords.mapNotNull { it.latitude }.average(),
        orgsWithCoords.mapNotNull { it.longitude }.average()
    )
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 10f)
    }

    Card(shape = RoundedCornerShape(16.dp)) {
        Column {
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                cameraPositionState = cameraState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                properties = MapProperties()
            ) {
                orgsWithCoords.forEach { org ->
                    val position = LatLng(org.latitude!!, org.longitude!!)
                    val circleColor = when (org.type) {
                        OrganizationType.GROCERY -> android.graphics.Color.argb(160, 76, 175, 80)
                        OrganizationType.NGO     -> android.graphics.Color.argb(160, 33, 150, 243)
                        OrganizationType.ADMIN   -> android.graphics.Color.argb(160, 156, 39, 176)
                    }
                    Circle(
                        center       = position,
                        radius       = 300.0,
                        fillColor    = androidx.compose.ui.graphics.Color(circleColor),
                        strokeColor  = androidx.compose.ui.graphics.Color(circleColor),
                        strokeWidth  = 2f
                    )
                }
            }
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf(
                    stringResource(R.string.org_type_grocery) to androidx.compose.ui.graphics.Color(0xFF4CAF50),
                    stringResource(R.string.org_type_ngo)     to androidx.compose.ui.graphics.Color(0xFF2196F3)
                ).forEach { (label, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(color)
                        }
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${orgsWithCoords.size} locations",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Daily Activity Heatmap ────────────────────────────────────────────────────

@Composable
private fun DailyHeatmapCard(
    trend:    List<DailyTrendItem>,
    modifier: Modifier = Modifier
) {
    val maxCount = trend.maxOfOrNull { it.count } ?: 1
    val cellColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CalendarViewMonth, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Activity Heatmap",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            val cols  = 7
            val cells = trend.takeLast(35)
            val rows  = (cells.size + cols - 1) / cols

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((rows * 20 + (rows - 1) * 4).dp)
            ) {
                val cellSize = (size.width - (cols - 1) * 4.dp.toPx()) / cols
                val gap      = 4.dp.toPx()
                cells.forEachIndexed { idx, item ->
                    val col   = idx % cols
                    val row   = idx / cols
                    val alpha = if (maxCount > 0) (item.count.toFloat() / maxCount).coerceIn(0.1f, 1f) else 0.1f
                    val color = if (item.count > 0) cellColor.copy(alpha = alpha) else emptyColor
                    drawRoundRect(
                        color        = color,
                        topLeft      = Offset(col * (cellSize + gap), row * (cellSize + gap)),
                        size         = Size(cellSize, cellSize),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.label_less), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(0.1f, 0.3f, 0.5f, 0.75f, 1.0f).forEach { alpha ->
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawRoundRect(
                                color        = cellColor.copy(alpha = alpha),
                                cornerRadius = CornerRadius(2.dp.toPx())
                            )
                        }
                    }
                }
                Text(stringResource(R.string.label_more), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${cells.sumOf { it.count }} total pickups in last ${cells.size} days",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Period-over-Period Comparison Card ───────────────────────────────────────

@Composable
private fun PeriodComparisonCard(
    current:  AdminDetailedStatsData,
    previous: AdminDetailedStatsData,
    preset:   String,
    modifier: Modifier = Modifier
) {
    val periodLabel = when (preset) {
        "today" -> stringResource(R.string.label_vs_yesterday)
        "week"  -> stringResource(R.string.label_vs_prev_week)
        "month" -> stringResource(R.string.label_vs_prev_month)
        else    -> stringResource(R.string.label_vs_prev_period)
    }

    data class ComparisonMetric(val label: String, val curr: Double, val prev: Double, val unit: String = "")

    val metrics = listOf(
        ComparisonMetric(stringResource(R.string.label_completed_pickups),
            current.requests.completedReqs.toDouble(), previous.requests.completedReqs.toDouble()),
        ComparisonMetric(stringResource(R.string.label_food_saved),
            current.impact.kgSaved, previous.impact.kgSaved, "kg"),
        ComparisonMetric(stringResource(R.string.label_beneficiaries),
            current.impact.totalBeneficiaries.toDouble(), previous.impact.totalBeneficiaries.toDouble()),
        ComparisonMetric(stringResource(R.string.label_new_listings),
            current.listings.totalListings.toDouble(), previous.listings.totalListings.toDouble())
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border   = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CompareArrows, null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    "Period Comparison",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    periodLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            metrics.forEach { metric ->
                val delta = metric.curr - metric.prev
                val pct   = if (metric.prev != 0.0) (delta / metric.prev * 100).roundToInt() else 0
                val isUp  = delta >= 0

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(metric.label, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${metric.curr.roundToInt()}${metric.unit}",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                null, modifier = Modifier.size(14.dp),
                                tint = if (isUp) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                            )
                            Text(
                                "${if (isUp) "+" else ""}${pct}%",
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                                color = if (isUp) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
