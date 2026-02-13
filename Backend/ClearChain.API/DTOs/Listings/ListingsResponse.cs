namespace ClearChain.API.DTOs.Listings;

public class ListingsResponse
{
    public string Message { get; set; } = string.Empty;
    public List<ListingData> Data { get; set; } = new();
}