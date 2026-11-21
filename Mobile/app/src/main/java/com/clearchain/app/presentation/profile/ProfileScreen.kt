package com.clearchain.app.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.clearchain.app.R
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.*
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAccountDetail: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    BlockBackWhile(state.isUploadingAvatar)
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                    event.message,
                    duration = SnackbarDuration.Short
                )
                is UiEvent.Navigate -> onLogout()
                else -> Unit
            }
        }
    }

    val profileTitle = stringResource(R.string.nav_profile)
    BrandScaffold(
        header = BrandHeader.Hero {
            state.user?.let { user ->
                ProfileHero(
                    user = user,
                    isUploadingAvatar = state.isUploadingAvatar,
                    onChangeAvatar = { showAvatarPickerDialog = true }
                )
            }
        },
        modifier = Modifier.semantics { paneTitle = profileTitle },
        snackbarHostState = snackbarHostState
    ) {
        when {
            state.isLoading && state.user == null ->
                LoadingState(Modifier.fillMaxWidth())

            state.user != null ->
                ProfileViewContent(
                    state = state,
                    onChangePassword = { showChangePasswordDialog = true },
                    onLogout = { showLogoutDialog = true },
                    onDeleteAccount = { showDeleteAccountDialog = true },
                    onNavigateToAnalytics = onNavigateToAnalytics,
                    onNavigateToHelp = onNavigateToHelp,
                    onNavigateToAccountDetail = onNavigateToAccountDetail,
                    onNavigateToSettings = onNavigateToSettings
                )

            else ->
                EmptyState(
                    icon = Icons.Default.ErrorOutline,
                    title = stringResource(R.string.error_generic),
                    subtitle = state.error,
                    actionLabel = stringResource(R.string.retry),
                    onAction = { viewModel.onEvent(ProfileEvent.Refresh) }
                )
        }
    }

    if (showLogoutDialog) {
        LogoutDialog(
            isLoading = state.isLoggingOut,
            onConfirm = { viewModel.onEvent(ProfileEvent.Logout) },
            onDismiss = { showLogoutDialog = false }
        )
    }

    // These dialogs stay open (confirm spinning) until their call settles.
    DismissWhenFinished(state.isChangingPassword) { showChangePasswordDialog = false }
    DismissWhenFinished(state.isUploadingAvatar) { showAvatarPickerDialog = false }
    DismissWhenFinished(state.isDeletingAccount) { showDeleteAccountDialog = false }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            isLoading = state.isChangingPassword,
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { current, newPassword ->
                viewModel.onEvent(ProfileEvent.ChangePassword(current, newPassword))
            }
        )
    }

    if (showAvatarPickerDialog) {
        PhotoPickerDialog(
            onPhotoSelected = { uri -> viewModel.onEvent(ProfileEvent.AvatarSelected(uri)) },
            confirmLoading = state.isUploadingAvatar,
            onDismiss = { showAvatarPickerDialog = false },
            title = stringResource(R.string.label_change_avatar),
            message = stringResource(R.string.msg_avatar_source),
            previewMessage = stringResource(R.string.msg_use_this_avatar)
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            isLoading = state.isDeletingAccount,
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = { password -> viewModel.onEvent(ProfileEvent.DeleteAccount(password)) }
        )
    }
}

/** Profile's rows, straight onto the brand sheet: account, analytics, help, settings and the account actions. */
@Composable
private fun ProfileViewContent(
    state: ProfileState,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAccountDetail: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val user = state.user!!

    DashboardActionCard(
        icon = Icons.Default.Info,
        title = stringResource(R.string.account_detail),
        onClick = onNavigateToAccountDetail
    )
    // Admins have their own Statistics tab; the analytics screen reports
    // reputation and pickup figures that only apply to an NGO or grocery.
    if (user.type != OrganizationType.ADMIN) {
        DashboardActionCard(
            icon = Icons.Default.BarChart,
            title = stringResource(R.string.label_analytics),
            onClick = onNavigateToAnalytics
        )
    }
    DashboardActionCard(
        icon = Icons.AutoMirrored.Filled.HelpOutline,
        title = stringResource(R.string.label_help_faq),
        onClick = onNavigateToHelp
    )
    DashboardActionCard(
        icon = Icons.Default.Settings,
        title = stringResource(R.string.settings),
        onClick = onNavigateToSettings
    )
    DashboardActionCard(
        icon = Icons.Default.Lock,
        title = stringResource(R.string.label_change_password),
        onClick = onChangePassword
    )
    DashboardActionCard(
        icon = Icons.AutoMirrored.Filled.Logout,
        title = stringResource(R.string.logout),
        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
        iconTint = MaterialTheme.colorScheme.error,
        titleColor = MaterialTheme.colorScheme.error,
        onClick = onLogout
    )
    DashboardActionCard(
        icon = Icons.Default.DeleteForever,
        title = stringResource(R.string.label_delete_account),
        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
        iconTint = MaterialTheme.colorScheme.error,
        titleColor = MaterialTheme.colorScheme.error,
        onClick = onDeleteAccount
    )
}

/**
 * Profile's gradient header: the avatar tile (the picture, or the role icon standing in for it),
 * the organization's name and its type and verification badges. It wraps its content, so it
 * grows with large text instead of clipping.
 */
@Composable
private fun ProfileHero(
    user: Organization,
    isUploadingAvatar: Boolean = false,
    onChangeAvatar: () -> Unit = {}
) {
    val context = LocalContext.current
    val changeAvatarLabel = stringResource(R.string.cd_change_avatar)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHPadding)
            .padding(top = ScreenVPadding, bottom = ScreenHPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ListGap)
    ) {
        // Tapping the avatar (or the role icon standing in for it) opens the
        // photo picker; every role owns a profile picture, so it is always enabled.
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(ProfileAvatarSize)
                    .clip(MaterialTheme.shapes.large)
                    .background(OnBrandGradient.copy(alpha = Alpha.LogoDisc))
                    .clickable(
                        enabled = !isUploadingAvatar,
                        onClickLabel = changeAvatarLabel,
                        onClick = onChangeAvatar
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!user.profilePictureUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(user.profilePictureUrl).crossfade(true).build(),
                        contentDescription = stringResource(R.string.cd_profile_photo_of, user.name),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = when (user.type) {
                            OrganizationType.GROCERY -> Icons.Default.Store
                            OrganizationType.NGO -> Icons.Default.VolunteerActivism
                            OrganizationType.ADMIN -> Icons.Default.AdminPanelSettings
                        },
                        contentDescription = stringResource(R.string.cd_org_type, user.type.name.lowercase()),
                        modifier = Modifier.size(ProfileAvatarIconSize),
                        tint = OnBrandGradient
                    )
                }

                if (isUploadingAvatar) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = Alpha.Scrim)),
                        contentAlignment = Alignment.Center
                    ) {
                        InlineSpinner(color = OnBrandGradient)
                    }
                }
            }

            // Hangs half a gap past the tile's corner, as a badge does.
            Box(
                modifier = Modifier
                    .offset(x = ListGap / 2, y = ListGap / 2)
                    .size(ActionIconButtonSize)
                    .clip(CircleShape)
                    .background(OnBrandGradient)
                    .clickable(
                        enabled = !isUploadingAvatar,
                        onClickLabel = changeAvatarLabel,
                        onClick = onChangeAvatar
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = changeAvatarLabel,
                    modifier = Modifier.size(DetailIconSize),
                    // Brand teal rather than the theme's primary: the disc is white in both themes.
                    tint = BrandTeal
                )
            }
        }

        Text(
            text = user.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = OnBrandGradient,
            textAlign = TextAlign.Center
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(ListGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(
                label = when (user.type) {
                    OrganizationType.GROCERY -> stringResource(R.string.role_grocery)
                    OrganizationType.NGO -> stringResource(R.string.role_ngo)
                    OrganizationType.ADMIN -> stringResource(R.string.role_admin)
                },
                backgroundColor = OnBrandGradient.copy(alpha = Alpha.LogoDisc),
                contentColor = OnBrandGradient
            )
            when (user.verificationStatus) {
                VerificationStatus.APPROVED ->
                    StatusBadge(
                        label = stringResource(R.string.label_verified_badge),
                        backgroundColor = OnBrandGradient.copy(alpha = Alpha.LogoDisc),
                        contentColor = OnBrandGradient,
                        icon = Icons.Default.CheckCircle
                    )
                VerificationStatus.PENDING ->
                    StatusBadge(
                        label = stringResource(R.string.label_pending_review_badge),
                        backgroundColor = StatusColors.PendingBg,
                        contentColor = StatusColors.PendingOnBg,
                        icon = Icons.Default.Schedule
                    )
                VerificationStatus.REJECTED ->
                    StatusBadge(
                        label = stringResource(R.string.label_rejected_badge),
                        backgroundColor = StatusColors.RejectedBg,
                        contentColor = StatusColors.RejectedOnBg,
                        icon = Icons.Default.Cancel
                    )
            }
        }
    }
}

// ── Change password dialog ───────────────────────────────────────────────────

@Composable
private fun ChangePasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val errCurrentRequired = stringResource(R.string.error_current_password_required)
    val errNewRequired = stringResource(R.string.error_new_password_required)
    val errMinLength = stringResource(R.string.error_password_min_length)
    val errNeedsUppercase = stringResource(R.string.error_password_needs_uppercase)
    val errNeedsLowercase = stringResource(R.string.error_password_needs_lowercase)
    val errNeedsNumber = stringResource(R.string.error_password_needs_number)
    val errDontMatch = stringResource(R.string.error_passwords_dont_match)

    ConfirmDialog(
        onDismiss = onDismiss,
        icon = Icons.Default.Lock,
        title = stringResource(R.string.label_change_password),
        message = stringResource(R.string.msg_change_password),
        confirmLabel = stringResource(R.string.action_change),
        dismissLabel = stringResource(R.string.cancel),
        confirmEnabled = !isLoading,
        confirmLoading = isLoading,
        onConfirm = {
            when {
                currentPassword.isBlank() -> error = errCurrentRequired
                newPassword.isBlank() -> error = errNewRequired
                newPassword.length < 8 -> error = errMinLength
                !newPassword.any { it.isUpperCase() } -> error = errNeedsUppercase
                !newPassword.any { it.isLowerCase() } -> error = errNeedsLowercase
                !newPassword.any { it.isDigit() } -> error = errNeedsNumber
                newPassword != confirmNewPassword -> error = errDontMatch
                else -> onConfirm(currentPassword, newPassword)
            }
        }
    ) {
        ClearChainTextField(
            value = currentPassword,
            onValueChange = {
                currentPassword = it
                error = null
            },
            label = stringResource(R.string.label_current_password),
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            isPassword = true,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
        ClearChainTextField(
            value = newPassword,
            onValueChange = {
                newPassword = it
                error = null
            },
            label = stringResource(R.string.label_new_password),
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            isPassword = true,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
        ClearChainTextField(
            value = confirmNewPassword,
            onValueChange = {
                confirmNewPassword = it
                error = null
            },
            label = stringResource(R.string.label_confirm_new_password),
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ── Logout dialog ────────────────────────────────────────────────────────────

@Composable
private fun LogoutDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ConfirmDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        title = stringResource(R.string.logout),
        message = stringResource(R.string.msg_logout_confirm),
        confirmLabel = stringResource(R.string.logout),
        dismissLabel = stringResource(R.string.cancel),
        isDestructive = true,
        icon = Icons.AutoMirrored.Filled.Logout,
        confirmEnabled = !isLoading,
        confirmLoading = isLoading,
        dismissEnabled = !isLoading,
        dismissible = !isLoading
    )
}

// ── Delete account dialog ────────────────────────────────────────────────────

@Composable
private fun DeleteAccountDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    ConfirmDialog(
        onDismiss = onDismiss,
        onConfirm = { if (password.isNotBlank()) onConfirm(password) },
        icon = Icons.Default.DeleteForever,
        title = stringResource(R.string.label_delete_account),
        message = stringResource(R.string.msg_delete_account_warning),
        confirmLabel = stringResource(R.string.label_delete_account),
        dismissLabel = stringResource(R.string.cancel),
        isDestructive = true,
        confirmEnabled = password.isNotBlank() && !isLoading,
        confirmLoading = isLoading,
        dismissEnabled = !isLoading,
        dismissible = !isLoading
    ) {
        ClearChainTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.label_confirm_password),
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
    }
}
