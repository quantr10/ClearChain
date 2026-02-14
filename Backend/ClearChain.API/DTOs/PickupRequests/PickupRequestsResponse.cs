namespace ClearChain.API.DTOs.PickupRequests;

public class PickupRequestsResponse
{
    public string Message { get; set; } = string.Empty;
    public List<PickupRequestData> Data { get; set; } = new();
}