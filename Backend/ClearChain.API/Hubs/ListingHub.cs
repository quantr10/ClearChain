using Microsoft.AspNetCore.SignalR;
using Microsoft.AspNetCore.Authorization;

namespace ClearChain.API.Hubs;

[Authorize]
public class ListingHub : Hub
{
    private readonly ILogger<ListingHub> _logger;

    public ListingHub(ILogger<ListingHub> logger)
    {
        _logger = logger;
    }

    public override async Task OnConnectedAsync()
    {
        var userId = Context.UserIdentifier;
        _logger.LogInformation($"User {userId} connected to ListingHub");
        
        // Join user-specific group
        if (!string.IsNullOrEmpty(userId))
        {
            await Groups.AddToGroupAsync(Context.ConnectionId, $"user_{userId}");
        }
        
        // Join "all_listings" group for browse feature
        await Groups.AddToGroupAsync(Context.ConnectionId, "all_listings");
        
        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        var userId = Context.UserIdentifier;
        _logger.LogInformation($"User {userId} disconnected from ListingHub");
        
        if (!string.IsNullOrEmpty(userId))
        {
            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"user_{userId}");
        }
        
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, "all_listings");
        
        await base.OnDisconnectedAsync(exception);
    }

    // Client can join specific listing room for detailed updates
    public async Task JoinListingRoom(string listingId)
    {
        await Groups.AddToGroupAsync(Context.ConnectionId, $"listing_{listingId}");
        _logger.LogInformation($"Connection {Context.ConnectionId} joined listing_{listingId}");
    }

    public async Task LeaveListingRoom(string listingId)
    {
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"listing_{listingId}");
        _logger.LogInformation($"Connection {Context.ConnectionId} left listing_{listingId}");
    }
}