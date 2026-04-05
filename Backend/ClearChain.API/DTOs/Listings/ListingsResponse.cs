namespace ClearChain.API.DTOs.Listings;

public class ListingsResponse
{
    public string Message { get; set; } = string.Empty;
    public List<ListingData> Data { get; set; } = new();
    public int Total { get; set; }
    public int Page { get; set; }
    public int PageSize { get; set; }
    public int TotalPages { get; set; }
}