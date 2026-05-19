using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

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
        if (!TryGetUserId(out var userId)) return Unauthorized();

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

    // GET api/reviews/organization/{id} — Get reviews for a grocery store
    [HttpGet("organization/{id}")]
    public async Task<IActionResult> GetReviewsForOrganization(Guid id,
        [FromQuery] int page = 1, [FromQuery] int pageSize = 20)
    {
        var clampedPage = Math.Max(1, page);
        var clampedSize = Math.Clamp(pageSize, 1, 50);

        var query = _context.Reviews
            .Include(r => r.Reviewer)
            .Where(r => r.ReviewedId == id)
            .OrderByDescending(r => r.CreatedAt);

        var total = await query.CountAsync();
        var items = await query
            .Skip((clampedPage - 1) * clampedSize)
            .Take(clampedSize)
            .ToListAsync();

        var avgRating = total > 0
            ? await _context.Reviews.Where(r => r.ReviewedId == id).AverageAsync(r => r.Rating)
            : 0.0;

        return Ok(new
        {
            message = "Reviews retrieved",
            data = items.Select(MapToDto).ToList(),
            averageRating = Math.Round(avgRating, 1),
            total,
            page = clampedPage,
            pageSize = clampedSize,
            totalPages = (int)Math.Ceiling((double)total / clampedSize)
        });
    }

    // GET api/reviews/my — Get reviews I submitted as NGO
    [HttpGet("my")]
    public async Task<IActionResult> GetMyReviews()
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var reviews = await _context.Reviews
            .Include(r => r.Reviewed)
            .Where(r => r.ReviewerId == userId)
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        return Ok(new { message = "Your reviews retrieved", data = reviews.Select(MapToDto).ToList() });
    }

    private bool TryGetUserId(out Guid userId)
    {
        var value = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.TryParse(value, out userId);
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
