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

    // ✅ ADD: Pickup time window
    [Required]
    [RegularExpression(@"^([0-1][0-9]|2[0-3]):[0-5][0-9]$", 
        ErrorMessage = "Invalid time format. Use HH:mm (e.g., 09:00)")]
    public string PickupTimeStart { get; set; } = string.Empty;  // HH:mm

    [Required]
    [RegularExpression(@"^([0-1][0-9]|2[0-3]):[0-5][0-9]$", 
        ErrorMessage = "Invalid time format. Use HH:mm (e.g., 17:00)")]
    public string PickupTimeEnd { get; set; } = string.Empty;  // HH:mm

    public string? ImageUrl { get; set; }
}
