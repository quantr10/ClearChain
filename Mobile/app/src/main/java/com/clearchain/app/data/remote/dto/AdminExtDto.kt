package com.clearchain.app.data.remote.dto

import kotlinx.serialization.Serializable

// ── Admin statistics ─────────────────────────────────────────────────────────
// Mirrors AdminStatisticsResponse on the API. Two kinds of number live here:
// period figures, counted over the selected range, and live figures (backlog,
// the organization register) that describe the platform right now.

@Serializable
data class AdminDetailedStatsResponse(
    val data: AdminDetailedStatsData? = null
)

@Serializable
data class AdminDetailedStatsData(
    val period: StatsPeriod = StatsPeriod(),
    val headline: StatsHeadline = StatsHeadline(),
    val funnel: StatsFunnel = StatsFunnel(),
    val backlog: StatsBacklog = StatsBacklog(),
    val timing: StatsTiming = StatsTiming(),
    val quality: StatsQuality = StatsQuality(),
    val leaderboards: StatsLeaderboards = StatsLeaderboards(),
    val organizations: StatsOrganizations = StatsOrganizations()
)

@Serializable
data class StatsPeriod(
    val from: String? = null,
    val to: String? = null,
    val preset: String = "all",
    val days: Int = 0,
    val isAllTime: Boolean = true
)

@Serializable
data class StatsHeadline(
    /** Pickups handed over in the period, whenever they were requested. */
    val completedPickups: Int = 0
)

@Serializable
data class StatsFunnel(
    val requests: Int = 0,
    val pending: Int = 0,
    val approved: Int = 0,
    val ready: Int = 0,
    val completed: Int = 0,
    val cancelled: Int = 0,
    val rejected: Int = 0
)

/** The live queue. Not scoped to the selected period. */
@Serializable
data class StatsBacklog(
    val openListings: Int = 0,
    val reservedListings: Int = 0,
    val expiredListings: Int = 0,
    val archivedListings: Int = 0,
    val expiringWithin24h: Int = 0,
    val pendingRequests: Int = 0,
    val approvedRequests: Int = 0,
    val readyRequests: Int = 0,
    val oldestPendingRequestHours: Double? = null,
    val pendingVerifications: Int = 0,
    val oldestPendingVerificationDays: Int? = null,
    val openDisputes: Int = 0,
    val pendingReports: Int = 0
)

@Serializable
data class StatsTiming(
    val medianHoursToReady: Double? = null,
    val medianHoursToPickup: Double? = null,
    val medianHoursToConfirm: Double? = null,
    val p90HoursToPickup: Double? = null,
    /** Share of completed pickups finished inside a day, 0..1. */
    val completedWithin24hRate: Double = 0.0,
    val sampleSize: Int = 0
)

@Serializable
data class StatsQuality(
    val averageRating: Double? = null,
    val reviewCount: Int = 0,
    /** Reviews / completed pickups, 0..1. */
    val reviewCoverage: Double = 0.0,
    val disputesOpened: Int = 0,
    /** Disputes / completed pickups, 0..1. */
    val disputeRate: Double = 0.0,
    val reportsFiled: Int = 0
)

@Serializable
data class StatsLeaderboards(
    val topGroceries: List<StatsGroceryLeader> = emptyList(),
    val topNgos: List<StatsNgoLeader> = emptyList()
)

@Serializable
data class StatsGroceryLeader(
    val id: String,
    val name: String = "",
    val completedPickups: Int = 0
)

@Serializable
data class StatsNgoLeader(
    val id: String,
    val name: String = "",
    val completedPickups: Int = 0
)

/** Admin accounts are excluded, so groceries + ngos always add up to total. */
@Serializable
data class StatsOrganizations(
    val total: Int = 0,
    val groceries: Int = 0,
    val ngos: Int = 0,
    val verified: Int = 0,
    val pendingVerification: Int = 0,
    val oldestPendingDays: Int? = null
)

// ── Alert feed ───────────────────────────────────────────────────────────────

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

// ── NGO reputation ───────────────────────────────────────────────────────────

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

// ── Today summary ────────────────────────────────────────────────────────────
// TodaySummaryResponse and TodaySummaryData are defined in OrganizationDto.kt

// ── Report ───────────────────────────────────────────────────────────────────

@Serializable
data class SubmitReportRequest(
    val listingId: String,
    val reason: String,
    val details: String? = null
)
