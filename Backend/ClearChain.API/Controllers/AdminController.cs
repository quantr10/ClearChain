using ClearChain.Infrastructure.Data;
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
                CreatedAt = o.CreatedAt.ToString("o")
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

    [HttpGet("statistics")]
    public async Task<ActionResult<AdminStatsResponse>> GetStatistics()
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
            var activeListings = await _context.ClearanceListings.CountAsync(l => l.Status.ToLower() == "open");
            var reservedListings = await _context.ClearanceListings.CountAsync(l => l.Status.ToLower() == "reserved");

            var totalRequests = await _context.PickupRequests.CountAsync();
            var pendingRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == "pending");
            var approvedRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == "approved");
            var readyRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == "ready");
            var rejectedRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == "rejected");
            var completedRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == "completed");
            var cancelledRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == "cancelled");

            var totalFoodSaved = await _context.PickupRequests
                .Where(pr => pr.Status == "completed")
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
                    Status = pr.Status ?? "pending",
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
}