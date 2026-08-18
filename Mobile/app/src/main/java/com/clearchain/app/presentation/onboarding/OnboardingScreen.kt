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
import androidx.compose.material3.SnackbarDuration

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarMessageEffect(snackbarHostState, state.error)

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.Navigate    -> onFinished()
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                else -> {}
            }
        }
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
                totalSteps  = state.totalSteps
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

            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                when (state.currentStep) {
                    1 -> ClearChainButton(
                        text    = stringResource(R.string.onboarding_continue),
                        onClick = { viewModel.onEvent(OnboardingEvent.NextStep) },
                        loading = state.isSaving,
                        enabled = !state.isSaving && state.canContinueStep1,
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
                            text    = stringResource(R.string.onboarding_complete),
                            onClick = { viewModel.onEvent(OnboardingEvent.NextStep) },
                            loading = state.isSaving,
                            enabled = !state.isSaving && state.canContinueStep2,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    3 -> Row(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClearChainOutlinedButton(
                            text    = stringResource(R.string.onboarding_back),
                            onClick = { viewModel.onEvent(OnboardingEvent.PreviousStep) },
                            modifier = Modifier.weight(1f)
                        )
                        ClearChainButton(
                            text    = stringResource(R.string.onboarding_get_started),
                            onClick = { viewModel.onEvent(OnboardingEvent.FinishOnboarding) },
                            icon    = Icons.Default.RocketLaunch,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ── Gradient header with segment progress ─────────────────────────────────────

@Composable
private fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BrandTeal, BrandGreen)))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text  = stringResource(R.string.onboarding_welcome),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = stringResource(R.string.onboarding_setup_profile),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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
        ClearChainTextField(
            value         = state.phone,
            onValueChange = { viewModel.onEvent(OnboardingEvent.PhoneChanged(it)) },
            label         = stringResource(R.string.onboarding_phone_label),
            placeholder   = stringResource(R.string.hint_phone_profile),
            leadingIcon   = Icons.Default.Phone,
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
            isOptional    = true,
            placeholder   = stringResource(R.string.onboarding_about_placeholder),
            leadingIcon   = Icons.Default.Description,
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
                leadingIcon   = Icons.Default.Person,
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
        AddressSuggestionField(
            value         = state.address,
            onValueChange = { viewModel.onEvent(OnboardingEvent.AddressChanged(it)) },
            onAddressSelected = { suggestion ->
                viewModel.onEvent(
                    OnboardingEvent.AddressSelected(
                        address = suggestion.fullAddress,
                        city    = suggestion.city,
                        state   = suggestion.state,
                        zipCode = suggestion.zipCode,
                        lat     = suggestion.latitude,
                        lng     = suggestion.longitude
                    )
                )
            },
            label       = stringResource(R.string.onboarding_address_label),
            placeholder = stringResource(R.string.onboarding_address_placeholder),
            enabled     = !state.isSaving,
            isError     = state.addressError != null,
            errorMessage = state.addressError
        )

        ClearChainTextField(
            value         = state.city,
            onValueChange = { viewModel.onEvent(OnboardingEvent.CityChanged(it)) },
            label         = stringResource(R.string.onboarding_city_label),
            placeholder   = stringResource(R.string.onboarding_city_placeholder),
            leadingIcon   = Icons.Default.Place,
            imeAction     = ImeAction.Next,
            isError       = state.cityError != null,
            errorMessage  = state.cityError,
            enabled       = !state.isSaving
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ClearChainTextField(
                value         = state.state,
                onValueChange = { viewModel.onEvent(OnboardingEvent.StateChanged(it)) },
                label         = stringResource(R.string.onboarding_state_label),
                placeholder   = stringResource(R.string.onboarding_state_placeholder),
                leadingIcon   = Icons.Default.Map,
                imeAction     = ImeAction.Next,
                enabled       = !state.isSaving,
                modifier      = Modifier.weight(1f)
            )
            ClearChainTextField(
                value         = state.zipCode,
                onValueChange = { viewModel.onEvent(OnboardingEvent.ZipCodeChanged(it)) },
                label         = stringResource(R.string.onboarding_zip_label),
                placeholder   = stringResource(R.string.onboarding_zip_placeholder),
                leadingIcon   = Icons.Default.LocalPostOffice,
                keyboardType  = KeyboardType.Number,
                imeAction     = ImeAction.Next,
                enabled       = !state.isSaving,
                modifier      = Modifier.weight(1f)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OptionalFieldLabel(
                text       = stringResource(R.string.onboarding_hours_label),
                isOptional = false,
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    TimePickerField(
                        value          = state.openTime,
                        onTimeSelected = { viewModel.onEvent(OnboardingEvent.OpenTimeChanged(it)) },
                        label          = "",
                        enabled        = !state.isSaving
                    )
                }
                Box(Modifier.weight(1f)) {
                    TimePickerField(
                        value          = state.closeTime,
                        onTimeSelected = { viewModel.onEvent(OnboardingEvent.CloseTimeChanged(it)) },
                        label          = "",
                        enabled        = !state.isSaving
                    )
                }
            }
        }

        if (state.userType == OrganizationType.GROCERY) {
            ClearChainTextField(
                value         = state.pickupInstructions,
                onValueChange = { viewModel.onEvent(OnboardingEvent.PickupInstructionsChanged(it)) },
                label         = stringResource(R.string.onboarding_pickup_instructions_label),
                isOptional    = true,
                placeholder   = stringResource(R.string.onboarding_pickup_instructions_placeholder),
                leadingIcon   = Icons.Default.DirectionsWalk,
                imeAction     = ImeAction.Done,
                enabled       = !state.isSaving,
                singleLine    = false,
                minLines      = 2,
                maxLines      = 4
            )
        }

        // ── Verification document upload ─────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionalFieldLabel(
                text       = stringResource(R.string.onboarding_doc_label),
                isOptional = true,
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = stringResource(R.string.onboarding_doc_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                ClearChainOutlinedButton(
                    text = stringResource(R.string.onboarding_upload_document),
                    onClick = { documentPickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                    icon = Icons.Default.Upload
                )
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
            style      = MaterialTheme.typography.titleMedium,
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
            style = MaterialTheme.typography.bodySmall,
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(alpha = alpha),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            features.forEach { (icon, label) ->
                DashboardActionCard(
                    icon = icon,
                    title = label,
                    onClick = null,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
