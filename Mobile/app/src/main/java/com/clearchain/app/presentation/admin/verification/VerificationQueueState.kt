package com.clearchain.app.presentation.admin.verification

import com.clearchain.app.domain.model.Organization

data class VerificationQueueState(
    val organizations: List<Organization> = emptyList(),
    // ❌ REMOVED: unverifiedOrganizations (not needed)

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)