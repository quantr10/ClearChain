package com.clearchain.app.presentation.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.util.UiEvent
import com.clearchain.app.util.mapsQuery
import com.clearchain.app.util.openInGoogleMaps

@Composable
fun AccountDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val user = state.user
    SnackbarMessageEffect(snackbarHostState, state.error)

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            if (event is UiEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
            }
        }
    }

    BackHandler(state.isEditing) {
        viewModel.onEvent(ProfileEvent.CancelEdit)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
          ScreenTitleRow(
              title = stringResource(R.string.title_account_details),
              onBack = {
                  if (state.isEditing) viewModel.onEvent(ProfileEvent.CancelEdit) else onNavigateBack()
              },
              modifier = Modifier.padding(horizontal = 16.dp)
          )
          Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading && user == null -> {
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center)
                    )
                }

                user == null -> {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = stringResource(R.string.no_data),
                        onAction = {}
                    )
                }

                else -> {
                    if (state.isEditing) {
                        AccountDetailEditContent(
                            state = state,
                            onEvent = viewModel::onEvent
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(ScreenPadding),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OrganizationSummaryCard(
                                user = user,
                                averageRating = state.averageRating,
                                reviewCount = state.reviewCount,
                                onEdit = { viewModel.onEvent(ProfileEvent.StartEdit) }
                            )

                            if (!user.description.isNullOrBlank()) {
                                AccountSectionCard(stringResource(R.string.about)) {
                                    Text(
                                        text = user.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            AccountSectionCard(stringResource(R.string.section_contact)) {
                                CompactAccountDetailRow(
                                    icon = Icons.Default.Email,
                                    label = "",
                                    value = user.email,
                                    isAction = true
                                ) {
                                    context.startActivity(
                                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${user.email}"))
                                    )
                                }
                                CompactAccountDetailRow(
                                    icon = Icons.Default.Phone,
                                    label = "",
                                    value = user.phone.ifBlank { stringResource(R.string.label_not_set) },
                                    enabled = user.phone.isNotBlank(),
                                    isAction = true
                                ) {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone}"))
                                    )
                                }
                            }

                            AccountSectionCard(stringResource(R.string.section_location_hours)) {
                                val addressParts = listOfNotNull(
                                    user.address.substringBefore(',').trim().takeIf { it.isNotBlank() },
                                    user.location.trim().takeIf { it.isNotBlank() },
                                    user.state?.trim()?.takeIf { it.isNotBlank() },
                                    user.zipCode?.trim()?.takeIf { it.isNotBlank() }
                                )
                                if (addressParts.isNotEmpty()) {
                                    val fullAddress = addressParts.joinToString(", ")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Home,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            fullAddress,
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
                                                    mapsQuery(user.latitude, user.longitude, fullAddress)
                                                )
                                            }
                                        )
                                    }
                                } else {
                                    CompactAccountDetailRow(
                                        Icons.Default.Home,
                                        "",
                                        stringResource(R.string.label_not_set)
                                    )
                                }
                                CompactAccountDetailRow(
                                    Icons.Default.Schedule,
                                    "",
                                    user.hours ?: stringResource(R.string.label_not_set)
                                )
                            }

                            if (user.type == OrganizationType.GROCERY) {
                                AccountSectionCard(
                                    stringResource(R.string.onboarding_pickup_instructions_label)
                                ) {
                                    Text(
                                        text = user.pickupInstructions
                                            ?: stringResource(R.string.label_not_set),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            TeamMembersCard(user = user)
                        }
                    }
                }
            }
          }
        }
    }
}

@Composable
private fun AccountDetailEditContent(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit
) {
    val busy = state.isSavingProfile
    val isNgoOrGrocery = state.user?.type == OrganizationType.NGO ||
        state.user?.type == OrganizationType.GROCERY

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Organization Name ────────────────────────────────────────────────
        FieldCard(label = stringResource(R.string.org_name_label)) {
            ClearChainTextField(
                value = state.editName,
                onValueChange = { onEvent(ProfileEvent.EditNameChanged(it)) },
                placeholder = stringResource(R.string.org_name_placeholder),
                leadingIcon = Icons.Default.Business,
                imeAction = ImeAction.Next,
                isError = state.editNameError != null,
                errorMessage = state.editNameError,
                enabled = !busy
            )
        }

        // ── Description ──────────────────────────────────────────────────────
        FieldCard(label = stringResource(R.string.label_description), isOptional = true) {
            ClearChainTextField(
                value = state.editDescription,
                onValueChange = { onEvent(ProfileEvent.EditDescriptionChanged(it)) },
                placeholder = stringResource(R.string.hint_org_description),
                leadingIcon = Icons.Default.Description,
                imeAction = ImeAction.Next,
                enabled = !busy,
                singleLine = false,
                minLines = 2,
                maxLines = 4
            )
        }

        // ── Email ────────────────────────────────────────────────────────────
        FieldCard(label = stringResource(R.string.label_email)) {
            ClearChainTextField(
                value = state.editEmail,
                onValueChange = { onEvent(ProfileEvent.EditEmailChanged(it)) },
                placeholder = stringResource(R.string.hint_email_org),
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                isError = state.editEmailError != null,
                errorMessage = state.editEmailError,
                enabled = !busy
            )
        }

        // ── Phone ────────────────────────────────────────────────────────────
        FieldCard(label = stringResource(R.string.onboarding_phone_label)) {
            ClearChainTextField(
                value = state.editPhone,
                onValueChange = { onEvent(ProfileEvent.EditPhoneChanged(it)) },
                placeholder = stringResource(R.string.hint_phone_profile),
                leadingIcon = Icons.Default.Phone,
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
                isError = state.editPhoneError != null,
                errorMessage = state.editPhoneError,
                enabled = !busy
            )
        }

        // ── Address ──────────────────────────────────────────────────────────
        FieldCard(label = stringResource(R.string.onboarding_address_label)) {
            AddressSuggestionField(
                value = state.editAddress,
                onValueChange = { onEvent(ProfileEvent.EditAddressChanged(it)) },
                onAddressSelected = { suggestion ->
                    onEvent(ProfileEvent.EditAddressChanged(suggestion.streetAddress))
                    onEvent(ProfileEvent.EditLocationChanged(suggestion.city))
                    onEvent(ProfileEvent.EditStateChanged(suggestion.state))
                    onEvent(ProfileEvent.EditZipCodeChanged(suggestion.zipCode))
                    onEvent(ProfileEvent.EditLocationCoordsChanged(suggestion.latitude, suggestion.longitude))
                },
                showLabel = false,
                placeholder = stringResource(R.string.onboarding_address_placeholder),
                enabled = !busy,
                isError = state.editAddressError != null,
                errorMessage = state.editAddressError
            )
        }

        // ── City ─────────────────────────────────────────────────────────────
        FieldCard(label = stringResource(R.string.onboarding_city_label)) {
            ClearChainTextField(
                value = state.editLocation,
                onValueChange = { onEvent(ProfileEvent.EditLocationChanged(it)) },
                placeholder = stringResource(R.string.onboarding_city_placeholder),
                leadingIcon = Icons.Default.Place,
                imeAction = ImeAction.Next,
                isError = state.editLocationError != null,
                errorMessage = state.editLocationError,
                enabled = !busy
            )
        }

        // ── State + ZIP ──────────────────────────────────────────────────────
        FieldCard(label = stringResource(R.string.onboarding_state_label)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ClearChainTextField(
                    value = state.editState,
                    onValueChange = { onEvent(ProfileEvent.EditStateChanged(it)) },
                    placeholder = stringResource(R.string.onboarding_state_placeholder),
                    leadingIcon = Icons.Default.Map,
                    imeAction = ImeAction.Next,
                    isError = state.editStateError != null,
                    errorMessage = state.editStateError,
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                )
                ClearChainTextField(
                    value = state.editZipCode,
                    onValueChange = { onEvent(ProfileEvent.EditZipCodeChanged(it)) },
                    placeholder = stringResource(R.string.onboarding_zip_placeholder),
                    leadingIcon = Icons.Default.LocalPostOffice,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                    isError = state.editZipCodeError != null,
                    errorMessage = state.editZipCodeError,
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Opening Hours ────────────────────────────────────────────────────
        FieldCard(label = stringResource(R.string.onboarding_hours_label)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    TimePickerField(
                        value = state.editOpenTime,
                        onTimeSelected = { onEvent(ProfileEvent.EditOpenTimeChanged(it)) },
                        label = "",
                        isError = state.editOpenTimeError != null,
                        errorMessage = state.editOpenTimeError,
                        enabled = !busy
                    )
                }
                Box(Modifier.weight(1f)) {
                    TimePickerField(
                        value = state.editCloseTime,
                        onTimeSelected = { onEvent(ProfileEvent.EditCloseTimeChanged(it)) },
                        label = "",
                        isError = state.editCloseTimeError != null,
                        errorMessage = state.editCloseTimeError,
                        enabled = !busy
                    )
                }
            }
        }

        // ── Pickup Instructions (Grocery only) ───────────────────────────────
        if (state.user?.type == OrganizationType.GROCERY) {
            FieldCard(
                label = stringResource(R.string.onboarding_pickup_instructions_label),
                isOptional = true
            ) {
                ClearChainTextField(
                    value = state.editPickupInstructions,
                    onValueChange = { onEvent(ProfileEvent.EditPickupInstructionsChanged(it)) },
                    placeholder = stringResource(R.string.hint_pickup_instructions_long),
                    leadingIcon = Icons.Default.DirectionsWalk,
                    imeAction = ImeAction.Done,
                    enabled = !busy,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3
                )
            }
        }

        // ── Contact Person (NGO / Grocery) ───────────────────────────────────
        if (isNgoOrGrocery) {
            FieldCard(label = stringResource(R.string.label_contact_person)) {
                ClearChainTextField(
                    value = state.editContactPerson,
                    onValueChange = { onEvent(ProfileEvent.EditContactPersonChanged(it)) },
                    placeholder = stringResource(R.string.hint_contact_person),
                    leadingIcon = Icons.Default.Person,
                    imeAction = ImeAction.Done,
                    isError = state.editContactPersonError != null,
                    errorMessage = state.editContactPersonError,
                    enabled = !busy
                )
            }
        }

        // ── Cancel + Save buttons ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ClearChainOutlinedButton(
                text = stringResource(R.string.cancel),
                onClick = { onEvent(ProfileEvent.CancelEdit) },
                modifier = Modifier.weight(1f),
                enabled = !busy
            )
            ClearChainButton(
                text = stringResource(R.string.save),
                onClick = { onEvent(ProfileEvent.SaveProfile) },
                loading = busy,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Save
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FieldCard(
    label: String,
    modifier: Modifier = Modifier,
    isOptional: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OptionalFieldLabel(
                text = label.replace("*", "").trim(),
                isOptional = isOptional,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun OrganizationSummaryCard(
    user: com.clearchain.app.domain.model.Organization,
    averageRating: Double,
    reviewCount: Int,
    onEdit: () -> Unit
) {
    val roleLabel = when (user.type) {
        OrganizationType.GROCERY -> stringResource(R.string.role_grocery)
        OrganizationType.NGO -> stringResource(R.string.role_ngo)
        OrganizationType.ADMIN -> stringResource(R.string.role_admin)
    }
    ProfileSummaryCard(
        name = user.name,
        roleLabel = roleLabel,
        verified = user.verified,
        averageRating = averageRating,
        reviewCount = reviewCount,
        profilePictureUrl = user.profilePictureUrl,
        onEdit = onEdit
    )
}

@Composable
private fun AccountSectionCard(
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
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun CompactAccountDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    enabled: Boolean = true,
    isAction: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val actionModifier = if (enabled && onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    val valueColor = if (isAction && enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(actionModifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
        if (isAction && enabled) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun TeamMembersCard(user: com.clearchain.app.domain.model.Organization) {
    AccountSectionCard(stringResource(R.string.section_team_members)) {
        if (!user.contactPerson.isNullOrBlank()) {
            MemberRow(
                name = user.contactPerson,
                badge = stringResource(R.string.label_contact_badge),
                tint = MaterialTheme.colorScheme.secondaryContainer,
                onTint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            HorizontalDivider()
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.label_invite_team_member),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = stringResource(R.string.label_coming_soon),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MemberRow(
    name: String,
    badge: String,
    tint: androidx.compose.ui.graphics.Color,
    onTint: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Default.Person, null, tint = onTint, modifier = Modifier.size(14.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Surface(shape = MaterialTheme.shapes.small, color = tint) {
            Text(badge, style = MaterialTheme.typography.labelSmall, color = onTint, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}
