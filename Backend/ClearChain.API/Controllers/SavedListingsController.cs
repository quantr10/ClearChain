using ClearChain.API.Common;
using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

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
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

        var listing = await _context.ClearanceListings.FindAsync(listingId);
        if (listing == null || listing.Status == ListingStatus.Archived)
            return NotFound(new { message = "Listing not found" });

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
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

        var saved = await _context.SavedListings
            .FirstOrDefaultAsync(s => s.NgoId == userId && s.ListingId == listingId);

        if (saved == null) return Ok(new { message = "Not saved", saved = false });

        _context.SavedListings.Remove(saved);
        await _context.SaveChangesAsync();

        return Ok(new { message = "Listing unsaved", saved = false });
    }

    // GET api/savedlistings/ids — Get list of saved listing IDs (for UI toggle state)
    [HttpGet("ids")]
    public async Task<IActionResult> GetSavedListingIds()
    {
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

        var ids = await _context.SavedListings
            .Where(s => s.NgoId == userId && s.Listing != null && s.Listing.Status != ListingStatus.Archived)
            .Select(s => s.ListingId.ToString())
            .ToListAsync();

        return Ok(new { data = ids });
    }

}
