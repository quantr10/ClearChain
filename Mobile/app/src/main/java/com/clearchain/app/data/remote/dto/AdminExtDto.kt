package com.clearchain.app.data.remote.dto

import kotlinx.serialization.Serializable

// ── Admin statistics ─────────────────────────────────────────────────────────
// Mirrors AdminStatisticsResponse on the API, always asked for over the platform's whole
// history. The organization register describes the platform right now.

@Serializable
data class AdminDetailedStatsResponse(
    val data: AdminDetailedStatsData? = null
)

@Serializable
data class AdminDetailedStatsData(
    val headline: StatsHeadline = StatsHeadline(),
    val funnel: StatsFunnel = StatsFunnel(),
    val timing: StatsTiming = StatsTiming(),
    val quality: StatsQuality = StatsQuality(),
    val leaderboards: StatsLeaderboards = StatsLeaderboards(),
    val organizations: StatsOrganizations = StatsOrganizations()
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
    val disputesOpened: Int = 0,
    /** Disputes / completed pickups, 0..1. */
    val disputeRate: Double = 0.0,
    /** Disputes opened in the period by reason key, see NgoDisputeReason; most common first. */
    val disputeReasons: List<StatsCount> = emptyList(),
    val disputeOutcomes: StatsDisputeOutcomes = StatsDisputeOutcomes(),
    /** Over the disputes resolved in the period, whenever they were opened. */
    val medianHoursToResolve: Double? = null,
    val disputesResolved: Int = 0
)

@Serializable
data class StatsCount(
    val key: String,
    val count: Int = 0
)

@Serializable
data class StatsDisputeOutcomes(
    val open: Int = 0,
    val resolvedForNgo: Int = 0,
    val resolvedForGrocery: Int = 0,
    val dismissed: Int = 0
)

@Serializable
data class StatsLeaderboards(
    val groceries: StatsLeaderboard = StatsLeaderboard(),
    val ngos: StatsLeaderboard = StatsLeaderboard(),
    /** Ratings an organization needs in the period before its average is ranked. */
    val minRatingsToRank: Int = 3
)

@Serializable
data class StatsLeaderboard(
    val byPickups: List<StatsLeader> = emptyList(),
    val byRating: List<StatsLeader> = emptyList()
)

@Serializable
data class StatsLeader(
    /** Equal scores share a place and the next is skipped (1, 2, 2, 4). */
    val rank: Int,
    val id: String,
    val name: String = "",
    val profilePictureUrl: String? = null,
    val completedPickups: Int = 0,
    val averageRating: Double? = null,
    val ratingCount: Int = 0
)

/** Admin accounts are excluded, so groceries + ngos always add up to total. */
@Serializable
data class StatsOrganizations(
    val total: Int = 0,
    val groceries: Int = 0,
    val ngos: Int = 0,
    val verified: Int = 0,
    val pendingVerification: Int = 0,
    /** [pendingVerification] split by type. */
    val pendingGroceries: Int = 0,
    val pendingNgos: Int = 0,
    val oldestPendingDays: Int? = null
)

// ── Alert feed ───────────────────────────────────────────────────────────────

@Serializable
data class AdminAlertFeedResponse(
    val data: List<AdminAlertItem> = emptyList(),
    /** Every open dispute, across all pages. */
    val total: Int = 0,
    val totalPages: Int = 1
)

@Serializable
data class AdminAlertItem(
    val type: String, // "dispute"
    val severity: String, // "high" | "medium"
    val id: String,
    val title: String,
    val reason: String? = null, // dispute reason key, see NgoDisputeReason
    val pickupRequestId: String? = null,
    val body: String,
    val status: String,
    val createdAt: String
)
