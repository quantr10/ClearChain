namespace ClearChain.API.DTOs.Admin;

/// <summary>
/// Payload for the admin Stats &amp; Analytics screen, over the platform's whole history. The
/// organization register is the platform as it stands right now. Everything here is measured
/// from the database; nothing is modelled or estimated.
/// </summary>
public class AdminStatisticsResponse
{
    public AdminStatisticsData Data { get; set; } = new();
}

public class AdminStatisticsData
{
    public StatsFunnelDto Funnel { get; set; } = new();
    public StatsTimingDto Timing { get; set; } = new();
    public StatsQualityDto Quality { get; set; } = new();
    public StatsLeaderboardsDto Leaderboards { get; set; } = new();
    public StatsOrganizationsDto Organizations { get; set; } = new();
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

/// <summary>
/// How long each hand-over leg took, over the completed pickups. Medians rather than means:
/// one request forgotten for a week would drag a mean away from what a typical hand-over
/// actually looks like.
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

/// <summary>Disputes: why they were opened and how they were settled.</summary>
public class StatsQualityDto
{
    public int DisputesOpened { get; set; }
    /// <summary>Disputes / completed pickups, 0..1.</summary>
    public double DisputeRate { get; set; }
    /// <summary>Disputes by reason key (see DisputeReasons), most common first.</summary>
    public List<StatsCountDto> DisputeReasons { get; set; } = new();
    /// <summary>Where each dispute stands now.</summary>
    public StatsDisputeOutcomesDto DisputeOutcomes { get; set; } = new();
    /// <summary>Opened until resolved, over the resolved disputes.</summary>
    public double? MedianHoursToResolve { get; set; }
    public int DisputesResolved { get; set; }
}

public class StatsCountDto
{
    public string Key { get; set; } = string.Empty;
    public int Count { get; set; }
}

public class StatsDisputeOutcomesDto
{
    public int Open { get; set; }
    public int ResolvedForNgo { get; set; }
    public int ResolvedForGrocery { get; set; }
    public int Dismissed { get; set; }
}

/// <summary>Top organizations of each type, ranked by pickups completed and by rating received.</summary>
public class StatsLeaderboardsDto
{
    public StatsLeaderboardDto Groceries { get; set; } = new();
    public StatsLeaderboardDto Ngos { get; set; } = new();
    /// <summary>
    /// Ratings an organization needs before its average is ranked: one 5-star review would
    /// otherwise outrank a store rated 4.8 forty times.
    /// </summary>
    public int MinRatingsToRank { get; set; }
}

public class StatsLeaderboardDto
{
    public List<StatsLeaderDto> ByPickups { get; set; } = new();
    public List<StatsLeaderDto> ByRating { get; set; } = new();
}

public class StatsLeaderDto
{
    /// <summary>Competition ranking: equal scores share a place and the next is skipped (1, 2, 2, 4).</summary>
    public int Rank { get; set; }
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string? ProfilePictureUrl { get; set; }
    public int CompletedPickups { get; set; }
    public double? AverageRating { get; set; }
    public int RatingCount { get; set; }
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
    /// <summary>
    /// The PendingVerification split by type, so a chart can show the queue as its own slice
    /// without counting an applicant under its type as well.
    /// </summary>
    public int PendingGroceries { get; set; }
    public int PendingNgos { get; set; }
    /// <summary>Age of the longest-waiting unverified application, in days.</summary>
    public int? OldestPendingDays { get; set; }
}
