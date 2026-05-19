using ClearChain.Domain.Entities;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class MessagesController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public MessagesController(ApplicationDbContext context)
    {
        _context = context;
    }

    // GET api/messages/pickup/{requestId} — Get conversation for a pickup request
    [HttpGet("pickup/{requestId}")]
    public async Task<IActionResult> GetMessages(Guid requestId)
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var pickup = await _context.PickupRequests.FindAsync(requestId);
        if (pickup == null) return NotFound(new { message = "Pickup request not found" });

        if (pickup.NgoId != userId && pickup.GroceryId != userId)
            return Forbid();

        var messages = await _context.Messages
            .Include(m => m.Sender)
            .Where(m => m.PickupRequestId == requestId)
            .OrderBy(m => m.SentAt)
            .ToListAsync();

        // Mark unread as read for this user
        var unread = messages.Where(m => m.ReceiverId == userId && !m.IsRead).ToList();
        foreach (var msg in unread)
        {
            msg.IsRead = true;
            msg.ReadAt = DateTime.UtcNow;
        }
        if (unread.Any()) await _context.SaveChangesAsync();

        return Ok(new
        {
            message = "Messages retrieved",
            data = messages.Select(MapToDto).ToList()
        });
    }

    // POST api/messages/pickup/{requestId} — Send a message
    [HttpPost("pickup/{requestId}")]
    public async Task<IActionResult> SendMessage(Guid requestId, [FromBody] SendMessageRequest request)
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        if (string.IsNullOrWhiteSpace(request.Content))
            return BadRequest(new { message = "Message content cannot be empty" });

        var pickup = await _context.PickupRequests.FindAsync(requestId);
        if (pickup == null) return NotFound(new { message = "Pickup request not found" });

        if (pickup.NgoId != userId && pickup.GroceryId != userId)
            return Forbid();

        var receiverId = pickup.NgoId == userId ? pickup.GroceryId : pickup.NgoId;

        var msg = new Message
        {
            Id = Guid.NewGuid(),
            PickupRequestId = requestId,
            SenderId = userId,
            ReceiverId = receiverId,
            Content = request.Content.Trim(),
            IsRead = false,
            SentAt = DateTime.UtcNow
        };

        _context.Messages.Add(msg);
        await _context.SaveChangesAsync();

        // Reload sender name
        msg.Sender = await _context.Organizations.FindAsync(userId);

        return Ok(new { message = "Message sent", data = MapToDto(msg) });
    }

    // GET api/messages/threads — Get all active message threads for current user
    [HttpGet("threads")]
    public async Task<IActionResult> GetThreads()
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        // Get last message per pickup request
        var threads = await _context.Messages
            .Include(m => m.Sender)
            .Include(m => m.PickupRequest)
            .Where(m => m.SenderId == userId || m.ReceiverId == userId)
            .GroupBy(m => m.PickupRequestId)
            .Select(g => g.OrderByDescending(m => m.SentAt).First())
            .ToListAsync();

        var unreadCounts = await _context.Messages
            .Where(m => m.ReceiverId == userId && !m.IsRead)
            .GroupBy(m => m.PickupRequestId)
            .Select(g => new { g.Key, Count = g.Count() })
            .ToDictionaryAsync(x => x.Key, x => x.Count);

        return Ok(new
        {
            message = "Threads retrieved",
            data = threads.Select(m => new
            {
                pickupRequestId = m.PickupRequestId.ToString(),
                lastMessage = MapToDto(m),
                unreadCount = unreadCounts.TryGetValue(m.PickupRequestId, out var c) ? c : 0
            }).ToList()
        });
    }

    private bool TryGetUserId(out Guid userId)
    {
        var value = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.TryParse(value, out userId);
    }

    private static object MapToDto(Message m) => new
    {
        id = m.Id.ToString(),
        pickupRequestId = m.PickupRequestId.ToString(),
        senderId = m.SenderId.ToString(),
        senderName = m.Sender?.Name ?? "",
        receiverId = m.ReceiverId.ToString(),
        content = m.Content,
        isRead = m.IsRead,
        sentAt = m.SentAt.ToString("o"),
        readAt = m.ReadAt?.ToString("o")
    };
}

public class SendMessageRequest
{
    public string Content { get; set; } = string.Empty;
}
