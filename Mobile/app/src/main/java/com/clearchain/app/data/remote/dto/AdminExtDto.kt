package com.clearchain.app.data.remote.dto

import kotlinx.serialization.Serializable

// ── Detailed statistics (date-range) ─────────────────────────────────────────

@Serializable
data class AdminDetailedStatsResponse(
    val data: AdminDetailedStatsData? = null
)

@Serializable
data class AdminDetailedStatsData(
    val period: StatsPeriod,
    val organizations: OrgBreakdown,
    val listings: ListingBreakdown,
    val requests: RequestBreakdown,
    val impact: ImpactBreakdown,
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val leaderboards: Leaderboards,
    val dailyTrend: List<DailyTrendItem> = emptyList()
)

@Serializable
data class StatsPeriod(val from: String? = null, val to: String? = null, val preset: String = "all")

@Serializable
data class OrgBreakdown(
    val totalOrgs: Int = 0,
    val totalGroceries: Int = 0,
    val totalNgos: Int = 0,
    val verifiedOrgs: Int = 0,
    val pendingVerif: Int = 0
)

@Serializable
data class ListingBreakdown(
    val totalListings: Int = 0,
    val activeListings: Int = 0,
    val archivedListings: Int = 0
)

@Serializable
data class RequestBreakdown(
    val totalRequests: Int = 0,
    val completedReqs: Int = 0,
    val pendingReqs: Int = 0,
    val cancelledReqs: Int = 0
)

@Serializable
data class ImpactBreakdown(
    val kgSaved: Double = 0.0,
    val mealsEquivalent: Int = 0,
    val co2Saved: Double = 0.0,
    val totalBeneficiaries: Int = 0
)

@Serializable
data class CategoryBreakdownItem(
    val category: String,
    val count: Int,
    val quantity: Int
)

@Serializable
data class Leaderboards(
    val topGroceries: List<LeaderboardEntry> = emptyList(),
    val topNgos: List<NgoLeaderboardEntry> = emptyList()
)

@Serializable
data class LeaderboardEntry(
    val id: String,
    val name: String,
    val completedPickups: Int,
    val totalKg: Int
)

@Serializable
data class NgoLeaderboardEntry(
    val id: String,
    val name: String,
    val completedPickups: Int
)

@Serializable
data class DailyTrendItem(
    val date: String,
    val count: Int,
    val quantity: Int
)

// ── System health ─────────────────────────────────────────────────────────────

@Serializable
data class AdminHealthResponse(
    val data: AdminHealthData? = null
)

@Serializable
data class AdminHealthData(
    val status: String = "healthy",
    val database: DatabaseHealth,
    val alerts: AlertCounts,
    val timestamp: String
)

@Serializable
data class DatabaseHealth(val ok: Boolean, val latencyMs: Long)

@Serializable
data class AlertCounts(
    val pendingVerifications: Int = 0,
    val openDisputes: Int = 0,
    val pendingReports: Int = 0,
    val unreadNotifications: Int = 0
)

// ── Alert feed ────────────────────────────────────────────────────────────────

@Serializable
data class AdminAlertFeedResponse(
    val data: List<AdminAlertItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class AdminAlertItem(
    val type: String,        // "dispute" | "report"
    val severity: String,    // "high" | "medium"
    val id: String,
    val title: String,
    val body: String,
    val initiator: String,
    val status: String,
    val createdAt: String
)

// ── User growth chart ─────────────────────────────────────────────────────────

@Serializable
data class UserGrowthResponse(
    val data: List<UserGrowthDay> = emptyList()
)

@Serializable
data class UserGrowthDay(
    val date: String,
    val count: Int
)

// ── Public org profile ────────────────────────────────────────────────────────

@Serializable
data class PublicProfileResponse(
    val data: PublicProfileData? = null
)

@Serializable
data class PublicProfileData(
    val id: String,
    val name: String,
    val type: String,
    val location: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val description: String? = null,
    val hours: String? = null,
    val profilePictureUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactPerson: String? = null,
    val verified: Boolean = false,
    val verificationStatus: String = "pending",
    val createdAt: String,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val completedPickups: Int = 0
)

// ── NGO reputation ────────────────────────────────────────────────────────────

@Serializable
data class NgoReputationResponse(
    val data: NgoReputationData? = null
)

@Serializable
data class NgoReputationData(
    val totalRequests: Int = 0,
    val completedPickups: Int = 0,
    val cancelledPickups: Int = 0,
    val completionRate: Double = 0.0
)

// ── Today summary ─────────────────────────────────────────────────────────────
// TodaySummaryResponse and TodaySummaryData are defined in OrganizationDto.kt

// ── Inventory stats ───────────────────────────────────────────────────────────

@Serializable
data class InventoryStatsResponse(
    val data: InventoryStatsData? = null
)

@Serializable
data class InventoryStatsData(
    val categoryBreakdown: List<InventoryCategoryItem> = emptyList(),
    val expiringSoonCount: Int = 0,
    val totalBeneficiariesServed: Int = 0,
    val totalActiveItems: Int = 0
)

@Serializable
data class InventoryCategoryItem(
    val category: String,
    val count: Int,
    val quantity: Double
)

// ── Report ────────────────────────────────────────────────────────────────────

@Serializable
data class SubmitReportRequest(
    val listingId: String,
    val reason: String,
    val details: String? = null
)
