package com.clearchain.app.presentation.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.BuildConfig
import com.clearchain.app.R
import com.clearchain.app.presentation.components.DetailTopBar

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Appearance ────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.theme))

            SettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.theme),
                subtitle = when (state.theme) {
                    "light" -> stringResource(R.string.theme_light)
                    "dark"  -> stringResource(R.string.theme_dark)
                    else    -> stringResource(R.string.theme_system)
                }
            ) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(
                            text = when (state.theme) {
                                "light" -> stringResource(R.string.theme_light)
                                "dark"  -> stringResource(R.string.theme_dark)
                                else    -> stringResource(R.string.theme_system)
                            }
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf(
                            "system" to R.string.theme_system,
                            "light"  to R.string.theme_light,
                            "dark"   to R.string.theme_dark
                        ).forEach { (value, labelRes) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(labelRes)) },
                                onClick = {
                                    viewModel.onEvent(SettingsEvent.ThemeChanged(value))
                                    expanded = false
                                },
                                leadingIcon = if (state.theme == value) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // ── Language ──────────────────────────────────────────────────
            SettingsItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.language),
                subtitle = when (state.language) {
                    "vi" -> "Tiếng Việt"
                    else -> "English"
                }
            ) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(
                            text = when (state.language) {
                                "vi" -> "VI"
                                else -> "EN"
                            }
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf(
                            "en" to "English",
                            "vi" to "Tiếng Việt"
                        ).forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    viewModel.onEvent(SettingsEvent.LanguageChanged(code))
                                    expanded = false
                                },
                                leadingIcon = if (state.language == code) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Notifications ─────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.notifications_settings))

            SettingsSwitchItem(
                icon = Icons.Default.LocalGroceryStore,
                title = stringResource(R.string.notif_new_listing),
                checked = state.notifNewListing,
                onCheckedChange = { viewModel.onEvent(SettingsEvent.NotifNewListingChanged(it)) }
            )

            SettingsSwitchItem(
                icon = Icons.Default.LocalShipping,
                title = stringResource(R.string.notif_request_update),
                checked = state.notifRequestUpdate,
                onCheckedChange = { viewModel.onEvent(SettingsEvent.NotifRequestUpdateChanged(it)) }
            )

            SettingsSwitchItem(
                icon = Icons.Default.Timer,
                title = stringResource(R.string.notif_expiry_reminder),
                checked = state.notifExpiry,
                onCheckedChange = { viewModel.onEvent(SettingsEvent.NotifExpiryChanged(it)) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── About ─────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.about))

            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about),
                subtitle = stringResource(R.string.app_version, BuildConfig.VERSION_NAME)
            )

            SettingsItem(
                icon = Icons.Default.Article,
                title = stringResource(R.string.terms_of_service)
            )

            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = stringResource(R.string.privacy_policy)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItem(
        icon = icon,
        title = title,
        subtitle = subtitle,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.height(24.dp)
            )
        },
        onClick = { onCheckedChange(!checked) }
    )
}
