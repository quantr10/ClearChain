using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Listings;

public class UpdateListingQuantityRequest
{
    [Required]
    [Range(1, 100000)]
    public int NewQuantity { get; set; }
}
