package com.clearchain.app.presentation.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.UiEvent

@Composable
fun AccountDetailScreen(
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { viewModel.onEvent(ProfileEvent.StartEdit) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.cd_edit_profile)
                                    )
                                }
                            }

                            // About
                            if (!user.description.isNullOrBlank()) {
                                DashboardSection(
                                    title = stringResource(R.string.about)
                                ) {
                                    InfoCard {
                                        Text(
                                            text = user.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Contact
                            DashboardSection(
                                title = stringResource(R.string.section_contact)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {

                                    ContactActionRow(
                                        icon = Icons.Default.Email,
                                        label = stringResource(R.string.email),
                                        value = user.email
                                    ) {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_SENDTO,
                                                Uri.parse("mailto:${user.email}")
                                            )
                                        )
                                    }

                                    ContactActionRow(
                                        icon = Icons.Default.Phone,
                                        label = stringResource(R.string.onboarding_phone_label),
                                        value = user.phone.ifBlank {
                                            stringResource(R.string.label_not_set)
                                        },
                                        enabled = user.phone.isNotBlank()
                                    ) {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_DIAL,
                                                Uri.parse("tel:${user.phone}")
                                            )
                                        )
                                    }
                                }
                            }

                            // Location & Hours
                            DashboardSection(
                                title = stringResource(R.string.section_location_hours)
                            ) {
                                InfoCard {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        InfoRow(
                                            Icons.Default.Home,
                                            stringResource(R.string.onboarding_address_label),
                                            user.address.ifBlank {
                                                stringResource(R.string.label_not_set)
                                            }
                                        )

                                        InfoRow(
                                            Icons.Default.Place,
                                            stringResource(R.string.onboarding_city_label),
                                            user.location.ifBlank {
                                                stringResource(R.string.label_not_set)
                                            }
                                        )

                                        InfoRow(
                                            Icons.Default.Map,
                                            stringResource(R.string.onboarding_state_label),
                                            user.state?.takeIf { it.isNotBlank() }
                                                ?: stringResource(R.string.label_not_set)
                                        )

                                        InfoRow(
                                            Icons.Default.LocalPostOffice,
                                            stringResource(R.string.onboarding_zip_label),
                                            user.zipCode?.takeIf { it.isNotBlank() }
                                                ?: stringResource(R.string.label_not_set)
                                        )

                                        InfoRow(
                                            Icons.Default.Schedule,
                                            stringResource(R.string.onboarding_hours_label),
                                            user.hours
                                                ?: stringResource(R.string.label_not_set)
                                        )
                                    }
                                }
                            }

                            // Organization details
                            if (user.type != OrganizationType.ADMIN) {
                                DashboardSection(
                                    title = stringResource(R.string.section_org_details)
                                ) {
                                    InfoCard {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            InfoRow(
                                                Icons.Default.Person,
                                                stringResource(R.string.onboarding_contact_label),
                                                user.contactPerson
                                                    ?: stringResource(R.string.label_not_set)
                                            )

                                            if (user.type == OrganizationType.GROCERY) {
                                                InfoRow(
                                                    Icons.Default.DirectionsWalk,
                                                    stringResource(R.string.onboarding_pickup_instructions_label),
                                                    user.pickupInstructions
                                                        ?: stringResource(R.string.label_not_set)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Team Members
                            DashboardSection(
                                title = stringResource(R.string.section_team_members)
                            ) {
                                TeamMembersCard(user = user)
                            }

                            Spacer(Modifier.height(24.dp))
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(stringResource(R.string.section_general))

        ClearChainTextField(
            value = state.editName,
            onValueChange = { onEvent(ProfileEvent.EditNameChanged(it)) },
            label = stringResource(R.string.org_name_label),
            leadingIcon = Icons.Default.Business,
            imeAction = ImeAction.Next,
            isError = state.editNameError != null,
            errorMessage = state.editNameError,
            enabled = !state.isSavingProfile
        )

        ClearChainTextField(
            value = state.editDescription,
            onValueChange = { onEvent(ProfileEvent.EditDescriptionChanged(it)) },
            label = stringResource(R.string.label_description),
            isOptional = true,
            placeholder = stringResource(R.string.hint_org_description),
            leadingIcon = Icons.Default.Description,
            imeAction = ImeAction.Next,
            enabled = !state.isSavingProfile,
            singleLine = false,
            minLines = 2,
            maxLines = 4
        )

        SectionHeader(stringResource(R.string.section_contact))

        ClearChainTextField(
            value = state.editPhone,
            onValueChange = { onEvent(ProfileEvent.EditPhoneChanged(it)) },
            label = stringResource(R.string.onboarding_phone_label),
            isOptional = true,
            placeholder = stringResource(R.string.hint_phone_profile),
            leadingIcon = Icons.Default.Phone,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next,
            isError = state.editPhoneError != null,
            errorMessage = state.editPhoneError,
            enabled = !state.isSavingProfile
        )

        AddressSuggestionField(
            value = state.editAddress,
            onValueChange = { onEvent(ProfileEvent.EditAddressChanged(it)) },
            onAddressSelected = { suggestion ->
                onEvent(ProfileEvent.EditAddressChanged(suggestion.fullAddress))
                onEvent(ProfileEvent.EditLocationChanged(suggestion.city))
                onEvent(ProfileEvent.EditStateChanged(suggestion.state))
                onEvent(ProfileEvent.EditZipCodeChanged(suggestion.zipCode))
                onEvent(ProfileEvent.EditLocationCoordsChanged(suggestion.latitude, suggestion.longitude))
            },
            label = stringResource(R.string.onboarding_address_label),
            isOptional = true,
            placeholder = stringResource(R.string.onboarding_address_placeholder),
            enabled = !state.isSavingProfile,
            isError = state.editAddressError != null,
            errorMessage = state.editAddressError
        )

        ClearChainTextField(
            value = state.editLocation,
            onValueChange = { onEvent(ProfileEvent.EditLocationChanged(it)) },
            label = stringResource(R.string.label_city_location),
            isOptional = true,
            placeholder = stringResource(R.string.onboarding_city_placeholder),
            leadingIcon = Icons.Default.Place,
            imeAction = ImeAction.Next,
            isError = state.editLocationError != null,
            errorMessage = state.editLocationError,
            enabled = !state.isSavingProfile
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ClearChainTextField(
                value = state.editState,
                onValueChange = { onEvent(ProfileEvent.EditStateChanged(it)) },
                label = stringResource(R.string.onboarding_state_label),
                isOptional = true,
                placeholder = stringResource(R.string.onboarding_state_placeholder),
                leadingIcon = Icons.Default.Map,
                imeAction = ImeAction.Next,
                enabled = !state.isSavingProfile,
                modifier = Modifier.weight(1f)
            )

            ClearChainTextField(
                value = state.editZipCode,
                onValueChange = { onEvent(ProfileEvent.EditZipCodeChanged(it)) },
                label = stringResource(R.string.onboarding_zip_label),
                isOptional = true,
                placeholder = stringResource(R.string.onboarding_zip_placeholder),
                leadingIcon = Icons.Default.LocalPostOffice,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                enabled = !state.isSavingProfile,
                modifier = Modifier.weight(1f)
            )
        }

        SectionHeader(stringResource(R.string.onboarding_hours_label))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.weight(1f)) {
                TimePickerField(
                    value = state.editOpenTime,
                    onTimeSelected = { onEvent(ProfileEvent.EditOpenTimeChanged(it)) },
                    label = stringResource(R.string.label_opening_time),
                    isOptional = true,
                    enabled = !state.isSavingProfile
                )
            }

            Box(Modifier.weight(1f)) {
                TimePickerField(
                    value = state.editCloseTime,
                    onTimeSelected = { onEvent(ProfileEvent.EditCloseTimeChanged(it)) },
                    label = stringResource(R.string.label_closing_time),
                    isOptional = true,
                    enabled = !state.isSavingProfile
                )
            }
        }

        if (state.user?.type == OrganizationType.NGO || state.user?.type == OrganizationType.GROCERY) {
            SectionHeader(stringResource(R.string.section_org_details))

            ClearChainTextField(
                value = state.editContactPerson,
                onValueChange = { onEvent(ProfileEvent.EditContactPersonChanged(it)) },
                label = stringResource(R.string.label_contact_person_star),
                placeholder = stringResource(R.string.hint_contact_person),
                leadingIcon = Icons.Default.Person,
                imeAction = ImeAction.Next,
                isError = state.editContactPersonError != null,
                errorMessage = state.editContactPersonError,
                enabled = !state.isSavingProfile
            )
        }

        if (state.user?.type == OrganizationType.GROCERY) {
            ClearChainTextField(
                value = state.editPickupInstructions,
                onValueChange = { onEvent(ProfileEvent.EditPickupInstructionsChanged(it)) },
                label = stringResource(R.string.onboarding_pickup_instructions_label),
                isOptional = true,
                placeholder = stringResource(R.string.hint_pickup_instructions_long),
                leadingIcon = Icons.Default.DirectionsWalk,
                imeAction = ImeAction.Done,
                enabled = !state.isSavingProfile,
                singleLine = false,
                minLines = 2,
                maxLines = 3
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ClearChainOutlinedButton(
                text = stringResource(R.string.cancel),
                onClick = { onEvent(ProfileEvent.CancelEdit) },
                modifier = Modifier.weight(1f)
            )
            ClearChainButton(
                text = stringResource(R.string.action_save_changes),
                onClick = { onEvent(ProfileEvent.SaveProfile) },
                loading = state.isSavingProfile,
                enabled = !state.isSavingProfile,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// Local copy of TeamMembersCard from ProfileScreen (kept here to avoid cross-file private access)
@Composable
private fun TeamMembersCard(user: com.clearchain.app.domain.model.Organization) {
    Card(shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MemberRow(name = user.name, detail = user.email, badge = stringResource(R.string.label_owner_badge), tint = MaterialTheme.colorScheme.primaryContainer, onTint = MaterialTheme.colorScheme.onPrimaryContainer)
            if (!user.contactPerson.isNullOrBlank()) {
                HorizontalDivider()
                MemberRow(name = user.contactPerson, detail = stringResource(R.string.label_contact_person), badge = stringResource(R.string.label_contact_badge), tint = MaterialTheme.colorScheme.secondaryContainer, onTint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_invite_team_member), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.label_coming_soon), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MemberRow(name: String, detail: String, badge: String, tint: androidx.compose.ui.graphics.Color, onTint: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small).background(tint), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, null, tint = onTint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = MaterialTheme.shapes.small, color = tint) {
            Text(badge, style = MaterialTheme.typography.labelSmall, color = onTint, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}
