using ClearChain.API.DTOs.Cart;
using ClearChain.API.DTOs.PickupRequests;
using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
using ClearChain.Infrastructure.Data;
using Microsoft.EntityFrameworkCore;
using System.Text.Json;

namespace ClearChain.API.Services;

public enum CartServiceError
{
    NotFound,
    Forbidden,
    InvalidInput,
    InvalidStatus,
    DatabaseError
}

public record CartServiceResult(
    bool Success,
    CartServiceError? Error = null,
    string? ErrorMessage = null,
    List<CartGroupData>? Cart = null,
    PickupRequestData? PickupRequest = null
);

public interface ICartService
{
    Task<CartServiceResult> GetCartAsync(Guid ngoId);
    Task<CartServiceResult> AddItemAsync(Guid ngoId, AddCartItemRequest request);
    Task<CartServiceResult> UpdateItemAsync(Guid ngoId, Guid itemId, UpdateCartItemRequest request);
    Task<CartServiceResult> RemoveItemAsync(Guid ngoId, Guid itemId);
    Task<CartServiceResult> CheckoutGroupAsync(Guid ngoId, CheckoutCartGroupRequest request);
}

public class CartService : ICartService
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<CartService> _logger;
    private readonly IPickupNotificationService _notificationService;
    private readonly IPushNotificationService _pushNotificationService;

    public CartService(
        ApplicationDbContext context,
        ILogger<CartService> logger,
        IPickupNotificationService notificationService,
        IPushNotificationService pushNotificationService)
    {
        _context = context;
        _logger = logger;
        _notificationService = notificationService;
        _pushNotificationService = pushNotificationService;
    }

    public async Task<CartServiceResult> GetCartAsync(Guid ngoId)
    {
        var cart = await GetOrCreateCartAsync(ngoId);
        return new CartServiceResult(true, Cart: await MapCartAsync(cart.Id));
    }

    public async Task<CartServiceResult> AddItemAsync(Guid ngoId, AddCartItemRequest request)
    {
        if (!Guid.TryParse(request.ListingId, out var listingId))
            return Fail(CartServiceError.InvalidInput, "Invalid listing id");

        var quantity = Math.Max(1, request.Quantity);
        var cart = await GetOrCreateCartAsync(ngoId);
        var listing = await _context.ClearanceListings
            .Include(l => l.Grocery)
            .FirstOrDefaultAsync(l => l.Id == listingId);

        if (listing == null)
            return Fail(CartServiceError.NotFound, "Listing not found");
        if (listing.Status != ListingStatus.Open)
            return Fail(CartServiceError.InvalidStatus, "Listing is no longer available");
        if (listing.GroceryId == ngoId)
            return Fail(CartServiceError.Forbidden, "Cannot add your own listing to cart");

        var existing = await _context.CartItems
            .FirstOrDefaultAsync(i => i.CartId == cart.Id && i.ListingId == listingId);

        if (existing == null)
        {
            _context.CartItems.Add(new CartItem
            {
                Id = Guid.NewGuid(),
                CartId = cart.Id,
                ListingId = listing.Id,
                GroceryId = listing.GroceryId,
                RequestedQuantity = quantity,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            });
        }
        else
        {
            existing.RequestedQuantity += quantity;
            existing.UpdatedAt = DateTime.UtcNow;
        }

        cart.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();
        return new CartServiceResult(true, Cart: await MapCartAsync(cart.Id));
    }

    public async Task<CartServiceResult> UpdateItemAsync(Guid ngoId, Guid itemId, UpdateCartItemRequest request)
    {
        var cart = await GetOrCreateCartAsync(ngoId);
        var item = await _context.CartItems.FirstOrDefaultAsync(i => i.Id == itemId && i.CartId == cart.Id);
        if (item == null)
            return Fail(CartServiceError.NotFound, "Cart item not found");

        if (request.Quantity <= 0)
            _context.CartItems.Remove(item);
        else
        {
            item.RequestedQuantity = request.Quantity;
            item.UpdatedAt = DateTime.UtcNow;
        }

        cart.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();
        return new CartServiceResult(true, Cart: await MapCartAsync(cart.Id));
    }

    public async Task<CartServiceResult> RemoveItemAsync(Guid ngoId, Guid itemId)
    {
        var cart = await GetOrCreateCartAsync(ngoId);
        var item = await _context.CartItems.FirstOrDefaultAsync(i => i.Id == itemId && i.CartId == cart.Id);
        if (item == null)
            return Fail(CartServiceError.NotFound, "Cart item not found");

        _context.CartItems.Remove(item);
        cart.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();
        return new CartServiceResult(true, Cart: await MapCartAsync(cart.Id));
    }

    public async Task<CartServiceResult> CheckoutGroupAsync(Guid ngoId, CheckoutCartGroupRequest request)
    {
        var ngo = await _context.Organizations.FindAsync(ngoId);
        if (ngo == null || ngo.Type.ToLower() != "ngo")
            return Fail(CartServiceError.Forbidden, "Only NGOs can create pickup requests");
        if (!Guid.TryParse(request.GroceryId, out var groceryId))
            return Fail(CartServiceError.InvalidInput, "Invalid grocery id");
        if (!DateTime.TryParse(request.PickupDate, out var pickupDate))
            return Fail(CartServiceError.InvalidInput, "Invalid pickup date format. Use yyyy-MM-dd.");
        if (string.IsNullOrWhiteSpace(request.PickupTime))
            return Fail(CartServiceError.InvalidInput, "Pickup time is required.");
        if (!TimeSpan.TryParse(request.PickupTime, out var pickupTime))
            return Fail(CartServiceError.InvalidInput, "Invalid pickup time format. Use HH:mm.");

        var pickupDateUtc = DateTime.SpecifyKind(pickupDate, DateTimeKind.Utc);
        if (pickupDateUtc.Date < DateTime.UtcNow.Date)
            return Fail(CartServiceError.InvalidInput, "Pickup date cannot be in the past");

        var cart = await GetOrCreateCartAsync(ngoId);
        var cartItems = await _context.CartItems
            .Where(i => i.CartId == cart.Id && i.GroceryId == groceryId)
            .Include(i => i.Listing)!.ThenInclude(l => l!.Group)
            .Include(i => i.Grocery)
            .ToListAsync();

        if (cartItems.Count == 0)
            return Fail(CartServiceError.NotFound, "No cart items found for this grocery");

        var invalid = ValidateItems(cartItems, pickupDateUtc).FirstOrDefault(i => !i.IsValid);
        if (invalid != null)
            return Fail(CartServiceError.InvalidInput, invalid.InvalidReason ?? "Cart has invalid items");

        var pickupWindow = GetPickupWindow(cartItems);
        if (pickupWindow.HasValue &&
            (pickupTime < pickupWindow.Value.Start || pickupTime > pickupWindow.Value.End))
        {
            return Fail(
                CartServiceError.InvalidInput,
                $"Pickup time must be between {pickupWindow.Value.Start:hh\\:mm} and {pickupWindow.Value.End:hh\\:mm}.");
        }

        var groceryName = cartItems.First().Grocery?.Name ?? "";
        await using var tx = await _context.Database.BeginTransactionAsync();
        try
        {
            var pickupRequest = new PickupRequest
            {
                Id = Guid.NewGuid(),
                NgoId = ngoId,
                GroceryId = groceryId,
                ListingId = null,
                PickupDate = pickupDateUtc,
                Status = PickupRequestStatus.Pending,
                RequestedAt = DateTime.UtcNow,
                PickupTime = request.PickupTime,
                Notes = request.Notes,
                RequiresRefrigeration = request.RequiresRefrigeration,
                IsFragile = request.IsFragile,
                IsHeavy = request.IsHeavy
            };

            _context.PickupRequests.Add(pickupRequest);

            foreach (var cartItem in cartItems)
            {
                var listing = cartItem.Listing!;
                var reservedListing = ReserveListing(listing, listing.Group!, cartItem.RequestedQuantity, pickupRequest.Id);
                pickupRequest.Items.Add(new PickupRequestItem
                {
                    Id = Guid.NewGuid(),
                    PickupRequestId = pickupRequest.Id,
                    ListingGroupId = listing.GroupId,
                    OriginalListingId = listing.Id,
                    ReservedListingId = reservedListing.Id,
                    RequestedQuantity = cartItem.RequestedQuantity,
                    ListingTitle = listing.ProductName,
                    ListingCategory = listing.Category,
                    ListingExpiryDate = listing.ExpirationDate?.ToString("yyyy-MM-dd"),
                    ListingUnit = listing.Unit,
                    ListingPhotoUrl = FirstImageUrl(listing.PhotoUrl),
                    CreatedAt = DateTime.UtcNow
                });
            }

            PickupRequestSummary.Apply(pickupRequest);

            _context.CartItems.RemoveRange(cartItems);
            cart.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
            await tx.CommitAsync();

            var data = MapPickupRequest(pickupRequest, ngo.Name, groceryName);
            // Cart checkout is the app's main way of creating a pickup request, so it needs the
            // same push as the single-listing path — SignalR alone only reaches a grocery that
            // happens to have the app open.
            await _notificationService.NotifyPickupRequestCreatedAsync(data);
            await _pushNotificationService.SendPickupRequestCreatedNotification(pickupRequest.GroceryId, data);

            return new CartServiceResult(true, PickupRequest: data, Cart: await MapCartAsync(cart.Id));
        }
        catch (Exception ex)
        {
            await tx.RollbackAsync();
            _logger.LogError(ex, "Cart checkout failed for NGO {NgoId} grocery {GroceryId}", ngoId, groceryId);
            return Fail(CartServiceError.DatabaseError, "Checkout failed. Please try again.");
        }
    }

    private async Task<Cart> GetOrCreateCartAsync(Guid ngoId)
    {
        var cart = await _context.Carts.FirstOrDefaultAsync(c => c.NgoId == ngoId);
        if (cart != null) return cart;

        cart = new Cart
        {
            Id = Guid.NewGuid(),
            NgoId = ngoId,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };
        _context.Carts.Add(cart);
        await _context.SaveChangesAsync();
        return cart;
    }

    private async Task<List<CartGroupData>> MapCartAsync(Guid cartId)
    {
        var items = await _context.CartItems
            .Where(i => i.CartId == cartId)
            .Include(i => i.Listing)!.ThenInclude(l => l!.Group)
            .Include(i => i.Grocery)
            .OrderBy(i => i.Grocery!.Name)
            .ThenBy(i => i.Listing!.ProductName)
            .ToListAsync();

        return ValidateItems(items)
            .GroupBy(i => new { i.GroceryId, i.GroceryName })
            .Select(g => new CartGroupData
            {
                GroceryId = g.Key.GroceryId,
                GroceryName = g.Key.GroceryName,
                GroceryProfilePictureUrl = g.First().GroceryProfilePictureUrl,
                Items = g.ToList(),
                EarliestExpiryDate = g
                    .Select(i => i.ExpiryDate)
                    .Where(d => !string.IsNullOrWhiteSpace(d))
                    .OrderBy(d => d)
                    .FirstOrDefault(),
                PickupTimeStart = g
                    .Select(i => i.PickupTimeStart)
                    .Where(t => !string.IsNullOrWhiteSpace(t))
                    .OrderByDescending(t => t)
                    .FirstOrDefault(),
                PickupTimeEnd = g
                    .Select(i => i.PickupTimeEnd)
                    .Where(t => !string.IsNullOrWhiteSpace(t))
                    .OrderBy(t => t)
                    .FirstOrDefault()
            })
            .ToList();
    }

    private static IEnumerable<CartItemData> ValidateItems(List<CartItem> items, DateTime? pickupDate = null)
    {
        foreach (var item in items)
        {
            var listing = item.Listing;
            string? invalidReason = null;
            var isValid = true;

            if (listing == null)
            {
                isValid = false;
                invalidReason = "Listing no longer exists. Remove it.";
            }
            else if (listing.Status != ListingStatus.Open)
            {
                isValid = false;
                invalidReason = "This listing is no longer available. Remove it.";
            }
            else if (listing.ExpirationDate.HasValue && listing.ExpirationDate.Value.Date < DateTime.UtcNow.Date)
            {
                isValid = false;
                invalidReason = "This listing is expired. Remove it.";
            }
            else if (listing.Quantity <= 0)
            {
                isValid = false;
                invalidReason = "Out of stock. Remove it.";
            }
            else if (item.RequestedQuantity > listing.Quantity)
            {
                isValid = false;
                invalidReason = $"Only {listing.Quantity} {listing.Unit} left. Reduce quantity or remove it.";
            }
            else if (pickupDate.HasValue && listing.ExpirationDate.HasValue && pickupDate.Value.Date > listing.ExpirationDate.Value.Date)
            {
                isValid = false;
                invalidReason = $"Pickup date cannot be after expiry date ({listing.ExpirationDate.Value:yyyy-MM-dd}).";
            }
            else if (listing.Group == null)
            {
                isValid = false;
                invalidReason = "Listing cannot be requested right now. Remove it.";
            }

            yield return new CartItemData
            {
                Id = item.Id.ToString(),
                ListingId = item.ListingId.ToString(),
                GroceryId = item.GroceryId.ToString(),
                GroceryName = item.Grocery?.Name ?? listing?.Grocery?.Name ?? "",
                GroceryProfilePictureUrl = item.Grocery?.ProfilePictureUrl
                                        ?? listing?.Grocery?.ProfilePictureUrl,
                Title = listing?.ProductName ?? "",
                Category = listing?.Category ?? "",
                Unit = listing?.Unit ?? "",
                RequestedQuantity = item.RequestedQuantity,
                MaxQuantity = listing?.Quantity ?? 0,
                ExpiryDate = listing?.ExpirationDate?.ToString("yyyy-MM-dd"),
                ImageUrl = FirstImageUrl(listing?.PhotoUrl),
                PickupTimeStart = (listing?.PickupTimeStart ?? ParseHoursWindow(item.Grocery?.Hours)?.Start)?.ToString(@"hh\:mm"),
                PickupTimeEnd = (listing?.PickupTimeEnd ?? ParseHoursWindow(item.Grocery?.Hours)?.End)?.ToString(@"hh\:mm"),
                Status = listing?.Status.ToString().ToLower() ?? "missing",
                IsValid = isValid,
                InvalidReason = invalidReason
            };
        }
    }

    private static (TimeSpan Start, TimeSpan End)? GetPickupWindow(List<CartItem> items)
    {
        var starts = items
            .Select(i => i.Listing?.PickupTimeStart ?? ParseHoursWindow(i.Grocery?.Hours)?.Start)
            .Where(t => t.HasValue)
            .Select(t => t!.Value)
            .ToList();
        var ends = items
            .Select(i => i.Listing?.PickupTimeEnd ?? ParseHoursWindow(i.Grocery?.Hours)?.End)
            .Where(t => t.HasValue)
            .Select(t => t!.Value)
            .ToList();

        if (starts.Count == 0 || ends.Count == 0)
            return null;

        return (starts.Max(), ends.Min());
    }

    private static (TimeSpan Start, TimeSpan End)? ParseHoursWindow(string? hours)
    {
        if (string.IsNullOrWhiteSpace(hours)) return null;

        var parts = hours
            .Replace("–", "-")
            .Replace("—", "-")
            .Split('-', StringSplitOptions.TrimEntries | StringSplitOptions.RemoveEmptyEntries);

        if (parts.Length < 2) return null;
        if (!TimeSpan.TryParse(parts[0], out var start)) return null;
        if (!TimeSpan.TryParse(parts[1], out var end)) return null;

        return start <= end ? (start, end) : null;
    }

    private ClearanceListing ReserveListing(ClearanceListing sourceListing, ListingGroup group, int requestedQuantity, Guid requestId)
    {
        if (requestedQuantity >= sourceListing.Quantity)
        {
            sourceListing.Status = ListingStatus.Reserved;
            sourceListing.RelatedRequestId = requestId;
            sourceListing.UpdatedAt = DateTime.UtcNow;
            group.TotalAvailable -= sourceListing.Quantity;
            group.TotalReserved += sourceListing.Quantity;
            group.UpdatedAt = DateTime.UtcNow;
            return sourceListing;
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
                : null,
            ClearanceDeadline = DateTime.SpecifyKind(sourceListing.ClearanceDeadline, DateTimeKind.Utc),
            Notes = sourceListing.Notes,
            PhotoUrl = sourceListing.PhotoUrl,
            PickupTimeStart = sourceListing.PickupTimeStart,
            PickupTimeEnd = sourceListing.PickupTimeEnd,
            Status = ListingStatus.Reserved,
            SplitReason = "cart_request",
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
        return reservedListing;
    }

    private static PickupRequestData MapPickupRequest(PickupRequest request, string ngoName, string groceryName) =>
        new()
        {
            Id = request.Id.ToString(),
            ListingId = "",
            NgoId = request.NgoId.ToString(),
            NgoName = ngoName,
            GroceryId = request.GroceryId.ToString(),
            GroceryName = groceryName,
            Status = request.Status.ToString().ToLower(),
            RequestedQuantity = request.RequestedQuantity ?? 0,
            PickupDate = request.PickupDate.ToString("yyyy-MM-dd"),
            PickupTime = request.PickupTime ?? "09:00",
            Notes = request.Notes,
            ListingTitle = request.ListingTitle,
            ListingCategory = request.ListingCategory,
            ListingExpiryDate = request.ListingExpiryDate,
            ListingUnit = request.ListingUnit,
            CreatedAt = request.RequestedAt.ToString("o"),
            RequiresRefrigeration = request.RequiresRefrigeration,
            IsFragile = request.IsFragile,
            IsHeavy = request.IsHeavy,
            Items = request.Items.Select(i => new PickupRequestItemData
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

    private static CartServiceResult Fail(CartServiceError error, string message) =>
        new(false, error, message);
}
