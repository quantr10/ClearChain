package com.clearchain.app.presentation.notifications

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.ui.theme.Alpha
import com.clearchain.app.ui.theme.BackButtonSize
import com.clearchain.app.ui.theme.OnBrandGradient

/** The most unread notifications the badge counts before it shows "99+". */
private const val BADGE_MAX = 99

/**
 * Bell on the dashboard's gradient header that opens the notification inbox, with the unread
 * count on it. It's the header back circle's style: a [BackButtonSize] disc of white at
 * [Alpha.LogoDisc]. Its spoken name carries the count ("Open notifications, 5 unread"), so the
 * badge itself is silent.
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
    val label = if (unreadCount > 0) {
        pluralStringResource(R.plurals.cd_open_notifications_unread, unreadCount, unreadCount)
    } else {
        stringResource(R.string.cd_open_notifications)
    }

    BadgedBox(
        modifier = modifier,
        badge = {
            if (unreadCount > 0) {
                Badge(Modifier.clearAndSetSemantics {}) {
                    Text(if (unreadCount > BADGE_MAX) "$BADGE_MAX+" else unreadCount.toString())
                }
            }
        }
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(BackButtonSize),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = OnBrandGradient.copy(alpha = Alpha.LogoDisc),
                contentColor = OnBrandGradient
            )
        ) {
            Icon(imageVector = Icons.Default.Notifications, contentDescription = label)
        }
    }
}
