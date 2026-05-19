package com.clearchain.app.presentation.publicprofile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.api.PublicProfileData
import com.clearchain.app.presentation.components.DetailTopBar
import com.clearchain.app.presentation.components.EmptyState
import com.clearchain.app.presentation.components.HapticPullToRefreshBox
import com.clearchain.app.ui.theme.BrandGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══ State ═══
data class PublicProfileState(
    val profile: PublicProfileData? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

// ═══ ViewModel ═══
@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val organizationApi: OrganizationApi
) : ViewModel() {

    private val orgId: String = savedStateHandle["orgId"] ?: ""

    private val _state = MutableStateFlow(PublicProfileState())
    val state: StateFlow<PublicProfileState> = _state.asStateFlow()

    init { load() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            load()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = organizationApi.getPublicProfile(orgId)
                _state.update { it.copy(profile = response.data, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load profile", isLoading = false) }
            }
        }
    }
}

// ═══ Screen ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: PublicProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.profile == null ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.error != null && state.profile == null ->
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = stringResource(R.string.error_failed_load_profile),
                        subtitle = state.error,
                        actionLabel = stringResource(R.string.retry),
                        onAction = { viewModel.refresh() }
                    )

                state.profile != null -> {
                    val profile = state.profile!!
                    HapticPullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = viewModel::refresh
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // ── Header ──────────────────────────────────────
                            PublicProfileHeader(profile)

                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                // ── Stats row ──────────────────────────────
                                if (profile.type == "grocery") {
                                    GroceryStatsRow(profile)
                                } else {
                                    NgoStatsRow(profile)
                                }

                                // ── Description ────────────────────────────
                                if (!profile.description.isNullOrBlank()) {
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(
                                            Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                stringResource(R.string.about),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                profile.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // ── Contact info ───────────────────────────
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            stringResource(R.string.label_contact_location),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (!profile.address.isNullOrBlank()) {
                                            InfoRow(Icons.Default.Place, profile.address)
                                        }
                                        if (!profile.phone.isNullOrBlank()) {
                                            InfoRow(Icons.Default.Phone, profile.phone)
                                        }
                                        if (!profile.hours.isNullOrBlank()) {
                                            InfoRow(Icons.Default.Schedule, profile.hours)
                                        }
                                        if (!profile.contactPerson.isNullOrBlank()) {
                                            InfoRow(Icons.Default.Person, stringResource(R.string.label_contact_prefix, profile.contactPerson))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicProfileHeader(profile: PublicProfileData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {}
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!profile.profilePictureUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profile.profilePictureUrl,
                    contentDescription = profile.name,
                    modifier = Modifier.size(72.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            profile.name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            profile.type.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (profile.verified) {
                        Icon(
                            Icons.Default.Verified, "Verified",
                            modifier = Modifier.size(16.dp),
                            tint = BrandGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroceryStatsRow(profile: PublicProfileData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatPill(
            icon = Icons.Default.Star,
            value = if (profile.averageRating > 0) String.format("%.1f", profile.averageRating) else "–",
            label = stringResource(R.string.label_n_reviews, profile.reviewCount),
            modifier = Modifier.weight(1f)
        )
        StatPill(
            icon = Icons.Default.CheckCircle,
            value = profile.completedPickups.toString(),
            label = stringResource(R.string.label_pickups_done),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NgoStatsRow(profile: PublicProfileData) {
    StatPill(
        icon = Icons.Default.VolunteerActivism,
        value = profile.completedPickups.toString(),
        label = stringResource(R.string.label_pickups_completed),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StatPill(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp).offset(y = 2.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
