using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using Microsoft.EntityFrameworkCore;
using ClearChain.API.Common;
using ClearChain.API.Services;
using ClearChain.API.DTOs.Organizations;
using ClearChain.API.DTOs.Common;
using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
using ClearChain.Infrastructure.Data;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class OrganizationsController : ControllerBase
{
    private readonly IOrganizationService _organizationService;
    private readonly ApplicationDbContext _context;
    private readonly IStorageService _storageService;
    private readonly IPushNotificationService _pushNotificationService;
    private readonly ILogger<OrganizationsController> _logger;

    public OrganizationsController(
        IOrganizationService organizationService,
        ApplicationDbContext context,
        IStorageService storageService,
        IPushNotificationService pushNotificationService,
        ILogger<OrganizationsController> logger)
    {
        _organizationService = organizationService;
        _context             = context;
        _storageService      = storageService;
        _pushNotificationService = pushNotificationService;
        _logger              = logger;
    }

    /// <summary>
    /// Get dashboard stats for the current user (NGO or Grocery).
    /// </summary>
    [HttpGet("my/stats")]
    public async Task<IActionResult> GetMyStats()
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;

        if (userId == null || !Guid.TryParse(userId, out var userGuid))
            return Unauthorized();

        var org = await _context.Organizations.FindAsync(userGuid);
        if (org == null) return NotFound();

        if (org.Type == "ngo")
        {
            // Every request bucketed at once: the headline counts are then slices of the
            // same read, so a total can never disagree with the breakdown beside it.
            var byStatus = await CountRequestsByStatus(pr => pr.NgoId == userGuid);

            // The whole inventory in one read. Rows carry their own unit, so the weight is
            // read off them rather than guessed from how many there are, and the three
            // status counts come from the same rows the weight did.
            var inventory = await _context.Inventories
                .Where(i => i.NgoId == userGuid)
                .Select(i => new { i.Status, i.Unit, i.Quantity })
                .AsNoTracking()
                .ToListAsync();

            var distributedRows = inventory.Where(i => i.Status == InventoryStatus.Distributed).ToList();
            var inventoryStatus = new InventoryStatusCounts(
                Active:      inventory.Count(i => i.Status == InventoryStatus.Active),
                Distributed: distributedRows.Count,
                Expired:     inventory.Count(i => i.Status == InventoryStatus.Expired));

            var availableListings = await _context.ClearanceListings
                .CountAsync(l => l.Status == ListingStatus.Open);
            var completedThisWeek = await CountCompletedThisWeek(pr => pr.NgoId == userGuid);

            var ngoRescued = QuantityUnits.Sum(
                distributedRows.Select(i => ((string?)i.Unit, (int)Math.Round(i.Quantity))));

            return Ok(new
            {
                data = new
                {
                    inStock           = inventoryStatus.Active,
                    activeRequests    = byStatus.InFlight,
                    distributed       = inventoryStatus.Distributed,
                    availableFood     = availableListings,
                    inventoryStatus,
                    totalCompleted    = byStatus.Completed,
                    completedThisWeek,
                    foodSaved         = (int)Math.Round(ngoRescued.Kg),
                    mealsEstimate     = (int)Math.Round(ngoRescued.Kg * QuantityUnits.MealsPerKg),
                    co2EstimateKg     = (int)Math.Round(ngoRescued.Kg * QuantityUnits.Co2PerKg),
                    requestStatus     = byStatus
                }
            });
        }
        else if (org.Type == "grocery")
        {
            var byStatus = await CountRequestsByStatus(pr => pr.GroceryId == userGuid);

            // A listing row is deleted once its food is collected, so what remains on file
            // is what did not move: still open, held for a pickup, or expired unclaimed.
            var listingRows = await _context.ClearanceListings
                .Where(l => l.GroceryId == userGuid)
                .GroupBy(l => l.Status)
                .Select(g => new { Status = g.Key, Count = g.Count() })
                .ToListAsync();

            int ListingsWith(ListingStatus status) =>
                listingRows.FirstOrDefault(r => r.Status == status)?.Count ?? 0;

            var listingStatus = new ListingStatusCounts(
                Open:     ListingsWith(ListingStatus.Open),
                Reserved: ListingsWith(ListingStatus.Reserved),
                Expired:  ListingsWith(ListingStatus.Expired),
                Archived: ListingsWith(ListingStatus.Archived));
            // A request's RequestedQuantity is the sum of its items across whatever units
            // they use, so totalling it would add kilograms to boxes. The line items keep
            // their own unit, which is the only level where a weight can be honest.
            var completedItems = await _context.PickupRequestItems
                .Where(i => i.PickupRequest!.GroceryId == userGuid
                         && i.PickupRequest.Status == PickupRequestStatus.Completed)
                .Select(i => new { i.ListingUnit, i.RequestedQuantity })
                .AsNoTracking()
                .ToListAsync();
            var completedThisWeek = await CountCompletedThisWeek(pr => pr.GroceryId == userGuid);

            var groceryRescued = QuantityUnits.Sum(
                completedItems.Select(i => ((string?)i.ListingUnit, i.RequestedQuantity)));

            return Ok(new
            {
                data = new
                {
                    activeListings  = listingStatus.Open,
                    pendingRequests = byStatus.Pending,
                    completed       = byStatus.Completed,
                    completedThisWeek,
                    foodSaved       = (int)Math.Round(groceryRescued.Kg),
                    mealsEstimate   = (int)Math.Round(groceryRescued.Kg * QuantityUnits.MealsPerKg),
                    co2EstimateKg   = (int)Math.Round(groceryRescued.Kg * QuantityUnits.Co2PerKg),
                    totalListings   = listingStatus.Total,
                    requestStatus   = byStatus,
                    listingStatus
                }
            });
        }

        return BadRequest(new { message = "Stats not available for this organization type" });
    }

    /// <summary>
    /// Pickups this organization actually completed in the last seven days. The hand-over
    /// timestamp decides, not the request date - a pickup belongs to the week it happened
    /// in, whenever it was asked for.
    /// </summary>
    private async Task<int> CountCompletedThisWeek(
        System.Linq.Expressions.Expression<Func<PickupRequest, bool>> scope)
    {
        var weekStart = DateTime.UtcNow.Date.AddDays(-6);
        return await _context.PickupRequests
            .Where(scope)
            .CountAsync(pr => pr.Status == PickupRequestStatus.Completed
                           && (pr.ConfirmedReceivedAt ?? pr.MarkedPickedUpAt ?? pr.RequestedAt) >= weekStart);
    }

    /// <summary>
    /// Where an organization's pickup requests stand, all of them, in one grouped read.
    /// Callers slice this rather than issuing a count per status, so the headline figures
    /// and the breakdown chart can never tell two different stories.
    /// </summary>
    private async Task<RequestStatusCounts> CountRequestsByStatus(
        System.Linq.Expressions.Expression<Func<PickupRequest, bool>> scope)
    {
        var rows = await _context.PickupRequests
            .Where(scope)
            .GroupBy(pr => pr.Status)
            .Select(g => new { Status = g.Key, Count = g.Count() })
            .ToListAsync();

        int Of(PickupRequestStatus status) =>
            rows.FirstOrDefault(r => r.Status == status)?.Count ?? 0;

        return new RequestStatusCounts(
            Pending:   Of(PickupRequestStatus.Pending),
            Approved:  Of(PickupRequestStatus.Approved),
            Ready:     Of(PickupRequestStatus.Ready),
            Completed: Of(PickupRequestStatus.Completed),
            Cancelled: Of(PickupRequestStatus.Cancelled),
            Rejected:  Of(PickupRequestStatus.Rejected));
    }

    /// <summary>
    /// What an NGO is holding. Distributed food has left the shelf but stays on the books
    /// as the record of what was handed on; expired is what spoiled before it could be.
    /// </summary>
    public sealed record InventoryStatusCounts(int Active, int Distributed, int Expired)
    {
        public int Total => Active + Distributed + Expired;
    }

    /// <summary>
    /// A store's listings as they stand. Collected listings are not here: the row is
    /// deleted on pickup, so these three are what has not moved.
    /// </summary>
    public sealed record ListingStatusCounts(int Open, int Reserved, int Expired, int Archived)
    {
        public int Total => Open + Reserved + Expired + Archived;
    }

    /// <summary>One bucket per pickup request status, so the six always add up to the total.</summary>
    public sealed record RequestStatusCounts(
        int Pending,
        int Approved,
        int Ready,
        int Completed,
        int Cancelled,
        int Rejected)
    {
        /// <summary>Neither won nor lost yet - still moving through the pipeline.</summary>
        public int InFlight => Pending + Approved + Ready;

        public int Total => Pending + Approved + Ready + Completed + Cancelled + Rejected;
    }

    /// <summary>
    /// Get the current user's activity for the last <paramref name="days"/> days (default 7,
    /// clamped to 1–90). The 7-day default matches the dashboard's "Actions this week" sparkline;
    /// the analytics screen requests a wider window.
    /// Grocery: listings created + pickup requests received.
    /// NGO: pickup requests made + inventory received.
    /// </summary>
    [HttpGet("my/activity")]
    public async Task<IActionResult> GetMyActivity([FromQuery] int days = 7)
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;

        if (userId == null || !Guid.TryParse(userId, out var userGuid))
            return Unauthorized();

        var org = await _context.Organizations.FindAsync(userGuid);
        if (org == null) return NotFound();

        // Start of the day (UTC) N-1 days ago, so the result spans N calendar days
        // including today — the same window the dashboard sparkline plots.
        var cutoff = DateTime.UtcNow.Date.AddDays(-(Math.Clamp(days, 1, 90) - 1));
        var activities = new List<ActivityDto>();

        if (org.Type == "grocery")
        {
            // Listings created in the window
            var listings = await _context.ClearanceListings
                .Where(l => l.GroceryId == userGuid && l.CreatedAt >= cutoff)
                .OrderByDescending(l => l.CreatedAt)
                .ToListAsync();

            foreach (var l in listings)
            {
                activities.Add(new ActivityDto
                {
                    Id        = l.Id.ToString(),
                    Type      = "listing_created",
                    Title     = l.ProductName,
                    Subtitle  = $"{l.Quantity} {l.Unit} listed for pickup",
                    Timestamp = l.CreatedAt.ToString("o"),
                    RelatedId = l.Id.ToString()
                });
            }

            // Pickup requests received in the window (with NGO name via navigation)
            var requests = await _context.PickupRequests
                .Include(pr => pr.Ngo)
                .Where(pr => pr.GroceryId == userGuid && pr.RequestedAt >= cutoff)
                .OrderByDescending(pr => pr.RequestedAt)
                .ToListAsync();

            foreach (var r in requests)
            {
                var ngoName = r.Ngo?.Name ?? "An NGO";
                var (type, title, subtitle) = r.Status switch
                {
                    PickupRequestStatus.Completed => (
                        "pickup_completed",
                        "Pickup completed",
                        $"{ngoName} collected {r.RequestedQuantity ?? 0} items of {r.ListingTitle}"),
                    PickupRequestStatus.Pending => (
                        "pickup_request",
                        "New pickup request",
                        $"{ngoName} requested {r.ListingTitle}"),
                    PickupRequestStatus.Approved => (
                        "pickup_approved",
                        "Pickup approved",
                        $"Approved pickup for {ngoName}"),
                    PickupRequestStatus.Ready => (
                        "pickup_ready",
                        "Marked ready",
                        r.ListingTitle),
                    PickupRequestStatus.Cancelled => (
                        "pickup_cancelled",
                        "Pickup cancelled",
                        $"{ngoName} · {r.ListingTitle}"),
                    _ => ("pickup_request", "Pickup request", r.ListingTitle)
                };

                activities.Add(new ActivityDto
                {
                    Id        = r.Id.ToString(),
                    Type      = type,
                    Title     = title,
                    Subtitle  = subtitle,
                    Timestamp = r.RequestedAt.ToString("o"),
                    RelatedId = r.Id.ToString()
                });
            }
        }
        else if (org.Type == "ngo")
        {
            // Pickup requests made in the window (with Grocery name via navigation)
            var requests = await _context.PickupRequests
                .Include(pr => pr.Grocery)
                .Where(pr => pr.NgoId == userGuid && pr.RequestedAt >= cutoff)
                .OrderByDescending(pr => pr.RequestedAt)
                .ToListAsync();

            foreach (var r in requests)
            {
                var groceryName = r.Grocery?.Name ?? "the grocery";
                var (type, title, subtitle) = r.Status switch
                {
                    PickupRequestStatus.Completed => (
                        "pickup_completed",
                        "Pickup completed",
                        $"Received {r.RequestedQuantity ?? 0} items of {r.ListingTitle}"),
                    PickupRequestStatus.Pending => (
                        "pickup_request",
                        "Awaiting approval",
                        $"Request sent to {groceryName} for {r.ListingTitle}"),
                    PickupRequestStatus.Approved => (
                        "pickup_approved",
                        "Pickup approved",
                        $"{groceryName} approved your request"),
                    PickupRequestStatus.Ready => (
                        "pickup_ready",
                        "Ready",
                        $"{groceryName} · {r.ListingTitle}"),
                    PickupRequestStatus.Cancelled => (
                        "pickup_cancelled",
                        "Pickup cancelled",
                        r.ListingTitle),
                    _ => ("pickup_request", "Pickup request", r.ListingTitle)
                };

                activities.Add(new ActivityDto
                {
                    Id        = r.Id.ToString(),
                    Type      = type,
                    Title     = title,
                    Subtitle  = subtitle,
                    Timestamp = r.RequestedAt.ToString("o"),
                    RelatedId = r.Id.ToString()
                });
            }

            // Inventory items received in the window
            var inventory = await _context.Inventories
                .Where(i => i.NgoId == userGuid && i.ReceivedAt >= cutoff)
                .OrderByDescending(i => i.ReceivedAt)
                .ToListAsync();

            foreach (var i in inventory)
            {
                activities.Add(new ActivityDto
                {
                    Id        = i.Id.ToString(),
                    Type      = "inventory_received",
                    Title     = $"{i.ProductName} received",
                    Subtitle  = $"{i.Quantity} {i.Unit} added to inventory",
                    Timestamp = i.ReceivedAt.ToString("o"),
                    RelatedId = i.Id.ToString()
                });
            }
        }

        var sorted = activities
            .OrderByDescending(a => a.Timestamp)
            .Take(200)
            .ToList();

        return Ok(new { data = sorted });
    }

    /// <summary>
    /// Get public profile for any organization (viewable by any authenticated user)
    /// Includes average rating and review count for grocery stores.
    /// </summary>
    [HttpGet("{id}/public")]
    public async Task<IActionResult> GetPublicProfile(Guid id)
    {
        var org = await _context.Organizations
            .FirstOrDefaultAsync(o => o.Id == id && !o.IsDeleted);

        if (org == null) return NotFound(new { message = "Organization not found" });

        double avgRating = 0;
        int reviewCount = 0;
        int completedPickups = 0;
        // Weighed the same way as my/stats, so an organization's public impact and the
        // figure it sees on its own dashboard are the same number.
        var rescued = QuantityUnits.Totals.Empty;

        if (org.Type == "grocery")
        {
            var reviews = await _context.Reviews.Where(r => r.ReviewedId == id).ToListAsync();
            reviewCount = reviews.Count;
            avgRating = reviewCount > 0 ? Math.Round(reviews.Average(r => r.Rating), 1) : 0;
            completedPickups = await _context.PickupRequests
                .CountAsync(pr => pr.GroceryId == id && pr.Status == Domain.Enums.PickupRequestStatus.Completed);

            var items = await _context.PickupRequestItems
                .Where(i => i.PickupRequest!.GroceryId == id
                         && i.PickupRequest.Status == Domain.Enums.PickupRequestStatus.Completed)
                .Select(i => new { i.ListingUnit, i.RequestedQuantity })
                .AsNoTracking()
                .ToListAsync();
            rescued = QuantityUnits.Sum(items.Select(i => ((string?)i.ListingUnit, i.RequestedQuantity)));
        }
        else if (org.Type == "ngo")
        {
            completedPickups = await _context.PickupRequests
                .CountAsync(pr => pr.NgoId == id && pr.Status == Domain.Enums.PickupRequestStatus.Completed);

            var distributed = await _context.Inventories
                .Where(i => i.NgoId == id && i.Status == InventoryStatus.Distributed)
                .Select(i => new { i.Unit, i.Quantity })
                .AsNoTracking()
                .ToListAsync();
            rescued = QuantityUnits.Sum(
                distributed.Select(i => ((string?)i.Unit, (int)Math.Round(i.Quantity))));
        }

        return Ok(new
        {
            data = new
            {
                id = org.Id,
                name = org.Name,
                type = org.Type,
                email = org.Email,
                location = org.Location,
                address = org.Address,
                state = org.State,
                zipCode = org.ZipCode,
                phone = org.Phone,
                description = org.Description,
                hours = org.Hours,
                profilePictureUrl = org.ProfilePictureUrl,
                latitude = org.Latitude,
                longitude = org.Longitude,
                contactPerson = org.ContactPerson,
                verified = org.Verified,
                verificationStatus = org.VerificationStatus,
                createdAt = org.CreatedAt.ToString("o"),
                averageRating = avgRating,
                reviewCount,
                completedPickups,
                foodSaved     = (int)Math.Round(rescued.Kg),
                mealsEstimate = (int)Math.Round(rescued.Kg * QuantityUnits.MealsPerKg)
            }
        });
    }

    /// <summary>
    /// Get NGO reputation score (pickup completion rate, avg response time).
    /// Used by grocery stores when reviewing pickup requests.
    /// </summary>
    [HttpGet("{id}/reputation")]
    public async Task<IActionResult> GetNgoReputation(Guid id)
    {
        var total = await _context.PickupRequests
            .CountAsync(pr => pr.NgoId == id);

        var completed = await _context.PickupRequests
            .CountAsync(pr => pr.NgoId == id && pr.Status == Domain.Enums.PickupRequestStatus.Completed);

        var cancelled = await _context.PickupRequests
            .CountAsync(pr => pr.NgoId == id && pr.Status == Domain.Enums.PickupRequestStatus.Cancelled);

        var completionRate = total > 0 ? Math.Round((double)completed / total * 100, 1) : 0.0;

        return Ok(new
        {
            data = new
            {
                totalRequests = total,
                completedPickups = completed,
                cancelledPickups = cancelled,
                completionRate
            }
        });
    }

    /// <summary>
    /// Today's summary for the grocery dashboard: listings expiring today,
    /// pickups today, items cleared today.
    /// </summary>
    [HttpGet("my/today-summary")]
    public async Task<IActionResult> GetTodaySummary()
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;

        if (userId == null || !Guid.TryParse(userId, out var userGuid))
            return Unauthorized();

        var org = await _context.Organizations.FindAsync(userGuid);
        if (org == null) return NotFound();

        var today = DateTime.UtcNow.Date;
        var tomorrow = today.AddDays(1);

        if (org.Type == "grocery")
        {
            var expiringToday = await _context.ClearanceListings
                .CountAsync(l => l.GroceryId == userGuid
                    && l.ExpirationDate.HasValue
                    && l.ExpirationDate.Value.Date == today
                    && l.Status == Domain.Enums.ListingStatus.Open);

            var groceryPickupsToday = await _context.PickupRequests
                .CountAsync(pr => pr.GroceryId == userGuid
                    && pr.PickupDate.Date == today
                    && pr.Status == Domain.Enums.PickupRequestStatus.Approved);

            var clearedToday = await _context.PickupRequests
                .CountAsync(pr => pr.GroceryId == userGuid
                    && pr.MarkedPickedUpAt.HasValue
                    && pr.MarkedPickedUpAt.Value.Date == today
                    && pr.Status == Domain.Enums.PickupRequestStatus.Completed);

            var groceryListingsCreatedToday = await _context.ClearanceListings
                .CountAsync(l => l.GroceryId == userGuid
                    && l.CreatedAt.Date == today
                    && l.Status == Domain.Enums.ListingStatus.Open);

            var groceryRequestsReceivedToday = await _context.PickupRequests
                .CountAsync(pr => pr.GroceryId == userGuid && pr.RequestedAt.Date == today);

            var groceryUpcomingPickups = await _context.PickupRequests
                .Include(pr => pr.Ngo)
                .Where(pr => pr.GroceryId == userGuid
                    && pr.PickupDate.Date == today
                    && (pr.Status == Domain.Enums.PickupRequestStatus.Approved
                        || pr.Status == Domain.Enums.PickupRequestStatus.Ready))
                .OrderBy(pr => pr.PickupTime)
                .Select(pr => new
                {
                    id           = pr.Id,
                    listingTitle = pr.ListingTitle,
                    ngoName      = pr.Ngo!.Name,
                    pickupTime   = pr.PickupTime,
                    status       = pr.Status.ToString().ToLower()
                })
                .ToListAsync();

            return Ok(new
            {
                data = new
                {
                    expiringToday,
                    pickupsToday          = groceryPickupsToday,
                    clearedToday,
                    listingsCreatedToday  = groceryListingsCreatedToday,
                    requestsCreatedToday  = groceryRequestsReceivedToday,
                    upcomingPickups       = groceryUpcomingPickups
                }
            });
        }

        // NGO today summary
        var upcomingPickups = await _context.PickupRequests
            .Include(pr => pr.Grocery)
            .Where(pr => pr.NgoId == userGuid
                && pr.PickupDate.Date == today
                && (pr.Status == Domain.Enums.PickupRequestStatus.Approved
                    || pr.Status == Domain.Enums.PickupRequestStatus.Ready))
            .Select(pr => new
            {
                id = pr.Id,
                listingTitle = pr.ListingTitle,
                groceryName = pr.Grocery!.Name,
                pickupTime = pr.PickupTime,
                status = pr.Status.ToString().ToLower()
            })
            .ToListAsync();

        var requestsCreatedToday = await _context.PickupRequests
            .CountAsync(pr => pr.NgoId == userGuid && pr.RequestedAt.Date == today);

        var pickupsToday = await _context.PickupRequests
            .CountAsync(pr => pr.NgoId == userGuid
                && pr.Status == Domain.Enums.PickupRequestStatus.Completed
                && pr.ConfirmedReceivedAt.HasValue
                && pr.ConfirmedReceivedAt.Value.Date == today);

        var distributedToday = await _context.Inventories
            .CountAsync(i => i.NgoId == userGuid
                && i.ReceivedAt.Date == today);

        return Ok(new { data = new { requestsCreatedToday, pickupsToday, distributedToday, upcomingPickups } });
    }

    /// <summary>
    /// Upload/replace profile avatar
    /// </summary>
    [HttpPost("avatar")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> UploadAvatar(IFormFile avatar)
    {
        if (avatar == null || avatar.Length == 0)
            return BadRequest(new { message = "No file provided" });

        if (!StorageBucketPolicy.IsAllowedImage(avatar.ContentType))
            return BadRequest(new { message = $"Only {StorageBucketPolicy.ImageTypesMessage} are accepted" });

        if (avatar.Length > StorageBucketPolicy.ImageMaxBytes)
            return BadRequest(new { message = $"File must be under {StorageBucketPolicy.ImageMaxBytes / 1024 / 1024} MB" });

        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;
        if (userId == null || !Guid.TryParse(userId, out var userGuid))
            return Unauthorized();

        var org = await _context.Organizations.FindAsync(userGuid);
        if (org == null) return NotFound(new { message = "Organization not found" });

        using var stream = avatar.OpenReadStream();
        var url = await _storageService.UploadFileAsync(stream, avatar.FileName, avatar.ContentType, "avatars");

        org.ProfilePictureUrl = url;
        org.UpdatedAt         = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return Ok(new { message = "Avatar updated", data = new { url } });
    }

    /// <summary>
    /// Upload the single verification document (business registration, charity certificate, etc.).
    /// Re-uploading replaces the existing document.
    /// </summary>
    [HttpPost("documents")]
    public async Task<IActionResult> UploadDocument(IFormFile document)
    {
        if (document == null || document.Length == 0)
            return BadRequest(new { message = "No document provided" });

        if (!StorageBucketPolicy.IsAllowedDocument(document.ContentType))
            return BadRequest(new { message = $"Only {StorageBucketPolicy.DocumentTypesMessage} are accepted" });

        if (document.Length > StorageBucketPolicy.DocumentMaxBytes)
            return BadRequest(new { message = $"File must be under {StorageBucketPolicy.DocumentMaxBytes / 1024 / 1024} MB" });

        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;
        if (userId == null || !Guid.TryParse(userId, out var userGuid))
            return Unauthorized();

        var org = await _context.Organizations.FindAsync(userGuid);
        if (org == null) return NotFound(new { message = "Organization not found" });

        using var stream = document.OpenReadStream();
        var url = await _storageService.UploadFileAsync(stream, document.FileName, document.ContentType, "documents");

        org.DocumentUrl      = url;
        org.DocumentMimeType = document.ContentType;

        var resubmitted = OrganizationService.ResubmitIfRejected(org);

        org.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        if (resubmitted)
        {
            try
            {
                await _pushNotificationService.SendResubmissionAlertToAdmins(new DTOs.Admin.OrganizationData
                {
                    Id = org.Id.ToString(),
                    Name = org.Name,
                    Email = org.Email,
                    Type = org.Type,
                    Phone = org.Phone ?? "",
                    Address = org.Address ?? "",
                    Location = org.Location ?? "",
                    Verified = org.Verified,
                    VerificationStatus = org.VerificationStatus,
                    CreatedAt = org.CreatedAt.ToString("o")
                });
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Failed to send resubmission alert for {OrgId}", org.Id);
            }
        }

        return Ok(new
        {
            message = resubmitted ? "Document uploaded and resubmitted for review" : "Document uploaded",
            data = new { url }
        });
    }

    /// <summary>
    /// Update current user's profile
    /// </summary>
    [HttpPut("profile")]
    public async Task<IActionResult> UpdateProfile([FromBody] UpdateProfileRequest request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;

        if (userId == null || !Guid.TryParse(userId, out var userGuid))
        {
            return Unauthorized();
        }

        var (success, message) = await _organizationService.UpdateProfileAsync(userGuid, request);

        if (!success)
        {
            return BadRequest(ApiResponse<object>.ErrorResponse(message));
        }

        return Ok(ApiResponse<object>.SuccessResponse(null!, message));
    }
}