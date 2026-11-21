package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.clearchain.app.R
import com.clearchain.app.ui.theme.Alpha
import com.clearchain.app.ui.theme.BackButtonSize
import com.clearchain.app.ui.theme.CardPadding
import com.clearchain.app.ui.theme.TitleIconSize
import com.clearchain.app.ui.theme.TitleRowVPadding
import com.clearchain.app.util.HapticUtils

/**
 * A small circular back button + a plain screen title, sitting right above where a
 * screen's content starts. Drop this in as the FIRST child of the content
 * container (the scrolling Column / LazyColumn) on every screen that is NOT a
 * bottom-nav-bar destination, padded by `ScreenHPadding` at the sides.
 *
 * Deliberately NOT a [androidx.compose.material3.TopAppBar] / Scaffold `topBar` and
 * NOT a full-width banner — it scrolls with the content and shares the screen's
 * normal horizontal gutter, so it reads as part of the content, not a header.
 *
 * The title is a heading and wraps onto as many lines as it needs. Its first line, the back
 * button and the [actions] share one band of [BackButtonSize], so they stay level when the
 * title wraps.
 */
@Composable
fun ScreenTitleRow(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** False while an action on the screen is in flight: back (button and system) waits. */
    backEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current
    BlockBackWhile(onBack != null && !backEnabled)
    val titleStyle = MaterialTheme.typography.titleMedium
    val lineHeight = with(LocalDensity.current) { titleStyle.lineHeight.toDp() }
    // Centres a one-line title on the back button; at large text sizes the line is taller
    // than the button and needs no offset.
    val titleTopPadding = ((BackButtonSize - lineHeight) / 2).coerceAtLeast(Dp.Hairline)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TitleRowVPadding),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(CardPadding)
    ) {
        if (onBack != null) {
            Surface(
                onClick = {
                    HapticUtils.tick(context)
                    onBack()
                },
                enabled = backEnabled,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(BackButtonSize)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        modifier = Modifier.size(TitleIconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (backEnabled) 1f else Alpha.Disabled
                        )
                    )
                }
            }
        }
        Text(
            text = title,
            style = titleStyle,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(top = titleTopPadding)
                .semantics { heading() }
        )
        Row(
            modifier = Modifier.heightIn(min = BackButtonSize),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}
