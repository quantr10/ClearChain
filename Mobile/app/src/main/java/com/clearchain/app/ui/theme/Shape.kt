package com.clearchain.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material's five steps, the design system's `shape-extra-small` (badges) through
 * `shape-extra-large` (dialogs and sheets).
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Design system `shape-medium`: text fields and search, and detail-screen section cards. */
val ShapeMedium = RoundedCornerShape(14.dp)

/** Design system `card-shape`: list cards, photo dialogs and dashboard icon tiles. */
val CardShape = RoundedCornerShape(12.dp)

/** Design system `button-shape`: buttons, the sort pill and the request cards' waiting strips. */
val ButtonShape = RoundedCornerShape(10.dp)

/** Design system `action-card-shape`: dashboard sections, dashboard action cards and Profile rows. */
val ActionCardShape = RoundedCornerShape(16.dp)
