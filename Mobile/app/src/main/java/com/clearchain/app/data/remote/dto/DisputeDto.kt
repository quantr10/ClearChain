package com.clearchain.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DisputeData(
    val id: String,
    val pickupRequestId: String,
    val initiatorId: String,
    val initiatorName: String,
    val reason: String,
    val ngoStatement: String? = null,
    val groceryStatement: String? = null,
    val photoEvidenceUrl: String? = null,
    val status: String, // open, under_review, resolved_ngo, resolved_grocery, dismissed
    val adminResolution: String? = null,
    val createdAt: String,
    val resolvedAt: String? = null
)

@Serializable
data class DisputeResponse(
    val message: String = "",
    val data: DisputeData? = null
)

/** Contact details for one side of a dispute — an admin resolves these by phone/email, so
 * there is no in-app negotiation flow between the NGO and the grocery. */
@Serializable
data class DisputePartyContact(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val profilePictureUrl: String? = null
)

@Serializable
data class DisputeListingItem(
    val title: String,
    val category: String = "",
    val quantity: Int = 0,
    val unit: String = "",
    val expiryDate: String? = null,
    val photoUrl: String? = null
)

@Serializable
data class DisputeListItemData(
    val id: String,
    val pickupRequestId: String,
    val listingTitle: String,
    val pickupDate: String,
    val items: List<DisputeListingItem> = emptyList(),
    val ngo: DisputePartyContact,
    val grocery: DisputePartyContact,
    val reason: String,
    val ngoStatement: String? = null,
    val groceryStatement: String? = null,
    val photoEvidenceUrl: String? = null,
    val status: String, // open, under_review, resolved_ngo, resolved_grocery, dismissed
    val adminResolution: String? = null,
    val createdAt: String,
    val resolvedAt: String? = null
)

@Serializable
data class DisputeListResponse(
    val message: String = "",
    val data: List<DisputeListItemData> = emptyList()
)

@Serializable
data class DisputeListItemResponse(
    val message: String = "",
    val data: DisputeListItemData
)

/** An NGO's view of its own dispute; the grocery's contact details are admin-only. */
@Serializable
data class MyDisputeData(
    val id: String,
    val pickupRequestId: String,
    val reason: String,
    val ngoStatement: String? = null,
    val photoEvidenceUrl: String? = null,
    val status: String, // open, under_review, resolved_ngo, resolved_grocery, dismissed
    val adminResolution: String? = null,
    val createdAt: String,
    val resolvedAt: String? = null
)

@Serializable
data class MyDisputeListResponse(
    val message: String = "",
    val data: List<MyDisputeData> = emptyList()
)

@Serializable
data class ResolveDisputeRequest(
    val status: String,
    val groceryStatement: String? = null,
    val adminResolution: String
)
