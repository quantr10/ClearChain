using Microsoft.AspNetCore.SignalR;
using Microsoft.AspNetCore.Authorization;

namespace ClearChain.API.Hubs;

[Authorize]
public class InventoryHub : Hub
{
    private readonly ILogger<InventoryHub> _logger;

    public InventoryHub(ILogger<InventoryHub> logger)
    {
        _logger = logger;
    }

    public override async Task OnConnectedAsync()
    {
        var userId = Context.UserIdentifier;
        _logger.LogInformation($"User {userId} connected to InventoryHub");
        
        // Join user-specific group (for NGO's inventory)
        if (!string.IsNullOrEmpty(userId))
        {
            await Groups.AddToGroupAsync(Context.ConnectionId, $"user_{userId}");
        }
        
        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        var userId = Context.UserIdentifier;
        _logger.LogInformation($"User {userId} disconnected from InventoryHub");
        
        if (!string.IsNullOrEmpty(userId))
        {
            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"user_{userId}");
        }
        
        await base.OnDisconnectedAsync(exception);
    }

    // Client can join specific inventory item room
    public async Task JoinInventoryItemRoom(string itemId)
    {
        await Groups.AddToGroupAsync(Context.ConnectionId, $"item_{itemId}");
        _logger.LogInformation($"Connection {Context.ConnectionId} joined item_{itemId}");
    }

    public async Task LeaveInventoryItemRoom(string itemId)
    {
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"item_{itemId}");
        _logger.LogInformation($"Connection {Context.ConnectionId} left item_{itemId}");
    }
}