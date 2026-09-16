namespace ClearChain.API.DTOs.Admin;

// ═════════════════════════════════════════════════════════════════════════════
// Payload for the admin Stats & Analytics screen.
//
// Two kinds of number live here and they must not be mixed up:
//   * period figures - counted over the requested date range (Headline, Funnel,
//                      Leaderboards, Timing, Quality).
//   * live figures   - the state of the platform right now, independent of the
//                      range (Backlog, and the Organizations register totals).
// Everything here is measured from the database; nothing is modelled or estimated.
// ═════════════════════════════════════════════════════════════════════════════

public class AdminStatisticsResponse
{
    public AdminStatisticsData Data { get; set; } = new();
}

public class AdminStatisticsData
{
    public StatsPeriodDto Period { get; set; } = new();
    public StatsHeadlineDto Headline { get; set; } = new();
    public StatsFunnelDto Funnel { get; set; } = new();
    public StatsBacklogDto Backlog { get; set; } = new();
    public StatsTimingDto Timing { get; set; } = new();
    public StatsQualityDto Quality { get; set; } = new();
    public StatsLeaderboardsDto Leaderboards { get; set; } = new();
    public StatsOrganizationsDto Organizations { get; set; } = new();
}

public class StatsPeriodDto
{
    public string? From { get; set; }
    public string? To { get; set; }
    public string Preset { get; set; } = "all";
    /// <summary>Days covered by the range; 0 when the range is open-ended ("all").</summary>
    public int Days { get; set; }
    public bool IsAllTime { get; set; }
}

public class StatsHeadlineDto
{
    /// <summary>Pickups handed over in the period, whenever they were requested.</summary>
    public int CompletedPickups { get; set; }
}

public class StatsFunnelDto
{
    public int Requests { get; set; }
    public int Pending { get; set; }
    public int Approved { get; set; }
    public int Ready { get; set; }
    public int Completed { get; set; }
    public int Cancelled { get; set; }
    public int Rejected { get; set; }
}

/// <summary>What is waiting for someone right now. Not scoped to the period.</summary>
public class StatsBacklogDto
{
    public int OpenListings { get; set; }
    public int ReservedListings { get; set; }
    public int ExpiredListings { get; set; }
    public int ArchivedListings { get; set; }
    /// <summary>Open listings whose collection deadline falls inside the next 24 hours.</summary>
    public int ExpiringWithin24h { get; set; }

    public int PendingRequests { get; set; }
    public int ApprovedRequests { get; set; }
    public int ReadyRequests { get; set; }
    /// <summary>Age of the longest-waiting request a grocery has not answered, in hours.</summary>
    public double? OldestPendingRequestHours { get; set; }

    public int PendingVerifications { get; set; }
    public int? OldestPendingVerificationDays { get; set; }

    public int OpenDisputes { get; set; }
    public int PendingReports { get; set; }
}

/// <summary>
/// How long each hand-over leg took, over the pickups completed in the period. Medians
/// rather than means: one request forgotten for a week would drag a mean away from what
/// a typical hand-over actually looks like.
/// </summary>
public class StatsTimingDto
{
    /// <summary>Request placed until the grocery marked it ready.</summary>
    public double? MedianHoursToReady { get; set; }
    /// <summary>Request placed until the food was collected.</summary>
    public double? MedianHoursToPickup { get; set; }
    /// <summary>Collected until the NGO confirmed receipt.</summary>
    public double? MedianHoursToConfirm { get; set; }
    /// <summary>The slow tail: 9 in 10 pickups finished faster than this.</summary>
    public double? P90HoursToPickup { get; set; }
    /// <summary>Share of completed pickups that finished inside a day, 0..1.</summary>
    public double CompletedWithin24hRate { get; set; }
    /// <summary>Completed pickups these timings were computed from.</summary>
    public int SampleSize { get; set; }
}

public class StatsQualityDto
{
    /// <summary>Mean of the 1-5 ratings NGOs left in the period; null when nobody rated.</summary>
    public double? AverageRating { get; set; }
    public int ReviewCount { get; set; }
    /// <summary>Reviews / completed pickups, 0..1. How much of the record is rated at all.</summary>
    public double ReviewCoverage { get; set; }
    public int DisputesOpened { get; set; }
    /// <summary>Disputes / completed pickups, 0..1.</summary>
    public double DisputeRate { get; set; }
    public int ReportsFiled { get; set; }
}

public class StatsLeaderboardsDto
{
    public List<StatsGroceryLeaderDto> TopGroceries { get; set; } = new();
    public List<StatsNgoLeaderDto> TopNgos { get; set; } = new();
}

public class StatsGroceryLeaderDto
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public int CompletedPickups { get; set; }
}

public class StatsNgoLeaderDto
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public int CompletedPickups { get; set; }
}

/// <summary>
/// The organization register. Admin accounts are staff, not participants, so they are
/// excluded everywhere: Groceries + Ngos always add up to Total.
/// </summary>
public class StatsOrganizationsDto
{
    public int Total { get; set; }
    public int Groceries { get; set; }
    public int Ngos { get; set; }
    public int Verified { get; set; }
    public int PendingVerification { get; set; }
    /// <summary>Age of the longest-waiting unverified application, in days.</summary>
    public int? OldestPendingDays { get; set; }
}
