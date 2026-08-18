package com.clearchain.app.presentation.publicprofile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.clearchain.app.data.remote.api.ListingApi
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.api.PublicProfileData
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.presentation.components.ClearChainActionIconButton
import com.clearchain.app.presentation.components.EmptyState
import com.clearchain.app.presentation.components.HapticPullToRefreshBox
import com.clearchain.app.presentation.components.ListingCard
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal
import com.clearchain.app.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══ State ═══
data class PublicProfileState(
    val profile: PublicProfileData? = null,
    val moreFromStore: List<Listing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

// ═══ ViewModel ═══
@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val organizationApi: OrganizationApi,
    private val listingApi: ListingApi
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
                if (response.data.type.equals("grocery", ignoreCase = true)) {
                    loadMoreFromStore(response.data.id)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load profile", isLoading = false) }
            }
        }
    }

    private fun loadMoreFromStore(groceryId: String) {
        viewModelScope.launch {
            runCatching {
                listingApi.getAllListings(status = "open", groceryId = groceryId, pageSize = 6)
                    .data
                    .map { it.toDomain() }
            }.onSuccess { listings ->
                _state.update { it.copy(moreFromStore = listings) }
            }
        }
    }
}

// ═══ Screen ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToListingDetail: (String) -> Unit = {},
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
                        title = stringResource(R.string.error_generic),
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
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // ── Description ────────────────────────────
                                if (!profile.description.isNullOrBlank()) {
                                    ProfileSectionCard(stringResource(R.string.about)) {
                                        Text(
                                            profile.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                ProfileStatsGrid(profile)

                                // ── Contact info ───────────────────────────
                                ContactInformationSection(profile)

                                if (profile.type.equals("grocery", ignoreCase = true) &&
                                    state.moreFromStore.isNotEmpty()
                                ) {
                                    ProfileSectionCard(stringResource(R.string.label_more_from_store)) {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(state.moreFromStore, key = { it.id }) { listing ->
                                                ListingCard(
                                                    listing = listing,
                                                    onClick = { onNavigateToListingDetail(listing.id) },
                                                    modifier = Modifier.width(220.dp)
                                                )
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
}

@Composable
private fun PublicProfileHeader(profile: PublicProfileData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(148.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!profile.profilePictureUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile.profilePictureUrl,
                        contentDescription = profile.name,
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                profile.name.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
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
                                modifier = Modifier.size(14.dp),
                                tint = BrandGreen
                            )
                        }
                    }
                    RatingRow(profile)
                }
            }
        }
    }
}

@Composable
private fun RatingRow(profile: PublicProfileData) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = if (profile.averageRating > 0) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(14.dp)
        )
        Text(
            if (profile.reviewCount > 0 && profile.averageRating > 0) {
                stringResource(
                    R.string.profile_rating_summary,
                    String.format("%.1f", profile.averageRating),
                    profile.reviewCount
                )
            } else {
                stringResource(R.string.profile_detail_no_reviews)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProfileStatsGrid(profile: PublicProfileData) {
    val isGrocery = profile.type.equals("grocery", ignoreCase = true)
    val mealsSaved = profile.completedPickups * 8

    ProfileSectionCard(stringResource(R.string.profile_section_impact)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell(
                icon = Icons.Default.DateRange,
                label = stringResource(R.string.profile_stat_joined_since),
                value = DateTimeUtils.formatDate(profile.createdAt),
                color = BrandGreen,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(modifier = Modifier.height(64.dp))
            StatCell(
                icon = if (isGrocery) Icons.Default.ShoppingCart else Icons.Default.VolunteerActivism,
                label = if (isGrocery) {
                    stringResource(R.string.profile_stat_orders_processed)
                } else {
                    stringResource(R.string.profile_stat_food_distributed)
                },
                value = profile.completedPickups.toString(),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(modifier = Modifier.height(64.dp))
            StatCell(
                icon = Icons.Default.Restaurant,
                label = stringResource(R.string.profile_stat_meals_saved),
                value = mealsSaved.toString(),
                color = BrandTeal,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCell(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = color.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
        ) {
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContactInformationSection(profile: PublicProfileData) {
    val context = LocalContext.current
    val address = profile.address?.takeIf { it.isNotBlank() }
        ?: profile.location?.takeIf { it.isNotBlank() }

    ProfileSectionCard(stringResource(R.string.label_contact_location)) {
        address?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                ClearChainActionIconButton(
                    icon = Icons.Default.Navigation,
                    contentDescription = stringResource(R.string.action_get_directions),
                    onClick = {
                        val query = profile.latitude?.let { lat ->
                            profile.longitude?.let { lng -> "$lat,$lng" }
                        } ?: it
                        openMap(context, query)
                    }
                )
            }
        }

        profile.phone?.takeIf { it.isNotBlank() }?.let { phone ->
            Row(
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    phone,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!profile.hours.isNullOrBlank()) {
            CompactInfoRow(Icons.Default.Schedule, profile.hours)
        }
        if (!profile.contactPerson.isNullOrBlank()) {
            CompactInfoRow(Icons.Default.Person, stringResource(R.string.label_contact_prefix, profile.contactPerson))
        }
    }
}

private fun openMap(context: Context, query: String) {
    val encoded = Uri.encode(query)
    val uri = Uri.parse("geo:0,0?q=$encoded")
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
        )
    }.onFailure {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$encoded")))
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun CompactInfoRow(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}
