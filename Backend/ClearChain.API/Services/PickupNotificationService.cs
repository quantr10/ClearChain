using Microsoft.AspNetCore.SignalR;
using ClearChain.API.Hubs;
using ClearChain.API.DTOs.PickupRequests;

namespace ClearChain.API.Services;

public class PickupNotificationService : IPickupNotificationService
{
    private readonly IHubContext<PickupRequestHub> _hubContext;
    private readonly ILogger<PickupNotificationService> _logger;

    public PickupNotificationService(
        IHubContext<PickupRequestHub> hubContext,
        ILogger<PickupNotificationService> logger)
    {
        _hubContext = hubContext;
        _logger = logger;
    }

    public async Task NotifyPickupRequestCreated(PickupRequestData request)
    {
        try
        {
            // Notify grocery (receiver)
            await _hubContext.Clients
                .Group($"user_{request.GroceryId}")
                .SendAsync("PickupRequestCreated", request);

            _logger.LogInformation(
                $"Notified grocery {request.GroceryId} about new request {request.Id}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending PickupRequestCreated notification");
        }
    }

    public async Task NotifyPickupRequestStatusChanged(PickupRequestData request, string oldStatus)
    {
        try
        {
            var notification = new
            {
                Request = request,
                OldStatus = oldStatus,
                NewStatus = request.Status,
                Timestamp = DateTime.UtcNow
            };

            // Notify both NGO and Grocery
            await _hubContext.Clients
                .Groups($"user_{request.NgoId}", $"user_{request.GroceryId}")
                .SendAsync("PickupRequestStatusChanged", notification);

            // Also notify specific pickup request room
            await _hubContext.Clients
                .Group($"pickup_{request.Id}")
                .SendAsync("PickupRequestUpdated", request);

            _logger.LogInformation(
                $"Notified status change for request {request.Id}: {oldStatus} → {request.Status}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending PickupRequestStatusChanged notification");
        }
    }

    public async Task NotifyPickupRequestCancelled(PickupRequestData request)
    {
        try
        {
            // Notify both parties
            await _hubContext.Clients
                .Groups($"user_{request.NgoId}", $"user_{request.GroceryId}")
                .SendAsync("PickupRequestCancelled", request);

            _logger.LogInformation($"Notified cancellation of request {request.Id}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending PickupRequestCancelled notification");
        }
    }
}