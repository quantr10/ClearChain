using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using ClearChain.API.Common;
using ClearChain.API.DTOs.Disputes;
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
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

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
            if (!StorageBucketPolicy.IsAllowedImage(photo.ContentType))
                return BadRequest(new { message = $"Only {StorageBucketPolicy.ImageTypesMessage} are accepted" });

            if (photo.Length > StorageBucketPolicy.ImageMaxBytes)
                return BadRequest(new { message = $"Photo must be under {StorageBucketPolicy.ImageMaxBytes / 1024 / 1024} MB" });

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

    // GET api/disputes — admin only. There is no in-app negotiation between the NGO and the
    // grocery, so this hands the admin everything needed (both parties' contact details, the
    // pickup context, the evidence) to resolve it by phone or email instead.
    [HttpGet]
    [Authorize(Roles = "admin")]
    public async Task<IActionResult> GetDisputes([FromQuery] string? status = null)
    {
        var query = _context.Disputes
            .Include(d => d.Initiator)
            .Include(d => d.PickupRequest!).ThenInclude(pr => pr.Grocery)
            .Include(d => d.PickupRequest!).ThenInclude(pr => pr.Ngo)
            .AsQueryable();

        if (!string.IsNullOrEmpty(status))
            query = query.Where(d => d.Status == status);

        var disputes = await query.OrderByDescending(d => d.CreatedAt).ToListAsync();

        return Ok(new DisputeListResponse
        {
            Message = "Disputes retrieved successfully",
            Data = disputes.Select(MapToListItem).ToList()
        });
    }

    // PUT api/disputes/{id}/resolve — admin only. Closes out a dispute the admin has already
    // handled outside the app; there is no customer-facing resolution workflow.
    [HttpPut("{id}/resolve")]
    [Authorize(Roles = "admin")]
    public async Task<IActionResult> ResolveDispute(Guid id, [FromBody] ResolveDisputeRequest request)
    {
        if (!this.TryGetUserId(out var adminId)) return Unauthorized();

        var dispute = await _context.Disputes
            .Include(d => d.Initiator)
            .Include(d => d.PickupRequest!).ThenInclude(pr => pr.Grocery)
            .Include(d => d.PickupRequest!).ThenInclude(pr => pr.Ngo)
            .FirstOrDefaultAsync(d => d.Id == id);

        if (dispute == null)
            return NotFound(new { message = "Dispute not found" });

        if (dispute.Status != "open" && dispute.Status != "under_review")
            return BadRequest(new { message = $"Cannot resolve a dispute with status '{dispute.Status}'" });

        dispute.Status = request.Status;
        if (!string.IsNullOrWhiteSpace(request.GroceryStatement))
            dispute.GroceryStatement = request.GroceryStatement;
        dispute.AdminResolution = request.AdminResolution;
        dispute.ResolvedByAdminId = adminId;
        dispute.ResolvedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        return Ok(new DisputeResponse { Message = "Dispute resolved successfully", Data = MapToListItem(dispute) });
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

    private static DisputeListItemData MapToListItem(Dispute d) => new()
    {
        Id = d.Id.ToString(),
        PickupRequestId = d.PickupRequestId.ToString(),
        ListingTitle = d.PickupRequest?.ListingTitle ?? "",
        PickupDate = d.PickupRequest?.PickupDate.ToString("yyyy-MM-dd") ?? "",
        Ngo = new DisputePartyContact
        {
            Id = d.Initiator?.Id.ToString() ?? "",
            Name = d.Initiator?.Name ?? "",
            Email = d.Initiator?.Email ?? "",
            Phone = d.Initiator?.Phone
        },
        Grocery = new DisputePartyContact
        {
            Id = d.PickupRequest?.Grocery?.Id.ToString() ?? "",
            Name = d.PickupRequest?.Grocery?.Name ?? "",
            Email = d.PickupRequest?.Grocery?.Email ?? "",
            Phone = d.PickupRequest?.Grocery?.Phone
        },
        Reason = d.Reason,
        NgoStatement = d.NgoStatement,
        GroceryStatement = d.GroceryStatement,
        PhotoEvidenceUrl = d.PhotoEvidenceUrl,
        Status = d.Status,
        AdminResolution = d.AdminResolution,
        CreatedAt = d.CreatedAt.ToString("o"),
        ResolvedAt = d.ResolvedAt?.ToString("o")
    };
}

public class OpenDisputeRequest
{
    public Guid PickupRequestId { get; set; }
    public string Reason { get; set; } = string.Empty;
    public string? Statement { get; set; }
}
