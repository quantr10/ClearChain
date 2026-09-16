package com.clearchain.app.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val labelResId: Int,
    /** Where the tab navigates. Differs from [route] when the pattern carries arguments. */
    val navRoute: String = route
)
