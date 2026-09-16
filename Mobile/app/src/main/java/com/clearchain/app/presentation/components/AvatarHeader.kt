package com.clearchain.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.clearchain.app.R
import com.clearchain.app.presentation.notifications.NotificationBell
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal

/**
 * Gradient welcome header used at the top of dashboards.
 */
@Composable
fun DashboardWelcomeHeader(
    userName: String,
    subtitle: String = "",
    roleLabel: String = "",
    profilePictureUrl: String? = null,
    onProfileClick: () -> Unit = {},
    onNotificationsClick: (() -> Unit)? = null,
    gradientColors: List<Color> = listOf(BrandTeal, BrandGreen),
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(colors = gradientColors)
            )
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (roleLabel.isNotBlank()) {
                    Text(
                        text = roleLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                        letterSpacing = androidx.compose.ui.unit.TextUnit(
                            1.5f,
                            androidx.compose.ui.unit.TextUnitType.Sp
                        )
                    )
                }
                Text(
                    text = stringResource(R.string.greeting_hi, userName),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sits above the avatar rather than beside it: the greeting column on the left
                // is the widest thing in this header, and a second element on the same row
                // squeezes it into wrapping on narrow screens.
                onNotificationsClick?.let { NotificationBell(onClick = it) }
                AvatarImage(
                    imageUrl = profilePictureUrl,
                    name = userName,
                    size = 48,
                    borderColor = Color.White,
                    onClick = onProfileClick
                )
                trailingContent?.invoke()
            }
        }
    }
}

@Composable
fun AvatarImage(
    imageUrl: String?,
    name: String,
    size: Int = 40,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    borderColor: Color? = null,
    borderWidth: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val avatarMod = modifier
        .size(size.dp)
        .clip(CircleShape)
        .let { if (borderColor != null) it.border(borderWidth, borderColor, CircleShape) else it }
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.cd_profile_picture_of, name),
            modifier = avatarMod,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = avatarMod.background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

/**
 * Small circular org avatar overlaid on a listing or inventory photo. Shows the
 * uploaded profile picture when the org has one, and falls back to the initial
 * otherwise, so every overlay stays consistent across browse, detail and inventory.
 */
@Composable
fun OverlayAvatar(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .size(size)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(2.dp, Color.White),
        shadowElevation = 3.dp
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.cd_profile_picture_of, name),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
