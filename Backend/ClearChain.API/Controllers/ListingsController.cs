using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using ClearChain.API.DTOs.Listings;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ListingsController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<ListingsController> _logger;

    public ListingsController(
        ApplicationDbContext context,
        ILogger<ListingsController> logger)
    {
        _context = context;
        _logger = logger;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER: Convert Listing Entity to DTO
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ListingData MapListingToDto(ClearanceListing listing, ListingGroup? group = null)
    {
        var dto = new ListingData
        {
            Id = listing.Id.ToString(),
            GroceryId = listing.GroceryId.ToString(),
            GroceryName = listing.Grocery?.Name ?? "",
            Title = listing.ProductName,
            Description = listing.Notes ?? "",
            Category = listing.Category,
            Quantity = (int)listing.Quantity,
            Unit = listing.Unit,
            ExpiryDate = listing.ExpirationDate?.ToString("yyyy-MM-dd") ?? "",
            PickupTimeStart = listing.PickupTimeStart?.ToString(@"hh\:mm") ?? "09:00",
            PickupTimeEnd = listing.PickupTimeEnd?.ToString(@"hh\:mm") ?? "17:00",
            Status = listing.Status,
            ImageUrl = listing.PhotoUrl,
            Location = listing.Grocery?.Location ?? "",
            CreatedAt = listing.CreatedAt.ToString("o"),
            
            // NEW: Group tracking
            GroupId = listing.GroupId?.ToString(),
            SplitReason = listing.SplitReason,
            RelatedRequestId = listing.RelatedRequestId?.ToString(),
            SplitIndex = listing.SplitIndex
        };

        // Include group summary if group is provided
        if (group != null)
        {
            dto.GroupSummary = new ListingGroupSummary
            {
                GroupId = group.Id.ToString(),
                OriginalQuantity = (int)group.OriginalQuantity,
                TotalReserved = (int)group.TotalReserved,
                TotalAvailable = (int)group.TotalAvailable,
                ChildListingsCount = group.ChildListings?.Count ?? 0
            };
        }

        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET: api/listings
    // ═══════════════════════════════════════════════════════════════════════════
    
    [HttpGet]
    [AllowAnonymous]
    public async Task<ActionResult<ListingsResponse>> GetAllListings(
        [FromQuery] string? status = null,
        [FromQuery] string? category = null)
    {
        try
        {
            var query = _context.ClearanceListings
                .Include(l => l.Grocery)
                .Include(l => l.Group)
                .AsQueryable();

            if (!string.IsNullOrEmpty(status))
            {
                query = query.Where(l => l.Status.ToLower() == status.ToLower());
            }

            if (!string.IsNullOrEmpty(category))
            {
                query = query.Where(l => l.Category.ToUpper() == category.ToUpper());
            }

            var listings = await query
                .OrderByDescending(l => l.CreatedAt)
                .ToListAsync();

            var listingDtos = listings.Select(l => MapListingToDto(l, l.Group)).ToList();

            return Ok(new ListingsResponse
            {
                Message = "Listings retrieved successfully",
                Data = listingDtos
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting listings");
            return StatusCode(500, new { message = "An error occurred while retrieving listings" });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET: api/listings/grocery/my
    // ═══════════════════════════════════════════════════════════════════════════
    
    [HttpGet("grocery/my")]
    [Authorize]
    public async Task<ActionResult<ListingsResponse>> GetMyListings()
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var listings = await _context.ClearanceListings
                .Include(l => l.Grocery)
                .Include(l => l.Group)
                .Where(l => l.GroceryId.ToString() == userId)
                .OrderByDescending(l => l.CreatedAt)
                .ToListAsync();

            var listingDtos = listings.Select(l => MapListingToDto(l, l.Group)).ToList();

            return Ok(new ListingsResponse
            {
                Message = "Your listings retrieved successfully",
                Data = listingDtos
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting user listings");
            return StatusCode(500, new { message = "An error occurred while retrieving your listings" });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POST: api/listings (UPDATED - Creates ListingGroup + Listing)
    // ═══════════════════════════════════════════════════════════════════════════
    
    [HttpPost]
    [Authorize]
    public async Task<ActionResult<ListingResponse>> CreateListing(
        [FromBody] CreateListingRequest request)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var grocery = await _context.Organizations
                .FirstOrDefaultAsync(o => o.Id.ToString() == userId);

            if (grocery == null)
            {
                return NotFound(new { message = "Grocery store not found" });
            }

            var expiryDate = DateTime.Parse(request.ExpiryDate);
            var expiryDateUtc = DateTime.SpecifyKind(expiryDate, DateTimeKind.Utc);
            var clearanceDeadlineUtc = expiryDateUtc.AddDays(1);

            TimeSpan? pickupTimeStart = null;
            TimeSpan? pickupTimeEnd = null;

            if (!string.IsNullOrEmpty(request.PickupTimeStart) &&
                TimeSpan.TryParse(request.PickupTimeStart, out var startTime))
            {
                pickupTimeStart = startTime;
            }

            if (!string.IsNullOrEmpty(request.PickupTimeEnd) &&
                TimeSpan.TryParse(request.PickupTimeEnd, out var endTime))
            {
                pickupTimeEnd = endTime;
            }

            // ═══════════════════════════════════════════════════════════════
            // CREATE LISTING GROUP (NEW)
            // ═══════════════════════════════════════════════════════════════
            
            var groupId = Guid.NewGuid();
            var listingId = Guid.NewGuid();

            var listingGroup = new ListingGroup
            {
                Id = groupId,
                OriginalListingId = listingId,
                GroceryId = Guid.Parse(userId),
                ProductName = request.Title,
                Category = request.Category.ToUpper(),
                Unit = request.Unit,
                Notes = request.Description,
                PhotoUrl = request.ImageUrl,
                ExpirationDate = expiryDateUtc,
                ClearanceDeadline = clearanceDeadlineUtc,
                PickupTimeStart = pickupTimeStart,
                PickupTimeEnd = pickupTimeEnd,
                OriginalQuantity = request.Quantity,
                TotalAvailable = request.Quantity,
                TotalReserved = 0,
                TotalCompleted = 0,
                IsFullyConsumed = false,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            // ═══════════════════════════════════════════════════════════════
            // CREATE INITIAL LISTING
            // ═══════════════════════════════════════════════════════════════
            
            var listing = new ClearanceListing
            {
                Id = listingId,
                GroupId = groupId,
                GroceryId = Guid.Parse(userId),
                ProductName = request.Title,
                Category = request.Category.ToUpper(),
                Quantity = request.Quantity,
                Unit = request.Unit,
                ExpirationDate = expiryDateUtc,
                ClearanceDeadline = clearanceDeadlineUtc,
                Notes = request.Description,
                Status = "open",
                PhotoUrl = request.ImageUrl,
                PickupTimeStart = pickupTimeStart,
                PickupTimeEnd = pickupTimeEnd,
                SplitReason = "new_listing",
                SplitIndex = 0,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            _context.ListingGroups.Add(listingGroup);
            _context.ClearanceListings.Add(listing);
            await _context.SaveChangesAsync();

            var listingDto = MapListingToDto(listing, listingGroup);

            return CreatedAtAction(
                nameof(GetListingById),
                new { id = listing.Id },
                new ListingResponse
                {
                    Message = "Listing created successfully",
                    Data = listingDto
                });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error creating listing");
            return StatusCode(500, new { message = "An error occurred while creating the listing" });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET: api/listings/{id}
    // ═══════════════════════════════════════════════════════════════════════════
    
    [HttpGet("{id}")]
    public async Task<ActionResult<ListingResponse>> GetListingById(Guid id)
    {
        try
        {
            var listing = await _context.ClearanceListings
                .Include(l => l.Grocery)
                .Include(l => l.Group)
                .FirstOrDefaultAsync(l => l.Id == id);

            if (listing == null)
            {
                return NotFound(new { message = "Listing not found" });
            }

            var listingDto = MapListingToDto(listing, listing.Group);

            return Ok(new ListingResponse
            {
                Message = "Listing retrieved successfully",
                Data = listingDto
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting listing by id");
            return StatusCode(500, new { message = "An error occurred while retrieving the listing" });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE: api/listings/{id} (UPDATED - Validates status & updates group)
    // ═══════════════════════════════════════════════════════════════════════════
    
[HttpDelete("{id}")]
[Authorize]
public async Task<ActionResult<ListingResponse>> DeleteListing(Guid id)
{
    try
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return Unauthorized(new { message = "User not authenticated" });
        }

        var listing = await _context.ClearanceListings
            .Include(l => l.Grocery)
            .Include(l => l.Group)
            .FirstOrDefaultAsync(l => l.Id == id && l.GroceryId.ToString() == userId);

        if (listing == null)
        {
            return NotFound(new { message = "Listing not found or you don't have permission to delete it" });
        }

        if (listing.Status != "open")
        {
            var statusMessage = listing.Status switch
            {
                "reserved" => "Cannot delete reserved listing. Wait for pickup completion or cancellation.",
                "expired" => "Cannot delete expired listing.",
                _ => $"Cannot delete listing with status: {listing.Status}"
            };
            
            return BadRequest(new { message = statusMessage });
        }


        // Now proceed with deletion
        if (listing.GroupId.HasValue && listing.Group != null)
        {
            listing.Group.TotalAvailable -= listing.Quantity;
            listing.Group.UpdatedAt = DateTime.UtcNow;
            
            var remainingChildren = await _context.ClearanceListings
                .Where(l => l.GroupId == listing.GroupId && l.Id != listing.Id)
                .CountAsync();
            
            if (remainingChildren == 0)
            {
                _context.ListingGroups.Remove(listing.Group);
            }
        }

        _context.ClearanceListings.Remove(listing);
        await _context.SaveChangesAsync();

        var listingData = MapListingToDto(listing, listing.Group);

        // ✅ Return ListingResponse with full data
        return Ok(new ListingResponse
        {
            Message = "Listing deleted successfully",
            Data = listingData
        });
    }
    catch (Exception ex)
    {
        _logger.LogError(ex, "Error deleting listing");
        return StatusCode(500, new { message = "An error occurred while deleting the listing" });
    }
}

// In ListingsController.cs

// ═══════════════════════════════════════════════════════════════════════════
// PUT: api/listings/{id}/quantity - Update quantity of AVAILABLE listing
// ═══════════════════════════════════════════════════════════════════════════

[HttpPut("{id}/quantity")]
[Authorize]
public async Task<ActionResult<ListingResponse>> UpdateListingQuantity(
    Guid id,
    [FromBody] UpdateListingQuantityRequest request)
{
    try
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return Unauthorized(new { message = "User not authenticated" });
        }

        var listing = await _context.ClearanceListings
            .Include(l => l.Grocery)
            .Include(l => l.Group)
            .FirstOrDefaultAsync(l => l.Id == id && l.GroceryId.ToString() == userId);

        if (listing == null)
        {
            return NotFound(new { message = "Listing not found or you don't have permission to edit it" });
        }

        // ═══════════════════════════════════════════════════════════════
        // VALIDATION: Can only update AVAILABLE listings
        // ═══════════════════════════════════════════════════════════════
        
        if (listing.Status != "open")
        {
            return BadRequest(new { 
                message = "Can only update quantity of available listings" 
            });
        }

        if (request.NewQuantity <= 0)
        {
            return BadRequest(new { 
                message = "Quantity must be greater than 0" 
            });
        }

        // ═══════════════════════════════════════════════════════════════
        // UPDATE LISTING & GROUP
        // ═══════════════════════════════════════════════════════════════
        
        var oldQuantity = listing.Quantity;
        var difference = request.NewQuantity - oldQuantity;

        listing.Quantity = request.NewQuantity;
        listing.UpdatedAt = DateTime.UtcNow;

        // Update group totals
        if (listing.GroupId.HasValue && listing.Group != null)
        {
            listing.Group.TotalAvailable += difference;
            listing.Group.UpdatedAt = DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();

        var listingDto = MapListingToDto(listing, listing.Group);

        return Ok(new ListingResponse
        {
            Message = "Quantity updated successfully",
            Data = listingDto
        });
    }
    catch (Exception ex)
    {
        _logger.LogError(ex, "Error updating listing quantity");
        return StatusCode(500, new { 
            message = "An error occurred while updating the quantity" 
        });
    }
}
}