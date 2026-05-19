using ClearChain.Domain.Entities;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class SavedListingsController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public SavedListingsController(ApplicationDbContext context)
    {
        _context = context;
    }

    // POST api/savedlistings/{listingId} — Save a listing
    [HttpPost("{listingId}")]
    public async Task<IActionResult> SaveListing(Guid listingId)
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var listing = await _context.ClearanceListings.FindAsync(listingId);
        if (listing == null) return NotFound(new { message = "Listing not found" });

        var alreadySaved = await _context.SavedListings
            .AnyAsync(s => s.NgoId == userId && s.ListingId == listingId);

        if (alreadySaved) return Ok(new { message = "Already saved", saved = true });

        _context.SavedListings.Add(new SavedListing
        {
            Id = Guid.NewGuid(),
            NgoId = userId,
            ListingId = listingId,
            SavedAt = DateTime.UtcNow
        });
        await _context.SaveChangesAsync();

        return Ok(new { message = "Listing saved", saved = true });
    }

    // DELETE api/savedlistings/{listingId} — Unsave a listing
    [HttpDelete("{listingId}")]
    public async Task<IActionResult> UnsaveListing(Guid listingId)
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var saved = await _context.SavedListings
            .FirstOrDefaultAsync(s => s.NgoId == userId && s.ListingId == listingId);

        if (saved == null) return Ok(new { message = "Not saved", saved = false });

        _context.SavedListings.Remove(saved);
        await _context.SaveChangesAsync();

        return Ok(new { message = "Listing unsaved", saved = false });
    }

    // GET api/savedlistings — Get my saved listings
    [HttpGet]
    public async Task<IActionResult> GetSavedListings([FromQuery] int page = 1, [FromQuery] int pageSize = 20)
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var clampedPage = Math.Max(1, page);
        var clampedSize = Math.Clamp(pageSize, 1, 50);

        var query = _context.SavedListings
            .Include(s => s.Listing).ThenInclude(l => l!.Grocery)
            .Where(s => s.NgoId == userId && s.Listing != null && !s.Listing.IsArchived);

        var total = await query.CountAsync();
        var items = await query
            .OrderByDescending(s => s.SavedAt)
            .Skip((clampedPage - 1) * clampedSize)
            .Take(clampedSize)
            .ToListAsync();

        // Get all saved listing IDs for status response
        var savedIds = items.Select(s => s.ListingId.ToString()).ToList();

        return Ok(new
        {
            message = "Saved listings retrieved",
            data = items.Select(s => new
            {
                savedId = s.Id.ToString(),
                savedAt = s.SavedAt.ToString("o"),
                listing = s.Listing == null ? null : new
                {
                    id = s.Listing.Id.ToString(),
                    title = s.Listing.ProductName,
                    category = s.Listing.Category,
                    quantity = (int)s.Listing.Quantity,
                    unit = s.Listing.Unit,
                    expiryDate = s.Listing.ExpirationDate?.ToString("yyyy-MM-dd"),
                    status = s.Listing.Status.ToString().ToLower(),
                    imageUrl = s.Listing.PhotoUrl,
                    groceryName = s.Listing.Grocery?.Name ?? "",
                    location = s.Listing.Grocery?.Location ?? "",
                    createdAt = s.Listing.CreatedAt.ToString("o")
                }
            }).ToList(),
            total,
            page = clampedPage,
            pageSize = clampedSize,
            totalPages = (int)Math.Ceiling((double)total / clampedSize)
        });
    }

    // GET api/savedlistings/ids — Get list of saved listing IDs (for UI toggle state)
    [HttpGet("ids")]
    public async Task<IActionResult> GetSavedListingIds()
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var ids = await _context.SavedListings
            .Where(s => s.NgoId == userId)
            .Select(s => s.ListingId.ToString())
            .ToListAsync();

        return Ok(new { data = ids });
    }

    private bool TryGetUserId(out Guid userId)
    {
        var value = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.TryParse(value, out userId);
    }
}
