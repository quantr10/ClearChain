package com.clearchain.app.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.clearchain.app.R
import com.clearchain.app.ui.theme.*
import kotlinx.coroutines.delay

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

/** How long [LoadingState] stays invisible before its spinner draws. */
private const val AREA_SPINNER_DELAY_MS = 300L

/** Kind A: the spinner that replaces a control's icon or content while its action runs. */
@Composable
fun InlineSpinner(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    CircularProgressIndicator(
        modifier = modifier.size(SpinnerInlineSize),
        color = color,
        strokeWidth = SpinnerInlineStroke
    )
}

/**
 * Kind B: an area (screen or section) with nothing to show yet. The spinner is centred in
 * whatever [modifier] sizes it to; pass `Modifier.fillMaxSize()` for a whole screen.
 *
 * It stays invisible for its first [AREA_SPINNER_DELAY_MS], so a load answered from the cache
 * doesn't flash it, but it takes its space from the start, so nothing moves when it appears.
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(AREA_SPINNER_DELAY_MS)
        visible = true
    }
    val loadingLabel = stringResource(R.string.cd_loading)

    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CardPadding),
            modifier = Modifier
                .padding(ScreenHPadding)
                .alpha(if (visible) 1f else 0f)
                .then(if (visible) Modifier else Modifier.clearAndSetSemantics {})
        ) {
            CircularProgressIndicator(
                Modifier
                    .size(SpinnerAreaSize)
                    .semantics { contentDescription = loadingLabel }
            )
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
    Box(modifier.fillMaxWidth().height(UpdatingBarHeight)) {
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
