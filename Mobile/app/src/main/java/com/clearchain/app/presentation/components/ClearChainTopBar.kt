// ═══════════════════════════════════════════════════════════════════════════════
// ClearChainTopBar.kt — Unified app bar for dashboard screens
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearChainTopBar(
    userName: String,
    userType: String,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "ClearChain",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        actions = {
            actions()
            IconButton(onClick = onProfileClick) {
                Icon(Icons.Default.Person, "Profile")
            }
            IconButton(onClick = onLogoutClick) {
                Icon(Icons.Default.Logout, "Logout")
            }
        }
    )
}