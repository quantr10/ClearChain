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

    // GET: api/listings
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

            var listingDtos = listings.Select(l => new ListingData
            {
                Id = l.Id.ToString(),
                GroceryId = l.GroceryId.ToString(),
                GroceryName = l.Grocery?.Name ?? "",
                Title = l.ProductName,
                Description = l.Notes ?? "",
                Category = l.Category,
                Quantity = (int)l.Quantity,
                Unit = l.Unit,
                ExpiryDate = l.ExpirationDate?.ToString("yyyy-MM-dd") ?? "",
                PickupTimeStart = l.PickupTimeStart?.ToString(@"hh\:mm") ?? "09:00",  // ✅ Real value
                PickupTimeEnd = l.PickupTimeEnd?.ToString(@"hh\:mm") ?? "17:00",
                Status = l.Status,
                ImageUrl = l.PhotoUrl,
                Location = l.Grocery?.Location ?? "",
                CreatedAt = l.CreatedAt.ToString("o")
            }).ToList();

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

    // GET: api/listings/grocery/my
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
                .Where(l => l.GroceryId.ToString() == userId)
                .OrderByDescending(l => l.CreatedAt)
                .ToListAsync();

            var listingDtos = listings.Select(l => new ListingData
            {
                Id = l.Id.ToString(),
                GroceryId = l.GroceryId.ToString(),
                GroceryName = l.Grocery?.Name ?? "",
                Title = l.ProductName,
                Description = l.Notes ?? "",
                Category = l.Category,
                Quantity = (int)l.Quantity,
                Unit = l.Unit,
                ExpiryDate = l.ExpirationDate?.ToString("yyyy-MM-dd") ?? "",
                PickupTimeStart = l.PickupTimeStart?.ToString(@"hh\:mm") ?? "09:00",  // ✅ Real value
                PickupTimeEnd = l.PickupTimeEnd?.ToString(@"hh\:mm") ?? "17:00",
                Status = l.Status,
                ImageUrl = l.PhotoUrl,
                Location = l.Grocery?.Location ?? "",
                CreatedAt = l.CreatedAt.ToString("o")
            }).ToList();

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

    // POST: api/listings
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

            // Parse and convert dates to UTC
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

            var listing = new ClearanceListing
            {
                Id = Guid.NewGuid(),
                GroceryId = Guid.Parse(userId),
                ProductName = request.Title,
                Category = request.Category.ToUpper(), // Direct mapping, no helper needed
                Quantity = request.Quantity,
                Unit = request.Unit,
                ExpirationDate = expiryDateUtc,
                ClearanceDeadline = clearanceDeadlineUtc,
                Notes = request.Description,
                Status = "open",
                PhotoUrl = request.ImageUrl,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow,
                PickupTimeStart = pickupTimeStart,
                PickupTimeEnd = pickupTimeEnd,
            };

            _context.ClearanceListings.Add(listing);
            await _context.SaveChangesAsync();

            var listingDto = new ListingData
            {
                Id = listing.Id.ToString(),
                GroceryId = listing.GroceryId.ToString(),
                GroceryName = grocery.Name,
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
                Location = grocery.Location,
                CreatedAt = listing.CreatedAt.ToString("o")
            };

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

    // GET: api/listings/{id}
    [HttpGet("{id}")]
    public async Task<ActionResult<ListingResponse>> GetListingById(Guid id)
    {
        try
        {
            var listing = await _context.ClearanceListings
                .Include(l => l.Grocery)
                .FirstOrDefaultAsync(l => l.Id == id);

            if (listing == null)
            {
                return NotFound(new { message = "Listing not found" });
            }

            var listingDto = new ListingData
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
                PickupTimeStart = "09:00",
                PickupTimeEnd = "17:00",
                Status = listing.Status,
                ImageUrl = listing.PhotoUrl,
                Location = listing.Grocery?.Location ?? "",
                CreatedAt = listing.CreatedAt.ToString("o")
            };

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

    // DELETE: api/listings/{id}
    [HttpDelete("{id}")]
    [Authorize]
    public async Task<ActionResult> DeleteListing(Guid id)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var listing = await _context.ClearanceListings
                .FirstOrDefaultAsync(l => l.Id == id && l.GroceryId.ToString() == userId);

            if (listing == null)
            {
                return NotFound(new { message = "Listing not found or you don't have permission to delete it" });
            }

            _context.ClearanceListings.Remove(listing);
            await _context.SaveChangesAsync();

            return Ok(new
            {
                message = "Listing deleted successfully",
                data = new { id = id.ToString() }
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error deleting listing");
            return StatusCode(500, new { message = "An error occurred while deleting the listing" });
        }
    }
}