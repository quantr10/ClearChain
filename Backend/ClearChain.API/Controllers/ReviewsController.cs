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
public class ReviewsController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public ReviewsController(ApplicationDbContext context)
    {
        _context = context;
    }

    // POST api/reviews — NGO or Grocery submits a review for a completed pickup
    [HttpPost]
    public async Task<IActionResult> SubmitReview([FromBody] SubmitReviewRequest request)
    {
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

        var pickup = await _context.PickupRequests
            .FirstOrDefaultAsync(pr => pr.Id == request.PickupRequestId
                && (pr.NgoId == userId || pr.GroceryId == userId)
                && pr.Status == PickupRequestStatus.Completed);

        if (pickup == null)
            return NotFound(new { message = "Completed pickup request not found or you don't have access" });

        var alreadyReviewed = await _context.Reviews
            .AnyAsync(r => r.PickupRequestId == request.PickupRequestId && r.ReviewerId == userId);

        if (alreadyReviewed)
            return BadRequest(new { message = "You have already reviewed this pickup" });

        if (request.Rating < 1 || request.Rating > 5)
            return BadRequest(new { message = "Rating must be between 1 and 5" });

        // Reviewer always rates the other party
        var reviewedId = pickup.NgoId == userId ? pickup.GroceryId : pickup.NgoId;

        var review = new Review
        {
            Id = Guid.NewGuid(),
            PickupRequestId = request.PickupRequestId,
            ReviewerId = userId,
            ReviewedId = reviewedId,
            Rating = request.Rating,
            Comment = request.Comment,
            CreatedAt = DateTime.UtcNow
        };

        _context.Reviews.Add(review);
        await _context.SaveChangesAsync();

        return Ok(new { message = "Review submitted successfully", data = MapToDto(review) });
    }

    // GET api/reviews/pickup/{pickupRequestId} — the reviews left on one pickup (at most one per
    // side). Readable by admins and by the two organizations on the pickup; anyone else gets the
    // same "not found" as for a pickup that does not exist.
    [HttpGet("pickup/{pickupRequestId}")]
    public async Task<IActionResult> GetReviewsForPickup(Guid pickupRequestId)
    {
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

        var pickup = await _context.PickupRequests
            .AsNoTracking()
            .FirstOrDefaultAsync(pr => pr.Id == pickupRequestId);

        if (pickup == null || (!User.IsInRole("admin") && pickup.NgoId != userId && pickup.GroceryId != userId))
            return NotFound(new { message = "Pickup request not found" });

        var reviews = await _context.Reviews
            .Include(r => r.Reviewer)
            .Include(r => r.Reviewed)
            .Where(r => r.PickupRequestId == pickupRequestId)
            .OrderBy(r => r.CreatedAt)
            .ToListAsync();

        return Ok(new { message = "Reviews retrieved", data = reviews.Select(MapToDto).ToList() });
    }

    // GET api/reviews/my — Get reviews I submitted as NGO
    [HttpGet("my")]
    public async Task<IActionResult> GetMyReviews()
    {
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

        var reviews = await _context.Reviews
            .Include(r => r.Reviewer)
            .Include(r => r.Reviewed)
            .Where(r => r.ReviewerId == userId)
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        return Ok(new { message = "Your reviews retrieved", data = reviews.Select(MapToDto).ToList() });
    }

    private static object MapToDto(Review r) => new
    {
        id = r.Id.ToString(),
        pickupRequestId = r.PickupRequestId.ToString(),
        reviewerId = r.ReviewerId.ToString(),
        reviewerName = r.Reviewer?.Name ?? "",
        reviewedId = r.ReviewedId.ToString(),
        reviewedName = r.Reviewed?.Name ?? "",
        rating = r.Rating,
        comment = r.Comment,
        createdAt = r.CreatedAt.ToString("o")
    };
}

public class SubmitReviewRequest
{
    public Guid PickupRequestId { get; set; }
    public int Rating { get; set; }
    public string? Comment { get; set; }
}
