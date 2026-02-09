package com.clearchain.app.presentation.grocery

import androidx.lifecycle.ViewModel
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GroceryDashboardViewModel @Inject constructor(
    val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel()