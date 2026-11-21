package com.clearchain.app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

// ── Screen content inset ─────────────────────────────────────────────────────
// Standard inset for a screen's primary scroll container (LazyColumn
// contentPadding, or .padding() on a verticalScroll Column). Keeps every screen
// aligned to the same left/right gutter and top/bottom breathing room.

/** Design system `screen-h-padding`: the left and right inset of a screen's scroll content. */
val ScreenHPadding = 16.dp

/** Design system `screen-v-padding`: the top and bottom inset of a screen's scroll content. */
val ScreenVPadding = 8.dp

/** Use as `contentPadding = ScreenPadding` on a LazyColumn / LazyRow / LazyGrid. */
val ScreenPadding = PaddingValues(horizontal = ScreenHPadding, vertical = ScreenVPadding)

// ── Component spacing ────────────────────────────────────────────────────────

/**
 * Design system `list-gap`: between cards in a list, and between the header rows above it
 * (search, filter chips, count and sort), so the header keeps the list's rhythm.
 */
val ListGap = 8.dp

/** Design system `icon-gap`: between an icon and its label in buttons, fields and detail rows. */
val IconGap = 6.dp

/** Design system `control-padding`: horizontal padding inside buttons, text fields and search. */
val ControlPadding = 8.dp

/** Design system `tile-gap`: between stat tiles. */
val TileGap = 10.dp

/** Design system `card-padding`: the content inset inside cards, sections and action cards. */
val CardPadding = 12.dp

/** Design system `header-padding`: the top and bottom padding of the dashboard greeting. */
val HeaderPadding = 24.dp

/** Design system `title-row-v-padding`: above and below a screen's title row. */
val TitleRowVPadding = 6.dp
