using Microsoft.AspNetCore.SignalR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;

namespace ClearChain.API.Hubs;

[Authorize]
public class InventoryHub : Hub
{
    private readonly ILogger<InventoryHub> _logger;
    private readonly ApplicationDbContext _context;

    public InventoryHub(ILogger<InventoryHub> logger, ApplicationDbContext context)
    {
        _logger = logger;
        _context = context;
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
        // Without this check, any authenticated org that knows or guesses an inventory
        // item's GUID could join its room and see another NGO's inventory updates.
        if (!Guid.TryParse(itemId, out var itemGuid) ||
            !Guid.TryParse(Context.UserIdentifier, out var callerId))
            return;

        var owns = await _context.Inventories.AnyAsync(i => i.Id == itemGuid && i.NgoId == callerId);
        if (!owns)
        {
            _logger.LogWarning($"Connection {Context.ConnectionId} (user {callerId}) denied joining item_{itemId} — not the owning NGO");
            return;
        }

        await Groups.AddToGroupAsync(Context.ConnectionId, $"item_{itemId}");
        _logger.LogInformation($"Connection {Context.ConnectionId} joined item_{itemId}");
    }

    public async Task LeaveInventoryItemRoom(string itemId)
    {
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"item_{itemId}");
        _logger.LogInformation($"Connection {Context.ConnectionId} left item_{itemId}");
    }
}
