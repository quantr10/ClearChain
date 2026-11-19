package com.clearchain.app.presentation.auth.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.R
import com.clearchain.app.presentation.auth.AuthDivider
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
    BlockBackWhile(state.isLoading)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Navigate -> navController.navigate(event.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                is UiEvent.NavigateUp -> navController.navigateUp()
                is UiEvent.ShareFile -> { }
            }
        }
    }

    BrandScaffold(
        header = BrandHeader.Compact(
            title = stringResource(R.string.create_your_account),
            onBack = { navController.navigateUp() },
            backLabel = stringResource(R.string.cd_back_to_sign_in),
            backEnabled = !state.isLoading
        ),
        snackbarHostState = snackbarHostState
    ) {
        Text(
            text = stringResource(R.string.register_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ── Role picker ──────────────────────────────────────────────────────
        OptionalFieldLabel(
            text = stringResource(R.string.register_role_label),
            isOptional = false,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ListGap)
        ) {
            RoleCard(
                icon = Icons.Default.Store,
                title = stringResource(R.string.role_grocery_title),
                subtitle = stringResource(R.string.role_grocery_subtitle),
                selected = state.type == "grocery",
                onClick = { viewModel.onEvent(RegisterEvent.TypeChanged("grocery")) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                enabled = !state.isLoading
            )
            RoleCard(
                icon = Icons.Default.VolunteerActivism,
                title = stringResource(R.string.role_ngo_title),
                subtitle = stringResource(R.string.role_ngo_subtitle),
                selected = state.type == "ngo",
                onClick = { viewModel.onEvent(RegisterEvent.TypeChanged("ngo")) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                enabled = !state.isLoading
            )
        }

        // ── Account fields ───────────────────────────────────────────────────
        ClearChainTextField(
            value = state.name,
            onValueChange = { viewModel.onEvent(RegisterEvent.NameChanged(it)) },
            label = stringResource(R.string.org_name_label),
            placeholder = stringResource(R.string.org_name_placeholder),
            leadingIcon = Icons.Default.Business,
            imeAction = ImeAction.Next,
            isError = state.nameError != null,
            errorMessage = state.nameError,
            enabled = !state.isLoading
        )

        val emailLabel = stringResource(R.string.email_address)
        ClearChainTextField(
            value = state.email,
            onValueChange = { viewModel.onEvent(RegisterEvent.EmailChanged(it)) },
            label = emailLabel,
            placeholder = stringResource(R.string.hint_email_org),
            leadingIcon = Icons.Default.Email,
            trailingIcon = {
                if (state.email.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.onEvent(RegisterEvent.ClearEmail) },
                        modifier = Modifier.size(FieldEndButtonSize)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.cd_clear_field, emailLabel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(ControlIconSize)
                        )
                    }
                }
            },
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            isError = state.emailError != null || state.emailAlreadyExists,
            errorMessage = state.emailError,
            enabled = !state.isLoading
        )

        // ── Password with strength meter ─────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(IconGap)) {
            ClearChainTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(RegisterEvent.PasswordChanged(it)) },
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.password_placeholder),
                leadingIcon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
                isPassword = true,
                isError = state.passwordError != null,
                errorMessage = state.passwordError,
                enabled = !state.isLoading
            )
            AnimatedVisibility(visible = state.passwordStrength != PasswordStrength.NONE) {
                PasswordStrengthMeter(strength = state.passwordStrength)
            }
        }

        ClearChainTextField(
            value = state.confirmPassword,
            onValueChange = { viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged(it)) },
            label = stringResource(R.string.confirm_password),
            placeholder = stringResource(R.string.confirm_password_placeholder),
            leadingIcon = Icons.Default.LockOpen,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            isError = state.confirmPasswordError != null,
            errorMessage = state.confirmPasswordError,
            onImeAction = { viewModel.onEvent(RegisterEvent.Register) },
            enabled = !state.isLoading
        )

        // ── Terms of service checkbox ────────────────────────────────────────
        val tosAgreePrefix = stringResource(R.string.tos_agree_prefix)
        val tosTerms = stringResource(R.string.terms_of_service)
        val tosAnd = stringResource(R.string.tos_agree_and)
        val tosPrivacy = stringResource(R.string.privacy_policy)
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ListGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTouchTarget)
                    .toggleable(
                        value = state.tosAccepted,
                        enabled = !state.isLoading,
                        role = Role.Checkbox,
                        onValueChange = { viewModel.onEvent(RegisterEvent.ToggleTos) }
                    )
            ) {
                Checkbox(
                    checked = state.tosAccepted,
                    onCheckedChange = null,
                    enabled = !state.isLoading,
                    colors = CheckboxDefaults.colors(
                        uncheckedColor = if (state.tosError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                )
                Text(
                    text = buildAnnotatedString {
                        append(tosAgreePrefix.trimEnd())
                        append(" ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) {
                            append(tosTerms)
                        }
                        append(" ")
                        append(tosAnd.trim())
                        append(" ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) {
                            append(tosPrivacy)
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.disabledIf(state.isLoading)
                )
            }
            if (state.tosError) {
                Text(
                    text = stringResource(R.string.must_accept_tos),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = FieldEndButtonSize + ListGap)
                )
            }
        }

        ClearChainButton(
            text = stringResource(R.string.create_account),
            onClick = { viewModel.onEvent(RegisterEvent.Register) },
            loading = state.isLoading,
            enabled = !state.isLoading &&
                state.name.isNotBlank() &&
                state.email.isNotBlank() &&
                state.password.isNotBlank() &&
                state.confirmPassword.isNotBlank() &&
                state.tosAccepted
        )

        AuthDivider()

        ClearChainOutlinedButton(
            text = stringResource(R.string.sign_in),
            onClick = { navController.navigateUp() },
            enabled = !state.isLoading,
            fillMaxWidth = true
        )
    }
}

// ── Password strength bar ────────────────────────────────────────────────────

@Composable
private fun PasswordStrengthMeter(strength: PasswordStrength) {
    val weakLabel = stringResource(R.string.password_strength_weak)
    val mediumLabel = stringResource(R.string.password_strength_medium)
    val strongLabel = stringResource(R.string.password_strength_strong)
    val (label, color, filledSegments) = when (strength) {
        PasswordStrength.WEAK -> Triple(weakLabel, MaterialTheme.colorScheme.error, 1)
        PasswordStrength.MEDIUM -> Triple(mediumLabel, MaterialTheme.colorScheme.tertiary, 2)
        PasswordStrength.STRONG -> Triple(strongLabel, MaterialTheme.colorScheme.primary, 3)
        else -> Triple("", Color.Transparent, 0)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(IconGap)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(StepBarHeight)
                    .clip(CircleShape)
                    .background(if (index < filledSegments) color else MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

// ── Role selection card ──────────────────────────────────────────────────────

/**
 * One of the two account types. It is a radio button to accessibility services, so a screen
 * reader announces the title, the subtitle and whether it is selected.
 */
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
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val iconColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val titleColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(CardShape)
            .background(containerColor)
            .border(width = BorderWidth, color = borderColor, shape = CardShape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(CardPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(TileIconSize),
                tint = iconColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = subtitleColor
            )
        }
    }
}
