namespace ClearChain.API.DTOs.Listings;

public class ListingData
{
    public string Id { get; set; } = string.Empty;
    public string GroceryId { get; set; } = string.Empty;
    public string GroceryName { get; set; } = string.Empty;
    public string? GroceryProfilePictureUrl { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public int Quantity { get; set; }
    public string Unit { get; set; } = string.Empty;
    public string ExpiryDate { get; set; } = string.Empty;
    public string PickupTimeStart { get; set; } = string.Empty;
    public string PickupTimeEnd { get; set; } = string.Empty;
    public string? GroceryHours { get; set; }
    public string Status { get; set; } = string.Empty;
    public string? ImageUrl { get; set; }
    public string Location { get; set; } = string.Empty;
    public string CreatedAt { get; set; } = string.Empty;

    // ── Distance from NGO's search location ──────────────────────────────────
    public double? DistanceKm { get; set; }

    // ── Analytics ────────────────────────────────────────────────────────────
    public int ViewCount { get; set; }
    public int RequestCount { get; set; }

    // ── Grocery coordinates (for map pins) ───────────────────────────────────
    public double? GroceryLatitude { get; set; }
    public double? GroceryLongitude { get; set; }
}
