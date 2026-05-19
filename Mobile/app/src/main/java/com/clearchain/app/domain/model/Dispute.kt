package com.clearchain.app.domain.model

data class Dispute(
    val id: String,
    val pickupRequestId: String,
    val initiatorId: String,
    val initiatorName: String,
    val reason: String,
    val ngoStatement: String?,
    val groceryStatement: String?,
    val photoEvidenceUrl: String?,
    val status: String,           // open, under_review, resolved_ngo, resolved_grocery, dismissed
    val adminResolution: String?,
    val createdAt: String,
    val resolvedAt: String?
) {
    val isResolved: Boolean get() = status.startsWith("resolved") || status == "dismissed"
    val isOpen: Boolean get() = status == "open"
    val isUnderReview: Boolean get() = status == "under_review"
}
