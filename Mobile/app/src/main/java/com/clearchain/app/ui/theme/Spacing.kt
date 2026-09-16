package com.clearchain.app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

// ── Screen content inset ─────────────────────────────────────────────────────
// Standard inset for a screen's primary scroll container (LazyColumn
// contentPadding, or .padding() on a verticalScroll Column). Keeps every screen
// aligned to the same left/right gutter and top/bottom breathing room.

val ScreenHPadding = 16.dp
val ScreenVPadding = 8.dp

/** Use as `contentPadding = ScreenPadding` on a LazyColumn / LazyRow / LazyGrid. */
val ScreenPadding = PaddingValues(horizontal = ScreenHPadding, vertical = ScreenVPadding)
