package com.clearchain.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.data.remote.signalr.ConnectionState
import com.clearchain.app.ui.theme.ShapeCircle
import kotlinx.coroutines.delay

/**
 * Tells the user that live updates have stopped arriving and the app is trying to get them back.
 *
 * Worth showing because the screens now lean on real-time: a grocery watching its request list,
 * an NGO watching a listing's remaining quantity. Without this, a dropped connection looks
 * exactly like "nothing is happening", and someone can act on numbers that stopped updating
 * minutes ago.
 *
 * Deliberately quiet — no error colour, no dismiss button, no blocking. The condition resolves
 * itself, everything on screen still works, and anything missed arrives as a notification.
 */
@Composable
fun ReconnectingBanner(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    // Only Reconnecting earns a banner. Disconnected is the deliberate state the app sits in
    // while backgrounded or signed out, and flagging that would be noise.
    val reconnecting = state is ConnectionState.Reconnecting

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(reconnecting) {
        if (reconnecting) {
            // A brief drop recovers on the first retry. Waiting past that keeps the banner from
            // flashing on every tunnel, lift and handover.
            delay(SHOW_DELAY_MS)
            visible = true
        } else {
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = ShapeCircle,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.realtime_reconnecting),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Long enough to ride out a single failed retry without the banner appearing. */
private const val SHOW_DELAY_MS = 2_500L
