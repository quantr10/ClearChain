using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using ClearChain.API.DTOs.PickupRequests;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class PickupRequestsController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<PickupRequestsController> _logger;

    public PickupRequestsController(
        ApplicationDbContext context,
        ILogger<PickupRequestsController> logger)
    {
        _context = context;
        _logger = logger;
    }

    // POST: api/pickuprequests
// POST: api/pickuprequests
[HttpPost]
public async Task<ActionResult<PickupRequestResponse>> CreatePickupRequest(
    [FromBody] CreatePickupRequestRequest request)
{
    try
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return Unauthorized(new { message = "User not authenticated" });
        }

        // Get NGO details
        var ngo = await _context.Organizations
            .FirstOrDefaultAsync(o => o.Id.ToString() == userId);

        if (ngo == null || ngo.Type.ToLower() != "ngo")
        {
            return BadRequest(new { message = "Only NGOs can create pickup requests" });
        }

        // Get listing
        var listing = await _context.ClearanceListings
            .Include(l => l.Grocery)
            .FirstOrDefaultAsync(l => l.Id.ToString() == request.ListingId);

        if (listing == null)
        {
            return NotFound(new { message = "Listing not found" });
        }

        if (listing.Status.ToLower() != "open")
        {
            return BadRequest(new { message = "Listing is not available" });
        }

        // Validate quantity
        if (request.RequestedQuantity > listing.Quantity)
        {
            return BadRequest(new { message = $"Requested quantity exceeds available quantity ({listing.Quantity})" });
        }

        // Parse pickup date
        var pickupDate = DateTime.Parse(request.PickupDate);
        var pickupDateUtc = DateTime.SpecifyKind(pickupDate, DateTimeKind.Utc);

        // ✅ Validate pickup date is not in the past
        if (pickupDateUtc.Date < DateTime.UtcNow.Date)
        {
            return BadRequest(new { message = "Pickup date cannot be in the past" });
        }

        // ✅ Validate pickup date is before expiry date
        if (listing.ExpirationDate.HasValue)
        {
            var expiryDate = listing.ExpirationDate.Value.Date;
            if (pickupDateUtc.Date > expiryDate)
            {
                return BadRequest(new { message = $"Pickup date cannot be after expiry date ({expiryDate:yyyy-MM-dd})" });
            }
        }

        // Create pickup request
        var pickupRequest = new PickupRequest
        {
            Id = Guid.NewGuid(),
            NgoId = Guid.Parse(userId),
            GroceryId = listing.GroceryId,
            ListingId = listing.Id,
            PickupDate = pickupDateUtc,
            Status = "pending",
            RequestedAt = DateTime.UtcNow,
            RequestedQuantity = request.RequestedQuantity,
            PickupTime = request.PickupTime,
            Notes = request.Notes
        };

        _context.PickupRequests.Add(pickupRequest);

        // ✅ ALWAYS reduce quantity first
        listing.Quantity -= request.RequestedQuantity;
        
        // Then check if all taken
        if (listing.Quantity <= 0)
        {
            // Taking all - mark as reserved
            listing.Status = "reserved";
            listing.PickupRequestId = pickupRequest.Id;
            listing.Quantity = 0;
        }
        else
        {
            // Still has remaining - keep open
            listing.Status = "open";
        }
        
        listing.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        var responseData = new PickupRequestData
        {
            Id = pickupRequest.Id.ToString(),
            ListingId = listing.Id.ToString(),
            NgoId = ngo.Id.ToString(),
            NgoName = ngo.Name,
            GroceryId = listing.GroceryId.ToString(),
            GroceryName = listing.Grocery?.Name ?? "",
            Status = pickupRequest.Status,
            RequestedQuantity = request.RequestedQuantity,
            PickupDate = pickupRequest.PickupDate.ToString("yyyy-MM-dd"),
            PickupTime = request.PickupTime,
            Notes = request.Notes,
            ListingTitle = listing.ProductName,
            ListingCategory = listing.Category,
            CreatedAt = pickupRequest.RequestedAt.ToString("o")
        };

        return CreatedAtAction(
            nameof(GetPickupRequestById),
            new { id = pickupRequest.Id },
            new PickupRequestResponse
            {
                Message = "Pickup request created successfully",
                Data = responseData
            });
    }
    catch (Exception ex)
    {
        _logger.LogError(ex, "Error creating pickup request");
        return StatusCode(500, new { message = "An error occurred while creating the pickup request" });
    }
}

    // GET: api/pickuprequests/ngo/my
    [HttpGet("ngo/my")]
    public async Task<ActionResult<PickupRequestsResponse>> GetMyPickupRequests()
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var pickupRequests = await _context.PickupRequests
                .Where(pr => pr.NgoId.ToString() == userId)
                .OrderByDescending(pr => pr.RequestedAt)
                .ToListAsync();

            var responseData = new List<PickupRequestData>();

            foreach (var pr in pickupRequests)
            {
                var ngo = await _context.Organizations.FindAsync(pr.NgoId);
                var grocery = await _context.Organizations.FindAsync(pr.GroceryId);
                
                // Get listing by stored ListingId
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

    // GET: api/pickuprequests/grocery/my
    [HttpGet("grocery/my")]
    public async Task<ActionResult<PickupRequestsResponse>> GetGroceryPickupRequests()
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var pickupRequests = await _context.PickupRequests
                .Where(pr => pr.GroceryId.ToString() == userId)
                .OrderByDescending(pr => pr.RequestedAt)
                .ToListAsync();

            var responseData = new List<PickupRequestData>();

            foreach (var pr in pickupRequests)
            {
                var ngo = await _context.Organizations.FindAsync(pr.NgoId);
                var grocery = await _context.Organizations.FindAsync(pr.GroceryId);
                var listing = await _context.ClearanceListings
                    .FirstOrDefaultAsync(l => l.PickupRequestId == pr.Id);

                responseData.Add(new PickupRequestData
                {
                    Id = pr.Id.ToString(),
                    ListingId = listing?.Id.ToString() ?? "",
                    NgoId = pr.NgoId.ToString(),
                    NgoName = ngo?.Name ?? "",
                    GroceryId = pr.GroceryId.ToString(),
                    GroceryName = grocery?.Name ?? "",
                    Status = pr.Status,
                    RequestedQuantity = (int)(listing?.Quantity ?? 0),
                    PickupDate = pr.PickupDate.ToString("yyyy-MM-dd"),
                    PickupTime = "09:00",
                    Notes = null,
                    ListingTitle = listing?.ProductName ?? "",
                    ListingCategory = listing?.Category ?? "",
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
            _logger.LogError(ex, "Error getting grocery pickup requests");
            return StatusCode(500, new { message = "An error occurred while retrieving pickup requests" });
        }
    }

    // GET: api/pickuprequests/{id}
    [HttpGet("{id}")]
    public async Task<ActionResult<PickupRequestResponse>> GetPickupRequestById(Guid id)
    {
        try
        {
            var pr = await _context.PickupRequests.FindAsync(id);
            if (pr == null)
            {
                return NotFound(new { message = "Pickup request not found" });
            }

            var ngo = await _context.Organizations.FindAsync(pr.NgoId);
            var grocery = await _context.Organizations.FindAsync(pr.GroceryId);
            var listing = await _context.ClearanceListings
                .FirstOrDefaultAsync(l => l.PickupRequestId == pr.Id);

            var responseData = new PickupRequestData
            {
                Id = pr.Id.ToString(),
                ListingId = listing?.Id.ToString() ?? "",
                NgoId = pr.NgoId.ToString(),
                NgoName = ngo?.Name ?? "",
                GroceryId = pr.GroceryId.ToString(),
                GroceryName = grocery?.Name ?? "",
                Status = pr.Status,
                RequestedQuantity = (int)(listing?.Quantity ?? 0),
                PickupDate = pr.PickupDate.ToString("yyyy-MM-dd"),
                PickupTime = "09:00",
                Notes = null,
                ListingTitle = listing?.ProductName ?? "",
                ListingCategory = listing?.Category ?? "",
                CreatedAt = pr.RequestedAt.ToString("o")
            };

            return Ok(new PickupRequestResponse
            {
                Message = "Pickup request retrieved successfully",
                Data = responseData
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting pickup request");
            return StatusCode(500, new { message = "An error occurred while retrieving the pickup request" });
        }
    }

    // DELETE: api/pickuprequests/{id}
[HttpDelete("{id}")]
public async Task<ActionResult> CancelPickupRequest(Guid id)
{
    try
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return Unauthorized(new { message = "User not authenticated" });
        }

        var pr = await _context.PickupRequests
            .FirstOrDefaultAsync(p => p.Id == id && p.NgoId.ToString() == userId);

        if (pr == null)
        {
            return NotFound(new { message = "Pickup request not found" });
        }

        // Get related data before deleting
        var ngo = await _context.Organizations.FindAsync(pr.NgoId);
        var grocery = await _context.Organizations.FindAsync(pr.GroceryId);
        var listing = pr.ListingId.HasValue 
            ? await _context.ClearanceListings.FindAsync(pr.ListingId.Value)
            : null;

        // Save request data for response
        var responseData = new PickupRequestData
        {
            Id = pr.Id.ToString(),
            ListingId = pr.ListingId?.ToString() ?? "",
            NgoId = pr.NgoId.ToString(),
            NgoName = ngo?.Name ?? "",
            GroceryId = pr.GroceryId.ToString(),
            GroceryName = grocery?.Name ?? "",
            Status = "cancelled",
            RequestedQuantity = pr.RequestedQuantity ?? 0,
            PickupDate = pr.PickupDate.ToString("yyyy-MM-dd"),
            PickupTime = pr.PickupTime ?? "09:00",
            Notes = pr.Notes ?? "",
            ListingTitle = listing?.ProductName ?? "",
            ListingCategory = listing?.Category ?? "",
            CreatedAt = pr.RequestedAt.ToString("o")
        };

        // Find listing and restore quantity
        if (pr.ListingId.HasValue && listing != null)
        {
            // Restore quantity
            listing.Quantity += pr.RequestedQuantity ?? 0;
            listing.Status = "open";
            listing.PickupRequestId = null;
            listing.UpdatedAt = DateTime.UtcNow;
        }

        _context.PickupRequests.Remove(pr);
        await _context.SaveChangesAsync();

        return Ok(new PickupRequestResponse
        {
            Message = "Pickup request cancelled successfully",
            Data = responseData
        });
    }
    catch (Exception ex)
    {
        _logger.LogError(ex, "Error cancelling pickup request");
        return StatusCode(500, new { message = ex.Message });
    }
}
}