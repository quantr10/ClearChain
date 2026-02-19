package com.clearchain.app.presentation.profile

import com.clearchain.app.domain.model.Organization

data class ProfileState(
    val user: Organization? = null,
    val isLoading: Boolean = false,
    val isChangingPassword: Boolean = false,
    val error: String? = null
)