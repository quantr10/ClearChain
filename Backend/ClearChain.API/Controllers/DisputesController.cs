using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using ClearChain.API.Common;
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
