package com.clearchain.app.presentation.auth.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.presentation.auth.AuthDivider
import com.clearchain.app.presentation.auth.AuthHeader
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.ui.theme.*
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Navigate     -> navController.navigate(event.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                is UiEvent.NavigateUp   -> navController.navigateUp()
                is UiEvent.ShareFile    -> { }
            }
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            AuthHeader(
                subtitle    = stringResource(R.string.create_your_account),
                navigationIcon = {
                    IconButton(
                        onClick  = { navController.navigateUp() },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back), tint = Color.White)
                    }
                }
            )

            AnimatedVisibility(
                visible = true,
                enter   = fadeIn() + slideInVertically { it / 4 }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text       = stringResource(R.string.join_clearchain),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // ── Role picker ────────────────────────────────────────
                    Text(
                        text  = stringResource(R.string.i_represent_a),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RoleCard(
                            icon     = Icons.Default.Store,
                            title    = stringResource(R.string.role_grocery_title),
                            subtitle = stringResource(R.string.role_grocery_subtitle),
                            selected = state.type == "grocery",
                            onClick  = { viewModel.onEvent(RegisterEvent.TypeChanged("grocery")) },
                            modifier = Modifier.weight(1f),
                            enabled  = !state.isLoading
                        )
                        RoleCard(
                            icon     = Icons.Default.VolunteerActivism,
                            title    = stringResource(R.string.role_ngo_title),
                            subtitle = stringResource(R.string.role_ngo_subtitle),
                            selected = state.type == "ngo",
                            onClick  = { viewModel.onEvent(RegisterEvent.TypeChanged("ngo")) },
                            modifier = Modifier.weight(1f),
                            enabled  = !state.isLoading
                        )
                    }

                    HorizontalDivider()

                    // ── Account fields ─────────────────────────────────────
                    ClearChainTextField(
                        value         = state.name,
                        onValueChange = { viewModel.onEvent(RegisterEvent.NameChanged(it)) },
                        label         = stringResource(R.string.org_name_label),
                        placeholder   = stringResource(R.string.org_name_placeholder),
                        leadingIcon   = { Icon(Icons.Default.Business, null) },
                        imeAction     = ImeAction.Next,
                        isError       = state.nameError != null,
                        errorMessage  = state.nameError,
                        enabled       = !state.isLoading
                    )

                    // ── Email field with availability indicator ────────────
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ClearChainTextField(
                            value         = state.email,
                            onValueChange = { viewModel.onEvent(RegisterEvent.EmailChanged(it)) },
                            label         = stringResource(R.string.email_address),
                            placeholder   = stringResource(R.string.hint_email_org),
                            leadingIcon   = { Icon(Icons.Default.Email, null) },
                            trailingIcon  = {
                                when {
                                    state.isCheckingEmail -> CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    state.emailAvailable == true ->
                                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                    state.emailAvailable == false ->
                                        Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error)
                                    else -> {}
                                }
                            },
                            keyboardType  = KeyboardType.Email,
                            imeAction     = ImeAction.Next,
                            isError       = state.emailError != null || state.emailAvailable == false,
                            errorMessage  = state.emailError,
                            enabled       = !state.isLoading
                        )
                        AnimatedVisibility(visible = state.emailAvailable == true) {
                            Text(
                                text  = stringResource(R.string.email_available),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }

                    // ── Password with strength meter ───────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ClearChainTextField(
                            value         = state.password,
                            onValueChange = { viewModel.onEvent(RegisterEvent.PasswordChanged(it)) },
                            label         = stringResource(R.string.password),
                            placeholder   = stringResource(R.string.password_placeholder),
                            leadingIcon   = { Icon(Icons.Default.Lock, null) },
                            keyboardType  = KeyboardType.Password,
                            imeAction     = ImeAction.Next,
                            isPassword    = true,
                            isError       = state.passwordError != null,
                            errorMessage  = state.passwordError,
                            enabled       = !state.isLoading
                        )
                        AnimatedVisibility(visible = state.passwordStrength != PasswordStrength.NONE) {
                            PasswordStrengthMeter(strength = state.passwordStrength)
                        }
                    }

                    ClearChainTextField(
                        value         = state.confirmPassword,
                        onValueChange = { viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged(it)) },
                        label         = stringResource(R.string.confirm_password),
                        placeholder   = stringResource(R.string.confirm_password_placeholder),
                        leadingIcon   = { Icon(Icons.Default.LockOpen, null) },
                        keyboardType  = KeyboardType.Password,
                        imeAction     = ImeAction.Done,
                        isPassword    = true,
                        isError       = state.confirmPasswordError != null,
                        errorMessage  = state.confirmPasswordError,
                        onImeAction   = { viewModel.onEvent(RegisterEvent.Register) },
                        enabled       = !state.isLoading
                    )

                    // ── Terms of Service checkbox ──────────────────────────
                    val tosAgreePrefix = stringResource(R.string.tos_agree_prefix)
                    val tosTerms = stringResource(R.string.terms_of_service)
                    val tosAnd = stringResource(R.string.tos_agree_and)
                    val tosPrivacy = stringResource(R.string.privacy_policy)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(enabled = !state.isLoading) {
                                viewModel.onEvent(RegisterEvent.ToggleTos)
                            }
                        ) {
                            Checkbox(
                                checked = state.tosAccepted,
                                onCheckedChange = { viewModel.onEvent(RegisterEvent.ToggleTos) },
                                enabled = !state.isLoading,
                                colors = CheckboxDefaults.colors(
                                    uncheckedColor = if (state.tosError) MaterialTheme.colorScheme.error
                                                     else MaterialTheme.colorScheme.outline
                                )
                            )
                            Text(
                                text = buildAnnotatedString {
                                    append(tosAgreePrefix)
                                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) {
                                        append(tosTerms)
                                    }
                                    append(tosAnd)
                                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) {
                                        append(tosPrivacy)
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (state.tosError) {
                            Text(
                                text  = stringResource(R.string.must_accept_tos),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 48.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut()) {
                        AlertBanner(
                            message = state.error ?: "",
                            type    = AlertType.ERROR,
                            icon    = Icons.Default.ErrorOutline
                        )
                    }

                    ClearChainButton(
                        text    = stringResource(R.string.create_account),
                        onClick = { viewModel.onEvent(RegisterEvent.Register) },
                        loading = state.isLoading,
                        enabled = !state.isLoading
                    )

                    AuthDivider()

                    ClearChainOutlinedButton(
                        text    = stringResource(R.string.sign_in),
                        onClick = { navController.navigateUp() },
                        enabled = !state.isLoading
                    )
                }
            }
        }
    }
}

// ── Password strength bar ──────────────────────────────────────────────────────

@Composable
private fun PasswordStrengthMeter(strength: PasswordStrength) {
    val weakLabel   = stringResource(R.string.password_strength_weak)
    val mediumLabel = stringResource(R.string.password_strength_medium)
    val strongLabel = stringResource(R.string.password_strength_strong)
    val (label, color, filledSegments) = when (strength) {
        PasswordStrength.WEAK   -> Triple(weakLabel,   MaterialTheme.colorScheme.error, 1)
        PasswordStrength.MEDIUM -> Triple(mediumLabel, MaterialTheme.colorScheme.tertiary, 2)
        PasswordStrength.STRONG -> Triple(strongLabel, MaterialTheme.colorScheme.primary, 3)
        else                    -> Triple("",           Color.Transparent, 0)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index < filledSegments) color else MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.width(40.dp)
        )
    }
}

// ── Role selection card ────────────────────────────────────────────────────────

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant
    val bgColor     = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                      else MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .clip(CardShape)
            .background(bgColor)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = CardShape
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text       = title,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
                color      = if (selected) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
