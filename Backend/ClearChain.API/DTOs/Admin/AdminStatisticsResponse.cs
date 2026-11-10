namespace ClearChain.API.DTOs.Admin;

/// <summary>
/// Payload for the admin Stats &amp; Analytics screen.
///
/// Two kinds of number live here and they must not be mixed up:
///   * period figures - counted over the requested date range (Headline, Funnel,
///                      Timing, Leaderboards, Quality).
///   * live figures   - the organization register as it stands right now, independent of
///                      the range.
/// Everything here is measured from the database; nothing is modelled or estimated.
/// </summary>
public class AdminStatisticsResponse
{
    public AdminStatisticsData Data { get; set; } = new();
}

/// <summary>UTC days ("yyyy-MM-dd", ascending) that have any period-scoped activity.</summary>
public class StatsActiveDatesResponse
{
    public List<string> Data { get; set; } = new();
}

public class AdminStatisticsData
{
    public StatsPeriodDto Period { get; set; } = new();
    public StatsHeadlineDto Headline { get; set; } = new();
    public StatsFunnelDto Funnel { get; set; } = new();
    public StatsTimingDto Timing { get; set; } = new();
    public StatsQualityDto Quality { get; set; } = new();
    public StatsLeaderboardsDto Leaderboards { get; set; } = new();
    public StatsOrganizationsDto Organizations { get; set; } = new();
    /// <summary>The dispute figures for the period compared against; null when none was asked for.</summary>
    public StatsComparisonDto? Comparison { get; set; }
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

/// <summary>Disputes opened in the period, why, and how they were settled.</summary>
public class StatsQualityDto
{
    public int DisputesOpened { get; set; }
    /// <summary>Disputes / completed pickups, 0..1.</summary>
    public double DisputeRate { get; set; }
    /// <summary>Disputes opened in the period by reason key (see DisputeReasons), most common first.</summary>
    public List<StatsCountDto> DisputeReasons { get; set; } = new();
    /// <summary>Where the disputes opened in the period stand now.</summary>
    public StatsDisputeOutcomesDto DisputeOutcomes { get; set; } = new();
    /// <summary>
    /// Opened until resolved, over the disputes resolved in the period - an event clock,
    /// like the timings.
    /// </summary>
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

/// <summary>
/// Top organizations of each type, ranked two ways. Pickups count the ones completed in
/// the period; ratings count the ones received in it.
/// </summary>
public class StatsLeaderboardsDto
{
    public StatsLeaderboardDto Groceries { get; set; } = new();
    public StatsLeaderboardDto Ngos { get; set; } = new();
    /// <summary>
    /// Ratings an organization needs in the period before its average is ranked: one 5-star
    /// review would otherwise outrank a store rated 4.8 forty times.
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

/// <summary>
/// The dispute figures over the comparison period, counted the same way as the current
/// ones. When the current period is still running, the comparison is cut to the same
/// length, so this week so far is set against last week up to the same point.
/// </summary>
public class StatsComparisonDto
{
    public string From { get; set; } = string.Empty;
    public string To { get; set; } = string.Empty;
    public int DisputesOpened { get; set; }
}
