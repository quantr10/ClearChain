namespace ClearChain.API.DTOs.PickupRequests;

public class CreatePickupRequestRequest
{
    public string ListingId { get; set; } = string.Empty;
    public int RequestedQuantity { get; set; }
    public string PickupDate { get; set; } = string.Empty;
    public string PickupTime { get; set; } = string.Empty;
    public string? Notes { get; set; }
}