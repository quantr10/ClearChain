using Microsoft.AspNetCore.SignalR;
using Microsoft.AspNetCore.Authorization;

namespace ClearChain.API.Hubs;

[Authorize]  // ✅ KEEP THIS - Production Ready
public class PickupRequestHub : Hub
{
    private readonly ILogger<PickupRequestHub> _logger;

    public PickupRequestHub(ILogger<PickupRequestHub> logger)
    {
        _logger = logger;
    }

    public override async Task OnConnectedAsync()
    {
        var userId = Context.UserIdentifier;
        _logger.LogInformation($"User {userId} connected to PickupRequestHub");
        
        if (!string.IsNullOrEmpty(userId))
        {
            await Groups.AddToGroupAsync(Context.ConnectionId, $"user_{userId}");
        }
        
        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        var userId = Context.UserIdentifier;
        _logger.LogInformation($"User {userId} disconnected from PickupRequestHub");
        
        if (!string.IsNullOrEmpty(userId))
        {
            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"user_{userId}");
        }
        
        await base.OnDisconnectedAsync(exception);
    }

    public async Task JoinPickupRequestRoom(string pickupRequestId)
    {
        await Groups.AddToGroupAsync(Context.ConnectionId, $"pickup_{pickupRequestId}");
        _logger.LogInformation($"Connection {Context.ConnectionId} joined pickup_{pickupRequestId}");
    }

    public async Task LeavePickupRequestRoom(string pickupRequestId)
    {
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"pickup_{pickupRequestId}");
        _logger.LogInformation($"Connection {Context.ConnectionId} left pickup_{pickupRequestId}");
    }
}