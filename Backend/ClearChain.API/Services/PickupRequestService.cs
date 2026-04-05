using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
using ClearChain.API.DTOs.PickupRequests;
using ClearChain.API.DTOs.Inventory;

namespace ClearChain.API.Services;

public enum PickupRequestServiceError
{
    NotFound,
    Forbidden,
    InvalidStatus,
    InvalidInput,
    StorageError,
    DatabaseError
}

public record PickupRequestServiceResult(
    bool Success,
    PickupRequestServiceError? Error = null,
    string? ErrorMessage = null,
    PickupRequestData? Data = null,
    PickupRequestsResponse? ListData = null,
    InventoryItemData? InventoryData = null
);

public interface IPickupRequestService
{
    Task<PickupRequestServiceResult> CreateAsync(Guid ngoId, CreatePickupRequestRequest request);
    Task<PickupRequestServiceResult> CancelAsync(Guid requestId, Guid callerId);
    Task<PickupRequestServiceResult> MarkPickedUpAsync(Guid requestId, Guid callerId, Stream photoStream, string fileName);
    Task<PickupRequestServiceResult> GetByIdAsync(Guid requestId);
    Task<PickupRequestServiceResult> GetNgoRequestsAsync(Guid ngoId, int page, int pageSize);
    Task<PickupRequestServiceResult> GetGroceryRequestsAsync(Guid groceryId, int page, int pageSize);
    Task<PickupRequestServiceResult> ApproveAsync(Guid requestId, Guid groceryId);
    Task<PickupRequestServiceResult> MarkReadyAsync(Guid requestId, Guid groceryId);
}

public class PickupRequestService : IPickupRequestService
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<PickupRequestService> _logger;
    private readonly IStorageService _storageService;
    private readonly IPickupNotificationService _notificationService;
    private readonly IInventoryNotificationService _inventoryNotificationService;
    private readonly IAdminNotificationService _adminNotificationService;
    private readonly IPushNotificationService _pushNotificationService;

    public PickupRequestService(
        ApplicationDbContext context,
        ILogger<PickupRequestService> logger,
        IStorageService storageService,
        IPickupNotificationService notificationService,
        IInventoryNotificationService inventoryNotificationService,
        IAdminNotificationService adminNotificationService,
        IPushNotificationService pushNotificationService)
    {
        _context = context;
        _logger = logger;
        _storageService = storageService;
        _notificationService = notificationService;
        _inventoryNotificationService = inventoryNotificationService;
        _adminNotificationService = adminNotificationService;
        _pushNotificationService = pushNotificationService;
    }

    public async Task<PickupRequestServiceResult> CreateAsync(Guid ngoId, CreatePickupRequestRequest request)
    {
        var ngo = await _context.Organizations.FindAsync(ngoId);
        if (ngo == null || ngo.Type.ToLower() != "ngo")
            return Fail(PickupRequestServiceError.Forbidden, "Only NGOs can create pickup requests");

        var listing = await _context.ClearanceListings
            .Include(l => l.Grocery)
            .Include(l => l.Group)
            .FirstOrDefaultAsync(l => l.Id.ToString() == request.ListingId);

        if (listing == null)
            return Fail(PickupRequestServiceError.NotFound, "Listing not found");
        if (listing.Status != ListingStatus.Open)
            return Fail(PickupRequestServiceError.InvalidStatus, "Listing is not available");
        if (request.RequestedQuantity > listing.Quantity)
            return Fail(PickupRequestServiceError.InvalidInput, $"Requested quantity exceeds available quantity ({listing.Quantity})");
        if (!DateTime.TryParse(request.PickupDate, out var pickupDate))
            return Fail(PickupRequestServiceError.InvalidInput, "Invalid pickup date format. Use yyyy-MM-dd.");
        if (listing.Group == null)
            return Fail(PickupRequestServiceError.InvalidInput, "Listing has no associated group");

        var pickupDateUtc = DateTime.SpecifyKind(pickupDate, DateTimeKind.Utc);
        if (pickupDateUtc.Date < DateTime.UtcNow.Date)
            return Fail(PickupRequestServiceError.InvalidInput, "Pickup date cannot be in the past");
        if (listing.ExpirationDate.HasValue && pickupDateUtc.Date > listing.ExpirationDate.Value.Date)
            return Fail(PickupRequestServiceError.InvalidInput, $"Pickup date cannot be after expiry date ({listing.ExpirationDate.Value:yyyy-MM-dd})");

        var (reservedListing, pickupRequest) = SplitListing(
            listing, listing.Group, request.RequestedQuantity,
            pickupDateUtc, request.PickupTime, request.Notes, ngoId);

        _context.PickupRequests.Add(pickupRequest);
        await _context.SaveChangesAsync();

        var data = MapToData(pickupRequest, reservedListing.Id, ngo.Name, listing.Grocery?.Name ?? "");

        await _notificationService.NotifyPickupRequestCreated(data);

        return new PickupRequestServiceResult(true, Data: data);
    }

    public async Task<PickupRequestServiceResult> CancelAsync(Guid requestId, Guid callerId)
    {
        var pr = await _context.PickupRequests
            .Include(p => p.Ngo)
            .Include(p => p.Grocery)
            .FirstOrDefaultAsync(p => p.Id == requestId &&
                (p.NgoId == callerId || p.GroceryId == callerId));

        if (pr == null)
            return Fail(PickupRequestServiceError.NotFound, "Pickup request not found");
        if (pr.Status != PickupRequestStatus.Pending)
            return Fail(PickupRequestServiceError.InvalidStatus,
                $"Cannot cancel request with status: {pr.Status.ToString().ToLower()}. Only PENDING requests can be cancelled.");

        bool isGroceryRejecting = pr.GroceryId == callerId;

        var listing = pr.ListingId.HasValue
            ? await _context.ClearanceListings
                .Include(l => l.Group)
                .FirstOrDefaultAsync(l => l.Id == pr.ListingId.Value)
            : null;

        await using var tx = await _context.Database.BeginTransactionAsync();
        try
        {
            pr.Status = isGroceryRejecting ? PickupRequestStatus.Rejected : PickupRequestStatus.Cancelled;
            pr.ListingId = null;

            if (listing?.Group != null)
                await SmartMergeOnCancel(listing, listing.Group);

            await _context.SaveChangesAsync();
            await tx.CommitAsync();
        }
        catch (Exception ex)
        {
            await tx.RollbackAsync();
            _logger.LogError(ex, "Transaction failed during CancelAsync for request {Id}", requestId);
            return Fail(PickupRequestServiceError.DatabaseError, "An error occurred. Please try again.");
        }

        var data = MapToData(pr);

        if (isGroceryRejecting)
        {
            await _notificationService.NotifyPickupRequestCancelled(data);
            await _pushNotificationService.SendPickupRejectedNotification(pr.NgoId, data);
        }
        else
        {
            await _notificationService.NotifyPickupRequestCancelled(data);
            await _pushNotificationService.SendPickupRequestCancelledNotification(pr.GroceryId, data);
        }

        return new PickupRequestServiceResult(true, Data: data);
    }

    public async Task<PickupRequestServiceResult> MarkPickedUpAsync(
        Guid requestId, Guid callerId, Stream photoStream, string fileName)
    {
        var pickupRequest = await _context.PickupRequests
            .Include(p => p.Ngo)
            .Include(p => p.Grocery)
            .FirstOrDefaultAsync(pr => pr.Id == requestId &&
                (pr.GroceryId == callerId || pr.NgoId == callerId));

        if (pickupRequest == null)
            return Fail(PickupRequestServiceError.NotFound, "Pickup request not found");
        if (pickupRequest.Status != PickupRequestStatus.Ready)
            return Fail(PickupRequestServiceError.InvalidStatus, "Can only mark ready requests as picked up");

        var listing = pickupRequest.ListingId.HasValue
            ? await _context.ClearanceListings
                .Include(l => l.Group)
                .FirstOrDefaultAsync(l => l.Id == pickupRequest.ListingId.Value)
            : null;

        // Upload outside transaction
        string proofPhotoUrl;
        try
        {
            proofPhotoUrl = await _storageService.UploadPickupProofAsync(photoStream, fileName);
            _logger.LogInformation("Proof photo uploaded: {Url}", proofPhotoUrl);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error uploading proof photo");
            return Fail(PickupRequestServiceError.StorageError, "Error uploading photo. Please try again.");
        }

        InventoryItemData? inventoryDto = null;
        await using var tx = await _context.Database.BeginTransactionAsync();
        try
        {
            pickupRequest.Status = PickupRequestStatus.Completed;
            pickupRequest.MarkedPickedUpAt = DateTime.UtcNow;
            pickupRequest.ConfirmedReceivedAt = DateTime.UtcNow;
            pickupRequest.ProofPhotoUrl = proofPhotoUrl;
            pickupRequest.ListingId = null;

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
                    Status = InventoryStatus.Active,
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
                        listing.Group.IsFullyConsumed = true;

                    var remainingChildren = await _context.ClearanceListings
                        .Where(l => l.GroupId == listing.GroupId && l.Id != listing.Id)
                        .CountAsync();

                    if (remainingChildren == 0 && listing.Group.IsFullyConsumed)
                        _context.ListingGroups.Remove(listing.Group);
                }

                _context.ClearanceListings.Remove(listing);

                inventoryDto = new InventoryItemData
                {
                    Id = inventoryItem.Id.ToString(),
                    NgoId = inventoryItem.NgoId.ToString(),
                    ProductName = inventoryItem.ProductName,
                    Category = inventoryItem.Category,
                    Quantity = inventoryItem.Quantity,
                    Unit = inventoryItem.Unit,
                    ExpiryDate = inventoryItem.ExpiryDate.ToString("yyyy-MM-dd"),
                    Status = inventoryItem.Status.ToString().ToLower(),
                    ReceivedAt = inventoryItem.ReceivedAt.ToString("o"),
                    PickupRequestId = inventoryItem.PickupRequestId.ToString()
                };
            }

            await _context.SaveChangesAsync();
            await tx.CommitAsync();
        }
        catch (Exception ex)
        {
            await tx.RollbackAsync();
            _logger.LogError(ex, "Transaction failed during MarkPickedUpAsync for request {Id}", requestId);
            return Fail(PickupRequestServiceError.DatabaseError, "An error occurred while completing the pickup. Please try again.");
        }

        var data = MapToData(pickupRequest, proofPhotoUrl: proofPhotoUrl);

        if (inventoryDto != null)
            await _inventoryNotificationService.NotifyInventoryItemAdded(inventoryDto);

        await _notificationService.NotifyPickupRequestStatusChanged(data, "ready");

        try
        {
            await _adminNotificationService.NotifyTransactionCompleted(new TransactionCompletedNotification
            {
                TransactionId = pickupRequest.Id.ToString(),
                NgoId = pickupRequest.NgoId.ToString(),
                NgoName = pickupRequest.Ngo?.Name ?? "",
                GroceryId = pickupRequest.GroceryId.ToString(),
                GroceryName = pickupRequest.Grocery?.Name ?? "",
                ProductName = listing?.ProductName ?? "",
                Quantity = pickupRequest.RequestedQuantity ?? 0,
                Unit = listing?.Unit ?? "",
                CompletedAt = DateTime.UtcNow
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send admin transaction notification");
        }

        await _pushNotificationService.SendPickupCompletedNotification(pickupRequest.GroceryId, data);
        await _pushNotificationService.SendInventoryAddedNotification(
            pickupRequest.NgoId,
            listing?.ProductName ?? "",
            pickupRequest.RequestedQuantity ?? 0,
            listing?.Unit ?? "");

        return new PickupRequestServiceResult(true, Data: data, InventoryData: inventoryDto);
    }

    public async Task<PickupRequestServiceResult> GetByIdAsync(Guid requestId)
    {
        var pr = await _context.PickupRequests
            .Include(p => p.Ngo)
            .Include(p => p.Grocery)
            .FirstOrDefaultAsync(p => p.Id == requestId);

        if (pr == null)
            return Fail(PickupRequestServiceError.NotFound, "Pickup request not found");

        var listing = pr.ListingId.HasValue
            ? await _context.ClearanceListings.FindAsync(pr.ListingId.Value)
            : null;

        var data = MapToData(pr);
        if (listing != null)
        {
            if (string.IsNullOrEmpty(data.ListingTitle)) data.ListingTitle = listing.ProductName;
            if (string.IsNullOrEmpty(data.ListingCategory)) data.ListingCategory = listing.Category;
        }

        return new PickupRequestServiceResult(true, Data: data);
    }

    public async Task<PickupRequestServiceResult> GetNgoRequestsAsync(Guid ngoId, int page, int pageSize)
    {
        var (clampedPage, clampedSize) = Clamp(page, pageSize);
        var baseQuery = _context.PickupRequests.Where(pr => pr.NgoId == ngoId);
        var total = await baseQuery.CountAsync();

        var items = await baseQuery
            .Include(pr => pr.Ngo)
            .Include(pr => pr.Grocery)
            .OrderByDescending(pr => pr.RequestedAt)
            .Skip((clampedPage - 1) * clampedSize)
            .Take(clampedSize)
            .ToListAsync();

        return new PickupRequestServiceResult(true, ListData: ToPagedResponse(items, total, clampedPage, clampedSize));
    }

    public async Task<PickupRequestServiceResult> GetGroceryRequestsAsync(Guid groceryId, int page, int pageSize)
    {
        var (clampedPage, clampedSize) = Clamp(page, pageSize);
        var baseQuery = _context.PickupRequests.Where(pr => pr.GroceryId == groceryId);
        var total = await baseQuery.CountAsync();

        var items = await baseQuery
            .Include(pr => pr.Ngo)
            .Include(pr => pr.Grocery)
            .OrderByDescending(pr => pr.RequestedAt)
            .Skip((clampedPage - 1) * clampedSize)
            .Take(clampedSize)
            .ToListAsync();

        return new PickupRequestServiceResult(true, ListData: ToPagedResponse(items, total, clampedPage, clampedSize));
    }

    public async Task<PickupRequestServiceResult> ApproveAsync(Guid requestId, Guid groceryId)
    {
        var pr = await _context.PickupRequests
            .Include(p => p.Ngo)
            .Include(p => p.Grocery)
            .FirstOrDefaultAsync(p => p.Id == requestId && p.GroceryId == groceryId);

        if (pr == null)
            return Fail(PickupRequestServiceError.NotFound, "Pickup request not found");
        if (pr.Status != PickupRequestStatus.Pending)
            return Fail(PickupRequestServiceError.InvalidStatus,
                $"Cannot approve request with status: {pr.Status.ToString().ToLower()}");

        pr.Status = PickupRequestStatus.Approved;
        await _context.SaveChangesAsync();

        var listing = pr.ListingId.HasValue
            ? await _context.ClearanceListings.FindAsync(pr.ListingId.Value)
            : null;

        var data = MapToData(pr);
        if (listing != null)
        {
            data.ListingTitle = listing.ProductName;
            data.ListingCategory = listing.Category;
        }

        await _notificationService.NotifyPickupRequestStatusChanged(data, "pending");
        await _pushNotificationService.SendPickupApprovedNotification(pr.NgoId, data);

        return new PickupRequestServiceResult(true, Data: data);
    }

    public async Task<PickupRequestServiceResult> MarkReadyAsync(Guid requestId, Guid groceryId)
    {
        var pr = await _context.PickupRequests
            .Include(p => p.Ngo)
            .Include(p => p.Grocery)
            .FirstOrDefaultAsync(p => p.Id == requestId && p.GroceryId == groceryId);

        if (pr == null)
            return Fail(PickupRequestServiceError.NotFound, "Pickup request not found");
        if (pr.Status != PickupRequestStatus.Approved)
            return Fail(PickupRequestServiceError.InvalidStatus,
                $"Can only mark approved requests as ready. Current status: {pr.Status.ToString().ToLower()}");

        pr.Status = PickupRequestStatus.Ready;
        pr.MarkedReadyAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        var listing = pr.ListingId.HasValue
            ? await _context.ClearanceListings.FindAsync(pr.ListingId.Value)
            : null;

        var data = MapToData(pr);
        if (listing != null)
        {
            data.ListingTitle = listing.ProductName;
            data.ListingCategory = listing.Category;
        }

        await _notificationService.NotifyPickupRequestStatusChanged(data, "approved");
        await _pushNotificationService.SendPickupReadyNotification(pr.NgoId, data);

        return new PickupRequestServiceResult(true, Data: data);
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    private static PickupRequestServiceResult Fail(PickupRequestServiceError error, string message) =>
        new(false, error, message);

    private static (int page, int size) Clamp(int page, int pageSize) =>
        (Math.Max(1, page), Math.Clamp(pageSize, 1, 100));

    private static PickupRequestData MapToData(
        PickupRequest pr,
        Guid? listingId = null,
        string? ngoName = null,
        string? groceryName = null,
        string? proofPhotoUrl = null)
    {
        return new PickupRequestData
        {
            Id = pr.Id.ToString(),
            ListingId = listingId?.ToString() ?? pr.ListingId?.ToString() ?? "",
            NgoId = pr.NgoId.ToString(),
            NgoName = ngoName ?? pr.Ngo?.Name ?? "",
            GroceryId = pr.GroceryId.ToString(),
            GroceryName = groceryName ?? pr.Grocery?.Name ?? "",
            Status = pr.Status.ToString().ToLower(),
            RequestedQuantity = pr.RequestedQuantity ?? 0,
            PickupDate = pr.PickupDate.ToString("yyyy-MM-dd"),
            PickupTime = pr.PickupTime ?? "09:00",
            Notes = pr.Notes ?? "",
            ListingTitle = pr.ListingTitle ?? "",
            ListingCategory = pr.ListingCategory ?? "",
            CreatedAt = pr.RequestedAt.ToString("o"),
            ProofPhotoUrl = proofPhotoUrl ?? pr.ProofPhotoUrl
        };
    }

    private static PickupRequestsResponse ToPagedResponse(
        List<PickupRequest> items, int total, int page, int pageSize)
    {
        return new PickupRequestsResponse
        {
            Message = "Pickup requests retrieved successfully",
            Data = items.Select(pr => MapToData(pr)).ToList(),
            Total = total,
            Page = page,
            PageSize = pageSize,
            TotalPages = (int)Math.Ceiling((double)total / pageSize)
        };
    }

    private (ClearanceListing reservedListing, PickupRequest request) SplitListing(
        ClearanceListing sourceListing, ListingGroup group,
        int requestedQuantity, DateTime pickupDate,
        string pickupTime, string? notes, Guid ngoId)
    {
        var requestId = Guid.NewGuid();

        if (requestedQuantity >= sourceListing.Quantity)
        {
            sourceListing.Status = ListingStatus.Reserved;
            sourceListing.RelatedRequestId = requestId;
            sourceListing.UpdatedAt = DateTime.UtcNow;
            group.TotalAvailable -= sourceListing.Quantity;
            group.TotalReserved += sourceListing.Quantity;
            group.UpdatedAt = DateTime.UtcNow;

            return (sourceListing, new PickupRequest
            {
                Id = requestId, NgoId = ngoId, GroceryId = sourceListing.GroceryId,
                ListingId = sourceListing.Id, PickupDate = pickupDate,
                Status = PickupRequestStatus.Pending, RequestedAt = DateTime.UtcNow,
                RequestedQuantity = requestedQuantity, PickupTime = pickupTime, Notes = notes,
                ListingTitle = sourceListing.ProductName, ListingCategory = sourceListing.Category
            });
        }

        var reservedListing = new ClearanceListing
        {
            Id = Guid.NewGuid(), GroupId = group.Id, GroceryId = sourceListing.GroceryId,
            ProductName = sourceListing.ProductName, Category = sourceListing.Category,
            Quantity = requestedQuantity, Unit = sourceListing.Unit,
            ExpirationDate = sourceListing.ExpirationDate.HasValue
                ? DateTime.SpecifyKind(sourceListing.ExpirationDate.Value, DateTimeKind.Utc)
                : (DateTime?)null,
            ClearanceDeadline = DateTime.SpecifyKind(sourceListing.ClearanceDeadline, DateTimeKind.Utc),
            Notes = sourceListing.Notes, PhotoUrl = sourceListing.PhotoUrl,
            PickupTimeStart = sourceListing.PickupTimeStart, PickupTimeEnd = sourceListing.PickupTimeEnd,
            Status = ListingStatus.Reserved, SplitReason = "partial_request",
            RelatedRequestId = requestId, SplitFromListingId = sourceListing.Id,
            SplitIndex = group.ChildListings?.Count ?? 1,
            CreatedAt = DateTime.UtcNow, UpdatedAt = DateTime.UtcNow
        };

        sourceListing.Quantity -= requestedQuantity;
        sourceListing.UpdatedAt = DateTime.UtcNow;
        group.TotalAvailable -= requestedQuantity;
        group.TotalReserved += requestedQuantity;
        group.UpdatedAt = DateTime.UtcNow;

        _context.ClearanceListings.Add(reservedListing);

        return (reservedListing, new PickupRequest
        {
            Id = requestId, NgoId = ngoId, GroceryId = sourceListing.GroceryId,
            ListingId = reservedListing.Id, PickupDate = pickupDate,
            Status = PickupRequestStatus.Pending, RequestedAt = DateTime.UtcNow,
            RequestedQuantity = requestedQuantity, PickupTime = pickupTime, Notes = notes,
            ListingTitle = sourceListing.ProductName, ListingCategory = sourceListing.Category
        });
    }

    private async Task SmartMergeOnCancel(ClearanceListing cancelledListing, ListingGroup group)
    {
        if (cancelledListing.GroupId == null) return;

        var availableSiblings = await _context.ClearanceListings
            .Where(l => l.GroupId == group.Id && l.Id != cancelledListing.Id && l.Status == ListingStatus.Open)
            .ToListAsync();

        if (availableSiblings.Any())
        {
            var target = availableSiblings.First();
            target.Quantity += cancelledListing.Quantity;
            target.SplitReason = "merge";
            target.UpdatedAt = DateTime.UtcNow;
            group.TotalReserved -= cancelledListing.Quantity;
            group.TotalAvailable += cancelledListing.Quantity;
            group.UpdatedAt = DateTime.UtcNow;
            _context.ClearanceListings.Remove(cancelledListing);
        }
        else
        {
            cancelledListing.Status = ListingStatus.Open;
            cancelledListing.RelatedRequestId = null;
            cancelledListing.SplitReason = "cancel_restore";
            cancelledListing.UpdatedAt = DateTime.UtcNow;
            group.TotalReserved -= cancelledListing.Quantity;
            group.TotalAvailable += cancelledListing.Quantity;
            group.UpdatedAt = DateTime.UtcNow;
        }
    }
}
