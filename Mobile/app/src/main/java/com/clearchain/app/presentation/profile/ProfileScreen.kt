package com.clearchain.app.presentation.profile

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.components.AddressSuggestionField
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal
import com.clearchain.app.util.DateTimeUtils
import com.clearchain.app.util.UiEvent

// ═══════════════════════════════════════════════════════════════════════════════
// Root
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                    event.message, duration = SnackbarDuration.Short
                )
                is UiEvent.Navigate -> onLogout()
                else -> {}
            }
        }
    }

    if (state.isEditing) {
        EditProfileScaffold(
            state             = state,
            snackbarHostState = snackbarHostState,
            onEvent           = viewModel::onEvent
        )
        return
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.user == null ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.user != null ->
                    ProfileViewContent(
                        state                 = state,
                        onNavigateBack        = onNavigateBack,
                        onEditClick           = { viewModel.onEvent(ProfileEvent.StartEdit) },
                        onChangePassword      = { showChangePasswordDialog = true },
                        onLogout              = { showLogoutDialog = true },
                        onAvatarSelected      = { uri -> viewModel.onEvent(ProfileEvent.AvatarSelected(uri)) },
                        onDeleteAccount       = { showDeleteAccountDialog = true },
                        onRefresh             = { viewModel.onEvent(ProfileEvent.Refresh) },
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onNavigateToHelp      = onNavigateToHelp
                    )

                else ->
                    EmptyState(
                        icon        = Icons.Default.ErrorOutline,
                        title       = stringResource(R.string.error_failed_load_profile),
                        actionLabel = stringResource(R.string.retry),
                        onAction    = { viewModel.onEvent(ProfileEvent.Refresh) }
                    )
            }
        }
    }

    if (showLogoutDialog) {
        DestructiveConfirmDialog(
            title        = stringResource(R.string.logout),
            message      = "",
            confirmLabel = stringResource(R.string.logout),
            onConfirm    = { showLogoutDialog = false; onLogout() },
            onDismiss    = { showLogoutDialog = false }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            isLoading = state.isChangingPassword,
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { current, new_ ->
                viewModel.onEvent(ProfileEvent.ChangePassword(current, new_))
                showChangePasswordDialog = false
            }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            isLoading = state.isDeletingAccount,
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = { password ->
                viewModel.onEvent(ProfileEvent.DeleteAccount(password))
                showDeleteAccountDialog = false
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// View mode
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileViewContent(
    state: ProfileState,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onRefresh: () -> Unit,
    onAvatarSelected: (android.net.Uri) -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {}
) {
    val user          = state.user!!
    val context       = LocalContext.current
    val missingFields = user.getMissingFields()
    val totalRequired = if (user.type == OrganizationType.ADMIN) 0 else 5
    val filledCount   = (totalRequired - missingFields.size).coerceAtLeast(0)
    val completeness  = if (totalRequired == 0) 1f else filledCount.toFloat() / totalRequired

    val avatarPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onAvatarSelected(uri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Hero ──────────────────────────────────────────────────────────
        ProfileHero(
            user               = user,
            onNavigateBack     = onNavigateBack,
            onEditClick        = onEditClick,
            isUploadingAvatar  = state.isUploadingAvatar,
            onAvatarClick      = {
                avatarPickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.label_edit_profile))
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Verification alert ─────────────────────────────────────────
            when (user.verificationStatus) {
                VerificationStatus.PENDING ->
                    AlertBanner(
                        message = stringResource(R.string.msg_org_under_review),
                        type    = AlertType.WARNING,
                        icon    = Icons.Default.Schedule
                    )
                VerificationStatus.REJECTED ->
                    AlertBanner(
                        message = stringResource(R.string.msg_verification_rejected_profile),
                        type    = AlertType.ERROR,
                        icon    = Icons.Default.Cancel
                    )
                VerificationStatus.APPROVED -> Unit
            }

            // ── Profile completeness ───────────────────────────────────────
            if (missingFields.isNotEmpty()) {
                ProfileCompletenessCard(
                    filledCount   = filledCount,
                    totalRequired = totalRequired,
                    completeness  = completeness,
                    missingFields = missingFields,
                    onEditClick   = onEditClick
                )
            }

            // ── About ──────────────────────────────────────────────────────
            if (!user.description.isNullOrBlank()) {
                DashboardSection(title = stringResource(R.string.about)) {
                    InfoCard {
                        Text(
                            text  = user.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // ── Contact ────────────────────────────────────────────────────
            DashboardSection(title = stringResource(R.string.section_contact)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContactActionRow(
                        icon  = Icons.Default.Email,
                        label = stringResource(R.string.email),
                        value = user.email
                    ) {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${user.email}"))
                        )
                    }
                    ContactActionRow(
                        icon    = Icons.Default.Phone,
                        label   = stringResource(R.string.onboarding_phone_label),
                        value   = user.phone.ifBlank { stringResource(R.string.label_not_set) },
                        enabled = user.phone.isNotBlank()
                    ) {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone}"))
                        )
                    }
                }
            }

            // ── Location & Hours ───────────────────────────────────────────
            DashboardSection(title = stringResource(R.string.section_location_hours)) {
                InfoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        InfoRow(Icons.Default.Home,     stringResource(R.string.onboarding_address_label), user.address.ifBlank { stringResource(R.string.label_not_set) })
                        InfoRow(Icons.Default.Place,    stringResource(R.string.onboarding_city_label),    user.location.ifBlank { stringResource(R.string.label_not_set) })
                        InfoRow(Icons.Default.Schedule, stringResource(R.string.onboarding_hours_label),   user.hours ?: stringResource(R.string.label_not_set))
                        if (user.latitude != null && user.longitude != null) {
                            InfoRow(
                                icon  = Icons.Default.MyLocation,
                                label = stringResource(R.string.label_gps_coordinates),
                                value = "%.5f, %.5f".format(user.latitude, user.longitude)
                            )
                        }
                    }
                }
            }

            // ── Organization details ───────────────────────────────────────
            if (user.type != OrganizationType.ADMIN) {
                DashboardSection(title = stringResource(R.string.section_org_details)) {
                    InfoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            InfoRow(
                                icon  = Icons.Default.Person,
                                label = stringResource(R.string.onboarding_contact_label),
                                value = user.contactPerson ?: stringResource(R.string.label_not_set)
                            )
                            if (user.type == OrganizationType.GROCERY) {
                                InfoRow(
                                    icon  = Icons.Default.DirectionsWalk,
                                    label = stringResource(R.string.onboarding_pickup_instructions_label),
                                    value = user.pickupInstructions ?: stringResource(R.string.label_not_set)
                                )
                            }
                        }
                    }
                }
            }

            // ── Account Stats ──────────────────────────────────────────────
            state.stats?.let { stats ->
                DashboardSection(title = stringResource(R.string.account_stats)) {
                    AccountStatsCard(stats = stats, orgType = user.type)
                }
            }

            // ── Team Members ──────────────────────────────────────────────────
            DashboardSection(title = stringResource(R.string.section_team_members)) {
                TeamMembersCard(user = user)
            }

            // ── Account ────────────────────────────────────────────────────
            DashboardSection(title = stringResource(R.string.section_account)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoCard {
                        InfoRow(
                            icon  = Icons.Default.CalendarToday,
                            label = stringResource(R.string.label_member_since_field),
                            value = runCatching { DateTimeUtils.formatDate(user.createdAt) }
                                .getOrDefault(user.createdAt)
                        )
                    }
                    SettingsClickableRow(
                        icon    = Icons.Default.BarChart,
                        label   = stringResource(R.string.label_analytics),
                        onClick = onNavigateToAnalytics
                    )
                    SettingsClickableRow(
                        icon    = Icons.Default.HelpOutline,
                        label   = stringResource(R.string.label_help_faq),
                        onClick = onNavigateToHelp
                    )
                    SettingsClickableRow(
                        icon  = Icons.Default.Lock,
                        label = stringResource(R.string.label_change_password),
                        onClick = onChangePassword
                    )
                    SettingsClickableRow(
                        icon          = Icons.Default.Logout,
                        label         = stringResource(R.string.logout),
                        isDestructive = true,
                        onClick       = onLogout
                    )
                    SettingsClickableRow(
                        icon          = Icons.Default.DeleteForever,
                        label         = stringResource(R.string.label_delete_account),
                        isDestructive = true,
                        onClick       = onDeleteAccount
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Hero header
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileHero(
    user: Organization,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onAvatarClick: () -> Unit = {},
    isUploadingAvatar: Boolean = false
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(248.dp)
            .background(Brush.verticalGradient(listOf(BrandTeal, BrandGreen)))
    ) {
        IconButton(
            onClick  = onNavigateBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_back), tint = Color.White)
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar — tappable to change photo
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(enabled = !isUploadingAvatar, onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                if (!user.profilePictureUrl.isNullOrBlank()) {
                    AsyncImage(
                        model              = ImageRequest.Builder(context)
                            .data(user.profilePictureUrl).crossfade(true).build(),
                        contentDescription = stringResource(R.string.cd_profile_photo_of, user.name),
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = when (user.type) {
                            OrganizationType.GROCERY -> Icons.Default.Store
                            OrganizationType.NGO     -> Icons.Default.VolunteerActivism
                            OrganizationType.ADMIN   -> Icons.Default.AdminPanelSettings
                        },
                        contentDescription = stringResource(R.string.cd_org_type, user.type.name.lowercase()),
                        modifier           = Modifier.size(44.dp),
                        tint               = Color.White
                    )
                }
                // Camera overlay / upload spinner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingAvatar) {
                        CircularProgressIndicator(
                            modifier  = Modifier.size(14.dp),
                            color     = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = stringResource(R.string.cd_add_photo),
                            modifier           = Modifier.size(14.dp),
                            tint               = Color.White
                        )
                    }
                }
            }

            Text(
                text       = user.name,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                StatusBadge(
                    label           = when (user.type) {
                        OrganizationType.GROCERY -> stringResource(R.string.role_grocery)
                        OrganizationType.NGO     -> stringResource(R.string.role_ngo)
                        OrganizationType.ADMIN   -> stringResource(R.string.role_admin)
                    },
                    backgroundColor = Color.White.copy(alpha = 0.22f),
                    contentColor    = Color.White
                )
                when (user.verificationStatus) {
                    VerificationStatus.APPROVED ->
                        StatusBadge(
                            label           = stringResource(R.string.label_verified_badge),
                            backgroundColor = Color.White.copy(alpha = 0.22f),
                            contentColor    = Color.White,
                            icon            = Icons.Default.CheckCircle
                        )
                    VerificationStatus.PENDING ->
                        StatusBadge(
                            label           = stringResource(R.string.label_pending_review_badge),
                            backgroundColor = Color(0xFFD97706).copy(alpha = 0.85f),
                            contentColor    = Color.White,
                            icon            = Icons.Default.Schedule
                        )
                    VerificationStatus.REJECTED ->
                        StatusBadge(
                            label           = stringResource(R.string.label_rejected_badge),
                            backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            contentColor    = Color.White,
                            icon            = Icons.Default.Cancel
                        )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Profile completeness card
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileCompletenessCard(
    filledCount: Int,
    totalRequired: Int,
    completeness: Float,
    missingFields: List<String>,
    onEditClick: () -> Unit
) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text       = stringResource(R.string.msg_profile_pct_complete, (completeness * 100).toInt()),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text  = stringResource(R.string.msg_fields_filled, filledCount, totalRequired),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                TextButton(onClick = onEditClick) {
                    Text(stringResource(R.string.label_complete), fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            LinearProgressIndicator(
                progress   = { completeness },
                modifier   = Modifier.fillMaxWidth(),
                color      = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f)
            )
            Text(
                text  = stringResource(R.string.msg_missing_fields, missingFields.joinToString(" · ")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Shared small components
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ContactActionRow(
    icon: ImageVector,
    label: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val notSet = stringResource(R.string.label_not_set)
    Surface(
        shape          = RoundedCornerShape(12.dp),
        color          = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier       = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Surface(
                shape    = CircleShape,
                color    = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text       = value,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color      = if (enabled && value != notSet)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            if (enabled && value != notSet) {
                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val fg = if (isDestructive) MaterialTheme.colorScheme.error
             else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick        = onClick,
        shape          = RoundedCornerShape(12.dp),
        color          = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier       = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = fg)
            Text(label,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = fg,
                modifier   = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Edit mode scaffold
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileScaffold(
    state: ProfileState,
    snackbarHostState: SnackbarHostState,
    onEvent: (ProfileEvent) -> Unit
) {
    BackHandler { onEvent(ProfileEvent.CancelEdit) }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(stringResource(R.string.section_general))

            ClearChainTextField(
                value         = state.editName,
                onValueChange = { onEvent(ProfileEvent.EditNameChanged(it)) },
                label         = stringResource(R.string.org_name_label),
                leadingIcon   = Icons.Default.Business,
                imeAction     = ImeAction.Next,
                isError       = state.editNameError != null,
                errorMessage  = state.editNameError,
                enabled       = !state.isSavingProfile
            )
            ClearChainTextField(
                value         = state.editDescription,
                onValueChange = { onEvent(ProfileEvent.EditDescriptionChanged(it)) },
                label         = stringResource(R.string.label_description),
                placeholder   = stringResource(R.string.hint_org_description),
                leadingIcon   = Icons.Default.Description,
                imeAction     = ImeAction.Next,
                enabled       = !state.isSavingProfile,
                singleLine    = false,
                minLines      = 2,
                maxLines      = 4
            )

            SectionHeader(stringResource(R.string.section_contact))

            ClearChainTextField(
                value         = state.editPhone,
                onValueChange = { onEvent(ProfileEvent.EditPhoneChanged(it)) },
                label         = stringResource(R.string.onboarding_phone_label),
                placeholder   = stringResource(R.string.hint_phone_profile),
                leadingIcon   = Icons.Default.Phone,
                keyboardType  = KeyboardType.Phone,
                imeAction     = ImeAction.Next,
                isError       = state.editPhoneError != null,
                errorMessage  = state.editPhoneError,
                enabled       = !state.isSavingProfile
            )
            AddressSuggestionField(
                value             = state.editAddress,
                onValueChange     = { onEvent(ProfileEvent.EditAddressChanged(it)) },
                onAddressSelected = { s ->
                    onEvent(ProfileEvent.EditAddressChanged(s.fullAddress))
                    onEvent(ProfileEvent.EditLocationChanged(s.city))
                    onEvent(ProfileEvent.EditLocationCoordsChanged(s.latitude, s.longitude))
                },
                label       = stringResource(R.string.onboarding_address_label),
                placeholder = stringResource(R.string.onboarding_address_placeholder),
                enabled     = !state.isSavingProfile
            )
            ClearChainTextField(
                value         = state.editLocation,
                onValueChange = { onEvent(ProfileEvent.EditLocationChanged(it)) },
                label         = stringResource(R.string.label_city_location),
                placeholder   = stringResource(R.string.onboarding_city_placeholder),
                leadingIcon   = Icons.Default.Place,
                imeAction     = ImeAction.Next,
                enabled       = !state.isSavingProfile
            )

            SectionHeader(stringResource(R.string.onboarding_hours_label))

            TimePickerField(
                value          = state.editOpenTime,
                onTimeSelected = { onEvent(ProfileEvent.EditOpenTimeChanged(it)) },
                label          = stringResource(R.string.label_opening_time),
                enabled        = !state.isSavingProfile
            )
            TimePickerField(
                value          = state.editCloseTime,
                onTimeSelected = { onEvent(ProfileEvent.EditCloseTimeChanged(it)) },
                label          = stringResource(R.string.label_closing_time),
                enabled        = !state.isSavingProfile
            )

            if (state.user?.type == OrganizationType.NGO || state.user?.type == OrganizationType.GROCERY) {
                SectionHeader(stringResource(R.string.section_org_details))
                ClearChainTextField(
                    value         = state.editContactPerson,
                    onValueChange = { onEvent(ProfileEvent.EditContactPersonChanged(it)) },
                    label         = stringResource(R.string.label_contact_person_star),
                    placeholder   = stringResource(R.string.hint_contact_person),
                    leadingIcon   = Icons.Default.Person,
                    imeAction     = ImeAction.Next,
                    isError       = state.editContactPersonError != null,
                    errorMessage  = state.editContactPersonError,
                    enabled       = !state.isSavingProfile
                )
            }

            if (state.user?.type == OrganizationType.GROCERY) {
                ClearChainTextField(
                    value         = state.editPickupInstructions,
                    onValueChange = { onEvent(ProfileEvent.EditPickupInstructionsChanged(it)) },
                    label         = stringResource(R.string.onboarding_pickup_instructions_label),
                    placeholder   = stringResource(R.string.hint_pickup_instructions_long),
                    leadingIcon   = Icons.Default.DirectionsWalk,
                    imeAction     = ImeAction.Done,
                    enabled       = !state.isSavingProfile,
                    singleLine    = false,
                    minLines      = 2,
                    maxLines      = 3
                )
            }

            AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut()) {
                AlertBanner(
                    message = state.error ?: "",
                    type    = AlertType.ERROR,
                    icon    = Icons.Default.ErrorOutline
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClearChainOutlinedButton(
                    text     = stringResource(R.string.cancel),
                    onClick  = { onEvent(ProfileEvent.CancelEdit) },
                    modifier = Modifier.weight(1f)
                )
                ClearChainButton(
                    text     = stringResource(R.string.action_save_changes),
                    onClick  = { onEvent(ProfileEvent.SaveProfile) },
                    loading  = state.isSavingProfile,
                    enabled  = !state.isSavingProfile,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Change Password Dialog
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChangePasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword    by remember { mutableStateOf("") }
    var newPassword        by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var currentPwVisible   by remember { mutableStateOf(false) }
    var newPwVisible       by remember { mutableStateOf(false) }
    var error              by remember { mutableStateOf<String?>(null) }

    val errCurrentRequired  = stringResource(R.string.error_current_password_required)
    val errNewRequired      = stringResource(R.string.error_new_password_required)
    val errMinLength        = stringResource(R.string.error_password_min_length)
    val errNeedsUppercase   = stringResource(R.string.error_password_needs_uppercase)
    val errNeedsNumber      = stringResource(R.string.error_password_needs_number)
    val errDontMatch        = stringResource(R.string.error_passwords_dont_match)

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text(stringResource(R.string.label_change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value                = currentPassword,
                    onValueChange        = { currentPassword = it; error = null },
                    label                = { Text(stringResource(R.string.label_current_password)) },
                    visualTransformation = if (currentPwVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentPwVisible = !currentPwVisible }) {
                            Icon(if (currentPwVisible) Icons.Default.Visibility
                                 else Icons.Default.VisibilityOff,
                                 if (currentPwVisible) stringResource(R.string.label_hide_password) else stringResource(R.string.label_show_password))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value                = newPassword,
                    onValueChange        = { newPassword = it; error = null },
                    label                = { Text(stringResource(R.string.label_new_password)) },
                    visualTransformation = if (newPwVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPwVisible = !newPwVisible }) {
                            Icon(if (newPwVisible) Icons.Default.Visibility
                                 else Icons.Default.VisibilityOff,
                                 if (newPwVisible) stringResource(R.string.label_hide_password) else stringResource(R.string.label_show_password))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value                = confirmNewPassword,
                    onValueChange        = { confirmNewPassword = it; error = null },
                    label                = { Text(stringResource(R.string.label_confirm_new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier             = Modifier.fillMaxWidth(), singleLine = true
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    when {
                        currentPassword.isBlank()             -> error = errCurrentRequired
                        newPassword.isBlank()                 -> error = errNewRequired
                        newPassword.length < 8                -> error = errMinLength
                        !newPassword.any { it.isUpperCase() } -> error = errNeedsUppercase
                        !newPassword.any { it.isDigit() }     -> error = errNeedsNumber
                        newPassword != confirmNewPassword      -> error = errDontMatch
                        else -> onConfirm(currentPassword, newPassword)
                    }
                }
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp))
                else Text(stringResource(R.string.action_change))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Account stats card
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AccountStatsCard(stats: com.clearchain.app.domain.model.OrgStats, orgType: OrganizationType) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (orgType == OrganizationType.GROCERY) {
                StatItem(stringResource(R.string.status_active), stats.activeListings.toString(), Icons.Default.Inventory)
                StatItem(stringResource(R.string.status_pending), stats.pendingRequests.toString(), Icons.Default.Pending)
                StatItem(stringResource(R.string.stat_completed), stats.completed.toString(), Icons.Default.CheckCircle)
                StatItem(stringResource(R.string.impact_food_saved), "${stats.foodSaved} kg", Icons.Default.Eco)
            } else {
                StatItem(stringResource(R.string.label_stat_requests), stats.totalCompleted.toString(), Icons.Default.LocalShipping)
                StatItem(stringResource(R.string.stat_in_stock), stats.inStock.toString(), Icons.Default.Inventory)
                StatItem(stringResource(R.string.stat_distributed), stats.distributed.toString(), Icons.Default.VolunteerActivism)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Team Members Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TeamMembersCard(user: Organization) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MemberRow(
                name   = user.name,
                detail = user.email,
                badge  = stringResource(R.string.label_owner_badge),
                tint   = MaterialTheme.colorScheme.primaryContainer,
                onTint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (!user.contactPerson.isNullOrBlank()) {
                HorizontalDivider()
                MemberRow(
                    name   = user.contactPerson,
                    detail = stringResource(R.string.label_contact_person),
                    badge  = stringResource(R.string.label_contact_badge),
                    tint   = MaterialTheme.colorScheme.secondaryContainer,
                    onTint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            HorizontalDivider()

            // Invite placeholder (multi-user support requires backend infrastructure)
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PersonAdd, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.label_invite_team_member),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.label_coming_soon),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MemberRow(
    name: String,
    detail: String,
    badge: String,
    tint: Color,
    onTint: Color
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person, null,
                tint     = onTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(shape = RoundedCornerShape(4.dp), color = tint) {
            Text(
                badge,
                style    = MaterialTheme.typography.labelSmall,
                color    = onTint,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Delete Account dialog
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DeleteAccountDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon  = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.label_delete_account), color = MaterialTheme.colorScheme.error) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.msg_delete_account_warning))
                OutlinedTextField(
                    value         = password,
                    onValueChange = { password = it },
                    label         = { Text(stringResource(R.string.label_confirm_password)) },
                    singleLine    = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                 if (showPassword) stringResource(R.string.cd_hide_password) else stringResource(R.string.cd_show_password))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled  = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { if (password.isNotBlank()) onConfirm(password) },
                enabled  = password.isNotBlank() && !isLoading,
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.label_delete_account))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text(stringResource(R.string.cancel)) }
        }
    )
}
