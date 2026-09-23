using Microsoft.AspNetCore.SignalR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;

namespace ClearChain.API.Hubs;

[Authorize]
public class PickupRequestHub : Hub
{
    private readonly ILogger<PickupRequestHub> _logger;
    private readonly ApplicationDbContext _context;

    public PickupRequestHub(ILogger<PickupRequestHub> logger, ApplicationDbContext context)
    {
        _logger = logger;
        _context = context;
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
        // Without this check, any authenticated org that knows or guesses a pickup
        // request's GUID could join its room and see every status-change broadcast for
        // it — notes, quantities, and both parties' names.
        if (!Guid.TryParse(pickupRequestId, out var requestGuid) ||
            !Guid.TryParse(Context.UserIdentifier, out var callerId))
            return;

        var isParty = await _context.PickupRequests
            .AnyAsync(pr => pr.Id == requestGuid && (pr.NgoId == callerId || pr.GroceryId == callerId));
        if (!isParty)
        {
            _logger.LogWarning($"Connection {Context.ConnectionId} (user {callerId}) denied joining pickup_{pickupRequestId} — not a party to it");
            return;
        }

        await Groups.AddToGroupAsync(Context.ConnectionId, $"pickup_{pickupRequestId}");
        _logger.LogInformation($"Connection {Context.ConnectionId} joined pickup_{pickupRequestId}");
    }

    public async Task LeavePickupRequestRoom(string pickupRequestId)
    {
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"pickup_{pickupRequestId}");
        _logger.LogInformation($"Connection {Context.ConnectionId} left pickup_{pickupRequestId}");
    }
}
