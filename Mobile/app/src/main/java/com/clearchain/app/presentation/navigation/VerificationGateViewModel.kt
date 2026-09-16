package com.clearchain.app.presentation.navigation

import androidx.lifecycle.ViewModel
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Thin Hilt entry point so NavGraph can read the cached current user from a Compose
 * destination without depending on that screen's own ViewModel. Used as a defense-in-depth
 * guard on dashboard routes: Splash/Login already keep a pending/rejected org off the
 * dashboard, this catches the rare case where a cached "approved" user is actually stale
 * (e.g. the app was killed mid-transition into PendingReview).
 */
@HiltViewModel
class VerificationGateViewModel @Inject constructor(
    val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel()
