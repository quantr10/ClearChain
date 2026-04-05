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

    public OrganizationsController(IOrganizationService organizationService, ApplicationDbContext context)
    {
        _organizationService = organizationService;
        _context = context;
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
    /// Get dashboard stats for the current user (NGO or Grocery)
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

            return Ok(new
            {
                data = new
                {
                    inStock = inventoryCount,
                    activeRequests,
                    distributed = distributedCount,
                    availableFood = availableListings
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

            return Ok(new
            {
                data = new
                {
                    activeListings,
                    pendingRequests,
                    completed = completedPickups,
                    foodSaved
                }
            });
        }

        return BadRequest(new { message = "Stats not available for this organization type" });
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