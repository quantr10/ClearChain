namespace ClearChain.API.DTOs.Notifications;

public class NotificationDto
{
    public string Id { get; set; } = string.Empty;
    public string Type { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string Body { get; set; } = string.Empty;
    public string? RelatedId { get; set; }
    public string? RelatedType { get; set; }
    public bool IsRead { get; set; }
    public string CreatedAt { get; set; } = string.Empty;
    public string? ReadAt { get; set; }
}

public class NotificationListResponse
{
    public string Message { get; set; } = string.Empty;
    public List<NotificationDto> Data { get; set; } = new();
    public int UnreadCount { get; set; }
    public int Total { get; set; }
    public int Page { get; set; }
    public int PageSize { get; set; }
    public int TotalPages { get; set; }

    /// <summary>
    /// The retention window this list was drawn from, so the client can label what it is
    /// showing instead of implying an inbox that goes back forever.
    /// </summary>
    public int RetentionDays { get; set; }
}
