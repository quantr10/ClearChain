package com.clearchain.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(14.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Convenience aliases used across the codebase
val ShapeXSmall   = RoundedCornerShape(6.dp)
val ShapeSmall    = RoundedCornerShape(10.dp)
val ShapeMedium   = RoundedCornerShape(14.dp)
val ShapeLarge    = RoundedCornerShape(20.dp)
val ShapeXLarge   = RoundedCornerShape(28.dp)
val ShapeCircle   = RoundedCornerShape(50)

// Card-specific shapes
val CardShape     = RoundedCornerShape(12.dp)
val ChipShape     = RoundedCornerShape(50)
val ButtonShape   = RoundedCornerShape(10.dp)
val BadgeShape    = RoundedCornerShape(6.dp)
val BottomSheet   = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
