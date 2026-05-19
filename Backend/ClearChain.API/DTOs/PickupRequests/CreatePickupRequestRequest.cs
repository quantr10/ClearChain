namespace ClearChain.API.DTOs.PickupRequests;

public class CreatePickupRequestRequest
{
    public string ListingId { get; set; } = string.Empty;
    public int RequestedQuantity { get; set; }
    public string PickupDate { get; set; } = string.Empty;
    public string PickupTime { get; set; } = string.Empty;
    public string? Notes { get; set; }
    public string? VehicleType { get; set; }
    public bool RequiresRefrigeration { get; set; } = false;
    public bool IsFragile { get; set; } = false;
    public bool IsHeavy { get; set; } = false;
}