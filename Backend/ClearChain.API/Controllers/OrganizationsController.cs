using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using Microsoft.EntityFrameworkCore;
using ClearChain.API.Services;
using ClearChain.API.DTOs.Organizations;
using ClearChain.API.DTOs.Common;
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

    public OrganizationsController(
        IOrganizationService organizationService,
        ApplicationDbContext context,
        IStorageService storageService)
    {
        _organizationService = organizationService;
        _context             = context;
        _storageService      = storageService;
    }

    /// <summary>
    /// Get pending verification requests (Admin only)
    /// </summary>
    [HttpGet("pending")]
    public async Task<IActionResult> GetPendingVerifications([FromQuery] string? type = null)
    {
        // TODO: Add admin role check in real implementation
        var userType = User.FindFirst("type")?.Value;
        if (userType != "admin")
        {
            return Forbid();
        }

        var organizations = await _organizationService.GetPendingVerificationsAsync(type);
        return Ok(ApiResponse<List<DTOs.Auth.OrganizationDto>>.SuccessResponse(
            organizations,
            $"Retrieved {organizations.Count} pending verifications"
        ));
    }

    /// <summary>
    /// Get verified organizations (Admin only)
    /// </summary>
    [HttpGet("verified")]
    public async Task<IActionResult> GetVerifiedOrganizations([FromQuery] string? type = null)
    {
        var userType = User.FindFirst("type")?.Value;
        if (userType != "admin")
        {
            return Forbid();
        }

        var organizations = await _organizationService.GetVerifiedOrganizationsAsync(type);
        return Ok(ApiResponse<List<DTOs.Auth.OrganizationDto>>.SuccessResponse(
            organizations,
            $"Retrieved {organizations.Count} verified organizations"
        ));
    }
    
    /// <summary>
    /// Get organization by ID
    /// </summary>
    [HttpGet("{id}")]
    public async Task<IActionResult> GetOrganizationById(Guid id)
    {
        var organization = await _organizationService.GetOrganizationByIdAsync(id);

        if (organization == null)
        {
            return NotFound(ApiResponse<object>.ErrorResponse("Organization not found"));
        }

        return Ok(ApiResponse<DTOs.Auth.OrganizationDto>.SuccessResponse(
            organization,
            "Organization retrieved successfully"
        ));
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
            var inventoryCount = await _context.Inventories
                .CountAsync(i => i.NgoId == userGuid && i.Status == InventoryStatus.Active);
            var activeRequests = await _context.PickupRequests
                .CountAsync(pr => pr.NgoId == userGuid &&
                    (pr.Status == PickupRequestStatus.Pending ||
                     pr.Status == PickupRequestStatus.Approved ||
                     pr.Status == PickupRequestStatus.Ready));
            var distributedCount = await _context.Inventories
                .CountAsync(i => i.NgoId == userGuid && i.Status == InventoryStatus.Distributed);
            var availableListings = await _context.ClearanceListings
                .CountAsync(l => l.Status == ListingStatus.Open);
            var totalCompleted = await _context.PickupRequests
                .CountAsync(pr => pr.NgoId == userGuid && pr.Status == PickupRequestStatus.Completed);

            return Ok(new
            {
                data = new
                {
                    inStock        = inventoryCount,
                    activeRequests,
                    distributed    = distributedCount,
                    availableFood  = availableListings,
                    totalCompleted
                }
            });
        }
        else if (org.Type == "grocery")
        {
            var activeListings = await _context.ClearanceListings
                .CountAsync(l => l.GroceryId == userGuid && l.Status == ListingStatus.Open);
            var pendingRequests = await _context.PickupRequests
                .CountAsync(pr => pr.GroceryId == userGuid && pr.Status == PickupRequestStatus.Pending);
            var completedPickups = await _context.PickupRequests
                .CountAsync(pr => pr.GroceryId == userGuid && pr.Status == PickupRequestStatus.Completed);
            var foodSaved = await _context.PickupRequests
                .Where(pr => pr.GroceryId == userGuid && pr.Status == PickupRequestStatus.Completed)
                .SumAsync(pr => pr.RequestedQuantity ?? 0);
            var totalListings = await _context.ClearanceListings
                .CountAsync(l => l.GroceryId == userGuid);

            return Ok(new
            {
                data = new
                {
                    activeListings,
                    pendingRequests,
                    completed    = completedPickups,
                    foodSaved,
                    totalListings
                }
            });
        }

        return BadRequest(new { message = "Stats not available for this organization type" });
    }

    /// <summary>
    /// Get the 10 most recent activity items for the current user.
    /// Grocery: recent listings + recent pickup requests received.
    /// NGO: recent pickup requests made + recent inventory received.
    /// </summary>
    [HttpGet("my/activity")]
    public async Task<IActionResult> GetMyActivity()
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;

        if (userId == null || !Guid.TryParse(userId, out var userGuid))
            return Unauthorized();

        var org = await _context.Organizations.FindAsync(userGuid);
        if (org == null) return NotFound();

        var activities = new List<ActivityDto>();

        if (org.Type == "grocery")
        {
            // Recent listings created
            var listings = await _context.ClearanceListings
                .Where(l => l.GroceryId == userGuid)
                .OrderByDescending(l => l.CreatedAt)
                .Take(15)
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

            // Recent pickup requests received (with NGO name via navigation)
            var requests = await _context.PickupRequests
                .Include(pr => pr.Ngo)
                .Where(pr => pr.GroceryId == userGuid)
                .OrderByDescending(pr => pr.RequestedAt)
                .Take(25)
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
                        "Marked ready for pickup",
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
            // Recent pickup requests made (with Grocery name via navigation)
            var requests = await _context.PickupRequests
                .Include(pr => pr.Grocery)
                .Where(pr => pr.NgoId == userGuid)
                .OrderByDescending(pr => pr.RequestedAt)
                .Take(25)
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
                        "Ready for pickup",
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

            // Recent inventory items received
            var inventory = await _context.Inventories
                .Where(i => i.NgoId == userGuid)
                .OrderByDescending(i => i.ReceivedAt)
                .Take(15)
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
            .Take(25)
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

        if (org.Type == "grocery")
        {
            var reviews = await _context.Reviews.Where(r => r.ReviewedId == id).ToListAsync();
            reviewCount = reviews.Count;
            avgRating = reviewCount > 0 ? Math.Round(reviews.Average(r => r.Rating), 1) : 0;
            completedPickups = await _context.PickupRequests
                .CountAsync(pr => pr.GroceryId == id && pr.Status == Domain.Enums.PickupRequestStatus.Completed);
        }
        else if (org.Type == "ngo")
        {
            completedPickups = await _context.PickupRequests
                .CountAsync(pr => pr.NgoId == id && pr.Status == Domain.Enums.PickupRequestStatus.Completed);
        }

        return Ok(new
        {
            data = new
            {
                id = org.Id,
                name = org.Name,
                type = org.Type,
                location = org.Location,
                address = org.Address,
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
                completedPickups
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
                    && !l.IsArchived
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
    /// Upload verification document (business registration, charity certificate, etc.)
    /// Accepts index 1 or 2 for primary/secondary document slots.
    /// </summary>
    [HttpPost("documents")]
    public async Task<IActionResult> UploadDocument(IFormFile document, [FromQuery] int slot = 1)
    {
        if (document == null || document.Length == 0)
            return BadRequest(new { message = "No document provided" });

        var allowedTypes = new[] { "application/pdf", "image/jpeg", "image/png", "image/webp" };
        if (!allowedTypes.Contains(document.ContentType.ToLower()))
            return BadRequest(new { message = "Only PDF and image files are accepted" });

        if (document.Length > 10 * 1024 * 1024)
            return BadRequest(new { message = "File must be under 10 MB" });

        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;
        if (userId == null || !Guid.TryParse(userId, out var userGuid))
            return Unauthorized();

        var org = await _context.Organizations.FindAsync(userGuid);
        if (org == null) return NotFound(new { message = "Organization not found" });

        using var stream = document.OpenReadStream();
        var url = await _storageService.UploadFileAsync(stream, document.FileName, document.ContentType, "documents");

        if (slot == 2)
        {
            org.DocumentUrl2     = url;
        }
        else
        {
            org.DocumentUrl       = url;
            org.DocumentMimeType  = document.ContentType;
        }
        org.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return Ok(new { message = "Document uploaded", data = new { url, slot } });
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