using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Listings;

public class CreateListingRequest
{
    [Required]
    [StringLength(200, MinimumLength = 3)]
    public string Title { get; set; } = string.Empty;

    public string Description { get; set; } = string.Empty;

    // Matches the mobile app's FoodCategory enum and AzureVisionService's detector output
    // (PACKAGED replaced CANNED_GOODS there; keep both vocabularies in sync).
    [Required]
    [RegularExpression(@"^(FRUITS|VEGETABLES|DAIRY|BAKERY|MEAT|SEAFOOD|PACKAGED|BEVERAGES|OTHER)$")]
    public string Category { get; set; } = string.Empty;

    [Required]
    [Range(1, 100000)]
    public int Quantity { get; set; }

    [Required]
    public string Unit { get; set; } = string.Empty;

    [Required]
    public string ExpiryDate { get; set; } = string.Empty;  // yyyy-MM-dd

    // The pickup window isn't taken from the request: a new listing gets the grocery's
    // operating hours from its profile, and an edit keeps the listing's existing window.

    public string? ImageUrl { get; set; }
}
