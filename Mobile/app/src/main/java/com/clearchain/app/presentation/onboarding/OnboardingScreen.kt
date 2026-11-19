package com.clearchain.app.presentation.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.BorderWidth
import com.clearchain.app.ui.theme.ControlHeight
import com.clearchain.app.ui.theme.ControlIconSize
import com.clearchain.app.ui.theme.ControlPadding
import com.clearchain.app.ui.theme.EyebrowStyle
import com.clearchain.app.ui.theme.FieldEndButtonSize
import com.clearchain.app.ui.theme.IconGap
import com.clearchain.app.ui.theme.ListGap
import com.clearchain.app.ui.theme.ShapeMedium
import com.clearchain.app.util.UiEvent
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * MIME types the verification-document picker offers. Mirrors the allow-list in
 * [com.clearchain.app.domain.usecase.profile.UploadVerificationDocumentUseCase] (and the
 * Supabase "documents" bucket) so the system file browser only surfaces files the upload
 * will actually accept.
 */
private val VERIFICATION_DOC_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/heic",
    "image/heif"
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    BlockBackWhile(state.isSaving || state.isUploadingDocument)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.Navigate -> onFinished()
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                else -> {}
            }
        }
    }

    BrandScaffold(
        header = BrandHeader.Compact(
            title = stringResource(R.string.onboarding_setup_profile),
            step = state.currentStep,
            totalSteps = state.totalSteps
        ),
        snackbarHostState = snackbarHostState,
        bottomBar = {
            when (state.currentStep) {
                1 -> ClearChainButton(
                    text = stringResource(R.string.onboarding_continue),
                    onClick = { viewModel.onEvent(OnboardingEvent.NextStep) },
                    loading = state.isSaving,
                    enabled = !state.isSaving && state.canContinueStep1,
                    modifier = Modifier.fillMaxWidth()
                )
                2 -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ListGap)
                ) {
                    ClearChainOutlinedButton(
                        text = stringResource(R.string.onboarding_back),
                        onClick = { viewModel.onEvent(OnboardingEvent.PreviousStep) },
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f)
                    )
                    ClearChainButton(
                        text = stringResource(R.string.onboarding_complete),
                        onClick = { viewModel.onEvent(OnboardingEvent.NextStep) },
                        loading = state.isSaving,
                        enabled = !state.isSaving && state.canContinueStep2,
                        modifier = Modifier.weight(1f)
                    )
                }
                3 -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ListGap)
                ) {
                    ClearChainOutlinedButton(
                        text = stringResource(R.string.onboarding_back),
                        onClick = { viewModel.onEvent(OnboardingEvent.PreviousStep) },
                        modifier = Modifier.weight(1f)
                    )
                    ClearChainButton(
                        text = stringResource(R.string.onboarding_view_status),
                        onClick = { viewModel.onEvent(OnboardingEvent.FinishOnboarding) },
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) {
        AnimatedContent(
            targetState = state.currentStep,
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
                1 -> Step1Content(state, viewModel)
                2 -> Step2Content(state, viewModel)
                else -> Step3Content(state.totalSteps)
            }
        }
    }
}

// ── Step intro ───────────────────────────────────────────────────────────────

/**
 * The step's eyebrow, such as "STEP 1 OF 3 · CONTACT DETAILS", and for steps 1 and 2 a subtitle
 * saying what the step asks for.
 */
@Composable
private fun StepIntro(step: Int, totalSteps: Int) {
    val name = stringResource(
        when (step) {
            1 -> R.string.onboarding_step_contact
            2 -> R.string.onboarding_step_location
            else -> R.string.onboarding_step_submitted
        }
    )
    Text(
        text = stringResource(R.string.onboarding_step_label_named, step, totalSteps, name)
            .uppercase(Locale.getDefault()),
        style = EyebrowStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val subtitle = when (step) {
        1 -> R.string.onboarding_step1_subtitle
        2 -> R.string.onboarding_step2_subtitle
        else -> null
    }
    subtitle?.let {
        Text(
            text = stringResource(it),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Step 1: phone, description, contact person ───────────────────────────────

@Composable
private fun Step1Content(state: OnboardingState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListGap)
    ) {
        StepIntro(1, state.totalSteps)

        ClearChainTextField(
            value = state.phone,
            onValueChange = { viewModel.onEvent(OnboardingEvent.PhoneChanged(it)) },
            label = stringResource(R.string.onboarding_phone_label),
            isOptional = false,
            placeholder = stringResource(R.string.hint_phone_profile),
            leadingIcon = Icons.Default.Phone,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next,
            isError = state.phoneError != null,
            errorMessage = state.phoneError,
            enabled = !state.isSaving
        )

        ClearChainTextField(
            value = state.description,
            onValueChange = { viewModel.onEvent(OnboardingEvent.DescriptionChanged(it)) },
            label = stringResource(R.string.onboarding_about_label),
            isOptional = true,
            placeholder = stringResource(R.string.onboarding_about_placeholder),
            leadingIcon = Icons.Default.Description,
            imeAction = ImeAction.Next,
            enabled = !state.isSaving,
            singleLine = false,
            minLines = 3,
            maxLines = 5
        )

        if (state.userType == OrganizationType.NGO || state.userType == OrganizationType.GROCERY) {
            ClearChainTextField(
                value = state.contactPerson,
                onValueChange = { viewModel.onEvent(OnboardingEvent.ContactPersonChanged(it)) },
                label = stringResource(R.string.onboarding_contact_label),
                isOptional = false,
                placeholder = stringResource(R.string.onboarding_contact_placeholder),
                leadingIcon = Icons.Default.Person,
                imeAction = ImeAction.Done,
                isError = state.contactPersonError != null,
                errorMessage = state.contactPersonError,
                enabled = !state.isSaving
            )
        }
    }
}

// ── Step 2: address, city, hours, pickup instructions, document upload ───────

@Composable
private fun Step2Content(state: OnboardingState, viewModel: OnboardingViewModel) {
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onEvent(OnboardingEvent.DocumentSelected(uri, uri.lastPathSegment ?: "document"))
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListGap)
    ) {
        StepIntro(2, state.totalSteps)

        AddressSuggestionField(
            value = state.address,
            onValueChange = { viewModel.onEvent(OnboardingEvent.AddressChanged(it)) },
            onAddressSelected = { suggestion ->
                viewModel.onEvent(
                    OnboardingEvent.AddressSelected(
                        address = suggestion.streetAddress,
                        city = suggestion.city,
                        state = suggestion.state,
                        zipCode = suggestion.zipCode,
                        lat = suggestion.latitude,
                        lng = suggestion.longitude
                    )
                )
            },
            label = stringResource(R.string.onboarding_address_label),
            placeholder = stringResource(R.string.onboarding_address_placeholder),
            enabled = !state.isSaving,
            isError = state.addressError != null,
            errorMessage = state.addressError
        )

        ClearChainTextField(
            value = state.city,
            onValueChange = { viewModel.onEvent(OnboardingEvent.CityChanged(it)) },
            label = stringResource(R.string.onboarding_city_label),
            placeholder = stringResource(R.string.onboarding_city_placeholder),
            leadingIcon = Icons.Default.LocationCity,
            imeAction = ImeAction.Next,
            isError = state.cityError != null,
            errorMessage = state.cityError,
            enabled = !state.isSaving
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ListGap)
        ) {
            ClearChainTextField(
                value = state.state,
                onValueChange = { viewModel.onEvent(OnboardingEvent.StateChanged(it)) },
                label = stringResource(R.string.onboarding_state_label),
                placeholder = stringResource(R.string.onboarding_state_placeholder),
                imeAction = ImeAction.Next,
                isError = state.stateError != null,
                errorMessage = state.stateError,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f)
            )
            ClearChainTextField(
                value = state.zipCode,
                onValueChange = { viewModel.onEvent(OnboardingEvent.ZipCodeChanged(it)) },
                label = stringResource(R.string.onboarding_zip_label),
                placeholder = stringResource(R.string.onboarding_zip_placeholder),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                isError = state.zipCodeError != null,
                errorMessage = state.zipCodeError,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ListGap)
        ) {
            TimePickerField(
                value = state.openTime,
                onTimeSelected = { viewModel.onEvent(OnboardingEvent.OpenTimeChanged(it)) },
                label = stringResource(R.string.onboarding_opens_label),
                isError = state.openTimeError != null,
                errorMessage = state.openTimeError,
                enabled = !state.isSaving,
                clearLabel = stringResource(R.string.cd_clear_opening_time),
                modifier = Modifier.weight(1f)
            )
            TimePickerField(
                value = state.closeTime,
                onTimeSelected = { viewModel.onEvent(OnboardingEvent.CloseTimeChanged(it)) },
                label = stringResource(R.string.onboarding_closes_label),
                isError = state.closeTimeError != null,
                errorMessage = state.closeTimeError,
                enabled = !state.isSaving,
                clearLabel = stringResource(R.string.cd_clear_closing_time),
                modifier = Modifier.weight(1f)
            )
        }

        if (state.userType == OrganizationType.GROCERY) {
            ClearChainTextField(
                value = state.pickupInstructions,
                onValueChange = { viewModel.onEvent(OnboardingEvent.PickupInstructionsChanged(it)) },
                label = stringResource(R.string.onboarding_pickup_instructions_label),
                isOptional = true,
                placeholder = stringResource(R.string.onboarding_pickup_instructions_placeholder),
                leadingIcon = Icons.AutoMirrored.Filled.DirectionsWalk,
                imeAction = ImeAction.Done,
                enabled = !state.isSaving,
                singleLine = false,
                minLines = 2,
                maxLines = 4
            )
        }

        // ── Verification document upload (required) ──────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(ListGap)) {
            OptionalFieldLabel(
                text = stringResource(R.string.onboarding_doc_label),
                isOptional = false,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.onboarding_doc_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val hasDoc = state.verificationDocumentUri != null || state.uploadedDocumentUrl != null
            if (hasDoc) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = ShapeMedium,
                    border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .heightIn(min = ControlHeight)
                            .padding(horizontal = ControlPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(IconGap)
                    ) {
                        when {
                            state.isUploadingDocument -> InlineSpinner(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            state.uploadedDocumentUrl != null -> Icon(
                                Icons.Default.CheckCircle,
                                null,
                                modifier = Modifier.size(ControlIconSize)
                            )
                            else -> Icon(
                                Icons.Default.AttachFile,
                                null,
                                modifier = Modifier.size(ControlIconSize)
                            )
                        }
                        Text(
                            text = when {
                                state.isUploadingDocument -> stringResource(R.string.onboarding_doc_uploading)
                                else -> state.verificationDocumentName ?: stringResource(R.string.onboarding_doc_selected)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (!state.isUploadingDocument) {
                            IconButton(
                                onClick = { viewModel.onEvent(OnboardingEvent.RemoveDocument) },
                                modifier = Modifier.size(FieldEndButtonSize)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    stringResource(R.string.cd_remove_document),
                                    modifier = Modifier.size(ControlIconSize)
                                )
                            }
                        }
                    }
                }
            } else {
                ClearChainOutlinedButton(
                    text = stringResource(R.string.onboarding_upload_document),
                    onClick = { documentPickerLauncher.launch(VERIFICATION_DOC_MIME_TYPES) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving && !state.isUploadingDocument,
                    icon = Icons.Default.Upload
                )
            }

            state.documentUploadError?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── Step 3: confirmation ─────────────────────────────────────────────────────

/** A short confirmation that the profile is on its way to review. */
@Composable
private fun Step3Content(totalSteps: Int) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        revealed = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "confirmation_alpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ListGap)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            StepIntro(3, totalSteps)
        }

        StatusDisc(
            icon = Icons.Default.TaskAlt,
            modifier = Modifier.graphicsLayer(alpha = alpha)
        )

        Text(
            text = stringResource(R.string.onboarding_submitted_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer(alpha = alpha)
        )

        Text(
            text = stringResource(R.string.onboarding_submitted_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer(alpha = alpha)
        )
    }
}
