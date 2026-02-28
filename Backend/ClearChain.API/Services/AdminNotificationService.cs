using Microsoft.AspNetCore.SignalR;
using ClearChain.API.Hubs;

namespace ClearChain.API.Services;

public class AdminNotificationService : IAdminNotificationService
{
    private readonly IHubContext<AdminHub> _hubContext;
    private readonly ILogger<AdminNotificationService> _logger;

    public AdminNotificationService(
        IHubContext<AdminHub> hubContext,
        ILogger<AdminNotificationService> logger)
    {
        _hubContext = hubContext;
        _logger = logger;
    }

    public async Task NotifyNewOrganizationRegistered(OrganizationRegisteredNotification notification)
    {
        try
        {
            await _hubContext.Clients
                .Group("admins")
                .SendAsync("NewOrganizationRegistered", notification);

            _logger.LogInformation($"Notified admins: New {notification.Type} registered - {notification.Name}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending NewOrganizationRegistered notification");
        }
    }

    public async Task NotifyTransactionCompleted(TransactionCompletedNotification notification)
    {
        try
        {
            await _hubContext.Clients
                .Group("admins")
                .SendAsync("TransactionCompleted", notification);

            _logger.LogInformation($"Notified admins: Transaction completed - {notification.TransactionId}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending TransactionCompleted notification");
        }
    }

    public async Task NotifyStatsUpdated(PlatformStatsNotification stats)
    {
        try
        {
            await _hubContext.Clients
                .Group("admins")
                .SendAsync("StatsUpdated", stats);

            _logger.LogInformation("Notified admins: Stats updated");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending StatsUpdated notification");
        }
    }

    public async Task NotifySystemAlert(SystemAlertNotification alert)
    {
        try
        {
            await _hubContext.Clients
                .Group("admins")
                .SendAsync("SystemAlert", alert);

            _logger.LogInformation($"Notified admins: System alert - {alert.Level}: {alert.Message}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending SystemAlert notification");
        }
    }
}