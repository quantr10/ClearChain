namespace ClearChain.API.DTOs.Organizations;

public class ActivityDto
{
    public string Id { get; set; } = string.Empty;

    /// <summary>
    /// One of: listing_created | pickup_request | pickup_approved |
    ///         pickup_ready | pickup_completed | pickup_cancelled | inventory_received
    /// </summary>
    public string Type { get; set; } = string.Empty;

    public string Title { get; set; } = string.Empty;
    public string Subtitle { get; set; } = string.Empty;

    /// <summary>ISO-8601 timestamp used for sorting and display.</summary>
    public string Timestamp { get; set; } = string.Empty;

    public string? RelatedId { get; set; }
}
