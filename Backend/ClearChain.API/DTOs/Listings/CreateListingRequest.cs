using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Listings;

public class CreateListingRequest
{
    [Required]
    [StringLength(200, MinimumLength = 3)]
    public string Title { get; set; } = string.Empty;

    public string Description { get; set; } = string.Empty;

    [Required]
    [RegularExpression(@"^(FRUITS|VEGETABLES|DAIRY|BAKERY|MEAT|SEAFOOD|CANNED_GOODS|BEVERAGES|FROZEN_FOODS|GRAINS)$")]
    public string Category { get; set; } = string.Empty;

    [Required]
    [Range(1, 100000)]
    public int Quantity { get; set; }

    [Required]
    public string Unit { get; set; } = string.Empty;

    [Required]
    public string ExpiryDate { get; set; } = string.Empty;  // yyyy-MM-dd

    // Pickup window is derived from the grocery's operating hours in their profile.
    // These are kept as optional for backward compatibility with existing data only.
    public string? PickupTimeStart { get; set; }
    public string? PickupTimeEnd { get; set; }

    public string? ImageUrl { get; set; }

    /// <summary>
    /// Multiple image URLs. If provided, ImageUrl is ignored and this list is serialized
    /// as a JSON array into the PhotoUrl column. Max 5 images.
    /// </summary>
    [MaxLength(5)]
    public List<string>? ImageUrls { get; set; }
}
