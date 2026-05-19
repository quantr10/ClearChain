package com.clearchain.app.presentation.admin.verification

import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.VerificationStatus

const val VERIFICATION_CHECKLIST_SIZE = 6
const val REJECTION_TEMPLATES_SIZE = 6

data class VerificationQueueState(
    val organizations: List<Organization> = emptyList(),

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,

    // Search + filter
    val searchQuery: String = "",
    val showFilterSheet: Boolean = false,
    val filterOrgType: String? = null, // null = all, "GROCERY"/"NGO"

    // Status filter
    val selectedStatus: String? = "PENDING", // null = all

    // Checklist for approval
    val showChecklistForId: String? = null,
    val checkedItems: Set<Int> = emptySet(), // indices of checked checklist items

    // Rejection dialog
    val showRejectDialogForId: String? = null,
    val rejectionReason: String = "",

    val isProcessing: Boolean = false,

    // Batch selection mode
    val isBatchMode: Boolean = false,
    val selectedOrgIds: Set<String> = emptySet()
) {
    val pendingOrgs: List<Organization> get() =
        organizations.filter { it.verificationStatus == VerificationStatus.PENDING }
    val approvedOrgs: List<Organization> get() =
        organizations.filter { it.verificationStatus == VerificationStatus.APPROVED }
    val rejectedOrgs: List<Organization> get() =
        organizations.filter { it.verificationStatus == VerificationStatus.REJECTED }

    val activeFilterCount: Int get() = if (filterOrgType != null) 1 else 0

    val filteredOrgs: List<Organization> get() {
        var result = when (selectedStatus) {
            "PENDING"  -> pendingOrgs
            "APPROVED" -> approvedOrgs
            "REJECTED" -> rejectedOrgs
            else       -> organizations
        }
        filterOrgType?.let { type -> result = result.filter { it.type.name == type } }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter {
                it.name.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.location.lowercase().contains(q)
            }
        }
        return result
    }

    val checklistComplete: Boolean get() =
        checkedItems.size == VERIFICATION_CHECKLIST_SIZE
}
