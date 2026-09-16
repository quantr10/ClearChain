package com.clearchain.app.presentation.publicprofile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.ListingApi
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.api.PublicProfileData
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.util.DateTimeUtils
import com.clearchain.app.util.mapsQuery
import com.clearchain.app.util.openInGoogleMaps
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── State ────────────────────────────────────────────────────────────────────
data class PublicProfileState(
    val profile: PublicProfileData? = null,
    val moreFromStore: List<Listing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

// ── ViewModel ────────────────────────────────────────────────────────────────
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

// ── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToListingDetail: (String) -> Unit = {},
    viewModel: PublicProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenTitleRow(
                title = stringResource(R.string.title_organization_profile),
                onBack = onNavigateBack,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
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
                            Column(
                                modifier = Modifier.padding(ScreenPadding),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // ── Header ───────────────────────────────────
                                PublicProfileHeader(profile)

                                // ── Description ──────────────────────────────
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

                                // ── Contact / Location & Hours ───────────────
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
}

@Composable
private fun PublicProfileHeader(profile: PublicProfileData) {
    val roleLabel = when (profile.type.lowercase()) {
        "grocery" -> stringResource(R.string.role_grocery)
        "ngo" -> stringResource(R.string.role_ngo)
        "admin" -> stringResource(R.string.role_admin)
        else -> profile.type.replaceFirstChar { it.uppercase() }
    }
    ProfileSummaryCard(
        name = profile.name,
        roleLabel = roleLabel,
        verified = profile.verified,
        averageRating = profile.averageRating,
        reviewCount = profile.reviewCount,
        profilePictureUrl = profile.profilePictureUrl
    )
}

@Composable
private fun ProfileStatsGrid(profile: PublicProfileData) {
    val isGrocery = profile.type.equals("grocery", ignoreCase = true)
    val mealsSaved = profile.mealsEstimate

    ProfileSectionCard(stringResource(R.string.profile_section_impact)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            shape = RoundedCornerShape(10.dp),
            color = color.copy(alpha = 0.12f)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContactInformationSection(profile: PublicProfileData) {
    val context = LocalContext.current
    val email = profile.email?.takeIf { it.isNotBlank() }
    val phone = profile.phone?.takeIf { it.isNotBlank() }
    val hours = profile.hours?.takeIf { it.isNotBlank() }
    val contactPerson = profile.contactPerson?.takeIf { it.isNotBlank() }

    // Street (up to the first comma, in case the stored address already includes
    // the city) + city + state + ZIP, on one line.
    val fullAddress = listOfNotNull(
        profile.address?.substringBefore(',')?.trim()?.takeIf { it.isNotBlank() },
        profile.location?.trim()?.takeIf { it.isNotBlank() },
        profile.state?.trim()?.takeIf { it.isNotBlank() },
        profile.zipCode?.trim()?.takeIf { it.isNotBlank() }
    ).joinToString(", ").takeIf { it.isNotBlank() }

    if (email != null || phone != null) {
        ProfileSectionCard(stringResource(R.string.section_contact)) {
            email?.let {
                ContactLinkRow(Icons.Default.Email, it) {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$it")))
                }
            }
            phone?.let {
                ContactLinkRow(Icons.Default.Phone, it) {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it")))
                }
            }
        }
    }

    if (fullAddress != null || hours != null) {
        ProfileSectionCard(stringResource(R.string.section_location_hours)) {
            fullAddress?.let {
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
                            openInGoogleMaps(
                                context,
                                mapsQuery(profile.latitude, profile.longitude, it)
                            )
                        }
                    )
                }
            }
            hours?.let {
                CompactInfoRow(Icons.Default.Schedule, it)
            }
        }
    }

    contactPerson?.let {
        ProfileSectionCard(stringResource(R.string.section_team_members)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        stringResource(R.string.label_contact_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactLinkRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
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
