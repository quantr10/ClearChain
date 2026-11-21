package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.clearchain.app.R
import com.clearchain.app.presentation.notifications.NotificationBell
import com.clearchain.app.ui.theme.CardPadding
import com.clearchain.app.ui.theme.EyebrowStyle
import com.clearchain.app.ui.theme.HeaderAvatarSize
import com.clearchain.app.ui.theme.ListGap
import com.clearchain.app.ui.theme.OnBrandGradient
import com.clearchain.app.ui.theme.ScreenHPadding
import com.clearchain.app.ui.theme.ScreenVPadding

/**
 * The dashboard's gradient row: "Hi, <name>", the bell and the avatar. It goes in a
 * [BrandHeader.Hero]. The greeting takes the width left after the bell and avatar and wraps onto
 * as many lines as it needs, so a long name never pushes them off screen.
 */
@Composable
fun DashboardGreeting(
    userName: String,
    profilePictureUrl: String?,
    onAvatarClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHPadding)
            .padding(top = ScreenVPadding, bottom = ScreenHPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CardPadding)
    ) {
        Text(
            text = stringResource(R.string.greeting_hi, userName),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = OnBrandGradient,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        NotificationBell(onClick = onNotificationsClick)
        AvatarImage(
            imageUrl = profilePictureUrl,
            name = userName,
            size = HeaderAvatarSize,
            borderColor = OnBrandGradient,
            onClick = onAvatarClick,
            clickLabel = stringResource(R.string.cd_open_account_details)
        )
    }
}

/**
 * The top of a dashboard's sheet: the [role] as an uppercase eyebrow and the [subtitle] under
 * it, both in `on-surface-variant`. They sit here rather than on the gradient, where they fall
 * short of contrast.
 */
@Composable
fun DashboardIntro(role: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListGap)
    ) {
        Text(
            text = role.uppercase(),
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
