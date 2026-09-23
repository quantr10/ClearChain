package com.clearchain.app.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.BuildConfig
import com.clearchain.app.R
import com.clearchain.app.data.local.SettingsStore
import com.clearchain.app.presentation.components.DashboardSection
import com.clearchain.app.presentation.components.ScreenTitleRow
import com.clearchain.app.ui.theme.ButtonShape
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.util.HapticUtils
import com.clearchain.app.util.LocaleUtils

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val openLinkFailed = stringResource(R.string.settings_open_link_failed)
    var linkError by remember { mutableStateOf(false) }

    LaunchedEffect(linkError) {
        if (linkError) {
            snackbarHostState.showSnackbar(openLinkFailed)
            linkError = false
        }
    }

    // Resources are resolved when the Activity is built, so a locale change only takes effect on
    // the next build of it. Recreating puts the user back on this screen, now fully translated.
    LaunchedEffect(Unit) {
        viewModel.languageApplied.collect {
            LocaleUtils.findActivity(context)?.recreate()
        }
    }

    // A browser is the only thing that can render these; a device without one is told so
    // rather than having the tap quietly do nothing.
    val openLink: (String) -> Unit = { url ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            linkError = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ScreenTitleRow(
                    title = stringResource(R.string.settings),
                    onBack = onNavigateBack
                )
            }

            // ── General ───────────────────────────────────────────────────
            item {
                DashboardSection(title = stringResource(R.string.settings_general)) {
                    SettingsDropdownRow(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.theme),
                        options = listOf(
                            SettingsStore.THEME_SYSTEM to stringResource(R.string.theme_system),
                            SettingsStore.THEME_LIGHT to stringResource(R.string.theme_light),
                            SettingsStore.THEME_DARK to stringResource(R.string.theme_dark)
                        ),
                        selected = state.theme,
                        onSelect = { viewModel.onEvent(SettingsEvent.ThemeChanged(it)) }
                    )

                    SettingsRowDivider()

                    SettingsDropdownRow(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.language),
                        options = listOf(
                            SettingsStore.LANG_EN to stringResource(R.string.language_english),
                            SettingsStore.LANG_VI to stringResource(R.string.language_vietnamese)
                        ),
                        selected = state.language,
                        onSelect = { viewModel.onEvent(SettingsEvent.LanguageChanged(it)) }
                    )
                }
            }

            // ── Notifications ─────────────────────────────────────────────
            item {
                DashboardSection(title = stringResource(R.string.notifications_settings)) {
                    SettingsSwitchRow(
                        icon = Icons.Default.LocalGroceryStore,
                        title = stringResource(R.string.notif_new_listing),
                        checked = state.notifNewListing,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.NotifNewListingChanged(it)) }
                    )

                    SettingsRowDivider()

                    SettingsSwitchRow(
                        icon = Icons.Default.LocalShipping,
                        title = stringResource(R.string.notif_request_update),
                        checked = state.notifRequestUpdate,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.NotifRequestUpdateChanged(it)) }
                    )

                    SettingsRowDivider()

                    SettingsSwitchRow(
                        icon = Icons.Default.Timer,
                        title = stringResource(R.string.notif_expiry_reminder),
                        checked = state.notifExpiry,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.NotifExpiryChanged(it)) }
                    )
                }
            }

            // ── About ─────────────────────────────────────────────────────
            item {
                val termsUrl = stringResource(R.string.url_terms_of_service)
                val privacyUrl = stringResource(R.string.url_privacy_policy)

                DashboardSection(title = stringResource(R.string.about)) {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.app_version, BuildConfig.VERSION_NAME)
                    )

                    SettingsRowDivider()

                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Article,
                        title = stringResource(R.string.terms_of_service),
                        onClick = { openLink(termsUrl) }
                    )

                    SettingsRowDivider()

                    SettingsRow(
                        icon = Icons.Default.PrivacyTip,
                        title = stringResource(R.string.privacy_policy),
                        onClick = { openLink(privacyUrl) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Sub-components ───────────────────────────────────────────────────────────
// Rows share the icon tile, title weight and trailing affordance of DashboardActionCard, so a
// settings group reads as the same furniture as the action lists on Profile and the dashboards.
// They sit as rows inside one DashboardSection card rather than as separate cards, because a
// setting is a line in a group, not a destination of its own.

@Composable
private fun SettingsIconTile(
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 40.dp
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = tint
            )
        }
    }
}

@Composable
private fun SettingsRowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    val row: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconTile(icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            when {
                trailing != null -> trailing()
                onClick != null -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = {
                HapticUtils.tick(context)
                onClick()
            },
            color = Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        ) { row() }
    } else {
        row()
    }
}

/**
 * A setting whose value is one of a short, fixed list. The current value is the control: it sits
 * at the end of the row as a tonal pill and opens a menu of the alternatives, so a row states
 * what it is set to without having to spell the value out in a subtitle as well.
 */
@Composable
private fun SettingsDropdownRow(
    icon: ImageVector,
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected

    SettingsRow(
        icon = icon,
        title = title,
        trailing = {
            Box {
                Surface(
                    onClick = {
                        HapticUtils.tick(context)
                        expanded = true
                    },
                    shape = ButtonShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = selectedLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                expanded = false
                                if (value != selected) {
                                    HapticUtils.tick(context)
                                    onSelect(value)
                                }
                            },
                            leadingIcon = {
                                // Always occupies the slot, so the labels stay on one left edge
                                // instead of shifting as the selection moves between rows.
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (value == selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val toggle = {
        HapticUtils.tick(context)
        onCheckedChange(!checked)
    }
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = toggle,
        trailing = {
            // The whole row is the click target; the switch reports state and delegates the
            // change back to the row, so a tap anywhere on the line behaves identically.
            Switch(
                checked = checked,
                onCheckedChange = { toggle() }
            )
        }
    )
}
