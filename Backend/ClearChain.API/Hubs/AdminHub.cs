using Microsoft.AspNetCore.SignalR;
using Microsoft.AspNetCore.Authorization;

namespace ClearChain.API.Hubs;

[Authorize(Roles = "admin")]  // ✅ Lowercase to match database
public class AdminHub : Hub
{
    private readonly ILogger<AdminHub> _logger;

    public AdminHub(ILogger<AdminHub> logger)
    {
        _logger = logger;
    }

    public override async Task OnConnectedAsync()
    {
        var userId = Context.UserIdentifier;
        _logger.LogInformation($"Admin {userId} connected to AdminHub");
        
        // All admins join "admins" group
        await Groups.AddToGroupAsync(Context.ConnectionId, "admins");
        
        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        var userId = Context.UserIdentifier;
        _logger.LogInformation($"Admin {userId} disconnected from AdminHub");
        
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, "admins");
        
        await base.OnDisconnectedAsync(exception);
    }
}