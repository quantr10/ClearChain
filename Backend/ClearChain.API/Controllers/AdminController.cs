using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Enums;
using ClearChain.API.DTOs.Admin;
using ClearChain.API.DTOs.PickupRequests;
using ClearChain.API.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class AdminController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<AdminController> _logger;
    private readonly IPushNotificationService _pushNotificationService;

    public AdminController(
        ApplicationDbContext context,
        ILogger<AdminController> logger,
        IPushNotificationService pushNotificationService)
    {
        _context = context;
        _logger = logger;
        _pushNotificationService = pushNotificationService;
    }

    [HttpGet("organizations")]
    public async Task<ActionResult<OrganizationListResponse>> GetAllOrganizations(
        [FromQuery] string? type = null,
        [FromQuery] bool? verified = null)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var user = await _context.Organizations.FindAsync(Guid.Parse(userId));
            if (user == null || user.Type.ToLower() != "admin")
            {
                return Forbid();
            }

            var query = _context.Organizations.AsQueryable();

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
                DocumentUrl2 = o.DocumentUrl2,
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
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting organizations");
            return StatusCode(500, new { message = "An error occurred while retrieving organizations" });
        }
    }

    [HttpGet("statistics/overview")]
    public async Task<ActionResult<AdminStatsResponse>> GetStatisticsOverview()
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var user = await _context.Organizations.FindAsync(Guid.Parse(userId));
            if (user == null || user.Type.ToLower() != "admin")
            {
                return Forbid();
            }

            var totalOrgs = await _context.Organizations.CountAsync();
            var totalGroceries = await _context.Organizations.CountAsync(o => o.Type.ToLower() == "grocery");
            var totalNgos = await _context.Organizations.CountAsync(o => o.Type.ToLower() == "ngo");
            var verifiedOrgs = await _context.Organizations.CountAsync(o => o.Verified);
            var unverifiedOrgs = totalOrgs - verifiedOrgs;

            var totalListings = await _context.ClearanceListings.CountAsync();
            var activeListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Open);
            var reservedListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Reserved);

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

            var stats = new AdminStatsData
            {
                TotalOrganizations = totalOrgs,
                TotalGroceries = totalGroceries,
                TotalNgos = totalNgos,
                VerifiedOrganizations = verifiedOrgs,
                UnverifiedOrganizations = unverifiedOrgs,

                TotalListings = totalListings,
                ActiveListings = activeListings,
                ReservedListings = reservedListings,

                TotalPickupRequests = totalRequests,
                PendingRequests = pendingRequests,
                ApprovedRequests = approvedRequests,
                ReadyRequests = readyRequests,
                RejectedRequests = rejectedRequests,
                CompletedRequests = completedRequests,
                CancelledRequests = cancelledRequests,

                TotalFoodSaved = totalFoodSaved
            };

            return Ok(new AdminStatsResponse
            {
                Message = "Statistics retrieved successfully",
                Data = stats
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting statistics");
            return StatusCode(500, new { message = "An error occurred while retrieving statistics" });
        }
    }

    [HttpGet("pickuprequests")]
    public async Task<ActionResult<PickupRequestsResponse>> GetAllPickupRequests()
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var user = await _context.Organizations.FindAsync(Guid.Parse(userId));
            if (user == null || user.Type.ToLower() != "admin")
            {
                return Forbid();
            }

            var pickupRequests = await _context.PickupRequests
                .OrderByDescending(pr => pr.RequestedAt)
                .ToListAsync();

            var responseData = new List<PickupRequestData>();

            foreach (var pr in pickupRequests)
            {
                var ngo = await _context.Organizations.FindAsync(pr.NgoId);
                var grocery = await _context.Organizations.FindAsync(pr.GroceryId);
                var listing = pr.ListingId.HasValue
                    ? await _context.ClearanceListings.FindAsync(pr.ListingId.Value)
                    : null;

                responseData.Add(new PickupRequestData
                {
                    Id = pr.Id.ToString(),
                    ListingId = pr.ListingId?.ToString() ?? "",
                    NgoId = pr.NgoId.ToString(),
                    NgoName = ngo?.Name ?? "",
                    GroceryId = pr.GroceryId.ToString(),
                    GroceryName = grocery?.Name ?? "",
                    Status = pr.Status.ToString().ToLower(),
                    RequestedQuantity = pr.RequestedQuantity ?? 0,
                    PickupDate = pr.PickupDate.ToString("yyyy-MM-dd"),
                    PickupTime = pr.PickupTime ?? "09:00",
                    Notes = pr.Notes ?? "",
                    ListingTitle = listing?.ProductName ?? "Unknown Item",
                    ListingCategory = listing?.Category ?? "OTHER",
                    CreatedAt = pr.RequestedAt.ToString("o")
                });
            }

            return Ok(new PickupRequestsResponse
            {
                Message = "Pickup requests retrieved successfully",
                Data = responseData
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting pickup requests");
            return StatusCode(500, new { message = "An error occurred while retrieving pickup requests" });
        }
    }

    // ── GET api/admin/statistics?from=&to= ────────────────────────────────────
    // Date-range statistics with breakdowns and leaderboards
    [HttpGet("statistics")]
    public async Task<IActionResult> GetStatistics(
        [FromQuery] string? from = null,
        [FromQuery] string? to = null,
        [FromQuery] string preset = "all")  // today, week, month, quarter, all
    {
        if (!IsAdmin()) return Forbid();

        var (start, end) = ResolveRange(from, to, preset);

        var orgsQuery = _context.Organizations.Where(o => !o.IsDeleted);
        var listingsQuery = _context.ClearanceListings.Where(l =>
            !start.HasValue || l.CreatedAt >= start.Value);
        var requestsQuery = _context.PickupRequests.Where(pr =>
            !start.HasValue || pr.RequestedAt >= start.Value);
        var inventoryQuery = _context.Inventories.Where(i =>
            !start.HasValue || i.ReceivedAt >= start.Value);

        if (end.HasValue)
        {
            listingsQuery  = listingsQuery.Where(l => l.CreatedAt <= end.Value);
            requestsQuery  = requestsQuery.Where(pr => pr.RequestedAt <= end.Value);
            inventoryQuery = inventoryQuery.Where(i => i.ReceivedAt <= end.Value);
        }

        var totalOrgs        = await orgsQuery.CountAsync();
        var totalGroceries   = await orgsQuery.CountAsync(o => o.Type == "grocery");
        var totalNgos        = await orgsQuery.CountAsync(o => o.Type == "ngo");
        var verifiedOrgs     = await orgsQuery.CountAsync(o => o.Verified);
        var pendingVerif     = await orgsQuery.CountAsync(o => o.VerificationStatus == "pending");

        var totalListings    = await listingsQuery.CountAsync();
        var activeListings   = await listingsQuery.CountAsync(l => l.Status == ListingStatus.Open);
        var archivedListings = await listingsQuery.CountAsync(l => l.IsArchived);

        var totalRequests    = await requestsQuery.CountAsync();
        var completedReqs    = await requestsQuery.CountAsync(pr => pr.Status == PickupRequestStatus.Completed);
        var pendingReqs      = await requestsQuery.CountAsync(pr => pr.Status == PickupRequestStatus.Pending);
        var cancelledReqs    = await requestsQuery.CountAsync(pr => pr.Status == PickupRequestStatus.Cancelled);

        var kgSaved          = await requestsQuery
            .Where(pr => pr.Status == PickupRequestStatus.Completed)
            .SumAsync(pr => (double)(pr.RequestedQuantity ?? 0));
        var mealsEquivalent  = (int)(kgSaved * 2.5);   // ~2.5 meals per kg
        var co2Saved         = Math.Round(kgSaved * 2.5, 1); // ~2.5 kg CO2 per kg food

        var totalBeneficiaries = await _context.Inventories
            .Where(i => i.Status == InventoryStatus.Distributed
                && (!start.HasValue || i.DistributedAt >= start))
            .SumAsync(i => i.BeneficiaryCount);

        // Category breakdown
        var categoryBreakdown = await requestsQuery
            .Where(pr => pr.Status == PickupRequestStatus.Completed)
            .GroupBy(pr => pr.ListingCategory)
            .Select(g => new { category = g.Key, count = g.Count(), quantity = g.Sum(pr => pr.RequestedQuantity ?? 0) })
            .OrderByDescending(g => g.count)
            .ToListAsync();

        // Top grocery leaderboard
        var topGroceries = await requestsQuery
            .Where(pr => pr.Status == PickupRequestStatus.Completed)
            .GroupBy(pr => pr.GroceryId)
            .Select(g => new { groceryId = g.Key, completedCount = g.Count(), totalQuantity = g.Sum(pr => pr.RequestedQuantity ?? 0) })
            .OrderByDescending(g => g.totalQuantity)
            .Take(10)
            .ToListAsync();

        var groceryIds = topGroceries.Select(g => g.groceryId).ToList();
        var groceryNames = await _context.Organizations
            .Where(o => groceryIds.Contains(o.Id))
            .Select(o => new { o.Id, o.Name })
            .ToDictionaryAsync(o => o.Id, o => o.Name);

        var topGroceriesWithNames = topGroceries.Select(g => new
        {
            id = g.groceryId,
            name = groceryNames.TryGetValue(g.groceryId, out var n) ? n : "",
            completedPickups = g.completedCount,
            totalKg = g.totalQuantity
        }).ToList();

        // Top NGO leaderboard
        var topNgos = await requestsQuery
            .Where(pr => pr.Status == PickupRequestStatus.Completed)
            .GroupBy(pr => pr.NgoId)
            .Select(g => new { ngoId = g.Key, completedCount = g.Count() })
            .OrderByDescending(g => g.completedCount)
            .Take(10)
            .ToListAsync();

        var ngoIds = topNgos.Select(g => g.ngoId).ToList();
        var ngoNames = await _context.Organizations
            .Where(o => ngoIds.Contains(o.Id))
            .Select(o => new { o.Id, o.Name })
            .ToDictionaryAsync(o => o.Id, o => o.Name);

        var topNgosWithNames = topNgos.Select(g => new
        {
            id = g.ngoId,
            name = ngoNames.TryGetValue(g.ngoId, out var n) ? n : "",
            completedPickups = g.completedCount
        }).ToList();

        // Daily trend (last 30 data points within range)
        var dailyTrend = await requestsQuery
            .Where(pr => pr.Status == PickupRequestStatus.Completed)
            .GroupBy(pr => pr.RequestedAt.Date)
            .Select(g => new { date = g.Key, count = g.Count(), quantity = g.Sum(pr => pr.RequestedQuantity ?? 0) })
            .OrderBy(g => g.date)
            .Take(30)
            .ToListAsync();

        return Ok(new
        {
            data = new
            {
                period = new { from = start?.ToString("o"), to = end?.ToString("o"), preset },
                organizations = new { totalOrgs, totalGroceries, totalNgos, verifiedOrgs, pendingVerif },
                listings = new { totalListings, activeListings, archivedListings },
                requests = new { totalRequests, completedReqs, pendingReqs, cancelledReqs },
                impact = new { kgSaved, mealsEquivalent, co2Saved, totalBeneficiaries },
                categoryBreakdown,
                leaderboards = new { topGroceries = topGroceriesWithNames, topNgos = topNgosWithNames },
                dailyTrend
            }
        });
    }

    // ── GET api/admin/health ──────────────────────────────────────────────────
    [HttpGet("health")]
    public async Task<IActionResult> SystemHealth()
    {
        if (!IsAdmin()) return Forbid();

        var dbOk = true;
        var dbLatencyMs = 0L;
        try
        {
            var sw = System.Diagnostics.Stopwatch.StartNew();
            await _context.Organizations.CountAsync();
            sw.Stop();
            dbLatencyMs = sw.ElapsedMilliseconds;
        }
        catch { dbOk = false; }

        var pendingVerifications = await _context.Organizations
            .CountAsync(o => o.VerificationStatus == "pending" && !o.IsDeleted);
        var openDisputes = await _context.Disputes.CountAsync(d => d.Status == "open");
        var pendingReports = await _context.Reports.CountAsync(r => r.Status == "pending");
        var unreadNotifications = await _context.Notifications.CountAsync(n => !n.IsRead);

        return Ok(new
        {
            data = new
            {
                status = dbOk ? "healthy" : "degraded",
                database = new { ok = dbOk, latencyMs = dbLatencyMs },
                alerts = new { pendingVerifications, openDisputes, pendingReports, unreadNotifications },
                timestamp = DateTime.UtcNow.ToString("o")
            }
        });
    }

    // ── GET api/admin/disputes ────────────────────────────────────────────────
    // Admin alert feed: open disputes + pending reports
    [HttpGet("alerts")]
    public async Task<IActionResult> GetAlertFeed()
    {
        if (!IsAdmin()) return Forbid();

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

    // ── GET api/admin/sparkline?days=7 ──────────────────────────────────────
    // Daily listing counts for sparkline preview (grocery dashboard)
    [HttpGet("sparkline")]
    [Authorize]
    public async Task<IActionResult> GetSparkline([FromQuery] int days = 7)
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId) || !Guid.TryParse(userId, out var orgId))
            return Unauthorized();

        days = Math.Clamp(days, 3, 30);
        var since = DateTime.UtcNow.Date.AddDays(-days + 1);

        var raw = await _context.ClearanceListings
            .Where(l => l.GroceryId == orgId && l.CreatedAt >= since)
            .GroupBy(l => l.CreatedAt.Date)
            .Select(g => new { Date = g.Key, Count = g.Count() })
            .ToListAsync();

        var result = Enumerable.Range(0, days).Select(i =>
        {
            var date = since.AddDays(i);
            return new
            {
                date = date.ToString("yyyy-MM-dd"),
                count = raw.FirstOrDefault(r => r.Date == date)?.Count ?? 0
            };
        }).ToList();

        return Ok(new { data = result });
    }

    // ── GET api/admin/user-growth?days=30 ───────────────────────────────────
    // Daily new org registrations for admin user growth chart
    [HttpGet("user-growth")]
    [Authorize]
    public async Task<IActionResult> GetUserGrowth([FromQuery] int days = 30)
    {
        if (!IsAdmin()) return Forbid();

        days = Math.Clamp(days, 7, 90);
        var since = DateTime.UtcNow.Date.AddDays(-days + 1);

        var raw = await _context.Organizations
            .Where(o => !o.IsDeleted && o.CreatedAt >= since)
            .GroupBy(o => o.CreatedAt.Date)
            .Select(g => new { Date = g.Key, Count = g.Count() })
            .ToListAsync();

        var result = Enumerable.Range(0, days).Select(i =>
        {
            var date = since.AddDays(i);
            return new
            {
                date = date.ToString("yyyy-MM-dd"),
                count = raw.FirstOrDefault(r => r.Date == date)?.Count ?? 0
            };
        }).ToList();

        return Ok(new { data = result });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private bool IsAdmin()
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (!Guid.TryParse(userId, out _)) return false;
        var type = User.FindFirst("type")?.Value;
        return type == "admin";
    }

    private static (DateTime? start, DateTime? end) ResolveRange(string? from, string? to, string preset)
    {
        if (!string.IsNullOrEmpty(from) && DateTime.TryParse(from, out var f) &&
            !string.IsNullOrEmpty(to) && DateTime.TryParse(to, out var t))
        {
            return (f.ToUniversalTime(), t.ToUniversalTime().AddDays(1).AddSeconds(-1));
        }

        var now = DateTime.UtcNow;
        return preset switch
        {
            "today"   => (now.Date, now.Date.AddDays(1).AddSeconds(-1)),
            "week"    => (now.Date.AddDays(-(int)now.DayOfWeek), now),
            "month"   => (new DateTime(now.Year, now.Month, 1, 0, 0, 0, DateTimeKind.Utc), now),
            "quarter" => (new DateTime(now.Year, (now.Month - 1) / 3 * 3 + 1, 1, 0, 0, 0, DateTimeKind.Utc), now),
            _         => (null, null)  // "all"
        };
    }
}