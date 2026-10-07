package com.clearchain.app.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/*
 * The app has exactly three ways to show that it is waiting. Pick by asking
 * "what is waiting?" — the indicator always sits where the result will appear.
 *
 * A. Something the user just tapped is working  -> [InlineSpinner] inside that control
 *    (ClearChainButton/OutlinedButton `loading`, ConfirmDialog `confirmLoading`, icon
 *    buttons, an avatar or file row being uploaded). Lock the action's scope meanwhile
 *    and block back with [BlockBackWhile].
 * B. Nothing to show yet                        -> [LoadingState] filling that area
 *    (a whole screen's first load, or one empty section).
 * C. Content is there and is being updated      -> [UpdatingBar] on that area's top edge,
 *    with the old content left in place (reload, period/filter switch, map refetch).
 *
 * Pull-to-refresh keeps its own system indicator. Images use a static placeholder,
 * never a spinner.
 */
object LoadingDefaults {
    /** The one spinner size inside controls (buttons, icon buttons, rows, overlays). */
    val InlineSize = 16.dp
    val InlineStroke = 2.dp

    /** The one spinner size for an area with nothing to show yet. */
    val AreaSize = 40.dp

    val BarHeight = 2.dp
}

/** Kind A: the spinner that replaces a control's icon or content while its action runs. */
@Composable
fun InlineSpinner(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    CircularProgressIndicator(
        modifier = modifier.size(LoadingDefaults.InlineSize),
        color = color,
        strokeWidth = LoadingDefaults.InlineStroke
    )
}

/** Kind B: an area (screen or section) with nothing to show yet. */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier.fillMaxSize(),
    message: String? = null
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            CircularProgressIndicator(Modifier.size(LoadingDefaults.AreaSize))
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Kind C: content is on screen and is being updated. Place it on the top edge of the
 * area that is changing. It always takes its height, so content never jumps when it
 * appears or goes away.
 */
@Composable
fun UpdatingBar(visible: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(LoadingDefaults.BarHeight)) {
        if (visible) LinearProgressIndicator(Modifier.fillMaxSize())
    }
}

/**
 * While an action is in flight the user stays on the screen: system back is swallowed so
 * the call is not cut off halfway (the same rule a spinning ConfirmDialog applies).
 */
@Composable
fun BlockBackWhile(busy: Boolean) {
    BackHandler(enabled = busy) { /* wait for the action to settle */ }
}
