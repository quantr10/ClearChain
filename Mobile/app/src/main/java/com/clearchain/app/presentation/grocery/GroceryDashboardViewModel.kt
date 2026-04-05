package com.clearchain.app.presentation.grocery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.dto.DashboardStatsData
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroceryDashboardViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val organizationApi: OrganizationApi
) : ViewModel() {

    private val _userName = MutableStateFlow("Grocery Store")
    val userName = _userName.asStateFlow()

    private val _stats = MutableStateFlow<DashboardStatsData?>(null)
    val stats = _stats.asStateFlow()

    init {
        loadUser()
        loadStats()
    }

    fun loadUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().first()?.let { user ->
                _userName.value = user.name
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                val response = organizationApi.getMyStats()
                _stats.value = response.data
            } catch (_: Exception) {
                // Dashboard shows "–" if stats fail — non-fatal
            }
        }
    }
}
