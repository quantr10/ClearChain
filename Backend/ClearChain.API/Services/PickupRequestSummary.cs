using ClearChain.Domain.Entities;

namespace ClearChain.API.Services;

/// <summary>
/// A pickup request's line items are the source of truth for what it moves. The
/// denormalized <c>Listing*</c> columns on <see cref="PickupRequest"/> are only a summary
/// of those items, kept so a client that has the request but not its items (an offline
/// cache, a push notification payload) still has something truthful to show.
///
/// Every write path must call <see cref="Apply"/> after building the items so the summary
/// can never drift from them.
/// </summary>
public static class PickupRequestSummary
{
    public const string MultipleCategory = "Multiple";
    public const string MixedUnit = "items";

    /// <summary>Recomputes the request's summary columns from its items.</summary>
    public static void Apply(PickupRequest request)
    {
        var items = request.Items.ToList();
        if (items.Count == 0)
            return;

        request.ListingTitle = Title(items);
        request.ListingCategory = Category(items);
        request.ListingUnit = Unit(items);
        request.ListingExpiryDate = EarliestExpiry(items);
        request.RequestedQuantity = items.Sum(i => i.RequestedQuantity);
    }

    /// <summary>
    /// One item reads as the product itself; several read as a count, because no single
    /// product name can stand for the rest.
    /// </summary>
    public static string Title(IReadOnlyCollection<PickupRequestItem> items) =>
        items.Count switch
        {
            0 => string.Empty,
            1 => items.First().ListingTitle,
            _ => $"{items.Count} items"
        };

    public static string Category(IReadOnlyCollection<PickupRequestItem> items)
    {
        var distinct = items
            .Select(i => i.ListingCategory)
            .Where(c => !string.IsNullOrWhiteSpace(c))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToList();

        return distinct.Count == 1 ? distinct[0] : MultipleCategory;
    }

    public static string Unit(IReadOnlyCollection<PickupRequestItem> items)
    {
        var distinct = items
            .Select(i => i.ListingUnit)
            .Where(u => !string.IsNullOrWhiteSpace(u))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToList();

        return distinct.Count == 1 ? distinct[0] : MixedUnit;
    }

    /// <summary>The whole request is only as fresh as its soonest-expiring item.</summary>
    public static string? EarliestExpiry(IReadOnlyCollection<PickupRequestItem> items) =>
        items
            .Select(i => i.ListingExpiryDate)
            .Where(d => !string.IsNullOrWhiteSpace(d))
            .OrderBy(d => d, StringComparer.Ordinal)
            .FirstOrDefault();
}
