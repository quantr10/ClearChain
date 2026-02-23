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

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS FOR SPLIT/MERGE LOGIC
    // ═══════════════════════════════════════════════════════════════════════════

    /// <summary>
    /// Creates a pickup request and handles listing split if needed
    /// </summary>
    private async Task<(ClearanceListing reservedListing, PickupRequest request)>
        CreateRequestAndSplitListing(
            ClearanceListing sourceListing,
            ListingGroup group,
            int requestedQuantity,
            DateTime pickupDate,
            string pickupTime,
            string? notes,
            Guid ngoId)
    {
        var requestId = Guid.NewGuid();

        // ═══════════════════════════════════════════════════════════════════
        // CASE 1: FULL PICKUP (request = listing quantity)
        // ═══════════════════════════════════════════════════════════════════

        if (requestedQuantity >= sourceListing.Quantity)
        {
            // Just change status to reserved
            sourceListing.Status = "reserved";
            sourceListing.RelatedRequestId = requestId;
            sourceListing.UpdatedAt = DateTime.UtcNow;

            // Update group
            group.TotalAvailable -= sourceListing.Quantity;
            group.TotalReserved += sourceListing.Quantity;
            group.UpdatedAt = DateTime.UtcNow;

            // Create pickup request
            var fullRequest = new PickupRequest
            {
                Id = requestId,
                NgoId = ngoId,
                GroceryId = sourceListing.GroceryId,
                ListingId = sourceListing.Id,
                PickupDate = pickupDate,
                Status = "pending",
                RequestedAt = DateTime.UtcNow,
                RequestedQuantity = requestedQuantity,
                PickupTime = pickupTime,
                Notes = notes,

                // ✅ NEW: Cache listing info
                ListingTitle = sourceListing.ProductName,
                ListingCategory = sourceListing.Category
            };

            return (sourceListing, fullRequest);
        }

        // ═══════════════════════════════════════════════════════════════════
        // CASE 2: PARTIAL PICKUP (request < listing quantity)
        // ═══════════════════════════════════════════════════════════════════

        var reservedListingId = Guid.NewGuid();

        // Create RESERVED part (new listing)
        // CREATE RESERVED part (new listing)
        var reservedListing = new ClearanceListing
        {
            Id = reservedListingId,
            GroupId = group.Id,
            GroceryId = sourceListing.GroceryId,
            ProductName = sourceListing.ProductName,
            Category = sourceListing.Category,
            Quantity = requestedQuantity,
            Unit = sourceListing.Unit,

            // ✅ FIX: Ensure UTC DateTimes
            ExpirationDate = sourceListing.ExpirationDate.HasValue
                ? DateTime.SpecifyKind(sourceListing.ExpirationDate.Value, DateTimeKind.Utc)
                : (DateTime?)null,
            ClearanceDeadline = DateTime.SpecifyKind(sourceListing.ClearanceDeadline, DateTimeKind.Utc),

            Notes = sourceListing.Notes,
            PhotoUrl = sourceListing.PhotoUrl,
            PickupTimeStart = sourceListing.PickupTimeStart,
            PickupTimeEnd = sourceListing.PickupTimeEnd,
            Status = "reserved",
            SplitReason = "partial_request",
            RelatedRequestId = requestId,
            SplitFromListingId = sourceListing.Id,
            SplitIndex = group.ChildListings?.Count ?? 1,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        // Update AVAILABLE part (reduce quantity)
        sourceListing.Quantity -= requestedQuantity;
        sourceListing.UpdatedAt = DateTime.UtcNow;

        // Update group totals
        group.TotalAvailable -= requestedQuantity;
        group.TotalReserved += requestedQuantity;
        group.UpdatedAt = DateTime.UtcNow;

        // Save the reserved listing
        _context.ClearanceListings.Add(reservedListing);

        // Create pickup request
        var partialRequest = new PickupRequest
        {
            Id = requestId,
            NgoId = ngoId,
            GroceryId = sourceListing.GroceryId,
            ListingId = reservedListing.Id,
            PickupDate = pickupDate,
            Status = "pending",
            RequestedAt = DateTime.UtcNow,
            RequestedQuantity = requestedQuantity,
            PickupTime = pickupTime,
            Notes = notes,

            // ✅ NEW: Cache listing info
            ListingTitle = sourceListing.ProductName,
            ListingCategory = sourceListing.Category
        };

        return (reservedListing, partialRequest);
    }

    /// <summary>
    /// Smart merge logic when request is cancelled
    /// </summary>
    private async Task SmartMergeOnCancel(ClearanceListing cancelledListing, ListingGroup group)
    {
        if (cancelledListing.GroupId == null) return;

        // Find AVAILABLE siblings in the same group
        var availableSiblings = await _context.ClearanceListings
            .Where(l => l.GroupId == group.Id &&
                       l.Id != cancelledListing.Id &&
                       l.Status == "open")
            .ToListAsync();

        // ═══════════════════════════════════════════════════════════════════
        // CASE 1: Has AVAILABLE sibling → MERGE into it
        // ═══════════════════════════════════════════════════════════════════

        if (availableSiblings.Any())
        {
            var targetListing = availableSiblings.First();

            // Merge quantity
            targetListing.Quantity += cancelledListing.Quantity;
            targetListing.SplitReason = "merge";
            targetListing.UpdatedAt = DateTime.UtcNow;

            // Update group
            group.TotalReserved -= cancelledListing.Quantity;
            group.TotalAvailable += cancelledListing.Quantity;
            group.UpdatedAt = DateTime.UtcNow;

            // Delete cancelled listing
            _context.ClearanceListings.Remove(cancelledListing);
        }

        // ═══════════════════════════════════════════════════════════════════
        // CASE 2: No AVAILABLE sibling → CONVERT to AVAILABLE
        // ═══════════════════════════════════════════════════════════════════

        else
        {
            cancelledListing.Status = "open";
            cancelledListing.RelatedRequestId = null;
            cancelledListing.SplitReason = "cancel_restore";
            cancelledListing.UpdatedAt = DateTime.UtcNow;

            // Update group
            group.TotalReserved -= cancelledListing.Quantity;
            group.TotalAvailable += cancelledListing.Quantity;
            group.UpdatedAt = DateTime.UtcNow;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POST: api/pickuprequests (UPDATED with split logic)
    // ═══════════════════════════════════════════════════════════════════════════

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

            var ngo = await _context.Organizations
                .FirstOrDefaultAsync(o => o.Id.ToString() == userId);

            if (ngo == null || ngo.Type.ToLower() != "ngo")
            {
                return BadRequest(new { message = "Only NGOs can create pickup requests" });
            }

            var listing = await _context.ClearanceListings
                .Include(l => l.Grocery)
                .Include(l => l.Group)
                .FirstOrDefaultAsync(l => l.Id.ToString() == request.ListingId);

            if (listing == null)
            {
                return NotFound(new { message = "Listing not found" });
            }

            if (listing.Status != "open")
            {
                return BadRequest(new { message = "Listing is not available" });
            }

            if (request.RequestedQuantity > listing.Quantity)
            {
                return BadRequest(new
                {
                    message = $"Requested quantity exceeds available quantity ({listing.Quantity})"
                });
            }

            var pickupDate = DateTime.Parse(request.PickupDate);
            var pickupDateUtc = DateTime.SpecifyKind(pickupDate, DateTimeKind.Utc);

            if (pickupDateUtc.Date < DateTime.UtcNow.Date)
            {
                return BadRequest(new { message = "Pickup date cannot be in the past" });
            }

            if (listing.ExpirationDate.HasValue &&
                pickupDateUtc.Date > listing.ExpirationDate.Value.Date)
            {
                return BadRequest(new
                {
                    message = $"Pickup date cannot be after expiry date ({listing.ExpirationDate.Value:yyyy-MM-dd})"
                });
            }

            // ═══════════════════════════════════════════════════════════════
            // SPLIT LISTING & CREATE REQUEST
            // ═══════════════════════════════════════════════════════════════

            if (listing.Group == null)
            {
                return BadRequest(new { message = "Listing has no associated group" });
            }

            var (reservedListing, pickupRequest) = await CreateRequestAndSplitListing(
                listing,
                listing.Group,
                request.RequestedQuantity,
                pickupDateUtc,
                request.PickupTime,
                request.Notes,
                Guid.Parse(userId)
            );

            _context.PickupRequests.Add(pickupRequest);
            await _context.SaveChangesAsync();

            var responseData = new PickupRequestData
            {
                Id = pickupRequest.Id.ToString(),
                ListingId = reservedListing.Id.ToString(),
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

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE: api/pickuprequests/{id} (UPDATED with smart merge)
    // ═══════════════════════════════════════════════════════════════════════════

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

            // Get related data before processing
            var ngo = await _context.Organizations.FindAsync(pr.NgoId);
            var grocery = await _context.Organizations.FindAsync(pr.GroceryId);
            var listing = pr.ListingId.HasValue
                ? await _context.ClearanceListings
                    .Include(l => l.Group)
                    .FirstOrDefaultAsync(l => l.Id == pr.ListingId.Value)
                : null;

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

            // ✅ STEP 1: Delete request FIRST and save immediately
            _context.PickupRequests.Remove(pr);
            await _context.SaveChangesAsync();

            // ✅ STEP 2: Then smart merge and save again
            if (listing != null && listing.Group != null)
            {
                await SmartMergeOnCancel(listing, listing.Group);
                await _context.SaveChangesAsync();
            }

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

    // ═══════════════════════════════════════════════════════════════════════════
    // PUT: api/pickuprequests/{id}/reject (UPDATED with smart merge)
    // ═══════════════════════════════════════════════════════════════════════════

    [HttpPut("{id}/reject")]
    public async Task<ActionResult<PickupRequestResponse>> RejectPickupRequest(Guid id)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var pr = await _context.PickupRequests
                .FirstOrDefaultAsync(p => p.Id == id && p.GroceryId.ToString() == userId);

            if (pr == null)
            {
                return NotFound(new { message = "Pickup request not found" });
            }

            if (pr.Status != "pending")
            {
                return BadRequest(new { message = $"Cannot reject request with status: {pr.Status}" });
            }

            // ✅ Get related data BEFORE modifying anything
            var ngo = await _context.Organizations.FindAsync(pr.NgoId);
            var grocery = await _context.Organizations.FindAsync(pr.GroceryId);

            var listing = pr.ListingId.HasValue
                ? await _context.ClearanceListings
                    .Include(l => l.Group)
                    .FirstOrDefaultAsync(l => l.Id == pr.ListingId.Value)
                : null;

            // ✅ Build response data BEFORE removing request
            var responseData = new PickupRequestData
            {
                Id = pr.Id.ToString(),
                ListingId = pr.ListingId?.ToString() ?? "",
                NgoId = pr.NgoId.ToString(),
                NgoName = ngo?.Name ?? "",
                GroceryId = pr.GroceryId.ToString(),
                GroceryName = grocery?.Name ?? "",
                Status = "rejected",  // ✅ Return "rejected" not pr.Status
                RequestedQuantity = pr.RequestedQuantity ?? 0,
                PickupDate = pr.PickupDate.ToString("yyyy-MM-dd"),
                PickupTime = pr.PickupTime ?? "09:00",
                Notes = pr.Notes ?? "",
                ListingTitle = listing?.ProductName ?? "",
                ListingCategory = listing?.Category ?? "",
                CreatedAt = pr.RequestedAt.ToString("o")
            };

            // ✅ STEP 1: Delete request FIRST and save immediately
            _context.PickupRequests.Remove(pr);
            await _context.SaveChangesAsync();

            // ✅ STEP 2: Then smart merge and save again
            if (listing != null && listing.Group != null)
            {
                await SmartMergeOnCancel(listing, listing.Group);
                await _context.SaveChangesAsync();
            }

            return Ok(new PickupRequestResponse
            {
                Message = "Pickup request rejected successfully",
                Data = responseData
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error rejecting pickup request");
            return StatusCode(500, new { message = ex.Message });
        }
    }
    // ═══════════════════════════════════════════════════════════════════════════
    // PUT: api/pickuprequests/{id}/picked-up (UPDATED - delete listing, update group)
    // ═══════════════════════════════════════════════════════════════════════════

    [HttpPut("{id}/picked-up")]
    public async Task<ActionResult<PickupRequestResponse>> MarkPickedUp(Guid id)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var pickupRequest = await _context.PickupRequests
                .FirstOrDefaultAsync(pr => pr.Id == id &&
                    (pr.GroceryId.ToString() == userId || pr.NgoId.ToString() == userId));

            if (pickupRequest == null)
            {
                return NotFound(new { message = "Pickup request not found" });
            }

            if (pickupRequest.Status != "ready")
            {
                return BadRequest(new { message = "Can only mark ready requests as picked up" });
            }

            // Get listing and orgs for response
            var listing = pickupRequest.ListingId.HasValue
                ? await _context.ClearanceListings
                    .Include(l => l.Group)
                    .FirstOrDefaultAsync(l => l.Id == pickupRequest.ListingId.Value)
                : null;

            var ngo = await _context.Organizations.FindAsync(pickupRequest.NgoId);
            var grocery = await _context.Organizations.FindAsync(pickupRequest.GroceryId);

            // Build response data BEFORE changes
            var responseData = new PickupRequestData
            {
                Id = pickupRequest.Id.ToString(),
                ListingId = pickupRequest.ListingId?.ToString() ?? "",
                NgoId = pickupRequest.NgoId.ToString(),
                NgoName = ngo?.Name ?? "",
                GroceryId = pickupRequest.GroceryId.ToString(),
                GroceryName = grocery?.Name ?? "",
                Status = "completed",
                RequestedQuantity = pickupRequest.RequestedQuantity ?? 0,
                PickupDate = pickupRequest.PickupDate.ToString("yyyy-MM-dd"),
                PickupTime = pickupRequest.PickupTime ?? "09:00",
                Notes = pickupRequest.Notes ?? "",
                ListingTitle = listing?.ProductName ?? "",
                ListingCategory = listing?.Category ?? "",
                CreatedAt = pickupRequest.RequestedAt.ToString("o")
            };

            // ✅ CRITICAL: Update request status AND remove FK reference
            pickupRequest.Status = "completed";
            pickupRequest.MarkedPickedUpAt = DateTime.UtcNow;
            pickupRequest.ConfirmedReceivedAt = DateTime.UtcNow;
            pickupRequest.ListingId = null; // ✅ NULL out FK BEFORE deleting listing!

            await _context.SaveChangesAsync();

            // ✅ Now safe to delete listing
            if (listing != null)
            {
                var expiryDate = listing.ExpirationDate.HasValue
                    ? DateTime.SpecifyKind(listing.ExpirationDate.Value, DateTimeKind.Utc)
                    : DateTime.UtcNow.AddDays(7);

                var inventoryItem = new Inventory
                {
                    Id = Guid.NewGuid(),
                    NgoId = pickupRequest.NgoId,
                    PickupRequestId = pickupRequest.Id,
                    ProductName = listing.ProductName,
                    Category = listing.Category,
                    Quantity = pickupRequest.RequestedQuantity ?? 0,
                    Unit = listing.Unit,
                    ExpiryDate = expiryDate,
                    Status = "active",
                    ReceivedAt = DateTime.UtcNow,
                    CreatedAt = DateTime.UtcNow,
                    UpdatedAt = DateTime.UtcNow
                };

                _context.Inventories.Add(inventoryItem);

                if (listing.Group != null)
                {
                    listing.Group.TotalReserved -= listing.Quantity;
                    listing.Group.TotalCompleted += listing.Quantity;
                    listing.Group.UpdatedAt = DateTime.UtcNow;

                    if (listing.Group.TotalCompleted >= listing.Group.OriginalQuantity)
                    {
                        listing.Group.IsFullyConsumed = true;
                    }

                    var remainingChildren = await _context.ClearanceListings
                        .Where(l => l.GroupId == listing.GroupId && l.Id != listing.Id)
                        .CountAsync();

                    if (remainingChildren == 0 && listing.Group.IsFullyConsumed)
                    {
                        _context.ListingGroups.Remove(listing.Group);
                    }
                }

                _context.ClearanceListings.Remove(listing);
                await _context.SaveChangesAsync();
            }

            return Ok(new PickupRequestResponse
            {
                Message = "Pickup completed successfully",
                Data = responseData
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error marking pickup as completed");
            return StatusCode(500, new
            {
                message = "An error occurred while marking the pickup as completed"
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UNCHANGED METHODS (keep as-is)
    // ═══════════════════════════════════════════════════════════════════════════

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

                    // ✅ Use cached info (no listing query needed)
                    ListingTitle = pr.ListingTitle ?? "Unknown Item",
                    ListingCategory = pr.ListingCategory ?? "OTHER",

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

                    // ✅ Use cached info
                    ListingTitle = pr.ListingTitle ?? "Unknown Item",
                    ListingCategory = pr.ListingCategory ?? "OTHER",

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

    [HttpGet("{id}")]
    public async Task<ActionResult<PickupRequestResponse>> GetPickupRequestById(Guid id)
    {
        // ... keep existing implementation ...
        try
        {
            var pr = await _context.PickupRequests.FindAsync(id);
            if (pr == null)
            {
                return NotFound(new { message = "Pickup request not found" });
            }

            var ngo = await _context.Organizations.FindAsync(pr.NgoId);
            var grocery = await _context.Organizations.FindAsync(pr.GroceryId);
            var listing = pr.ListingId.HasValue
                ? await _context.ClearanceListings.FindAsync(pr.ListingId.Value)
                : null;

            var responseData = new PickupRequestData
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

    [HttpPut("{id}/approve")]
    public async Task<ActionResult<PickupRequestResponse>> ApprovePickupRequest(Guid id)
    {
        // ... keep existing implementation (no changes needed) ...
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var pr = await _context.PickupRequests
                .FirstOrDefaultAsync(p => p.Id == id && p.GroceryId.ToString() == userId);

            if (pr == null)
            {
                return NotFound(new { message = "Pickup request not found" });
            }

            if (pr.Status != "pending")
            {
                return BadRequest(new { message = $"Cannot approve request with status: {pr.Status}" });
            }

            pr.Status = "approved";
            await _context.SaveChangesAsync();

            var ngo = await _context.Organizations.FindAsync(pr.NgoId);
            var grocery = await _context.Organizations.FindAsync(pr.GroceryId);
            var listing = pr.ListingId.HasValue
                ? await _context.ClearanceListings.FindAsync(pr.ListingId.Value)
                : null;

            var responseData = new PickupRequestData
            {
                Id = pr.Id.ToString(),
                ListingId = pr.ListingId?.ToString() ?? "",
                NgoId = pr.NgoId.ToString(),
                NgoName = ngo?.Name ?? "",
                GroceryId = pr.GroceryId.ToString(),
                GroceryName = grocery?.Name ?? "",
                Status = pr.Status,
                RequestedQuantity = pr.RequestedQuantity ?? 0,
                PickupDate = pr.PickupDate.ToString("yyyy-MM-dd"),
                PickupTime = pr.PickupTime ?? "09:00",
                Notes = pr.Notes ?? "",
                ListingTitle = listing?.ProductName ?? "",
                ListingCategory = listing?.Category ?? "",
                CreatedAt = pr.RequestedAt.ToString("o")
            };

            return Ok(new PickupRequestResponse
            {
                Message = "Pickup request approved successfully",
                Data = responseData
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error approving pickup request");
            return StatusCode(500, new { message = ex.Message });
        }
    }

    [HttpPut("{id}/ready")]
    public async Task<ActionResult<PickupRequestResponse>> MarkReadyForPickup(Guid id)
    {
        // ... keep existing implementation (no changes needed) ...
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var pr = await _context.PickupRequests
                .FirstOrDefaultAsync(p => p.Id == id && p.GroceryId.ToString() == userId);

            if (pr == null)
            {
                return NotFound(new { message = "Pickup request not found" });
            }

            if (pr.Status != "approved")
            {
                return BadRequest(new { message = $"Can only mark approved requests as ready. Current status: {pr.Status}" });
            }

            pr.Status = "ready";
            pr.MarkedReadyAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();

            var ngo = await _context.Organizations.FindAsync(pr.NgoId);
            var grocery = await _context.Organizations.FindAsync(pr.GroceryId);
            var listing = pr.ListingId.HasValue
                ? await _context.ClearanceListings.FindAsync(pr.ListingId.Value)
                : null;

            var responseData = new PickupRequestData
            {
                Id = pr.Id.ToString(),
                ListingId = pr.ListingId?.ToString() ?? "",
                NgoId = pr.NgoId.ToString(),
                NgoName = ngo?.Name ?? "",
                GroceryId = pr.GroceryId.ToString(),
                GroceryName = grocery?.Name ?? "",
                Status = pr.Status,
                RequestedQuantity = pr.RequestedQuantity ?? 0,
                PickupDate = pr.PickupDate.ToString("yyyy-MM-dd"),
                PickupTime = pr.PickupTime ?? "09:00",
                Notes = pr.Notes ?? "",
                ListingTitle = listing?.ProductName ?? "",
                ListingCategory = listing?.Category ?? "",
                CreatedAt = pr.RequestedAt.ToString("o")
            };

            return Ok(new PickupRequestResponse
            {
                Message = "Pickup request marked as ready",
                Data = responseData
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error marking request as ready");
            return StatusCode(500, new { message = ex.Message });
        }
    }
}