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

    public async Task NotifyPickupRequestCreatedAsync(PickupRequestData request)
    {
        try
        {
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

    public async Task NotifyPickupRequestStatusChangedAsync(PickupRequestData request)
    {
        try
        {
            var notification = new
            {
                Request = request,
                NewStatus = request.Status,
                Timestamp = DateTime.UtcNow
            };

            await _hubContext.Clients
                .Groups($"user_{request.NgoId}", $"user_{request.GroceryId}")
                .SendAsync("PickupRequestStatusChanged", notification);

            // Anyone with this request's detail screen open.
            await _hubContext.Clients
                .Group($"pickup_{request.Id}")
                .SendAsync("PickupRequestUpdated", request);

            _logger.LogInformation(
                $"Notified status change for request {request.Id} → {request.Status}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending PickupRequestStatusChanged notification");
        }
    }

    public async Task NotifyPickupRequestCancelledAsync(PickupRequestData request)
    {
        try
        {
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
