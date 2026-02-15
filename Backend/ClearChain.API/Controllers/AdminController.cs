using ClearChain.Infrastructure.Data;
using ClearChain.API.DTOs.Admin;
using ClearChain.API.DTOs.PickupRequests;
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

    public AdminController(
        ApplicationDbContext context,
        ILogger<AdminController> logger)
    {
        _context = context;
        _logger = logger;
    }

    // GET: api/admin/organizations
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

            // Check if user is admin
            var user = await _context.Organizations.FindAsync(Guid.Parse(userId));
            if (user == null || user.Type.ToLower() != "admin")
            {
                return Forbid();
            }

            var query = _context.Organizations.AsQueryable();

            // Filter by type
            if (!string.IsNullOrEmpty(type))
            {
                query = query.Where(o => o.Type.ToLower() == type.ToLower());
            }

            // Filter by verification status
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

    // PUT: api/admin/organizations/{id}/verify
[HttpPut("{id}/verify")]
public async Task<ActionResult<OrganizationResponse>> VerifyOrganization(Guid id)
{
    try
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return Unauthorized(new { message = "User not authenticated" });
        }

        // Check if user is admin
        var admin = await _context.Organizations.FindAsync(Guid.Parse(userId));
        if (admin == null || admin.Type.ToLower() != "admin")
        {
            return Forbid();
        }

        var organization = await _context.Organizations.FindAsync(id);
        if (organization == null)
        {
            return NotFound(new { message = "Organization not found" });
        }

        organization.Verified = true;
        organization.VerificationStatus = "approved";  // ✅ CHANGED FROM "verified" TO "approved"
        organization.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

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
            VerificationStatus = organization.VerificationStatus ?? "approved",
            CreatedAt = organization.CreatedAt.ToString("o")
        };

        return Ok(new OrganizationResponse
        {
            Message = "Organization verified successfully",
            Data = orgData
        });
    }
    catch (Exception ex)
    {
        _logger.LogError(ex, "Error verifying organization");
        return StatusCode(500, new { message = "An error occurred while verifying the organization" });
    }
}

    // PUT: api/admin/organizations/{id}/unverify
    [HttpPut("organizations/{id}/unverify")]
    public async Task<ActionResult<OrganizationResponse>> UnverifyOrganization(Guid id)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            // Check if user is admin
            var admin = await _context.Organizations.FindAsync(Guid.Parse(userId));
            if (admin == null || admin.Type.ToLower() != "admin")
            {
                return Forbid();
            }

            var organization = await _context.Organizations.FindAsync(id);
            if (organization == null)
            {
                return NotFound(new { message = "Organization not found" });
            }

            organization.Verified = false;
            organization.VerificationStatus = "pending";
            organization.UpdatedAt = DateTime.UtcNow;

            await _context.SaveChangesAsync();

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
                VerificationStatus = organization.VerificationStatus ?? "pending",
                CreatedAt = organization.CreatedAt.ToString("o")
            };

            return Ok(new OrganizationResponse
            {
                Message = "Organization unverified successfully",
                Data = orgData
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error unverifying organization");
            return StatusCode(500, new { message = "An error occurred while unverifying the organization" });
        }
    }

    // GET: api/admin/statistics
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

        // Check if user is admin
        var user = await _context.Organizations.FindAsync(Guid.Parse(userId));
        if (user == null || user.Type.ToLower() != "admin")
        {
            return Forbid();
        }

        // Organization stats
        var totalOrgs = await _context.Organizations.CountAsync();
        var totalGroceries = await _context.Organizations.CountAsync(o => o.Type.ToLower() == "grocery");
        var totalNgos = await _context.Organizations.CountAsync(o => o.Type.ToLower() == "ngo");
        var verifiedOrgs = await _context.Organizations.CountAsync(o => o.Verified);
        var unverifiedOrgs = totalOrgs - verifiedOrgs;

        // Listing stats
        var totalListings = await _context.ClearanceListings.CountAsync();
        var activeListings = await _context.ClearanceListings.CountAsync(l => l.Status.ToLower() == "open");
        var reservedListings = await _context.ClearanceListings.CountAsync(l => l.Status.ToLower() == "reserved");

        // Pickup request stats
        var totalRequests = await _context.PickupRequests.CountAsync();
        var pendingRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == "pending");
        var approvedRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == "approved");
        var readyRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == "ready");           // ✅ ADD
        var rejectedRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == "rejected");     // ✅ ADD
        var completedRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == "completed");
        var cancelledRequests = await _context.PickupRequests.CountAsync(pr => pr.Status == "cancelled");   // ✅ ADD

        // Total food saved (sum of completed request quantities)
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
            ReadyRequests = readyRequests,           // ✅ ADD
            RejectedRequests = rejectedRequests,     // ✅ ADD
            CompletedRequests = completedRequests,
            CancelledRequests = cancelledRequests,   // ✅ ADD
            
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

// GET: api/admin/pickuprequests
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

        // Check if user is admin
        var user = await _context.Organizations.FindAsync(Guid.Parse(userId));
        if (user == null || user.Type.ToLower() != "admin")
        {
            return Forbid();
        }

        // Get ALL pickup requests (admin can see all)
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