using Microsoft.AspNetCore.SignalR;  // ✅ This is correct
using System.Security.Claims;

namespace ClearChain.API.Services;

public class CustomUserIdProvider : IUserIdProvider
{
    public string? GetUserId(HubConnectionContext connection)
    {
        // Get user ID from JWT claims
        return connection.User?.FindFirst(ClaimTypes.NameIdentifier)?.Value;
    }
}