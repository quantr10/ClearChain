package com.clearchain.app.presentation.admin.verification

import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.VerificationStatus
import com.clearchain.app.presentation.components.CommonSortOptions
import com.clearchain.app.presentation.components.SortOption

const val VERIFICATION_CHECKLIST_SIZE = 6

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

    // Sort
    val selectedSort: SortOption = CommonSortOptions.CREATED_DATE_DESC,
    val availableSortOptions: List<SortOption> = listOf(
        CommonSortOptions.CREATED_DATE_DESC,
        CommonSortOptions.CREATED_DATE_ASC,
        CommonSortOptions.NAME_ASC,
        CommonSortOptions.NAME_DESC
    ),

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
            "PENDING" -> pendingOrgs
            "APPROVED" -> approvedOrgs
            "REJECTED" -> rejectedOrgs
            else -> organizations
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
        result = when (selectedSort.value) {
            "date_asc" -> result.sortedBy { it.createdAt }
            "name_asc" -> result.sortedBy { it.name.lowercase() }
            "name_desc" -> result.sortedByDescending { it.name.lowercase() }
            else -> result.sortedByDescending { it.createdAt } // "date_desc" and default
        }
        return result
    }

    val checklistComplete: Boolean get() =
        checkedItems.size == VERIFICATION_CHECKLIST_SIZE

    val allSelected: Boolean get() =
        filteredOrgs.isNotEmpty() && selectedOrgIds.containsAll(filteredOrgs.map { it.id })
}
