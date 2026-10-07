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
    private readonly IPushNotificationService _pushNotificationService;

    private static readonly string[] ClosedStatuses = { "resolved_ngo", "resolved_grocery", "dismissed" };

    public DisputesController(
        ApplicationDbContext context,
        IStorageService storageService,
        IPushNotificationService pushNotificationService)
    {
        _context = context;
        _storageService = storageService;
        _pushNotificationService = pushNotificationService;
    }

    // POST api/disputes — NGO opens a dispute on a completed pickup
    [HttpPost]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> OpenDispute(
        [FromForm] OpenDisputeRequest request,
        IFormFile? photo)
    {
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

        if (!DisputeReasons.IsValidNgoReason(request.Reason))
            return BadRequest(new { message = "Unknown dispute reason" });

        var statement = request.Statement?.Trim();
        if (string.IsNullOrEmpty(statement))
            return BadRequest(new { message = "A statement is required" });
        if (statement.Length > 2000)
            return BadRequest(new { message = "Statement must be 2000 characters or fewer" });

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
            NgoStatement = statement,
            PhotoEvidenceUrl = photoUrl,
            Status = "open",
            CreatedAt = DateTime.UtcNow
        };

        _context.Disputes.Add(dispute);
        await _context.SaveChangesAsync();

        var initiator = await _context.Organizations.FindAsync(userId);
        await _pushNotificationService.SendNewDisputeAlertToAdmins(
            dispute.Id, DisputeReasons.Label(dispute.Reason), initiator?.Name ?? "An NGO");

        return Ok(new { message = "Dispute opened successfully", data = MapToDto(dispute) });
    }

    // GET api/disputes/mine — the NGO's own disputes, so the app can tell whether a pickup has
    // already been reported and show how the admin resolved it.
    [HttpGet("mine")]
    public async Task<IActionResult> GetMyDisputes()
    {
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

        var disputes = await _context.Disputes
            .Where(d => d.InitiatorId == userId)
            .OrderByDescending(d => d.CreatedAt)
            .ToListAsync();

        return Ok(new MyDisputeListResponse
        {
            Message = "Disputes retrieved successfully",
            Data = disputes.Select(MapToMyDispute).ToList()
        });
    }

    // GET api/disputes — admin only. There is no in-app negotiation between the NGO and the
    // grocery, so this hands the admin everything needed (both parties' contact details, the
    // pickup context, the evidence) to resolve it by phone or email instead.
    [HttpGet]
    [Authorize(Roles = "admin")]
    public async Task<IActionResult> GetDisputes([FromQuery] string? status = null, [FromQuery] Guid? pickupRequestId = null)
    {
        var query = _context.Disputes
            .Include(d => d.Initiator)
            .Include(d => d.PickupRequest!).ThenInclude(pr => pr.Grocery)
            .Include(d => d.PickupRequest!).ThenInclude(pr => pr.Ngo)
            .Include(d => d.PickupRequest!).ThenInclude(pr => pr.Items)
            .AsQueryable();

        // The admin request detail screen asks for the dispute of one pickup.
        if (pickupRequestId.HasValue)
            query = query.Where(d => d.PickupRequestId == pickupRequestId.Value);

        // "resolved" is the closed tab: every outcome an admin can record, not one exact status.
        if (status == "resolved")
            query = query.Where(d => ClosedStatuses.Contains(d.Status));
        else if (!string.IsNullOrEmpty(status))
            query = query.Where(d => d.Status == status);

        var disputes = await query.OrderByDescending(d => d.CreatedAt).ToListAsync();

        return Ok(new DisputeListResponse
        {
            Message = "Disputes retrieved successfully",
            Data = disputes.Select(MapToListItem).ToList()
        });
    }

    // PUT api/disputes/{id}/review — admin only. Claims an open dispute so it moves from the
    // open queue to "under review" while the admin contacts both parties.
    [HttpPut("{id}/review")]
    [Authorize(Roles = "admin")]
    public async Task<IActionResult> StartReview(Guid id)
    {
        var dispute = await _context.Disputes
            .Include(d => d.Initiator)
            .Include(d => d.PickupRequest!).ThenInclude(pr => pr.Grocery)
            .Include(d => d.PickupRequest!).ThenInclude(pr => pr.Ngo)
            .Include(d => d.PickupRequest!).ThenInclude(pr => pr.Items)
            .FirstOrDefaultAsync(d => d.Id == id);

        if (dispute == null)
            return NotFound(new { message = "Dispute not found" });

        if (dispute.Status != "open")
            return BadRequest(new { message = $"Cannot start a review on a dispute with status '{dispute.Status}'" });

        dispute.Status = "under_review";
        await _context.SaveChangesAsync();

        return Ok(new DisputeResponse { Message = "Dispute is now under review", Data = MapToListItem(dispute) });
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
            .Include(d => d.PickupRequest!).ThenInclude(pr => pr.Items)
            .FirstOrDefaultAsync(d => d.Id == id);

        if (dispute == null)
            return NotFound(new { message = "Dispute not found" });

        if (dispute.Status != "open" && dispute.Status != "under_review")
            return BadRequest(new { message = $"Cannot resolve a dispute with status '{dispute.Status}'" });

        if (!ClosedStatuses.Contains(request.Status))
            return BadRequest(new { message = "Unknown resolution outcome" });

        dispute.Status = request.Status;
        if (!string.IsNullOrWhiteSpace(request.GroceryStatement))
            dispute.GroceryStatement = request.GroceryStatement;
        dispute.AdminResolution = request.AdminResolution;
        dispute.ResolvedByAdminId = adminId;
        dispute.ResolvedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        var reasonLabel = DisputeReasons.Label(dispute.Reason);
        await _pushNotificationService.SendDisputeResolvedNotification(
            dispute.InitiatorId, dispute.PickupRequestId, reasonLabel, dispute.Status, dispute.AdminResolution, "my_requests");
        if (dispute.PickupRequest != null)
        {
            await _pushNotificationService.SendDisputeResolvedNotification(
                dispute.PickupRequest.GroceryId, dispute.PickupRequestId, reasonLabel, dispute.Status, dispute.AdminResolution, "grocery_requests");
        }

        return Ok(new DisputeResponse { Message = "Dispute resolved successfully", Data = MapToListItem(dispute) });
    }

    private static object MapToDto(Dispute d) => new
    {
        id = d.Id.ToString(),
        pickupRequestId = d.PickupRequestId.ToString(),
        reason = d.Reason,
        ngoStatement = d.NgoStatement,
        groceryStatement = d.GroceryStatement,
        photoEvidenceUrl = d.PhotoEvidenceUrl,
        status = d.Status,
        adminResolution = d.AdminResolution,
        createdAt = d.CreatedAt.ToString("o")
    };

    private static MyDisputeData MapToMyDispute(Dispute d) => new()
    {
        Id = d.Id.ToString(),
        PickupRequestId = d.PickupRequestId.ToString(),
        Reason = d.Reason,
        NgoStatement = d.NgoStatement,
        PhotoEvidenceUrl = d.PhotoEvidenceUrl,
        Status = d.Status,
        AdminResolution = d.AdminResolution,
        CreatedAt = d.CreatedAt.ToString("o")
    };

    // A pickup holds one line per listing; older single-listing pickups only carry the snapshot
    // fields on the request itself, so fall back to those when there are no item rows.
    private static List<DisputeListingItem> MapListingItems(PickupRequest? pr)
    {
        if (pr == null) return new();
        if (pr.Items.Count > 0)
        {
            return pr.Items.Select(i => new DisputeListingItem
            {
                Title = i.ListingTitle,
                Category = i.ListingCategory,
                Quantity = i.RequestedQuantity,
                Unit = i.ListingUnit,
                ExpiryDate = i.ListingExpiryDate,
                PhotoUrl = i.ListingPhotoUrl
            }).ToList();
        }
        return new()
        {
            new DisputeListingItem
            {
                Title = pr.ListingTitle,
                Category = pr.ListingCategory,
                Quantity = pr.RequestedQuantity ?? 0,
                Unit = pr.ListingUnit,
                ExpiryDate = pr.ListingExpiryDate
            }
        };
    }

    private static DisputeListItemData MapToListItem(Dispute d) => new()
    {
        Id = d.Id.ToString(),
        PickupRequestId = d.PickupRequestId.ToString(),
        ListingTitle = d.PickupRequest?.ListingTitle ?? "",
        PickupDate = d.PickupRequest?.PickupDate.ToString("yyyy-MM-dd") ?? "",
        Items = MapListingItems(d.PickupRequest),
        Ngo = new DisputePartyContact
        {
            Id = d.Initiator?.Id.ToString() ?? "",
            Name = d.Initiator?.Name ?? "",
            Email = d.Initiator?.Email ?? "",
            Phone = d.Initiator?.Phone,
            ProfilePictureUrl = d.Initiator?.ProfilePictureUrl
        },
        Grocery = new DisputePartyContact
        {
            Id = d.PickupRequest?.Grocery?.Id.ToString() ?? "",
            Name = d.PickupRequest?.Grocery?.Name ?? "",
            Email = d.PickupRequest?.Grocery?.Email ?? "",
            Phone = d.PickupRequest?.Grocery?.Phone,
            ProfilePictureUrl = d.PickupRequest?.Grocery?.ProfilePictureUrl
        },
        Reason = d.Reason,
        NgoStatement = d.NgoStatement,
        GroceryStatement = d.GroceryStatement,
        PhotoEvidenceUrl = d.PhotoEvidenceUrl,
        Status = d.Status,
        AdminResolution = d.AdminResolution,
        CreatedAt = d.CreatedAt.ToString("o")
    };
}

public class OpenDisputeRequest
{
    public Guid PickupRequestId { get; set; }
    public string Reason { get; set; } = string.Empty;
    public string? Statement { get; set; }
}
