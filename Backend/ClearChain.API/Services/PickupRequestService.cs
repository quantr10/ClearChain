using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
using ClearChain.API.DTOs.PickupRequests;
using ClearChain.API.DTOs.Inventory;
using System.Text.Json;

namespace ClearChain.API.Services;

public enum PickupRequestServiceError
{
    NotFound,
    Forbidden,
    InvalidStatus,
    InvalidInput,
    StorageError,
    DatabaseError,
    Conflict
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
    Task<PickupRequestServiceResult> CancelAsync(Guid requestId, Guid callerId, string? reason = null);
    Task<PickupRequestServiceResult> MarkPickedUpAsync(Guid requestId, Guid callerId, Stream photoStream, string fileName, string contentType);
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
        if (request.RequestedQuantity <= 0)
            return Fail(PickupRequestServiceError.InvalidInput, "Requested quantity must be at least 1");
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
            pickupDateUtc, request.PickupTime, request.Notes, ngoId,
            request.RequiresRefrigeration, request.IsFragile, request.IsHeavy);

        _context.PickupRequests.Add(pickupRequest);
        try
        {
            await _context.SaveChangesAsync();
        }
        catch (DbUpdateConcurrencyException)
        {
            // The listing/group was reserved or modified by someone else between our read
            // and this write (caught by the xmin concurrency token) — refuse rather than
            // silently overselling the same stock.
            return Fail(PickupRequestServiceError.Conflict,
                "This listing was just updated by someone else. Please refresh and try again.");
        }

        var data = MapToData(pickupRequest, reservedListing.Id, ngo.Name, listing.Grocery?.Name ?? "");

        // SignalR reaches the grocery only while they have the app open; the push is what
        // gets a new request in front of them when it isn't.
        await _notificationService.NotifyPickupRequestCreatedAsync(data);
        await _pushNotificationService.SendPickupRequestCreatedNotification(pickupRequest.GroceryId, data);

        return new PickupRequestServiceResult(true, Data: data);
    }

    public async Task<PickupRequestServiceResult> CancelAsync(Guid requestId, Guid callerId, string? reason = null)
    {
        var pr = await _context.PickupRequests
            .Include(p => p.Ngo)
            .Include(p => p.Grocery)
            .Include(p => p.Items)
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
            pr.CancellationReason = reason;
            pr.ListingId = null;

            if (pr.Items.Any())
            {
                var reservedIds = pr.Items
                    .Where(i => i.ReservedListingId.HasValue)
                    .Select(i => i.ReservedListingId!.Value)
                    .ToList();
                var reservedListings = await _context.ClearanceListings
                    .Include(l => l.Group)
                    .Where(l => reservedIds.Contains(l.Id))
                    .ToListAsync();
                foreach (var reserved in reservedListings)
                {
                    if (reserved.Group != null)
                        await SmartMergeOnCancel(reserved, reserved.Group);
                }

                var deletedReservedIds = reservedListings
                    .Where(l => _context.Entry(l).State == EntityState.Deleted)
                    .Select(l => l.Id)
                    .ToHashSet();

                foreach (var item in pr.Items)
                {
                    if (item.OriginalListingId.HasValue && deletedReservedIds.Contains(item.OriginalListingId.Value))
                        item.OriginalListingId = null;
                    item.ReservedListingId = null;
                }
            }
            else if (listing?.Group != null)
            {
                await SmartMergeOnCancel(listing, listing.Group);
            }

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
            await _notificationService.NotifyPickupRequestCancelledAsync(data);
            await _pushNotificationService.SendPickupRejectedNotification(pr.NgoId, data);
        }
        else
        {
            await _notificationService.NotifyPickupRequestCancelledAsync(data);
            await _pushNotificationService.SendPickupRequestCancelledNotification(pr.GroceryId, data);
        }

        return new PickupRequestServiceResult(true, Data: data);
    }

    public async Task<PickupRequestServiceResult> MarkPickedUpAsync(
        Guid requestId, Guid callerId, Stream photoStream, string fileName, string contentType)
    {
        var pickupRequest = await _context.PickupRequests
            .Include(p => p.Ngo)
            .Include(p => p.Grocery)
            .Include(p => p.Items)
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
            proofPhotoUrl = await _storageService.UploadPickupProofAsync(photoStream, fileName, contentType);
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

            if (pickupRequest.Items.Any())
            {
                var reservedIds = pickupRequest.Items
                    .Where(i => i.ReservedListingId.HasValue)
                    .Select(i => i.ReservedListingId!.Value)
                    .ToList();
                var reservedListings = await _context.ClearanceListings
                    .Include(l => l.Group)
                    .Where(l => reservedIds.Contains(l.Id))
                    .ToListAsync();

                foreach (var item in pickupRequest.Items)
                {
                    var reserved = item.ReservedListingId.HasValue
                        ? reservedListings.FirstOrDefault(l => l.Id == item.ReservedListingId.Value)
                        : null;
                    var expiryDate = reserved?.ExpirationDate.HasValue == true
                        ? DateTime.SpecifyKind(reserved.ExpirationDate.Value, DateTimeKind.Utc)
                        : DateTime.UtcNow.AddDays(7);

                    var inventoryItem = new Inventory
                    {
                        Id = Guid.NewGuid(),
                        NgoId = pickupRequest.NgoId,
                        PickupRequestId = pickupRequest.Id,
                        ProductName = item.ListingTitle,
                        Category = item.ListingCategory,
                        Quantity = item.RequestedQuantity,
                        Unit = item.ListingUnit,
                        ExpiryDate = expiryDate,
                        PhotoUrl = item.ListingPhotoUrl ?? FirstImageUrl(reserved?.PhotoUrl),
                        Status = InventoryStatus.Active,
                        ReceivedAt = DateTime.UtcNow,
                        CreatedAt = DateTime.UtcNow,
                        UpdatedAt = DateTime.UtcNow
                    };
                    _context.Inventories.Add(inventoryItem);

                    if (reserved != null)
                    {
                        if (reserved.Group != null)
                        {
                            reserved.Group.TotalReserved -= reserved.Quantity;
                            reserved.Group.TotalCompleted += reserved.Quantity;
                            reserved.Group.UpdatedAt = DateTime.UtcNow;
                            if (reserved.Group.TotalCompleted + reserved.Group.TotalRemoved >= reserved.Group.OriginalQuantity)
                                reserved.Group.IsFullyConsumed = true;
                        }
                        item.ReservedListingId = null;
                        if (item.OriginalListingId == reserved.Id)
                            item.OriginalListingId = null;
                        _context.ClearanceListings.Remove(reserved);
                    }

                    inventoryDto ??= new InventoryItemData
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
                        PickupRequestId = inventoryItem.PickupRequestId.ToString(),
                        PhotoUrl = inventoryItem.PhotoUrl
                    };
                }
            }
            else if (listing != null)
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
                    PhotoUrl = FirstImageUrl(listing.PhotoUrl),
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

                    if (listing.Group.TotalCompleted + listing.Group.TotalRemoved >= listing.Group.OriginalQuantity)
                        listing.Group.IsFullyConsumed = true;

                    var remainingChildren = await _context.ClearanceListings
                        .Where(l => l.GroupId == listing.GroupId && l.Id != listing.Id)
                        .CountAsync();

                    if (remainingChildren == 0 && listing.Group.IsFullyConsumed)
                    {
                        var hasPickupHistory = await _context.PickupRequestItems
                            .AnyAsync(i => i.ListingGroupId == listing.Group.Id);
                        if (!hasPickupHistory)
                            _context.ListingGroups.Remove(listing.Group);
                    }
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
                    PickupRequestId = inventoryItem.PickupRequestId.ToString(),
                    PhotoUrl = inventoryItem.PhotoUrl
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
            await _inventoryNotificationService.NotifyInventoryItemAddedAsync(inventoryDto);

        await _notificationService.NotifyPickupRequestStatusChangedAsync(data, "ready");

        try
        {
            await _adminNotificationService.NotifyTransactionCompletedAsync(new TransactionCompletedNotification
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

            var today = DateTime.UtcNow.Date;
            await _adminNotificationService.NotifyStatsUpdatedAsync(new PlatformStatsNotification
            {
                TotalNGOs = await _context.Organizations.CountAsync(o => o.Type == "ngo"),
                TotalGroceries = await _context.Organizations.CountAsync(o => o.Type == "grocery"),
                TotalDonations = await _context.PickupRequests.CountAsync(),
                ActiveListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Open),
                PendingRequests = await _context.PickupRequests.CountAsync(r => r.Status == PickupRequestStatus.Pending),
                CompletedToday = await _context.PickupRequests.CountAsync(r =>
                    r.Status == PickupRequestStatus.Completed && r.RequestedAt.Date == today),
                UpdatedAt = DateTime.UtcNow
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
            .Include(p => p.Items)
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
            data.ListingDescription = listing.Notes;
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
            .Include(pr => pr.Items)
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
            .Include(pr => pr.Items)
            .OrderByDescending(pr => pr.RequestedAt)
            .Skip((clampedPage - 1) * clampedSize)
            .Take(clampedSize)
            .ToListAsync();

        // Build NGO pickup-rate map for all distinct NGOs in this page
        var ngoIds = items.Select(pr => pr.NgoId).Distinct().ToList();
        var ngoStats = await _context.PickupRequests
            .Where(pr => ngoIds.Contains(pr.NgoId))
            .GroupBy(pr => pr.NgoId)
            .Select(g => new
            {
                NgoId = g.Key,
                Total = g.Count(),
                Completed = g.Count(x => x.Status == PickupRequestStatus.Completed)
            })
            .ToDictionaryAsync(x => x.NgoId);

        var dtos = items.Select(pr =>
        {
            double? rate = null;
            int completed = 0;
            if (ngoStats.TryGetValue(pr.NgoId, out var stats) && stats.Total > 0)
            {
                rate = Math.Round((double)stats.Completed / stats.Total, 2);
                completed = stats.Completed;
            }
            return MapToData(pr, ngoPickupRate: rate, ngoTotalCompleted: completed);
        }).ToList();

        return new PickupRequestServiceResult(true, ListData: new PickupRequestsResponse
        {
            Message = "Pickup requests retrieved successfully",
            Data = dtos,
            Total = total,
            Page = clampedPage,
            PageSize = clampedSize,
            TotalPages = (int)Math.Ceiling((double)total / clampedSize)
        });
    }

    public async Task<PickupRequestServiceResult> ApproveAsync(Guid requestId, Guid groceryId)
    {
        var pr = await _context.PickupRequests
            .Include(p => p.Ngo)
            .Include(p => p.Grocery)
            .Include(p => p.Items)
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

        await _notificationService.NotifyPickupRequestStatusChangedAsync(data, "pending");
        await _pushNotificationService.SendPickupApprovedNotification(pr.NgoId, data);

        return new PickupRequestServiceResult(true, Data: data);
    }

    public async Task<PickupRequestServiceResult> MarkReadyAsync(Guid requestId, Guid groceryId)
    {
        var pr = await _context.PickupRequests
            .Include(p => p.Ngo)
            .Include(p => p.Grocery)
            .Include(p => p.Items)
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

        await _notificationService.NotifyPickupRequestStatusChangedAsync(data, "approved");
        await _pushNotificationService.SendPickupReadyNotification(pr.NgoId, data);

        return new PickupRequestServiceResult(true, Data: data);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private static PickupRequestServiceResult Fail(PickupRequestServiceError error, string message) =>
        new(false, error, message);

    private static (int page, int size) Clamp(int page, int pageSize) =>
        (Math.Max(1, page), Math.Clamp(pageSize, 1, 100));

    private static PickupRequestData MapToData(
        PickupRequest pr,
        Guid? listingId = null,
        string? ngoName = null,
        string? groceryName = null,
        string? proofPhotoUrl = null,
        double? ngoPickupRate = null,
        int ngoTotalCompleted = 0)
    {
        return new PickupRequestData
        {
            Id = pr.Id.ToString(),
            ListingId = listingId?.ToString() ?? pr.ListingId?.ToString() ?? "",
            NgoId = pr.NgoId.ToString(),
            NgoName = ngoName ?? pr.Ngo?.Name ?? "",
            NgoProfilePictureUrl = pr.Ngo?.ProfilePictureUrl,
            GroceryId = pr.GroceryId.ToString(),
            GroceryName = groceryName ?? pr.Grocery?.Name ?? "",
            GroceryProfilePictureUrl = pr.Grocery?.ProfilePictureUrl,
            Status = pr.Status.ToString().ToLower(),
            RequestedQuantity = pr.RequestedQuantity ?? 0,
            PickupDate = pr.PickupDate.ToString("yyyy-MM-dd"),
            PickupTime = pr.PickupTime ?? "09:00",
            Notes = pr.Notes ?? "",
            ListingTitle = pr.ListingTitle ?? "",
            ListingCategory = pr.ListingCategory ?? "",
            ListingExpiryDate = pr.ListingExpiryDate,
            ListingUnit = pr.ListingUnit ?? "",
            CreatedAt = pr.RequestedAt.ToString("o"),
            ProofPhotoUrl = proofPhotoUrl ?? pr.ProofPhotoUrl,
            NgoPickupRate = ngoPickupRate,
            NgoTotalCompleted = ngoTotalCompleted,
            MarkedReadyAt = pr.MarkedReadyAt?.ToString("o"),
            MarkedPickedUpAt = pr.MarkedPickedUpAt?.ToString("o"),
            ConfirmedReceivedAt = pr.ConfirmedReceivedAt?.ToString("o"),
            RequiresRefrigeration = pr.RequiresRefrigeration,
            IsFragile = pr.IsFragile,
            IsHeavy = pr.IsHeavy,
            GroceryLocation = pr.Grocery?.Location ?? pr.Grocery?.Address,
            // Both organizations store their own coordinates, so the distance needs no
            // caller context; it is null whenever either side never set one.
            DistanceKm = HaversineKm(
                pr.Ngo?.Latitude, pr.Ngo?.Longitude,
                pr.Grocery?.Latitude, pr.Grocery?.Longitude),
            Items = pr.Items.Select(i => new PickupRequestItemData
            {
                Id = i.Id.ToString(),
                ListingGroupId = i.ListingGroupId?.ToString(),
                OriginalListingId = i.OriginalListingId?.ToString(),
                ReservedListingId = i.ReservedListingId?.ToString(),
                RequestedQuantity = i.RequestedQuantity,
                ListingTitle = i.ListingTitle,
                ListingCategory = i.ListingCategory,
                ListingExpiryDate = i.ListingExpiryDate,
                ListingUnit = i.ListingUnit,
                ListingPhotoUrl = i.ListingPhotoUrl
            }).ToList()
        };
    }

    private static double? HaversineKm(double? lat1, double? lon1, double? lat2, double? lon2)
    {
        if (lat1 is null || lon1 is null || lat2 is null || lon2 is null)
            return null;

        const double earthRadiusKm = 6371;
        static double ToRadians(double degrees) => degrees * Math.PI / 180;

        var dLat = ToRadians(lat2.Value - lat1.Value);
        var dLon = ToRadians(lon2.Value - lon1.Value);
        var a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                Math.Cos(ToRadians(lat1.Value)) * Math.Cos(ToRadians(lat2.Value)) *
                Math.Sin(dLon / 2) * Math.Sin(dLon / 2);
        var c = 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
        return Math.Round(earthRadiusKm * c, 1);
    }

    private static PickupRequestItem BuildItem(
        Guid requestId,
        ClearanceListing sourceListing,
        ClearanceListing reservedListing,
        int requestedQuantity) =>
        new()
        {
            Id = Guid.NewGuid(),
            PickupRequestId = requestId,
            ListingGroupId = sourceListing.GroupId,
            OriginalListingId = sourceListing.Id,
            ReservedListingId = reservedListing.Id,
            RequestedQuantity = requestedQuantity,
            ListingTitle = sourceListing.ProductName,
            ListingCategory = sourceListing.Category,
            ListingExpiryDate = sourceListing.ExpirationDate?.ToString("yyyy-MM-dd"),
            ListingUnit = sourceListing.Unit,
            ListingPhotoUrl = FirstImageUrl(sourceListing.PhotoUrl),
            CreatedAt = DateTime.UtcNow
        };

    private static string? FirstImageUrl(string? photoUrl)
    {
        if (string.IsNullOrWhiteSpace(photoUrl))
            return null;

        var trimmed = photoUrl.Trim();
        if (!trimmed.StartsWith("["))
            return trimmed;

        try
        {
            return JsonSerializer.Deserialize<List<string>>(trimmed)
                ?.FirstOrDefault(url => !string.IsNullOrWhiteSpace(url));
        }
        catch
        {
            return null;
        }
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
        string pickupTime, string? notes, Guid ngoId,
        bool requiresRefrigeration = false,
        bool isFragile = false, bool isHeavy = false)
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

            var wholeRequest = new PickupRequest
            {
                Id = requestId,
                NgoId = ngoId,
                GroceryId = sourceListing.GroceryId,
                ListingId = sourceListing.Id,
                PickupDate = pickupDate,
                Status = PickupRequestStatus.Pending,
                RequestedAt = DateTime.UtcNow,
                RequestedQuantity = requestedQuantity,
                PickupTime = pickupTime,
                Notes = notes,
                RequiresRefrigeration = requiresRefrigeration,
                IsFragile = isFragile,
                IsHeavy = isHeavy
            };
            wholeRequest.Items.Add(BuildItem(wholeRequest.Id, sourceListing, sourceListing, requestedQuantity));
            PickupRequestSummary.Apply(wholeRequest);
            return (sourceListing, wholeRequest);
        }

        var reservedListing = new ClearanceListing
        {
            Id = Guid.NewGuid(),
            GroupId = group.Id,
            GroceryId = sourceListing.GroceryId,
            ProductName = sourceListing.ProductName,
            Category = sourceListing.Category,
            Quantity = requestedQuantity,
            Unit = sourceListing.Unit,
            ExpirationDate = sourceListing.ExpirationDate.HasValue
                ? DateTime.SpecifyKind(sourceListing.ExpirationDate.Value, DateTimeKind.Utc)
                : (DateTime?)null,
            ClearanceDeadline = DateTime.SpecifyKind(sourceListing.ClearanceDeadline, DateTimeKind.Utc),
            Notes = sourceListing.Notes,
            PhotoUrl = sourceListing.PhotoUrl,
            PickupTimeStart = sourceListing.PickupTimeStart,
            PickupTimeEnd = sourceListing.PickupTimeEnd,
            Status = ListingStatus.Reserved,
            SplitReason = "partial_request",
            RelatedRequestId = requestId,
            SplitFromListingId = sourceListing.Id,
            SplitIndex = group.ChildListings?.Count ?? 1,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        sourceListing.Quantity -= requestedQuantity;
        sourceListing.UpdatedAt = DateTime.UtcNow;
        group.TotalAvailable -= requestedQuantity;
        group.TotalReserved += requestedQuantity;
        group.UpdatedAt = DateTime.UtcNow;

        _context.ClearanceListings.Add(reservedListing);

        var partialRequest = new PickupRequest
        {
            Id = requestId,
            NgoId = ngoId,
            GroceryId = sourceListing.GroceryId,
            ListingId = reservedListing.Id,
            PickupDate = pickupDate,
            Status = PickupRequestStatus.Pending,
            RequestedAt = DateTime.UtcNow,
            RequestedQuantity = requestedQuantity,
            PickupTime = pickupTime,
            Notes = notes,
            RequiresRefrigeration = requiresRefrigeration,
            IsFragile = isFragile,
            IsHeavy = isHeavy
        };
        partialRequest.Items.Add(BuildItem(partialRequest.Id, sourceListing, reservedListing, requestedQuantity));
        PickupRequestSummary.Apply(partialRequest);
        return (reservedListing, partialRequest);
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
