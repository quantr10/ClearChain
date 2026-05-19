package com.clearchain.app.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.dto.DashboardStatsData
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.presentation.components.DetailTopBar
import com.clearchain.app.presentation.components.HapticPullToRefreshBox
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══ ViewModel ═══
data class AnalyticsState(
    val stats: DashboardStatsData? = null,
    val orgType: OrganizationType = OrganizationType.GROCERY,
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

    init { loadAll() }

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
                val stats = organizationApi.getMyStats()
                _state.update {
                    it.copy(
                        stats = stats.data,
                        orgType = user?.type ?: OrganizationType.GROCERY,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}

// ═══ Screen ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.stats == null ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                else -> {
                    HapticPullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = viewModel::refresh
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            val s = state.stats
                            if (s != null) {
                                if (state.orgType == OrganizationType.GROCERY) {
                                    GroceryAnalytics(s)
                                } else {
                                    NgoAnalytics(s)
                                }
                            } else if (state.error != null) {
                                Text(
                                    state.error!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroceryAnalytics(s: DashboardStatsData) {
    SectionTitle(stringResource(R.string.analytics_food_impact))
    BarChartCard(
        title = stringResource(R.string.analytics_listings_overview),
        bars = listOf(
            BarData(stringResource(R.string.status_active),    s.activeListings, BrandGreen),
            BarData(stringResource(R.string.status_pending),   s.pendingRequests, MaterialTheme.colorScheme.tertiary),
            BarData(stringResource(R.string.status_completed), s.completed, MaterialTheme.colorScheme.primary),
            BarData(stringResource(R.string.analytics_total_label), s.totalListings, MaterialTheme.colorScheme.secondary)
        )
    )

    SectionTitle(stringResource(R.string.analytics_food_cleared))
    BigStatCard(
        icon = Icons.Default.Eco,
        value = "${s.foodSaved} kg",
        label = stringResource(R.string.analytics_food_saved_total),
        color = BrandGreen
    )

    SectionTitle(stringResource(R.string.analytics_pickup_perf))
    val total = s.completed + s.pendingRequests
    val completionRate = if (total > 0) (s.completed.toFloat() / total * 100).toInt() else 0
    RateCard(
        label = stringResource(R.string.analytics_pickup_completion_rate),
        percent = completionRate,
        description = stringResource(R.string.analytics_completed_of_requests, s.completed, total)
    )
}

@Composable
private fun NgoAnalytics(s: DashboardStatsData) {
    SectionTitle(stringResource(R.string.analytics_inventory_summary))
    BarChartCard(
        title = stringResource(R.string.analytics_inventory_status),
        bars = listOf(
            BarData(stringResource(R.string.status_in_stock),    s.inStock, BrandGreen),
            BarData(stringResource(R.string.status_distributed), s.distributed, MaterialTheme.colorScheme.tertiary),
            BarData(stringResource(R.string.status_available),   s.availableFood, MaterialTheme.colorScheme.primary)
        )
    )

    SectionTitle(stringResource(R.string.analytics_impact))
    val mealsEstimate = s.distributed * 3
    val co2Estimate = (s.distributed * 2.5).toInt()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BigStatCard(
            icon = Icons.Default.Restaurant,
            value = "$mealsEstimate",
            label = stringResource(R.string.analytics_estimated_meals),
            color = BrandGreen,
            modifier = Modifier.weight(1f)
        )
        BigStatCard(
            icon = Icons.Default.EnergySavingsLeaf,
            value = "${co2Estimate}kg",
            label = stringResource(R.string.analytics_co2_avoided),
            color = BrandTeal,
            modifier = Modifier.weight(1f)
        )
    }

    SectionTitle(stringResource(R.string.analytics_request_activity))
    val total = s.totalCompleted + s.activeRequests
    val completionRate = if (total > 0) (s.totalCompleted.toFloat() / total * 100).toInt() else 0
    RateCard(
        label = stringResource(R.string.analytics_request_success_rate),
        percent = completionRate,
        description = stringResource(R.string.analytics_completed_pickups_count, s.totalCompleted)
    )
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

private data class BarData(val label: String, val value: Int, val color: Color)

@Composable
private fun BarChartCard(title: String, bars: List<BarData>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val maxVal = bars.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
            bars.forEach { bar ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(bar.label, style = MaterialTheme.typography.bodySmall)
                        Text(
                            bar.value.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = bar.color
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(bar.value.toFloat() / maxVal)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(bar.color)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BigStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RateCard(label: String, percent: Int, description: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "$percent%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (percent >= 70) BrandGreen else MaterialTheme.colorScheme.error
                )
            }
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (percent >= 70) BrandGreen else MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
