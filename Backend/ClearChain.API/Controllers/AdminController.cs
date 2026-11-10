using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Enums;
using ClearChain.API.Common;
using ClearChain.API.DTOs.Admin;
using ClearChain.API.DTOs.Disputes;
using ClearChain.API.DTOs.PickupRequests;
using ClearChain.API.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

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
    /// Operational analytics for the admin Stats &amp; Analytics screen, over the platform's
    /// whole history. The funnel places every request by where it stands today; the timings
    /// and the leaderboards count the pickups completed. The organization register is the
    /// platform as it stands right now.
    /// </summary>
    [HttpGet("statistics")]
    public async Task<ActionResult<AdminStatisticsResponse>> GetStatistics()
    {
        // ── Requests ─────────────────────────────────────────────────────────
        var requests = await _context.PickupRequests
            .Select(pr => new RequestRow(
                pr.Status, pr.RequestedAt, pr.MarkedReadyAt,
                pr.MarkedPickedUpAt, pr.ConfirmedReceivedAt, pr.GroceryId, pr.NgoId))
            .AsNoTracking()
            .ToListAsync();

        var completed = requests.Where(r => r.Status == PickupRequestStatus.Completed).ToList();

        int CountRequests(PickupRequestStatus s) => requests.Count(r => r.Status == s);

        // ── Timing, over the completed pickups ───────────────────────────────
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

        // ── Ratings received, for the rating boards ──────────────────────────
        var ratings = await _context.Reviews
            .GroupBy(r => r.ReviewedId)
            .Select(g => new RatingRow(g.Key, g.Count(), g.Average(r => (double)r.Rating)))
            .ToDictionaryAsync(r => r.OrgId);

        // ── Organizations ────────────────────────────────────────────────────
        // Every account, deleted ones included: their pickups happened, so they are still
        // ranked, under the name they had.
        var orgs = await _context.Organizations
            .Select(o => new OrgRow(
                o.Id, o.Name, o.Type, o.ProfilePictureUrl,
                o.Verified, o.VerificationStatus, o.CreatedAt, o.IsDeleted))
            .AsNoTracking()
            .ToDictionaryAsync(o => o.Id);

        // The register is the platform as it stands, so it leaves deleted accounts out. Admin
        // accounts are staff, not participants: leaving them in would mean groceries + NGOs
        // never add up to the total.
        var register = orgs.Values
            .Where(o => !o.IsDeleted && !string.Equals(o.Type, "admin", StringComparison.OrdinalIgnoreCase))
            .ToList();

        var pendingOrgs = register.Where(o => o.VerificationStatus == "pending").ToList();
        var oldestPendingDays = pendingOrgs.Count == 0
            ? (int?)null
            : (int)Math.Floor((DateTime.UtcNow - pendingOrgs.Min(o => o.CreatedAt)).TotalDays);

        int CountType(IEnumerable<OrgRow> orgsIn, string type) =>
            orgsIn.Count(o => string.Equals(o.Type, type, StringComparison.OrdinalIgnoreCase));

        var data = new AdminStatisticsData
        {
            Funnel = new StatsFunnelDto
            {
                Requests = requests.Count,
                Pending = CountRequests(PickupRequestStatus.Pending),
                Approved = CountRequests(PickupRequestStatus.Approved),
                Ready = CountRequests(PickupRequestStatus.Ready),
                Completed = CountRequests(PickupRequestStatus.Completed),
                Cancelled = CountRequests(PickupRequestStatus.Cancelled),
                Rejected = CountRequests(PickupRequestStatus.Rejected)
            },
            Timing = timing,
            Quality = await BuildQuality(completed.Count),
            Leaderboards = BuildLeaderboards(completed, ratings, orgs),
            Organizations = new StatsOrganizationsDto
            {
                Total = register.Count,
                Groceries = CountType(register, "grocery"),
                Ngos = CountType(register, "ngo"),
                Verified = register.Count(o => o.Verified),
                PendingVerification = pendingOrgs.Count,
                PendingGroceries = CountType(pendingOrgs, "grocery"),
                PendingNgos = CountType(pendingOrgs, "ngo"),
                OldestPendingDays = oldestPendingDays
            }
        };

        return Ok(new AdminStatisticsResponse { Data = data });
    }

    // ── Statistics helpers ───────────────────────────────────────────────────

    private sealed record RequestRow(
        PickupRequestStatus Status,
        DateTime RequestedAt,
        DateTime? MarkedReadyAt,
        DateTime? MarkedPickedUpAt,
        DateTime? ConfirmedReceivedAt,
        Guid GroceryId,
        Guid NgoId);

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

    private sealed record RatingRow(Guid OrgId, int Count, double Average);

    private sealed record OrgRow(
        Guid Id,
        string Name,
        string Type,
        string? ProfilePictureUrl,
        bool Verified,
        string VerificationStatus,
        DateTime CreatedAt,
        bool IsDeleted);

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
        IReadOnlyDictionary<Guid, RatingRow> ratings,
        IReadOnlyDictionary<Guid, OrgRow> profiles)
    {
        StatsLeaderboardDto Board(string type, Func<RequestRow, Guid> party)
        {
            var completedBy = completed.ToLookup(party);

            // Everyone with a pickup to their name, and everyone of this type rated often enough to rank.
            var pool = completedBy.Select(g => g.Key)
                .Concat(ratings.Values
                    .Where(r => r.Count >= MinRatingsToRank
                             && profiles.TryGetValue(r.OrgId, out var p)
                             && string.Equals(p.Type, type, StringComparison.OrdinalIgnoreCase))
                    .Select(r => r.OrgId))
                .Distinct()
                .Select(id =>
                {
                    var rating = ratings.GetValueOrDefault(id);
                    return new LeaderStats(
                        Id: id,
                        Pickups: completedBy[id].Count(),
                        Rating: rating?.Average,
                        RatingCount: rating?.Count ?? 0);
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
        IReadOnlyDictionary<Guid, OrgRow> profiles)
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

    /// <summary>Disputes, split the ways an admin acts on them.</summary>
    private async Task<StatsQualityDto> BuildQuality(int completedPickups)
    {
        var disputes = await _context.Disputes
            .AsNoTracking()
            .Select(d => new { d.Reason, d.Status, d.CreatedAt, d.ResolvedAt })
            .ToListAsync();

        var hoursToResolve = disputes
            .Where(d => d.ResolvedAt.HasValue)
            .Select(d => (d.ResolvedAt!.Value - d.CreatedAt).TotalHours)
            .Where(h => h >= 0)
            .OrderBy(h => h)
            .ToList();

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
        var paging = PageRequest.Clamp(page, pageSize, maxSize: 100);

        var open = _context.Disputes.Where(d => d.Status == "open");
        var total = await open.CountAsync();

        // Id breaks ties on CreatedAt, so two pages never share or both skip a dispute.
        var disputes = await open
            .OrderByDescending(d => d.CreatedAt)
            .ThenBy(d => d.Id)
            .Skip(paging.SkipWithin(total))
            .Take(paging.Size)
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
            page = paging.Page,
            pageSize = paging.Size,
            totalPages = paging.TotalPages(total)
        });
    }

}

public class RejectOrganizationRequest
{
    public string? Reason { get; set; }
}
