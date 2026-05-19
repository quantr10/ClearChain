package com.clearchain.app.presentation.grocery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.dto.ActivityItemData
import com.clearchain.app.data.remote.dto.DashboardStatsData
import com.clearchain.app.data.remote.dto.TodaySummaryData
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroceryDashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val organizationApi: OrganizationApi
) : ViewModel() {

    private val _userName = MutableStateFlow(context.getString(R.string.label_grocery_store_name))
    val userName = _userName.asStateFlow()

    private val _user = MutableStateFlow<Organization?>(null)
    val user = _user.asStateFlow()

    private val _profileCompleteness = MutableStateFlow(0)
    val profileCompleteness = _profileCompleteness.asStateFlow()

    private val _stats = MutableStateFlow<DashboardStatsData?>(null)
    val stats = _stats.asStateFlow()

    private val _todaySummary = MutableStateFlow<TodaySummaryData?>(null)
    val todaySummary = _todaySummary.asStateFlow()

    private val _activities = MutableStateFlow<List<ActivityItemData>>(emptyList())
    val activities = _activities.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        loadAll()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadAll()
            _isRefreshing.value = false
        }
    }

    private fun loadAll() {
        loadUser()
        loadStats()
        loadTodaySummary()
        loadActivity()
    }

    private fun loadUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().first()?.let { user ->
                _userName.value = user.name
                _user.value = user
                _profileCompleteness.value = computeCompleteness(user)
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                val response = organizationApi.getMyStats()
                _stats.value = response.data
            } catch (_: Exception) {}
        }
    }

    private fun loadTodaySummary() {
        viewModelScope.launch {
            try {
                val response = organizationApi.getTodaySummary()
                _todaySummary.value = response.data
            } catch (_: Exception) {}
        }
    }

    private fun loadActivity() {
        viewModelScope.launch {
            try {
                val response = organizationApi.getMyActivity()
                _activities.value = response.data
            } catch (_: Exception) {}
        }
    }

    private fun computeCompleteness(org: Organization): Int {
        val fields = listOf(
            org.phone.isNotBlank(),
            org.address.isNotBlank(),
            org.location.isNotBlank(),
            !org.hours.isNullOrBlank(),
            !org.contactPerson.isNullOrBlank(),
            !org.description.isNullOrBlank(),
            org.profilePictureUrl != null
        )
        return (fields.count { it } * 100 / fields.size)
    }
}
