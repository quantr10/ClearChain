package com.clearchain.app.presentation.ngo

import androidx.lifecycle.ViewModel
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NgoDashboardViewModel @Inject constructor(
    val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel()