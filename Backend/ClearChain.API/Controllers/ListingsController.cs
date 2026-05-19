using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
using ClearChain.API.DTOs.Listings;
using ClearChain.API.Services;
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
    private readonly IListingNotificationService _listingNotificationService;
    private readonly IPushNotificationService _pushNotificationService;

    public ListingsController(
        ApplicationDbContext context,
        ILogger<ListingsController> logger,
        IListingNotificationService listingNotificationService,
        IPushNotificationService pushNotificationService)
    {
        _context = context;
        _logger = logger;
        _listingNotificationService = listingNotificationService;
        _pushNotificationService = pushNotificationService;
    }

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
            Status = listing.Status.ToString().ToLower(),
            ImageUrl = listing.PhotoUrl,
            Location = listing.Grocery?.Location ?? "",
            CreatedAt = listing.CreatedAt.ToString("o"),
            GroupId = listing.GroupId?.ToString(),
            SplitReason = listing.SplitReason,
            RelatedRequestId = listing.RelatedRequestId?.ToString(),
            SplitIndex = listing.SplitIndex,
            ViewCount = listing.ViewCount,
            IsArchived = listing.IsArchived,
            ArchivedAt = listing.ArchivedAt?.ToString("o"),
            ImageUrls = string.IsNullOrEmpty(listing.PhotoUrl)
                ? new List<string>()
                : listing.PhotoUrl.StartsWith("[")
                    ? System.Text.Json.JsonSerializer.Deserialize<List<string>>(listing.PhotoUrl) ?? new List<string>()
                    : new List<string> { listing.PhotoUrl },
            GroceryLatitude  = listing.Grocery?.Latitude,
            GroceryLongitude = listing.Grocery?.Longitude,
            GroceryHours     = listing.Grocery?.Hours
        };

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

    // ═══════════════════════════════════════════════════════════════════
    // UPDATED: GetAllListings with geospatial filter (Part 2)
    // ═══════════════════════════════════════════════════════════════════
    [HttpGet]
    [AllowAnonymous]
    public async Task<ActionResult<ListingsResponse>> GetAllListings(
        [FromQuery] string? status = null,
        [FromQuery] string? category = null,
        [FromQuery] string? groceryId = null,
        [FromQuery] double? lat = null,
        [FromQuery] double? lng = null,
        [FromQuery] double? radiusKm = null,
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 50)
    {
        try
        {
            var query = _context.ClearanceListings
                .Include(l => l.Grocery)
                .Include(l => l.Group)
                .AsQueryable();

            if (!string.IsNullOrEmpty(status) && Enum.TryParse<ListingStatus>(status, ignoreCase: true, out var statusEnum))
            {
                query = query.Where(l => l.Status == statusEnum);
            }

            if (!string.IsNullOrEmpty(category))
            {
                query = query.Where(l => l.Category.ToUpper() == category.ToUpper());
            }

            if (!string.IsNullOrEmpty(groceryId) && Guid.TryParse(groceryId, out var groceryGuid))
            {
                query = query.Where(l => l.GroceryId == groceryGuid);
            }

            // Bounding box pre-filter: eliminates distant rows before Haversine in memory
            if (lat.HasValue && lng.HasValue && radiusKm.HasValue)
            {
                var deltaLat = radiusKm.Value / 111.0;
                var deltaLng = radiusKm.Value / (111.0 * Math.Cos(lat.Value * Math.PI / 180.0));
                var minLat = lat.Value - deltaLat;
                var maxLat = lat.Value + deltaLat;
                var minLng = lng.Value - deltaLng;
                var maxLng = lng.Value + deltaLng;

                query = query.Where(l =>
                    l.Grocery != null &&
                    l.Grocery.Latitude != null && l.Grocery.Longitude != null &&
                    l.Grocery.Latitude >= minLat && l.Grocery.Latitude <= maxLat &&
                    l.Grocery.Longitude >= minLng && l.Grocery.Longitude <= maxLng);
            }

            var listings = await query
                .OrderByDescending(l => l.CreatedAt)
                .ToListAsync();

            // Map to DTOs and calculate distance if location provided
            var listingDtos = listings.Select(l =>
            {
                var dto = MapListingToDto(l, l.Group);

                if (lat.HasValue && lng.HasValue &&
                    l.Grocery?.Latitude != null && l.Grocery?.Longitude != null)
                {
                    dto.DistanceKm = CalculateHaversineDistance(
                        lat.Value, lng.Value,
                        l.Grocery.Latitude.Value, l.Grocery.Longitude.Value
                    );
                }

                return dto;
            }).ToList();

            // Filter by radius if specified
            if (lat.HasValue && lng.HasValue && radiusKm.HasValue)
            {
                var withinRadius = listingDtos
                    .Where(l => l.DistanceKm.HasValue && l.DistanceKm.Value <= radiusKm.Value)
                    .OrderBy(l => l.DistanceKm)
                    .ToList();

                var noCoordinates = listingDtos
                    .Where(l => !l.DistanceKm.HasValue)
                    .ToList();

                listingDtos = withinRadius.Concat(noCoordinates).ToList();
            }

            var clampedPage = Math.Max(1, page);
            var clampedPageSize = Math.Clamp(pageSize, 1, 100);
            var total = listingDtos.Count;
            var paged = listingDtos
                .Skip((clampedPage - 1) * clampedPageSize)
                .Take(clampedPageSize)
                .ToList();

            return Ok(new ListingsResponse
            {
                Message = "Listings retrieved successfully",
                Data = paged,
                Total = total,
                Page = clampedPage,
                PageSize = clampedPageSize,
                TotalPages = (int)Math.Ceiling((double)total / clampedPageSize)
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting listings");
            return StatusCode(500, new { message = "An error occurred while retrieving listings" });
        }
    }

    // ═══ NEW: Haversine distance calculation (Part 2) ═══
    private static double CalculateHaversineDistance(
        double lat1, double lon1, double lat2, double lon2)
    {
        const double R = 6371; // Earth's radius in km
        var dLat = ToRadians(lat2 - lat1);
        var dLon = ToRadians(lon2 - lon1);
        var a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                Math.Cos(ToRadians(lat1)) * Math.Cos(ToRadians(lat2)) *
                Math.Sin(dLon / 2) * Math.Sin(dLon / 2);
        var c = 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
        return Math.Round(R * c, 1);
    }

    private static double ToRadians(double degrees) => degrees * Math.PI / 180;

    // ═══════════════════════════════════════════════════════════════════
    // Remaining endpoints — UNCHANGED from original
    // ═══════════════════════════════════════════════════════════════════

    [HttpGet("grocery/my")]
    [Authorize]
    public async Task<ActionResult<ListingsResponse>> GetMyListings(
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 20)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var clampedPage = Math.Max(1, page);
            var clampedPageSize = Math.Clamp(pageSize, 1, 100);

            var baseQuery = _context.ClearanceListings
                .Where(l => l.GroceryId.ToString() == userId);

            var total = await baseQuery.CountAsync();

            var listings = await baseQuery
                .Include(l => l.Grocery)
                .Include(l => l.Group)
                .OrderByDescending(l => l.CreatedAt)
                .Skip((clampedPage - 1) * clampedPageSize)
                .Take(clampedPageSize)
                .ToListAsync();

            var listingIds = listings.Select(l => (Guid?)l.Id).ToList();
            var requestCounts = await _context.PickupRequests
                .Where(r => r.ListingId != null && listingIds.Contains(r.ListingId))
                .GroupBy(r => r.ListingId!.Value)
                .Select(g => new { ListingId = g.Key, Count = g.Count() })
                .ToDictionaryAsync(x => x.ListingId, x => x.Count);

            var listingDtos = listings.Select(l =>
            {
                var dto = MapListingToDto(l, l.Group);
                dto.RequestCount = requestCounts.GetValueOrDefault(l.Id, 0);
                return dto;
            }).ToList();

            return Ok(new ListingsResponse
            {
                Message = "Your listings retrieved successfully",
                Data = listingDtos,
                Total = total,
                Page = clampedPage,
                PageSize = clampedPageSize,
                TotalPages = (int)Math.Ceiling((double)total / clampedPageSize)
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting user listings");
            return StatusCode(500, new { message = "An error occurred while retrieving your listings" });
        }
    }

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

            if (!DateTime.TryParse(request.ExpiryDate, out var expiryDate))
                return BadRequest(new { message = "Invalid expiry date format. Use yyyy-MM-dd." });

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

            // Resolve photo storage: prefer ImageUrls array, fall back to single ImageUrl
            string? resolvedPhotoUrl = null;
            if (request.ImageUrls != null && request.ImageUrls.Count > 0)
                resolvedPhotoUrl = System.Text.Json.JsonSerializer.Serialize(request.ImageUrls.Take(5).ToList());
            else if (!string.IsNullOrEmpty(request.ImageUrl))
                resolvedPhotoUrl = request.ImageUrl;

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
                Status = ListingStatus.Open,
                PhotoUrl = resolvedPhotoUrl,
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

            listing.Grocery = grocery;
            var listingDto = MapListingToDto(listing, listingGroup);

            await _listingNotificationService.NotifyListingCreated(listingDto);
            await _pushNotificationService.SendNewListingNotificationToAllNGOs(listingDto);

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

            if (listing.Status != ListingStatus.Open)
            {
                var statusMessage = listing.Status switch
                {
                    ListingStatus.Reserved => "Cannot delete reserved listing. Wait for pickup completion or cancellation.",
                    ListingStatus.Expired => "Cannot delete expired listing.",
                    _ => $"Cannot delete listing with status: {listing.Status.ToString().ToLower()}"
                };
                return BadRequest(new { message = statusMessage });
            }

            var deletedListingId = listing.Id.ToString();

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

            await _listingNotificationService.NotifyListingDeleted(deletedListingId);

            var listingData = MapListingToDto(listing, listing.Group);

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

    // GET api/listings/{id}/view — Increment view count (call on open detail screen)
    [HttpPost("{id}/view")]
    public async Task<IActionResult> TrackView(Guid id)
    {
        try
        {
            var listing = await _context.ClearanceListings.FindAsync(id);
            if (listing == null) return NotFound();
            listing.ViewCount++;
            listing.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
            return Ok(new { viewCount = listing.ViewCount });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error tracking view");
            return StatusCode(500, new { message = "Error tracking view" });
        }
    }

    // PUT api/listings/{id}/archive — Soft-delete (archive) a listing
    [HttpPut("{id}/archive")]
    [Authorize]
    public async Task<ActionResult<ListingResponse>> ArchiveListing(Guid id)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
                return Unauthorized(new { message = "User not authenticated" });

            var listing = await _context.ClearanceListings
                .Include(l => l.Grocery).Include(l => l.Group)
                .FirstOrDefaultAsync(l => l.Id == id && l.GroceryId.ToString() == userId);

            if (listing == null)
                return NotFound(new { message = "Listing not found" });

            if (listing.IsArchived)
                return BadRequest(new { message = "Listing is already archived" });

            listing.IsArchived = true;
            listing.ArchivedAt = DateTime.UtcNow;
            listing.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();

            return Ok(new ListingResponse
            {
                Message = "Listing archived successfully",
                Data = MapListingToDto(listing, listing.Group)
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error archiving listing");
            return StatusCode(500, new { message = "An error occurred" });
        }
    }

    // PUT api/listings/{id}/unarchive — Restore an archived listing
    [HttpPut("{id}/unarchive")]
    [Authorize]
    public async Task<ActionResult<ListingResponse>> UnarchiveListing(Guid id)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
                return Unauthorized(new { message = "User not authenticated" });

            var listing = await _context.ClearanceListings
                .Include(l => l.Grocery).Include(l => l.Group)
                .FirstOrDefaultAsync(l => l.Id == id && l.GroceryId.ToString() == userId);

            if (listing == null)
                return NotFound(new { message = "Listing not found" });

            listing.IsArchived = false;
            listing.ArchivedAt = null;
            listing.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();

            return Ok(new ListingResponse
            {
                Message = "Listing restored successfully",
                Data = MapListingToDto(listing, listing.Group)
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error unarchiving listing");
            return StatusCode(500, new { message = "An error occurred" });
        }
    }

    // GET api/listings/{id}/similar — Get similar listings (same category, same grocery or nearby)
    [HttpGet("{id}/similar")]
    public async Task<IActionResult> GetSimilarListings(Guid id)
    {
        try
        {
            var listing = await _context.ClearanceListings
                .Include(l => l.Grocery)
                .FirstOrDefaultAsync(l => l.Id == id);

            if (listing == null) return NotFound();

            var similar = await _context.ClearanceListings
                .Include(l => l.Grocery)
                .Where(l => l.Id != id
                    && !l.IsArchived
                    && l.Status == ListingStatus.Open
                    && (l.Category == listing.Category || l.GroceryId == listing.GroceryId))
                .OrderByDescending(l => l.CreatedAt)
                .Take(10)
                .ToListAsync();

            return Ok(new
            {
                message = "Similar listings retrieved",
                data = similar.Select(l => MapListingToDto(l)).ToList()
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting similar listings");
            return StatusCode(500, new { message = "An error occurred" });
        }
    }

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

            if (listing.Status != ListingStatus.Open)
            {
                return BadRequest(new
                {
                    message = "Can only update quantity of available listings"
                });
            }

            if (request.NewQuantity <= 0)
            {
                return BadRequest(new
                {
                    message = "Quantity must be greater than 0"
                });
            }

            var oldQuantity = (int)listing.Quantity;
            var difference = request.NewQuantity - oldQuantity;

            listing.Quantity = request.NewQuantity;
            listing.UpdatedAt = DateTime.UtcNow;

            if (listing.GroupId.HasValue && listing.Group != null)
            {
                listing.Group.TotalAvailable += difference;
                listing.Group.UpdatedAt = DateTime.UtcNow;
            }

            await _context.SaveChangesAsync();

            var listingDto = MapListingToDto(listing, listing.Group);

            await _listingNotificationService.NotifyListingQuantityChanged(
                listingDto,
                oldQuantity,
                request.NewQuantity
            );

            return Ok(new ListingResponse
            {
                Message = "Quantity updated successfully",
                Data = listingDto
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating listing quantity");
            return StatusCode(500, new
            {
                message = "An error occurred while updating the quantity"
            });
        }
    }
}