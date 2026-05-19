using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;
using ClearChain.API.Services;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class DisputesController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly IStorageService _storageService;

    public DisputesController(ApplicationDbContext context, IStorageService storageService)
    {
        _context = context;
        _storageService = storageService;
    }

    // POST api/disputes — NGO opens a dispute on a completed pickup
    [HttpPost]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> OpenDispute(
        [FromForm] OpenDisputeRequest request,
        IFormFile? photo)
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var pickup = await _context.PickupRequests
            .FirstOrDefaultAsync(pr => pr.Id == request.PickupRequestId
                && pr.NgoId == userId
                && pr.Status == PickupRequestStatus.Completed);

        if (pickup == null)
            return NotFound(new { message = "Completed pickup not found or you don't have access" });

        var existing = await _context.Disputes
            .AnyAsync(d => d.PickupRequestId == request.PickupRequestId);
        if (existing)
            return BadRequest(new { message = "A dispute already exists for this pickup" });

        string? photoUrl = null;
        if (photo != null && photo.Length > 0)
        {
            using var stream = photo.OpenReadStream();
            photoUrl = await _storageService.UploadFileAsync(stream, photo.FileName, photo.ContentType, "disputes");
        }

        var dispute = new Dispute
        {
            Id = Guid.NewGuid(),
            PickupRequestId = request.PickupRequestId,
            InitiatorId = userId,
            Reason = request.Reason,
            NgoStatement = request.Statement,
            PhotoEvidenceUrl = photoUrl,
            Status = "open",
            CreatedAt = DateTime.UtcNow
        };

        _context.Disputes.Add(dispute);
        await _context.SaveChangesAsync();

        return Ok(new { message = "Dispute opened successfully", data = MapToDto(dispute) });
    }

    // GET api/disputes/{id}
    [HttpGet("{id}")]
    public async Task<IActionResult> GetDispute(Guid id)
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var dispute = await _context.Disputes
            .Include(d => d.Initiator)
            .Include(d => d.PickupRequest)
            .FirstOrDefaultAsync(d => d.Id == id);

        if (dispute == null) return NotFound(new { message = "Dispute not found" });

        // Only parties involved or admin can view
        var userType = User.FindFirst("type")?.Value;
        if (userType != "admin"
            && dispute.InitiatorId != userId
            && dispute.PickupRequest?.GroceryId != userId)
            return Forbid();

        return Ok(new { message = "Dispute retrieved", data = MapToDto(dispute) });
    }

    // GET api/disputes/my — my disputes (as NGO initiator or Grocery respondent)
    [HttpGet("my")]
    public async Task<IActionResult> GetMyDisputes()
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var disputes = await _context.Disputes
            .Include(d => d.Initiator)
            .Include(d => d.PickupRequest)
            .Where(d => d.InitiatorId == userId
                || (d.PickupRequest != null && d.PickupRequest.GroceryId == userId))
            .OrderByDescending(d => d.CreatedAt)
            .ToListAsync();

        return Ok(new { message = "Disputes retrieved", data = disputes.Select(MapToDto).ToList() });
    }

    // PUT api/disputes/{id}/grocery-statement — Grocery responds to dispute
    [HttpPut("{id}/grocery-statement")]
    public async Task<IActionResult> AddGroceryStatement(Guid id, [FromBody] AddStatementRequest request)
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var dispute = await _context.Disputes
            .Include(d => d.PickupRequest)
            .FirstOrDefaultAsync(d => d.Id == id);

        if (dispute == null) return NotFound();
        if (dispute.PickupRequest?.GroceryId != userId) return Forbid();
        if (dispute.Status != "open") return BadRequest(new { message = "Dispute is no longer open" });

        dispute.GroceryStatement = request.Statement;
        dispute.Status = "under_review";
        await _context.SaveChangesAsync();

        return Ok(new { message = "Statement submitted", data = MapToDto(dispute) });
    }

    // PUT api/disputes/{id}/resolve — Admin resolves
    [HttpPut("{id}/resolve")]
    public async Task<IActionResult> ResolveDispute(Guid id, [FromBody] ResolveDisputeRequest request)
    {
        var userType = User.FindFirst("type")?.Value;
        if (userType != "admin") return Forbid();

        if (!TryGetUserId(out var adminId)) return Unauthorized();

        var dispute = await _context.Disputes.FindAsync(id);
        if (dispute == null) return NotFound();

        dispute.Status = request.Resolution; // "resolved_ngo", "resolved_grocery", "dismissed"
        dispute.AdminResolution = request.Note;
        dispute.ResolvedByAdminId = adminId;
        dispute.ResolvedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return Ok(new { message = "Dispute resolved", data = MapToDto(dispute) });
    }

    // GET api/disputes — Admin: all disputes
    [HttpGet]
    public async Task<IActionResult> GetAllDisputes(
        [FromQuery] string? status = null,
        [FromQuery] int page = 1, [FromQuery] int pageSize = 20)
    {
        var userType = User.FindFirst("type")?.Value;
        if (userType != "admin") return Forbid();

        var clampedPage = Math.Max(1, page);
        var clampedSize = Math.Clamp(pageSize, 1, 50);

        var query = _context.Disputes
            .Include(d => d.Initiator)
            .Include(d => d.PickupRequest)
            .AsQueryable();

        if (!string.IsNullOrEmpty(status))
            query = query.Where(d => d.Status == status);

        var total = await query.CountAsync();
        var items = await query
            .OrderByDescending(d => d.CreatedAt)
            .Skip((clampedPage - 1) * clampedSize)
            .Take(clampedSize)
            .ToListAsync();

        return Ok(new
        {
            message = "Disputes retrieved",
            data = items.Select(MapToDto).ToList(),
            total,
            page = clampedPage,
            pageSize = clampedSize,
            totalPages = (int)Math.Ceiling((double)total / clampedSize)
        });
    }

    private bool TryGetUserId(out Guid userId)
    {
        var value = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.TryParse(value, out userId);
    }

    private static object MapToDto(Dispute d) => new
    {
        id = d.Id.ToString(),
        pickupRequestId = d.PickupRequestId.ToString(),
        initiatorId = d.InitiatorId.ToString(),
        initiatorName = d.Initiator?.Name ?? "",
        reason = d.Reason,
        ngoStatement = d.NgoStatement,
        groceryStatement = d.GroceryStatement,
        photoEvidenceUrl = d.PhotoEvidenceUrl,
        status = d.Status,
        adminResolution = d.AdminResolution,
        createdAt = d.CreatedAt.ToString("o"),
        resolvedAt = d.ResolvedAt?.ToString("o")
    };
}

public class OpenDisputeRequest
{
    public Guid PickupRequestId { get; set; }
    public string Reason { get; set; } = string.Empty;
    public string? Statement { get; set; }
}

public class AddStatementRequest
{
    public string Statement { get; set; } = string.Empty;
}

public class ResolveDisputeRequest
{
    public string Resolution { get; set; } = string.Empty;
    public string? Note { get; set; }
}
