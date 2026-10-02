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
    val phone: String? = null
)

@Serializable
data class DisputeListItemData(
    val id: String,
    val pickupRequestId: String,
    val listingTitle: String,
    val pickupDate: String,
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

@Serializable
data class ResolveDisputeRequest(
    val status: String,
    val groceryStatement: String? = null,
    val adminResolution: String
)
