using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Enums;
using ClearChain.API.DTOs.Admin;
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
        // [Authorize(Roles = "admin")] on the controller already guarantees the caller is
        // an admin — this used to re-fetch and re-check the same thing on every action.
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

    // PUT: api/admin/organizations/{id}/verify  — approve an organization
    [HttpPut("organizations/{id}/verify")]
    public async Task<ActionResult<OrganizationResponse>> VerifyOrganization(Guid id)
        => await SetVerification(id, approved: true, reason: null);

    // PUT: api/admin/organizations/{id}/unverify  — reject an organization (optional reason)
    [HttpPut("organizations/{id}/unverify")]
    public async Task<ActionResult<OrganizationResponse>> UnverifyOrganization(
        Guid id, [FromBody] RejectOrganizationRequest? body = null)
        => await SetVerification(id, approved: false, reason: body?.Reason);

    private async Task<ActionResult<OrganizationResponse>> SetVerification(Guid id, bool approved, string? reason)
    {
        // [Authorize(Roles = "admin")] on the controller already guarantees the caller is
        // an admin — this used to re-fetch and re-check the same thing on every action.
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
        // [Authorize(Roles = "admin")] on the controller already guarantees the caller is
        // an admin — this used to re-fetch and re-check the same thing on every action.

        // Admin accounts live in the same table but are not organizations on the
        // platform, so they stay out of every count the dashboard charts.
        var orgs = _context.Organizations.Where(o => !o.IsDeleted && o.Type.ToLower() != "admin");

        var totalOrgs = await orgs.CountAsync();
        var totalGroceries = await orgs.CountAsync(o => o.Type.ToLower() == "grocery");
        var totalNgos = await orgs.CountAsync(o => o.Type.ToLower() == "ngo");
        var verifiedOrgs = await orgs.CountAsync(o => o.Verified);
        var unverifiedOrgs = totalOrgs - verifiedOrgs;

        var totalListings = await _context.ClearanceListings.CountAsync();
        var activeListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Open);
        var reservedListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Reserved);
        var expiredListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Expired);

        var totalRequests = await _context.PickupRequests.CountAsync();
        var pendingRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == PickupRequestStatus.Pending);
        var approvedRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == PickupRequestStatus.Approved);
        var readyRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == PickupRequestStatus.Ready);
        var rejectedRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == PickupRequestStatus.Rejected);
        var completedRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == PickupRequestStatus.Completed);
        var cancelledRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == PickupRequestStatus.Cancelled);

        var totalFoodSaved = await _context.PickupRequests
            .Where(pr => pr.Status == PickupRequestStatus.Completed)
            .SumAsync(pr => pr.RequestedQuantity ?? 0);

        var stats = new AdminStatsOverviewData
        {
            TotalOrganizations = totalOrgs,
            TotalGroceries = totalGroceries,
            TotalNgos = totalNgos,
            VerifiedOrganizations = verifiedOrgs,
            UnverifiedOrganizations = unverifiedOrgs,

            TotalListings = totalListings,
            ActiveListings = activeListings,
            ReservedListings = reservedListings,
            ExpiredListings = expiredListings,

            TotalPickupRequests = totalRequests,
            PendingRequests = pendingRequests,
            ApprovedRequests = approvedRequests,
            ReadyRequests = readyRequests,
            RejectedRequests = rejectedRequests,
            CompletedRequests = completedRequests,
            CancelledRequests = cancelledRequests,

            TotalFoodSaved = totalFoodSaved
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
        // [Authorize(Roles = "admin")] on the controller already guarantees the caller is
        // an admin — this used to re-fetch and re-check the same thing on every action.
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
                MarkedReadyAt = pr.MarkedReadyAt?.ToString("o"),
                MarkedPickedUpAt = pr.MarkedPickedUpAt?.ToString("o"),
                ConfirmedReceivedAt = pr.ConfirmedReceivedAt?.ToString("o"),
                ProofPhotoUrl = pr.ProofPhotoUrl,
                RequiresRefrigeration = pr.RequiresRefrigeration,
                IsFragile = pr.IsFragile,
                IsHeavy = pr.IsHeavy,
                Items = pr.Items.Select(i => new PickupRequestItemData
                {
                    Id = i.Id.ToString(),
                    ListingGroupId = i.ListingGroupId?.ToString(),
                    OriginalListingId = i.OriginalListingId?.ToString(),
                    ReservedListingId = i.ReservedListingId?.ToString(),
                    RequestedQuantity = i.RequestedQuantity,
                    ListingTitle = i.ListingTitle,
                    ListingCategory = i.ListingCategory,
                    ListingExpiryDate = i.ListingExpiryDate,
                    ListingUnit = i.ListingUnit,
                    ListingPhotoUrl = i.ListingPhotoUrl
                }).ToList()
            });
        }

        return Ok(new PickupRequestsResponse
        {
            Message = "Pickup requests retrieved successfully",
            Data = responseData
        });
    }

    // ── GET api/admin/statistics?from=&to=&preset= ───────────────────────────
    // Operational analytics for the admin Stats & Analytics screen.
    //
    // Two clocks run here and keeping them apart is what makes the numbers mean
    // something:
    //   * the funnel is a cohort - requests *created* in the period, and where each
    //     one stands today. Only that way do the stages add up to the requests.
    //   * timings and the leaderboards are events - pickups *completed* in the
    //     period, whenever they happened to be requested.
    // Backlog is neither: it is the queue as it stands right now.
    [HttpGet("statistics")]
    public async Task<ActionResult<AdminStatisticsResponse>> GetStatistics(
        [FromQuery] string? from = null,
        [FromQuery] string? to = null,
        [FromQuery] string preset = "all")
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

        // ── Leaderboards, ranked by the measure the board actually shows ─────
        var groceryStats = completed
            .GroupBy(r => r.GroceryId)
            .Select(g => new { Id = g.Key, Pickups = g.Count() })
            .OrderByDescending(g => g.Pickups)
            .Take(5)
            .ToList();

        var ngoStats = completed
            .GroupBy(r => r.NgoId)
            .Select(g => new { Id = g.Key, Pickups = g.Count() })
            .OrderByDescending(g => g.Pickups)
            .Take(5)
            .ToList();

        var leaderIds = groceryStats.Select(g => g.Id).Concat(ngoStats.Select(n => n.Id)).Distinct().ToList();
        var leaderNames = await _context.Organizations
            .Where(o => leaderIds.Contains(o.Id))
            .Select(o => new { o.Id, o.Name })
            .ToDictionaryAsync(o => o.Id, o => o.Name);

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

        // ── Quality signals raised in the period ─────────────────────────────
        var reviews = await _context.Reviews
            .AsNoTracking()
            .Where(r => (!start.HasValue || r.CreatedAt >= start.Value)
                     && (!end.HasValue || r.CreatedAt <= end.Value))
            .Select(r => r.Rating)
            .ToListAsync();

        var disputesOpened = await _context.Disputes
            .CountAsync(d => (!start.HasValue || d.CreatedAt >= start.Value)
                          && (!end.HasValue || d.CreatedAt <= end.Value));

        var reportsFiled = await _context.Reports
            .CountAsync(r => (!start.HasValue || r.CreatedAt >= start.Value)
                          && (!end.HasValue || r.CreatedAt <= end.Value));

        var quality = new StatsQualityDto
        {
            AverageRating = reviews.Count == 0 ? null : Math.Round(reviews.Average(), 2),
            ReviewCount = reviews.Count,
            ReviewCoverage = completed.Count == 0 ? 0 : Math.Round((double)reviews.Count / completed.Count, 3),
            DisputesOpened = disputesOpened,
            DisputeRate = completed.Count == 0 ? 0 : Math.Round((double)disputesOpened / completed.Count, 3),
            ReportsFiled = reportsFiled
        };

        // ── Backlog: the live queue, deliberately not scoped to the period ───
        var soon = now.AddHours(24);
        var openListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Open);
        var reservedListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Reserved);
        var expiredListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Expired);
        var archivedListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Archived);
        var expiringSoon = await _context.ClearanceListings
            .CountAsync(l => l.Status == ListingStatus.Open
                          && l.ClearanceDeadline > now && l.ClearanceDeadline <= soon);

        var liveByStatus = await _context.PickupRequests
            .Where(pr => pr.Status == PickupRequestStatus.Pending
                      || pr.Status == PickupRequestStatus.Approved
                      || pr.Status == PickupRequestStatus.Ready)
            .GroupBy(pr => pr.Status)
            .Select(g => new { Status = g.Key, Count = g.Count(), Oldest = g.Min(x => x.RequestedAt) })
            .ToListAsync();

        var pendingLive = liveByStatus.FirstOrDefault(s => s.Status == PickupRequestStatus.Pending);

        var backlog = new StatsBacklogDto
        {
            OpenListings = openListings,
            ReservedListings = reservedListings,
            ExpiredListings = expiredListings,
            ArchivedListings = archivedListings,
            ExpiringWithin24h = expiringSoon,
            PendingRequests = pendingLive?.Count ?? 0,
            ApprovedRequests = liveByStatus.FirstOrDefault(s => s.Status == PickupRequestStatus.Approved)?.Count ?? 0,
            ReadyRequests = liveByStatus.FirstOrDefault(s => s.Status == PickupRequestStatus.Ready)?.Count ?? 0,
            OldestPendingRequestHours = pendingLive == null ? null : Math.Round((now - pendingLive.Oldest).TotalHours, 1),
            PendingVerifications = pendingOrgs.Count,
            OldestPendingVerificationDays = oldestPendingDays,
            // Both states still sit on an admin's desk, which is what this queue counts.
            OpenDisputes = await _context.Disputes.CountAsync(d => d.Status == "open" || d.Status == "under_review"),
            PendingReports = await _context.Reports.CountAsync(r => r.Status == "pending")
        };

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
            Backlog = backlog,
            Timing = timing,
            Quality = quality,
            Leaderboards = new StatsLeaderboardsDto
            {
                TopGroceries = groceryStats.Select(g => new StatsGroceryLeaderDto
                {
                    Id = g.Id.ToString(),
                    Name = leaderNames.GetValueOrDefault(g.Id, ""),
                    CompletedPickups = g.Pickups
                }).ToList(),
                TopNgos = ngoStats.Select(n => new StatsNgoLeaderDto
                {
                    Id = n.Id.ToString(),
                    Name = leaderNames.GetValueOrDefault(n.Id, ""),
                    CompletedPickups = n.Pickups
                }).ToList()
            },
            Organizations = new StatsOrganizationsDto
            {
                Total = orgs.Count,
                Groceries = orgs.Count(o => o.Type.ToLower() == "grocery"),
                Ngos = orgs.Count(o => o.Type.ToLower() == "ngo"),
                Verified = orgs.Count(o => o.Verified),
                PendingVerification = pendingOrgs.Count,
                OldestPendingDays = oldestPendingDays
            }
        };

        return Ok(new AdminStatisticsResponse { Data = data });
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
        /// <summary>When the hand-over actually happened, falling back down the chain of
        /// timestamps an older record may be missing.</summary>
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

    // ── GET api/admin/disputes ───────────────────────────────────────────────
    // Admin alert feed: open disputes + pending reports
    [HttpGet("alerts")]
    public async Task<IActionResult> GetAlertFeed()
    {
        var disputes = await _context.Disputes
            .Include(d => d.Initiator)
            .Where(d => d.Status == "open" || d.Status == "under_review")
            .OrderByDescending(d => d.CreatedAt)
            .Take(20)
            .Select(d => new
            {
                type = "dispute",
                severity = "high",
                id = d.Id.ToString(),
                title = $"Dispute: {d.Reason}",
                body = d.NgoStatement ?? "",
                initiator = d.Initiator!.Name,
                status = d.Status,
                createdAt = d.CreatedAt.ToString("o")
            })
            .ToListAsync<object>();

        var reports = await _context.Reports
            .Include(r => r.Reporter)
            .Include(r => r.Listing)
            .Where(r => r.Status == "pending")
            .OrderByDescending(r => r.CreatedAt)
            .Take(20)
            .Select(r => new
            {
                type = "report",
                severity = "medium",
                id = r.Id.ToString(),
                title = $"Report: {r.Reason}",
                body = r.Details ?? "",
                initiator = r.Reporter!.Name,
                status = r.Status,
                createdAt = r.CreatedAt.ToString("o")
            })
            .ToListAsync<object>();

        var feed = disputes.Concat(reports)
            .OrderByDescending(x => ((dynamic)x).createdAt)
            .Take(30)
            .ToList();

        return Ok(new { data = feed, total = feed.Count });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static (DateTime? start, DateTime? end) ResolveRange(string? from, string? to, string preset)
    {
        if (!string.IsNullOrEmpty(from) && DateTime.TryParse(from, out var f) &&
            !string.IsNullOrEmpty(to) && DateTime.TryParse(to, out var t))
        {
            return (f.ToUniversalTime(), t.ToUniversalTime().AddDays(1).AddSeconds(-1));
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
