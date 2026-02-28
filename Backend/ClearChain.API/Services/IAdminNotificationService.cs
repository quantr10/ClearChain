namespace ClearChain.API.Services;

public interface IAdminNotificationService
{
    Task NotifyNewOrganizationRegistered(OrganizationRegisteredNotification notification);
    Task NotifyTransactionCompleted(TransactionCompletedNotification notification);
    Task NotifyStatsUpdated(PlatformStatsNotification stats);
    Task NotifySystemAlert(SystemAlertNotification alert);
}

// Notification DTOs
public class OrganizationRegisteredNotification
{
    public string OrganizationId { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Type { get; set; } = string.Empty; // "NGO" or "Grocery"
    public string Email { get; set; } = string.Empty;
    public string Location { get; set; } = string.Empty;
    public DateTime RegisteredAt { get; set; }
}

public class TransactionCompletedNotification
{
    public string TransactionId { get; set; } = string.Empty;
    public string NgoId { get; set; } = string.Empty;
    public string NgoName { get; set; } = string.Empty;
    public string GroceryId { get; set; } = string.Empty;
    public string GroceryName { get; set; } = string.Empty;
    public string ProductName { get; set; } = string.Empty;
    public int Quantity { get; set; }
    public string Unit { get; set; } = string.Empty;
    public DateTime CompletedAt { get; set; }
}

public class PlatformStatsNotification
{
    public int TotalNGOs { get; set; }
    public int TotalGroceries { get; set; }
    public int TotalDonations { get; set; }
    public int ActiveListings { get; set; }
    public int PendingRequests { get; set; }
    public int CompletedToday { get; set; }
    public DateTime UpdatedAt { get; set; }
}

public class SystemAlertNotification
{
    public string Level { get; set; } = "info"; // "info", "warning", "error"
    public string Message { get; set; } = string.Empty;
    public string? Details { get; set; }
    public DateTime Timestamp { get; set; }
}