namespace ClearChain.API.DTOs.PickupRequests;

public class PickupRequestResponse
{
    public string Message { get; set; } = string.Empty;
    public PickupRequestData Data { get; set; } = new();
}