// DTOs/Listings/UpdateListingQuantityRequest.cs
namespace ClearChain.API.DTOs.Listings;
using System.ComponentModel.DataAnnotations;

public class UpdateListingQuantityRequest
{
    [Required]
    [Range(1, 100000)]
    public int NewQuantity { get; set; }
}