namespace ClearChain.API.DTOs.PickupRequests;

public class PickupRequestsResponse
{
    public string Message { get; set; } = string.Empty;
    public List<PickupRequestData> Data { get; set; } = new();
    public int Total { get; set; }
    public int Page { get; set; }
    public int PageSize { get; set; }
    public int TotalPages { get; set; }
}