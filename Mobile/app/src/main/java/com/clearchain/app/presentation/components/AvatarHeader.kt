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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.clearchain.app.R
import com.clearchain.app.ui.theme.AvatarRingWidth

/** [AvatarImage] with its size in whole dp, for the callers that pass a number. */
@Composable
fun AvatarImage(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 40,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    borderColor: Color? = null,
    borderWidth: Dp = AvatarRingWidth,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null
) = AvatarImage(
    imageUrl = imageUrl,
    name = name,
    size = size.dp,
    modifier = modifier,
    backgroundColor = backgroundColor,
    textColor = textColor,
    borderColor = borderColor,
    borderWidth = borderWidth,
    onClick = onClick,
    clickLabel = clickLabel
)

/**
 * An organization's round avatar: its picture, or its initial on [backgroundColor] when it has
 * none. With [onClick] and [clickLabel] it is a button named by what it opens ("Open account
 * details"), and the picture's own description and the initial are not read.
 */
@Composable
fun AvatarImage(
    imageUrl: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    borderColor: Color? = null,
    borderWidth: Dp = AvatarRingWidth,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null
) {
    val avatarMod = modifier
        .size(size)
        .clip(CircleShape)
        .let { if (borderColor != null) it.border(borderWidth, borderColor, CircleShape) else it }
        .let {
            if (onClick != null && clickLabel != null) {
                it.clearAndSetSemantics {
                    contentDescription = clickLabel
                    role = Role.Button
                    onClick {
                        onClick()
                        true
                    }
                }
            } else {
                it
            }
        }
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
