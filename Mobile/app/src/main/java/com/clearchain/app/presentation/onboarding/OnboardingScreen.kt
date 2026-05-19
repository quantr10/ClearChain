package com.clearchain.app.presentation.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal
import com.clearchain.app.util.UiEvent
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.Navigate    -> onFinished()
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    // Draft recovery dialog
    if (state.showDraftRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(OnboardingEvent.DismissDraftDialog) },
            icon = { Icon(Icons.Default.RestoreFromTrash, null) },
            title = { Text(stringResource(R.string.onboarding_draft_title)) },
            text = { Text(stringResource(R.string.onboarding_draft_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(OnboardingEvent.RestoreDraft) }) {
                    Text(stringResource(R.string.onboarding_restore_draft))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(OnboardingEvent.DismissDraftDialog) }) {
                    Text(stringResource(R.string.onboarding_start_fresh))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OnboardingHeader(
                currentStep = state.currentStep,
                totalSteps  = state.totalSteps,
                userType    = state.userType,
                onSaveDraft = { viewModel.onEvent(OnboardingEvent.SaveDraft) }
            )

            AnimatedContent(
                targetState = state.currentStep,
                modifier    = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it / 2 } + fadeIn(tween(280)) togetherWith
                            slideOutHorizontally { -it / 2 } + fadeOut(tween(200))
                    } else {
                        slideInHorizontally { -it / 2 } + fadeIn(tween(280)) togetherWith
                            slideOutHorizontally { it / 2 } + fadeOut(tween(200))
                    }
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    1    -> Step1Content(state, viewModel)
                    2    -> Step2Content(state, viewModel)
                    else -> Step3Content(state)
                }
            }

            AnimatedVisibility(
                visible = state.error != null,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                AlertBanner(
                    message = state.error ?: "",
                    type    = AlertType.ERROR,
                    icon    = Icons.Default.ErrorOutline,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                when (state.currentStep) {
                    1 -> ClearChainButton(
                        text    = stringResource(R.string.onboarding_continue),
                        onClick = { viewModel.onEvent(OnboardingEvent.NextStep) },
                        loading = state.isSaving,
                        enabled = !state.isSaving,
                        icon    = Icons.Default.ArrowForward,
                        modifier = Modifier.fillMaxWidth()
                    )
                    2 -> Row(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClearChainOutlinedButton(
                            text    = stringResource(R.string.onboarding_back),
                            onClick = { viewModel.onEvent(OnboardingEvent.PreviousStep) },
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1f)
                        )
                        ClearChainButton(
                            text    = if (state.isSaving) "" else stringResource(R.string.onboarding_complete),
                            onClick = { viewModel.onEvent(OnboardingEvent.NextStep) },
                            loading = state.isSaving,
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    3 -> ClearChainButton(
                        text    = stringResource(R.string.onboarding_get_started),
                        onClick = { viewModel.onEvent(OnboardingEvent.FinishOnboarding) },
                        icon    = Icons.Default.RocketLaunch,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ── Gradient header with segment progress ─────────────────────────────────────

@Composable
private fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int,
    userType: OrganizationType?,
    onSaveDraft: () -> Unit
) {
    val stepLabels = listOf(
        stringResource(R.string.onboarding_step_your_info),
        stringResource(R.string.onboarding_step_location),
        stringResource(R.string.onboarding_step_all_set)
    )
    val label      = stepLabels.getOrNull(currentStep - 1) ?: ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BrandTeal, BrandGreen)))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = stringResource(R.string.onboarding_setup_profile),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (currentStep < 3) {
                        TextButton(
                            onClick = onSaveDraft,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Save, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.onboarding_save_draft), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text     = label,
                            style    = MaterialTheme.typography.labelSmall,
                            color    = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(totalSteps) { index ->
                    val stepNum  = index + 1
                    val isDone   = stepNum < currentStep
                    val isActive = stepNum == currentStep
                    val fraction by animateFloatAsState(
                        targetValue  = when { isDone -> 1f; isActive -> 1f; else -> 0f },
                        animationSpec = tween(500, easing = FastOutSlowInEasing),
                        label        = "seg_$index"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                        )
                    }
                }
            }

            Text(
                text  = stringResource(R.string.onboarding_step_label, currentStep, totalSteps),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

// ── Step 1: Phone + Description + Contact Person ──────────────────────────────

@Composable
private fun Step1Content(state: OnboardingState, viewModel: OnboardingViewModel) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text       = stringResource(R.string.onboarding_welcome),
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = if (state.userType == OrganizationType.GROCERY)
                stringResource(R.string.onboarding_grocery_intro)
            else
                stringResource(R.string.onboarding_ngo_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        ClearChainTextField(
            value         = state.phone,
            onValueChange = { viewModel.onEvent(OnboardingEvent.PhoneChanged(it)) },
            label         = stringResource(R.string.onboarding_phone_label),
            placeholder   = stringResource(R.string.hint_phone_profile),
            leadingIcon   = { Icon(Icons.Default.Phone, null) },
            keyboardType  = KeyboardType.Phone,
            imeAction     = ImeAction.Next,
            isError       = state.phoneError != null,
            errorMessage  = state.phoneError,
            enabled       = !state.isSaving
        )

        ClearChainTextField(
            value         = state.description,
            onValueChange = { viewModel.onEvent(OnboardingEvent.DescriptionChanged(it)) },
            label         = stringResource(R.string.onboarding_about_label),
            placeholder   = stringResource(R.string.onboarding_about_placeholder),
            leadingIcon   = { Icon(Icons.Default.Description, null) },
            imeAction     = ImeAction.Next,
            enabled       = !state.isSaving,
            singleLine    = false,
            minLines      = 3,
            maxLines      = 5
        )

        if (state.userType == OrganizationType.NGO || state.userType == OrganizationType.GROCERY) {
            ClearChainTextField(
                value         = state.contactPerson,
                onValueChange = { viewModel.onEvent(OnboardingEvent.ContactPersonChanged(it)) },
                label         = stringResource(R.string.onboarding_contact_label),
                placeholder   = stringResource(R.string.onboarding_contact_placeholder),
                leadingIcon   = { Icon(Icons.Default.Person, null) },
                imeAction     = ImeAction.Done,
                isError       = state.contactPersonError != null,
                errorMessage  = state.contactPersonError,
                enabled       = !state.isSaving
            )
        }
    }
}

// ── Step 2: Address + City + Hours + Pickup Instructions + Document Upload ────

@Composable
private fun Step2Content(state: OnboardingState, viewModel: OnboardingViewModel) {
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onEvent(OnboardingEvent.DocumentSelected(uri, uri.lastPathSegment ?: "document"))
        }
    }

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text       = stringResource(R.string.onboarding_location_heading),
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = stringResource(R.string.onboarding_location_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        AddressSuggestionField(
            value         = state.address,
            onValueChange = { viewModel.onEvent(OnboardingEvent.AddressChanged(it)) },
            onAddressSelected = { suggestion ->
                viewModel.onEvent(
                    OnboardingEvent.AddressSelected(
                        address = suggestion.fullAddress,
                        city    = suggestion.city,
                        lat     = suggestion.latitude,
                        lng     = suggestion.longitude
                    )
                )
            },
            label       = stringResource(R.string.onboarding_address_label),
            placeholder = stringResource(R.string.onboarding_address_placeholder),
            enabled     = !state.isSaving
        )

        ClearChainTextField(
            value         = state.city,
            onValueChange = { viewModel.onEvent(OnboardingEvent.CityChanged(it)) },
            label         = stringResource(R.string.onboarding_city_label),
            placeholder   = stringResource(R.string.onboarding_city_placeholder),
            leadingIcon   = { Icon(Icons.Default.Place, null) },
            imeAction     = ImeAction.Next,
            isError       = state.cityError != null,
            errorMessage  = state.cityError,
            enabled       = !state.isSaving
        )

        Surface(
            color  = MaterialTheme.colorScheme.surfaceVariant,
            shape  = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text       = stringResource(R.string.onboarding_hours_label),
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        TimePickerField(
                            value          = state.openTime,
                            onTimeSelected = { viewModel.onEvent(OnboardingEvent.OpenTimeChanged(it)) },
                            label          = stringResource(R.string.onboarding_hours_opens),
                            enabled        = !state.isSaving
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        TimePickerField(
                            value          = state.closeTime,
                            onTimeSelected = { viewModel.onEvent(OnboardingEvent.CloseTimeChanged(it)) },
                            label          = stringResource(R.string.onboarding_hours_closes),
                            enabled        = !state.isSaving
                        )
                    }
                }
            }
        }

        if (state.userType == OrganizationType.GROCERY) {
            ClearChainTextField(
                value         = state.pickupInstructions,
                onValueChange = { viewModel.onEvent(OnboardingEvent.PickupInstructionsChanged(it)) },
                label         = stringResource(R.string.onboarding_pickup_instructions_label),
                placeholder   = stringResource(R.string.onboarding_pickup_instructions_placeholder),
                leadingIcon   = { Icon(Icons.Default.DirectionsWalk, null) },
                imeAction     = ImeAction.Done,
                enabled       = !state.isSaving,
                singleLine    = false,
                minLines      = 2,
                maxLines      = 4
            )
        }

        // ── Verification document upload ─────────────────────────────
        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text  = stringResource(R.string.onboarding_doc_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text  = stringResource(R.string.onboarding_doc_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.verificationDocumentUri != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text  = state.verificationDocumentName ?: stringResource(R.string.onboarding_doc_selected),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = { viewModel.onEvent(OnboardingEvent.RemoveDocument) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, stringResource(R.string.onboarding_remove_icon), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { documentPickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving
                ) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.onboarding_upload_document))
                }
            }
        }
    }
}

// ── Step 3: Celebration ───────────────────────────────────────────────────────

@Composable
private fun Step3Content(state: OnboardingState) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); revealed = true }

    val scale by animateFloatAsState(
        targetValue  = if (revealed) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label        = "celebrate_scale"
    )
    val alpha by animateFloatAsState(
        targetValue  = if (revealed) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label        = "celebrate_alpha"
    )

    Column(
        modifier                = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment     = Alignment.CenterHorizontally,
        verticalArrangement     = Arrangement.Center
    ) {
        Box(
            modifier           = Modifier
                .scale(scale)
                .size(100.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(BrandTeal.copy(alpha = 0.15f), BrandGreen.copy(alpha = 0.05f)))),
            contentAlignment   = Alignment.Center
        ) {
            Box(
                modifier         = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(BrandTeal, BrandGreen))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector     = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier        = Modifier.size(44.dp),
                    tint            = Color.White
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text       = stringResource(R.string.onboarding_all_set),
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.graphicsLayer(alpha = alpha)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text  = if (state.userType == OrganizationType.GROCERY)
                stringResource(R.string.onboarding_grocery_complete)
            else
                stringResource(R.string.onboarding_ngo_complete),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier  = Modifier.graphicsLayer(alpha = alpha)
        )

        Spacer(Modifier.height(32.dp))

        val features = if (state.userType == OrganizationType.GROCERY) {
            listOf(
                Icons.Default.AddCircle to stringResource(R.string.onboarding_feature_create),
                Icons.Default.Notifications to stringResource(R.string.onboarding_feature_requests),
                Icons.Default.BarChart to stringResource(R.string.onboarding_feature_impact)
            )
        } else {
            listOf(
                Icons.Default.Search to stringResource(R.string.onboarding_feature_browse),
                Icons.Default.LocalShipping to stringResource(R.string.onboarding_feature_pickups),
                Icons.Default.Inventory to stringResource(R.string.onboarding_feature_inventory)
            )
        }

        Surface(
            color    = MaterialTheme.colorScheme.surfaceVariant,
            shape    = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(alpha = alpha)
        ) {
            Column(
                modifier            = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                features.forEach { (icon, label) ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector    = icon,
                                contentDescription = null,
                                modifier       = Modifier.size(18.dp),
                                tint           = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text  = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
