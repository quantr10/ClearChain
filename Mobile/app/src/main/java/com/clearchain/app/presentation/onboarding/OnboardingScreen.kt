package com.clearchain.app.presentation.onboarding

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.components.ClearChainButton
import com.clearchain.app.presentation.components.ClearChainTextField
import com.clearchain.app.presentation.components.TimePickerField
import com.clearchain.app.presentation.components.AddressSuggestionField
import com.clearchain.app.util.UiEvent

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
                is UiEvent.Navigate -> onFinished()
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            // ── Progress Indicator ──────────────────────────────────
            StepIndicator(currentStep = state.currentStep, totalSteps = state.totalSteps)

            Spacer(modifier = Modifier.height(24.dp))

            // ── Step Content ────────────────────────────────────────
            AnimatedContent(
                targetState = state.currentStep,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    1 -> Step1Content(state, viewModel)
                    2 -> Step2Content(state, viewModel)
                    3 -> Step3Content(state)
                }
            }

            // ── Error ───────────────────────────────────────────────
            state.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Bottom Buttons ──────────────────────────────────────
            when (state.currentStep) {
                1 -> {
                    ClearChainButton(
                        text = "Next",
                        onClick = { viewModel.onEvent(OnboardingEvent.NextStep) },
                        loading = state.isSaving,
                        enabled = !state.isSaving
                    )
                }
                2 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onEvent(OnboardingEvent.PreviousStep) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            enabled = !state.isSaving,
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Back") }
                        Button(
                            onClick = { viewModel.onEvent(OnboardingEvent.NextStep) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            enabled = !state.isSaving,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else { Text("Next") }
                        }
                    }
                }
                3 -> {
                    ClearChainButton(
                        text = "Get Started",
                        onClick = { viewModel.onEvent(OnboardingEvent.FinishOnboarding) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ═══ Step Indicator (● ○ ○) ═══
@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { index ->
                val step = index + 1
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = CircleShape,
                    color = if (step <= currentStep)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outlineVariant
                ) {}
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (currentStep <= 2) "Step $currentStep of $totalSteps" else "Complete!",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══ Step 1: Phone + Description (+ Contact Person for NGO) ═══
@Composable
private fun Step1Content(state: OnboardingState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome to ClearChain!",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = if (state.userType == OrganizationType.GROCERY)
                "Let's set up your profile so you can start donating food."
            else "Let's set up your profile so you can start receiving donations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        ClearChainTextField(
            value = state.phone,
            onValueChange = { viewModel.onEvent(OnboardingEvent.PhoneChanged(it)) },
            label = "Phone Number *",
            placeholder = "+84-123-456-789",
            leadingIcon = { Icon(Icons.Default.Phone, null) },
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next,
            isError = state.phoneError != null,
            errorMessage = state.phoneError,
            enabled = !state.isSaving
        )

        ClearChainTextField(
            value = state.description,
            onValueChange = { viewModel.onEvent(OnboardingEvent.DescriptionChanged(it)) },
            label = "Description",
            placeholder = "Tell others about your organization",
            leadingIcon = { Icon(Icons.Default.Description, null) },
            imeAction = if (state.userType == OrganizationType.NGO) ImeAction.Next else ImeAction.Done,
            enabled = !state.isSaving,
            singleLine = false
        )

        if (state.userType == OrganizationType.NGO || state.userType == OrganizationType.GROCERY) {
            ClearChainTextField(
                value = state.contactPerson,
                onValueChange = { viewModel.onEvent(OnboardingEvent.ContactPersonChanged(it)) },
                label = "Contact Person *",
                placeholder = "Name of primary contact",
                leadingIcon = { Icon(Icons.Default.Person, null) },
                imeAction = ImeAction.Done,
                isError = state.contactPersonError != null,
                errorMessage = state.contactPersonError,
                enabled = !state.isSaving
            )
        }
    }
}

// ═══ Step 2: Address + City + Hours (+ Pickup Instructions for Grocery) ═══
@Composable
private fun Step2Content(state: OnboardingState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Where are you located?",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Help us connect you with nearby partners.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        AddressSuggestionField(
            value = state.address,
            onValueChange = { viewModel.onEvent(OnboardingEvent.AddressChanged(it)) },
            onAddressSelected = { suggestion ->
                viewModel.onEvent(OnboardingEvent.AddressSelected(
                    address = suggestion.fullAddress,
                    city = suggestion.city,
                    lat = suggestion.latitude,
                    lng = suggestion.longitude
                ))
            },
            label = "Address *",
            placeholder = "Start typing an address...",
            enabled = !state.isSaving
        )

        ClearChainTextField(
            value = state.city,
            onValueChange = { viewModel.onEvent(OnboardingEvent.CityChanged(it)) },
            label = "City *",
            placeholder = "Ho Chi Minh City",
            leadingIcon = { Icon(Icons.Default.Place, null) },
            imeAction = ImeAction.Next,
            isError = state.cityError != null,
            errorMessage = state.cityError,
            enabled = !state.isSaving
        )

        TimePickerField(
            value = state.openTime,
            onTimeSelected = { viewModel.onEvent(OnboardingEvent.OpenTimeChanged(it)) },
            label = "Opening Time",
            enabled = !state.isSaving
        )

        TimePickerField(
            value = state.closeTime,
            onTimeSelected = { viewModel.onEvent(OnboardingEvent.CloseTimeChanged(it)) },
            label = "Closing Time",
            enabled = !state.isSaving
        )

        if (state.userType == OrganizationType.GROCERY) {
            ClearChainTextField(
                value = state.pickupInstructions,
                onValueChange = { viewModel.onEvent(OnboardingEvent.PickupInstructionsChanged(it)) },
                label = "Pickup Instructions",
                placeholder = "Back door, ask at service counter",
                leadingIcon = { Icon(Icons.Default.DirectionsWalk, null) },
                imeAction = ImeAction.Done,
                enabled = !state.isSaving,
                singleLine = false
            )
        }
    }
}

// ═══ Step 3: Celebration ═══
@Composable
private fun Step3Content(state: OnboardingState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🎉", fontSize = 72.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text("You're All Set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (state.userType == OrganizationType.GROCERY)
                "Your profile is complete. You can now create food listings and help reduce waste."
            else "Your profile is complete. You can now browse and request surplus food.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}