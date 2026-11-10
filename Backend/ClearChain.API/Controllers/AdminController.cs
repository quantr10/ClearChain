using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Enums;
using ClearChain.API.DTOs.Admin;
using ClearChain.API.DTOs.Disputes;
using ClearChain.API.DTOs.PickupRequests;
using ClearChain.API.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Globalization;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = "admin")]
public class AdminController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly IPushNotificationService _pushNotificationService;

    public AdminController(
        ApplicationDbContext context,
        IPushNotificationService pushNotificationService)
    {
        _context = context;
        _pushNotificationService = pushNotificationService;
    }

    [HttpGet("organizations")]
    public async Task<ActionResult<OrganizationListResponse>> GetAllOrganizations(
        [FromQuery] string? type = null,
        [FromQuery] bool? verified = null)
    {
        var query = _context.Organizations.Where(o => !o.IsDeleted);

        if (!string.IsNullOrEmpty(type))
        {
            query = query.Where(o => o.Type.ToLower() == type.ToLower());
        }

        if (verified.HasValue)
        {
            query = query.Where(o => o.Verified == verified.Value);
        }

        var organizations = await query
            .OrderByDescending(o => o.CreatedAt)
            .ToListAsync();

        var orgData = organizations.Select(o => new OrganizationData
        {
            Id = o.Id.ToString(),
            Name = o.Name,
            Email = o.Email,
            Type = o.Type,
            Phone = o.Phone ?? "",
            Address = o.Address ?? "",
            Location = o.Location ?? "",
            Verified = o.Verified,
            VerificationStatus = o.VerificationStatus ?? "pending",
            CreatedAt = o.CreatedAt.ToString("o"),
            DocumentUrl = o.DocumentUrl,
            DocumentMimeType = o.DocumentMimeType,
            Latitude = o.Latitude,
            Longitude = o.Longitude
        }).ToList();

        return Ok(new OrganizationListResponse
        {
            Message = "Organizations retrieved successfully",
            Data = orgData
        });
    }

    /// <summary>Approves an organization.</summary>
    [HttpPut("organizations/{id}/verify")]
    public async Task<ActionResult<OrganizationResponse>> VerifyOrganization(Guid id)
        => await SetVerification(id, approved: true, reason: null);

    /// <summary>Rejects an organization, with an optional reason.</summary>
    [HttpPut("organizations/{id}/unverify")]
    public async Task<ActionResult<OrganizationResponse>> UnverifyOrganization(
        Guid id, [FromBody] RejectOrganizationRequest? body = null)
        => await SetVerification(id, approved: false, reason: body?.Reason);

    private async Task<ActionResult<OrganizationResponse>> SetVerification(Guid id, bool approved, string? reason)
    {
        var organization = await _context.Organizations.FindAsync(id);
        if (organization == null)
            return NotFound(new { message = "Organization not found" });

        organization.Verified = approved;
        organization.VerificationStatus = approved ? "approved" : "rejected";
        organization.VerificationNotes = approved ? null : reason;
        organization.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        if (approved)
            await _pushNotificationService.SendVerificationApprovedNotification(organization.Id, organization.Type);
        else
            await _pushNotificationService.SendVerificationRejectedNotification(organization.Id, reason);

        var orgData = new OrganizationData
        {
            Id = organization.Id.ToString(),
            Name = organization.Name,
            Email = organization.Email,
            Type = organization.Type,
            Phone = organization.Phone ?? "",
            Address = organization.Address ?? "",
            Location = organization.Location ?? "",
            Verified = organization.Verified,
            VerificationStatus = organization.VerificationStatus,
            CreatedAt = organization.CreatedAt.ToString("o"),
            DocumentUrl = organization.DocumentUrl,
            DocumentMimeType = organization.DocumentMimeType,
            Latitude = organization.Latitude,
            Longitude = organization.Longitude
        };

        return Ok(new OrganizationResponse
        {
            Message = approved ? "Organization approved successfully" : "Organization rejected",
            Data = orgData
        });
    }

    [HttpGet("statistics/overview")]
    public async Task<ActionResult<AdminStatsOverviewResponse>> GetStatisticsOverview()
    {
        // Admin accounts live in the same table but are not organizations on the
        // platform, so they stay out of every count the dashboard charts.
        var orgs = _context.Organizations.Where(o => !o.IsDeleted && o.Type.ToLower() != "admin");

        var totalOrgs = await orgs.CountAsync();
        var totalGroceries = await orgs.CountAsync(o => o.Type.ToLower() == "grocery");
        var totalNgos = await orgs.CountAsync(o => o.Type.ToLower() == "ngo");
        var pendingGroceries = await orgs.CountAsync(o => o.Type.ToLower() == "grocery" && o.VerificationStatus == "pending");
        var pendingNgos = await orgs.CountAsync(o => o.Type.ToLower() == "ngo" && o.VerificationStatus == "pending");

        var activeListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Open);
        var reservedListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Reserved);
        var expiredListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Expired);

        var totalRequests = await _context.PickupRequests.CountAsync();
        var pendingRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == PickupRequestStatus.Pending);
        var approvedRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == PickupRequestStatus.Approved);
        var readyRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == PickupRequestStatus.Ready);
        var completedRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == PickupRequestStatus.Completed);
        var cancelledRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == PickupRequestStatus.Cancelled);

        var stats = new AdminStatsOverviewData
        {
            TotalOrganizations = totalOrgs,
            TotalGroceries = totalGroceries,
            TotalNgos = totalNgos,
            PendingGroceries = pendingGroceries,
            PendingNgos = pendingNgos,

            ActiveListings = activeListings,
            ReservedListings = reservedListings,
            ExpiredListings = expiredListings,

            TotalPickupRequests = totalRequests,
            PendingRequests = pendingRequests,
            ApprovedRequests = approvedRequests,
            ReadyRequests = readyRequests,
            CompletedRequests = completedRequests,
            CancelledRequests = cancelledRequests
        };

        return Ok(new AdminStatsOverviewResponse
        {
            Message = "Statistics retrieved successfully",
            Data = stats
        });
    }

    [HttpGet("pickuprequests")]
    public async Task<ActionResult<PickupRequestsResponse>> GetAllPickupRequests()
    {
        var pickupRequests = await _context.PickupRequests
            .Include(pr => pr.Ngo)
            .Include(pr => pr.Grocery)
            .Include(pr => pr.Items)
            .OrderByDescending(pr => pr.RequestedAt)
            .AsNoTracking()
            .ToListAsync();

        // Requests carry a denormalized listing snapshot (and, for cart requests, their
        // line items). Only legacy rows written before those fields existed need a lookup.
        var missingSnapshotIds = pickupRequests
            .Where(pr => pr.ListingId.HasValue && string.IsNullOrWhiteSpace(pr.ListingTitle))
            .Select(pr => pr.ListingId!.Value)
            .Distinct()
            .ToList();

        var listingsById = await _context.ClearanceListings
            .Where(l => missingSnapshotIds.Contains(l.Id))
            .AsNoTracking()
            .ToDictionaryAsync(l => l.Id);

        // A pickup can only be disputed once (see DisputesController); taking the newest still
        // gives a sensible answer should older data hold more than one.
        var disputes = await _context.Disputes
            .AsNoTracking()
            .Select(d => new { d.PickupRequestId, d.Status, d.CreatedAt })
            .ToListAsync();
        var disputeStatusByRequest = disputes
            .GroupBy(d => d.PickupRequestId)
            .ToDictionary(g => g.Key, g => g.OrderByDescending(d => d.CreatedAt).First().Status);

        var responseData = new List<PickupRequestData>();

        foreach (var pr in pickupRequests)
        {
            var listing = pr.ListingId.HasValue && listingsById.TryGetValue(pr.ListingId.Value, out var l)
                ? l
                : null;
            var firstItem = pr.Items.FirstOrDefault();

            responseData.Add(new PickupRequestData
            {
                Id = pr.Id.ToString(),
                ListingId = pr.ListingId?.ToString() ?? "",
                NgoId = pr.NgoId.ToString(),
                NgoName = pr.Ngo?.Name ?? "",
                GroceryId = pr.GroceryId.ToString(),
                GroceryName = pr.Grocery?.Name ?? "",
                Status = pr.Status.ToString().ToLower(),
                RequestedQuantity = pr.RequestedQuantity ?? 0,
                PickupDate = pr.PickupDate.ToString("yyyy-MM-dd"),
                PickupTime = pr.PickupTime ?? "09:00",
                Notes = pr.Notes ?? "",
                ListingTitle = !string.IsNullOrWhiteSpace(pr.ListingTitle)
                    ? pr.ListingTitle
                    : listing?.ProductName ?? firstItem?.ListingTitle ?? "Unknown Item",
                ListingCategory = !string.IsNullOrWhiteSpace(pr.ListingCategory)
                    ? pr.ListingCategory
                    : listing?.Category ?? firstItem?.ListingCategory ?? "OTHER",
                ListingExpiryDate = pr.ListingExpiryDate,
                ListingUnit = pr.ListingUnit,
                CreatedAt = pr.RequestedAt.ToString("o"),
                MarkedPickedUpAt = pr.MarkedPickedUpAt?.ToString("o"),
                ConfirmedReceivedAt = pr.ConfirmedReceivedAt?.ToString("o"),
                ProofPhotoUrl = pr.ProofPhotoUrl,
                RequiresRefrigeration = pr.RequiresRefrigeration,
                IsFragile = pr.IsFragile,
                IsHeavy = pr.IsHeavy,
                DisputeStatus = disputeStatusByRequest.GetValueOrDefault(pr.Id),
                Items = pr.Items.Select(PickupRequestItemData.From).ToList()
            });
        }

        return Ok(new PickupRequestsResponse
        {
            Message = "Pickup requests retrieved successfully",
            Data = responseData
        });
    }

    /// <summary>
    /// Operational analytics for the admin Stats &amp; Analytics screen.
    ///
    /// Two clocks run here and keeping them apart is what makes the numbers mean
    /// something:
    ///   * the funnel is a cohort - requests *created* in the period, and where each
    ///     one stands today. Only that way do the stages add up to the requests.
    ///   * timings and the leaderboards are events - pickups *completed* in the
    ///     period, whenever they happened to be requested.
    /// The organization register is neither: it is the platform as it stands right now.
    ///
    /// <paramref name="compareFrom"/> and <paramref name="compareTo"/> name a period to set
    /// the trust figures against, usually the one before.
    /// </summary>
    [HttpGet("statistics")]
    public async Task<ActionResult<AdminStatisticsResponse>> GetStatistics(
        [FromQuery] string? from = null,
        [FromQuery] string? to = null,
        [FromQuery] string preset = "all",
        [FromQuery] string? compareFrom = null,
        [FromQuery] string? compareTo = null)
    {
        var now = DateTime.UtcNow;
        var (start, end) = ResolveRange(from, to, preset);

        bool InPeriod(DateTime at) => (!start.HasValue || at >= start.Value) && (!end.HasValue || at <= end.Value);

        // ── Requests ─────────────────────────────────────────────────────────
        // A request completed inside the range may have been raised long before it, so
        // the lower bound has to admit either timestamp. The upper bound can stay on
        // RequestedAt alone: nothing is completed before it was requested.
        var requestRows = await _context.PickupRequests
            .Where(pr => !start.HasValue
                      || pr.RequestedAt >= start.Value
                      || (pr.ConfirmedReceivedAt.HasValue && pr.ConfirmedReceivedAt.Value >= start.Value)
                      || (pr.MarkedPickedUpAt.HasValue && pr.MarkedPickedUpAt.Value >= start.Value))
            .Where(pr => !end.HasValue || pr.RequestedAt <= end.Value)
            .Select(pr => new RequestRow(
                pr.Id, pr.Status, pr.RequestedAt, pr.MarkedReadyAt,
                pr.MarkedPickedUpAt, pr.ConfirmedReceivedAt, pr.GroceryId, pr.NgoId))
            .AsNoTracking()
            .ToListAsync();

        // Cohort: raised in the period. This is what the funnel reports.
        var raised = requestRows.Where(r => InPeriod(r.RequestedAt)).ToList();

        // Events: handed over in the period. This is what the timings and boards report.
        var completed = requestRows
            .Where(r => r.Status == PickupRequestStatus.Completed && InPeriod(r.CompletedAt))
            .ToList();
        var completedIds = completed.Select(r => r.Id).ToList();

        int CountRaised(PickupRequestStatus s) => raised.Count(r => r.Status == s);

        // ── Timing, over the pickups completed in the period ─────────────────
        List<double> Hours(Func<RequestRow, TimeSpan?> leg) => completed
            .Select(leg)
            .Where(d => d.HasValue && d.Value.TotalHours >= 0)
            .Select(d => d!.Value.TotalHours)
            .OrderBy(h => h)
            .ToList();

        var toReady = Hours(r => r.MarkedReadyAt - r.RequestedAt);
        var toPickup = Hours(r => (r.MarkedPickedUpAt ?? r.ConfirmedReceivedAt) - r.RequestedAt);
        var toConfirm = Hours(r => r.ConfirmedReceivedAt - r.MarkedPickedUpAt);

        var timing = new StatsTimingDto
        {
            MedianHoursToReady = Round1(Percentile(toReady, 0.5)),
            MedianHoursToPickup = Round1(Percentile(toPickup, 0.5)),
            MedianHoursToConfirm = Round1(Percentile(toConfirm, 0.5)),
            P90HoursToPickup = Round1(Percentile(toPickup, 0.9)),
            CompletedWithin24hRate = toPickup.Count == 0
                ? 0
                : Math.Round((double)toPickup.Count(h => h <= 24) / toPickup.Count, 3),
            SampleSize = toPickup.Count
        };

        // ── Reviews left in the period, for the rating boards ────────────────
        var reviews = await _context.Reviews
            .AsNoTracking()
            .Where(r => (!start.HasValue || r.CreatedAt >= start.Value)
                     && (!end.HasValue || r.CreatedAt <= end.Value))
            .Select(r => new ReviewRow(r.ReviewedId, r.Rating))
            .ToListAsync();

        // Everyone ranked or rated, deleted accounts included: their pickups happened, so
        // they still count, under the name they had.
        var profileIds = completed.SelectMany(r => new[] { r.GroceryId, r.NgoId })
            .Concat(reviews.Select(r => r.ReviewedId))
            .Distinct()
            .ToList();
        var profiles = await _context.Organizations
            .Where(o => profileIds.Contains(o.Id))
            .Select(o => new OrgProfile(o.Id, o.Name, o.Type, o.ProfilePictureUrl))
            .AsNoTracking()
            .ToDictionaryAsync(o => o.Id);

        // ── The organization register ────────────────────────────────────────
        // Admin accounts sit in the same table but are staff, not participants: leaving
        // them in would mean groceries + NGOs never add up to the total.
        var orgs = await _context.Organizations
            .Where(o => !o.IsDeleted && o.Type.ToLower() != "admin")
            .Select(o => new { o.Id, o.Type, o.Verified, o.VerificationStatus, o.CreatedAt })
            .AsNoTracking()
            .ToListAsync();

        var pendingOrgs = orgs.Where(o => o.VerificationStatus == "pending").ToList();
        var oldestPendingDays = pendingOrgs.Count == 0
            ? (int?)null
            : (int)Math.Floor((now - pendingOrgs.Min(o => o.CreatedAt)).TotalDays);

        var data = new AdminStatisticsData
        {
            Period = new StatsPeriodDto
            {
                From = start?.ToString("o"),
                To = end?.ToString("o"),
                Preset = preset,
                Days = start.HasValue ? Math.Max(1, (int)Math.Ceiling(((end ?? now) - start.Value).TotalDays)) : 0,
                IsAllTime = !start.HasValue
            },
            Headline = new StatsHeadlineDto
            {
                CompletedPickups = completed.Count
            },
            Funnel = new StatsFunnelDto
            {
                Requests = raised.Count,
                Pending = CountRaised(PickupRequestStatus.Pending),
                Approved = CountRaised(PickupRequestStatus.Approved),
                Ready = CountRaised(PickupRequestStatus.Ready),
                Completed = CountRaised(PickupRequestStatus.Completed),
                Cancelled = CountRaised(PickupRequestStatus.Cancelled),
                Rejected = CountRaised(PickupRequestStatus.Rejected)
            },
            Timing = timing,
            Quality = await BuildQuality(completedIds, start, end),
            Leaderboards = BuildLeaderboards(completed, reviews, profiles),
            Organizations = new StatsOrganizationsDto
            {
                Total = orgs.Count,
                Groceries = orgs.Count(o => o.Type.ToLower() == "grocery"),
                Ngos = orgs.Count(o => o.Type.ToLower() == "ngo"),
                Verified = orgs.Count(o => o.Verified),
                PendingVerification = pendingOrgs.Count,
                PendingGroceries = pendingOrgs.Count(o => o.Type.ToLower() == "grocery"),
                PendingNgos = pendingOrgs.Count(o => o.Type.ToLower() == "ngo"),
                OldestPendingDays = oldestPendingDays
            },
            Comparison = await BuildComparison(compareFrom, compareTo, start, end, now)
        };

        return Ok(new AdminStatisticsResponse { Data = data });
    }

    /// <summary>
    /// The UTC days on which something the period-scoped statistics count happened: a
    /// request raised or handed over, a review written, a dispute opened. The app's
    /// range picker only offers these days, so a range never starts or ends on a blank.
    /// </summary>
    [HttpGet("statistics/active-dates")]
    public async Task<ActionResult<StatsActiveDatesResponse>> GetStatisticsActiveDates()
    {
        var requests = await _context.PickupRequests
            .AsNoTracking()
            .Select(pr => new { pr.RequestedAt, CompletedAt = pr.Status == PickupRequestStatus.Completed
                ? pr.ConfirmedReceivedAt ?? pr.MarkedPickedUpAt
                : null })
            .ToListAsync();
        var reviews = await _context.Reviews.AsNoTracking().Select(r => r.CreatedAt).ToListAsync();
        var disputes = await _context.Disputes.AsNoTracking().Select(d => d.CreatedAt).ToListAsync();

        var dates = requests.Select(r => r.RequestedAt)
            .Concat(requests.Where(r => r.CompletedAt.HasValue).Select(r => r.CompletedAt!.Value))
            .Concat(reviews)
            .Concat(disputes)
            .Select(at => at.ToString("yyyy-MM-dd"))
            .Distinct()
            .OrderBy(d => d)
            .ToList();

        return Ok(new StatsActiveDatesResponse { Data = dates });
    }

    // ── Statistics helpers ───────────────────────────────────────────────────

    private sealed record RequestRow(
        Guid Id,
        PickupRequestStatus Status,
        DateTime RequestedAt,
        DateTime? MarkedReadyAt,
        DateTime? MarkedPickedUpAt,
        DateTime? ConfirmedReceivedAt,
        Guid GroceryId,
        Guid NgoId)
    {
        /// <summary>
        /// When the hand-over actually happened, falling back down the chain of timestamps an
        /// older record may be missing.
        /// </summary>
        public DateTime CompletedAt => ConfirmedReceivedAt ?? MarkedPickedUpAt ?? RequestedAt;
    }

    /// <summary>Linear-interpolated percentile over an already sorted list.</summary>
    private static double? Percentile(List<double> sorted, double p)
    {
        if (sorted.Count == 0) return null;
        if (sorted.Count == 1) return sorted[0];

        var rank = p * (sorted.Count - 1);
        var low = (int)Math.Floor(rank);
        var high = (int)Math.Ceiling(rank);
        return sorted[low] + (sorted[high] - sorted[low]) * (rank - low);
    }

    private static double? Round1(double? value) => value.HasValue ? Math.Round(value.Value, 1) : null;

    private sealed record ReviewRow(Guid ReviewedId, int Rating);

    private sealed record OrgProfile(Guid Id, string Name, string Type, string? ProfilePictureUrl);

    private sealed record LeaderStats(
        Guid Id,
        int Pickups,
        double? Rating,
        int RatingCount);

    private const int LeaderboardSize = 10;
    private const int MinRatingsToRank = 3;

    /// <summary>The top stores and NGOs by pickups completed and by rating received.</summary>
    private static StatsLeaderboardsDto BuildLeaderboards(
        List<RequestRow> completed,
        List<ReviewRow> reviews,
        IReadOnlyDictionary<Guid, OrgProfile> profiles)
    {
        var ratingsByOrg = reviews.ToLookup(r => r.ReviewedId, r => r.Rating);

        StatsLeaderboardDto Board(string type, Func<RequestRow, Guid> party)
        {
            var completedBy = completed.ToLookup(party);

            // Everyone with a pickup to their name, and everyone of this type rated often enough to rank.
            var pool = completedBy.Select(g => g.Key)
                .Concat(ratingsByOrg
                    .Where(g => g.Count() >= MinRatingsToRank
                             && profiles.TryGetValue(g.Key, out var p)
                             && string.Equals(p.Type, type, StringComparison.OrdinalIgnoreCase))
                    .Select(g => g.Key))
                .Distinct()
                .Select(id =>
                {
                    var ratings = ratingsByOrg[id].ToList();
                    return new LeaderStats(
                        Id: id,
                        Pickups: completedBy[id].Count(),
                        Rating: ratings.Count == 0 ? null : ratings.Average(),
                        RatingCount: ratings.Count);
                })
                .ToList();

            // Scores are compared as they are shown, so two stores both showing 4.7 share a place.
            return new StatsLeaderboardDto
            {
                ByPickups = Rank(pool, s => s.Pickups, s => s.Rating ?? 0, profiles),
                ByRating = Rank(
                    pool.Where(s => s.RatingCount >= MinRatingsToRank),
                    s => Math.Round(s.Rating ?? 0, 1),
                    s => s.RatingCount,
                    profiles)
            };
        }

        return new StatsLeaderboardsDto
        {
            Groceries = Board("grocery", r => r.GroceryId),
            Ngos = Board("ngo", r => r.NgoId),
            MinRatingsToRank = MinRatingsToRank
        };
    }

    /// <summary>
    /// The top of the pool by score, best first. Equal scores share a place and the places
    /// they fill are skipped (1, 2, 2, 4); the tie-break and then the name only order them
    /// within it. Nobody scoring zero is ranked.
    /// </summary>
    private static List<StatsLeaderDto> Rank(
        IEnumerable<LeaderStats> pool,
        Func<LeaderStats, double> score,
        Func<LeaderStats, double> tieBreak,
        IReadOnlyDictionary<Guid, OrgProfile> profiles)
    {
        var top = pool
            .Where(s => score(s) > 0)
            .OrderByDescending(score)
            .ThenByDescending(tieBreak)
            .ThenBy(s => profiles.GetValueOrDefault(s.Id)?.Name, StringComparer.OrdinalIgnoreCase)
            .Take(LeaderboardSize)
            .ToList();

        var ranked = new List<StatsLeaderDto>(top.Count);
        for (var i = 0; i < top.Count; i++)
        {
            var s = top[i];
            var profile = profiles.GetValueOrDefault(s.Id);
            ranked.Add(new StatsLeaderDto
            {
                Rank = i > 0 && score(s) == score(top[i - 1]) ? ranked[i - 1].Rank : i + 1,
                Id = s.Id.ToString(),
                Name = profile?.Name ?? "",
                ProfilePictureUrl = profile?.ProfilePictureUrl,
                CompletedPickups = s.Pickups,
                AverageRating = s.Rating.HasValue ? Math.Round(s.Rating.Value, 2) : null,
                RatingCount = s.RatingCount
            });
        }
        return ranked;
    }

    /// <summary>Disputes over the period, split the ways an admin acts on them.</summary>
    private async Task<StatsQualityDto> BuildQuality(List<Guid> completedIds, DateTime? start, DateTime? end)
    {
        var disputes = await _context.Disputes
            .AsNoTracking()
            .Where(d => (!start.HasValue || d.CreatedAt >= start.Value)
                     && (!end.HasValue || d.CreatedAt <= end.Value))
            .Select(d => new { d.Reason, d.Status })
            .ToListAsync();

        // An event clock, like the timings: disputes closed in the period, whenever opened.
        var resolved = await _context.Disputes
            .AsNoTracking()
            .Where(d => d.ResolvedAt.HasValue
                     && (!start.HasValue || d.ResolvedAt.Value >= start.Value)
                     && (!end.HasValue || d.ResolvedAt.Value <= end.Value))
            .Select(d => new { d.CreatedAt, ResolvedAt = d.ResolvedAt!.Value })
            .ToListAsync();
        var hoursToResolve = resolved
            .Select(d => (d.ResolvedAt - d.CreatedAt).TotalHours)
            .Where(h => h >= 0)
            .OrderBy(h => h)
            .ToList();

        var completedPickups = completedIds.Count;
        return new StatsQualityDto
        {
            DisputesOpened = disputes.Count,
            DisputeRate = completedPickups == 0 ? 0 : Math.Round((double)disputes.Count / completedPickups, 3),
            DisputeReasons = disputes
                .GroupBy(d => d.Reason)
                .Select(g => new StatsCountDto { Key = g.Key, Count = g.Count() })
                .OrderByDescending(c => c.Count)
                .ThenBy(c => c.Key, StringComparer.Ordinal)
                .ToList(),
            DisputeOutcomes = new StatsDisputeOutcomesDto
            {
                Open = disputes.Count(d => d.Status == "open"),
                ResolvedForNgo = disputes.Count(d => d.Status == "resolved_ngo"),
                ResolvedForGrocery = disputes.Count(d => d.Status == "resolved_grocery"),
                Dismissed = disputes.Count(d => d.Status == "dismissed")
            },
            MedianHoursToResolve = Round1(Percentile(hoursToResolve, 0.5)),
            DisputesResolved = hoursToResolve.Count
        };
    }

    /// <summary>
    /// The dispute count over the comparison range, counted the way GetStatistics counts
    /// its own. A period still running is set against the same stretch of the other one -
    /// today so far against yesterday up to this hour - or it would always look like a slump.
    /// </summary>
    private async Task<StatsComparisonDto?> BuildComparison(
        string? compareFrom, string? compareTo, DateTime? start, DateTime? end, DateTime now)
    {
        if (!start.HasValue) return null;
        var (compareStart, compareEnd) = ResolveRange(compareFrom, compareTo, preset: "custom");
        if (!compareStart.HasValue || !compareEnd.HasValue) return null;

        var s = compareStart.Value;
        var e = compareEnd.Value;
        if (end.HasValue && end.Value > now)
        {
            var sameStretch = s + (now - start.Value);
            if (sameStretch < e) e = sameStretch;
        }

        return new StatsComparisonDto
        {
            From = s.ToString("o"),
            To = e.ToString("o"),
            DisputesOpened = await _context.Disputes.CountAsync(d => d.CreatedAt >= s && d.CreatedAt <= e)
        };
    }

    // ── Alert feed ───────────────────────────────────────────────────────────

    /// <summary>
    /// The disputes still waiting on the admin (status open), newest first, a page at a time.
    /// Total counts every open dispute, so the dashboard's badge is right whichever page it read.
    /// </summary>
    [HttpGet("alerts")]
    public async Task<IActionResult> GetAlertFeed(
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 20)
    {
        var clampedPage = Math.Max(1, page);
        var clampedSize = Math.Clamp(pageSize, 1, 100);

        var open = _context.Disputes.Where(d => d.Status == "open");
        var total = await open.CountAsync();

        // Id breaks ties on CreatedAt, so two pages never share or both skip a dispute.
        var disputes = await open
            .OrderByDescending(d => d.CreatedAt)
            .ThenBy(d => d.Id)
            .Skip((clampedPage - 1) * clampedSize)
            .Take(clampedSize)
            .ToListAsync();

        // Reason is stored as a key; the label is what an admin should read.
        var feed = disputes.Select(d => new
        {
            type = "dispute",
            severity = "high",
            id = d.Id.ToString(),
            title = $"Dispute: {DisputeReasons.Label(d.Reason)}",
            // The key lets the app show the reason in its own language; the request id is where
            // the admin resolves the dispute.
            reason = d.Reason,
            pickupRequestId = d.PickupRequestId.ToString(),
            body = d.NgoStatement ?? "",
            status = d.Status,
            createdAt = d.CreatedAt.ToString("o")
        }).ToList();

        return Ok(new
        {
            data = feed,
            total,
            page = clampedPage,
            pageSize = clampedSize,
            totalPages = (int)Math.Ceiling((double)total / clampedSize)
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static (DateTime? start, DateTime? end) ResolveRange(string? from, string? to, string preset)
    {
        // A bare "yyyy-MM-dd" is a UTC day, like every preset below; parsing it as the
        // server's local time would shift the range on any host not running in UTC.
        const DateTimeStyles utc = DateTimeStyles.AssumeUniversal | DateTimeStyles.AdjustToUniversal;
        if (!string.IsNullOrEmpty(from) && DateTime.TryParse(from, CultureInfo.InvariantCulture, utc, out var f) &&
            !string.IsNullOrEmpty(to) && DateTime.TryParse(to, CultureInfo.InvariantCulture, utc, out var t))
        {
            return (f, t.AddDays(1).AddSeconds(-1));
        }

        var now = DateTime.UtcNow;
        var weekStart = now.Date.AddDays(-(int)now.DayOfWeek);
        var monthStart = new DateTime(now.Year, now.Month, 1, 0, 0, 0, DateTimeKind.Utc);

        return preset switch
        {
            "today" => (now.Date, now.Date.AddDays(1).AddSeconds(-1)),
            "week" => (weekStart, now),
            "month" => (monthStart, now),
            "quarter" => (new DateTime(now.Year, (now.Month - 1) / 3 * 3 + 1, 1, 0, 0, 0, DateTimeKind.Utc), now),
            // The matching previous period, so a comparison compares like with like.
            "yesterday" => (now.Date.AddDays(-1), now.Date.AddSeconds(-1)),
            "last_week" => (weekStart.AddDays(-7), weekStart.AddSeconds(-1)),
            "last_month" => (monthStart.AddMonths(-1), monthStart.AddSeconds(-1)),
            _ => (null, null)  // "all"
        };
    }
}

public class RejectOrganizationRequest
{
    public string? Reason { get; set; }
}
