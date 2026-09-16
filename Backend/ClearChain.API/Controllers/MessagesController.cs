using ClearChain.API.Common;
using ClearChain.Domain.Entities;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

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
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

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
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

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
