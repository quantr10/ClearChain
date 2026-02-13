namespace ClearChain.API.DTOs.Listings;

public class ListingResponse
{
    public string Message { get; set; } = string.Empty;
    public ListingData Data { get; set; } = new();
}