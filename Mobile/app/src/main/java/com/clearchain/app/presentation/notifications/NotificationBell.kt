package com.clearchain.app.presentation.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.presentation.components.ClearChainActionIconButton

/**
 * Bell that opens the notification inbox, with the unread count on it.
 *
 * Built from [ClearChainActionIconButton] and Material's [Badge] rather than its own sized Box,
 * so it stays the same size as every other counted icon button in the app — the filter button
 * it sits nearest to, above all — instead of drifting the next time those defaults change.
 *
 * Reads its own count from Room through [NotificationBellViewModel] rather than taking it as a
 * parameter, so the three dashboards that show it don't each have to thread inbox state through
 * their own view models for a badge none of them otherwise care about.
 */
@Composable
fun NotificationBell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationBellViewModel = hiltViewModel()
) {
    val unreadCount by viewModel.unreadCount.collectAsState()

    BadgedBox(
        modifier = modifier,
        badge = {
            if (unreadCount > 0) {
                Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) }
            }
        }
    ) {
        ClearChainActionIconButton(
            icon = Icons.Default.Notifications,
            contentDescription = stringResource(R.string.cd_open_notifications),
            onClick = onClick,
            // The header is a green gradient, so the button's surface colours would disappear
            // into it; white-on-translucent is the same treatment the avatar's border uses.
            tint = Color.White,
            containerColor = Color.White.copy(alpha = 0.18f)
        )
    }
}
