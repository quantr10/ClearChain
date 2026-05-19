using ClearChain.API.DTOs.Notifications;
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
public class NotificationsController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public NotificationsController(ApplicationDbContext context)
    {
        _context = context;
    }

    // GET api/notifications?page=1&pageSize=20
    [HttpGet]
    public async Task<IActionResult> GetMyNotifications(
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 20,
        [FromQuery] bool unreadOnly = false)
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var clampedPage = Math.Max(1, page);
        var clampedSize = Math.Clamp(pageSize, 1, 50);

        var query = _context.Notifications
            .Where(n => n.RecipientId == userId);

        if (unreadOnly)
            query = query.Where(n => !n.IsRead);

        var total = await query.CountAsync();
        var unreadCount = await _context.Notifications
            .CountAsync(n => n.RecipientId == userId && !n.IsRead);

        var items = await query
            .OrderByDescending(n => n.CreatedAt)
            .Skip((clampedPage - 1) * clampedSize)
            .Take(clampedSize)
            .ToListAsync();

        return Ok(new NotificationListResponse
        {
            Message = "Notifications retrieved",
            Data = items.Select(MapToDto).ToList(),
            UnreadCount = unreadCount,
            Total = total,
            Page = clampedPage,
            PageSize = clampedSize,
            TotalPages = (int)Math.Ceiling((double)total / clampedSize)
        });
    }

    // GET api/notifications/unread-count
    [HttpGet("unread-count")]
    public async Task<IActionResult> GetUnreadCount()
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var count = await _context.Notifications
            .CountAsync(n => n.RecipientId == userId && !n.IsRead);

        return Ok(new UnreadCountResponse { UnreadCount = count });
    }

    // PUT api/notifications/{id}/read
    [HttpPut("{id}/read")]
    public async Task<IActionResult> MarkAsRead(Guid id)
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var notification = await _context.Notifications
            .FirstOrDefaultAsync(n => n.Id == id && n.RecipientId == userId);

        if (notification == null) return NotFound(new { message = "Notification not found" });

        notification.IsRead = true;
        notification.ReadAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return Ok(new { message = "Marked as read", data = MapToDto(notification) });
    }

    // PUT api/notifications/read-all
    [HttpPut("read-all")]
    public async Task<IActionResult> MarkAllAsRead()
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var unread = await _context.Notifications
            .Where(n => n.RecipientId == userId && !n.IsRead)
            .ToListAsync();

        foreach (var n in unread)
        {
            n.IsRead = true;
            n.ReadAt = DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();

        return Ok(new { message = $"{unread.Count} notifications marked as read" });
    }

    // DELETE api/notifications/{id}
    [HttpDelete("{id}")]
    public async Task<IActionResult> DeleteNotification(Guid id)
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var notification = await _context.Notifications
            .FirstOrDefaultAsync(n => n.Id == id && n.RecipientId == userId);

        if (notification == null) return NotFound(new { message = "Notification not found" });

        _context.Notifications.Remove(notification);
        await _context.SaveChangesAsync();

        return Ok(new { message = "Notification deleted" });
    }

    // Internal helper called by other services to persist a notification
    public static async Task CreateAsync(
        ApplicationDbContext context,
        Guid recipientId,
        string type,
        string title,
        string body,
        string? relatedId = null,
        string? relatedType = null)
    {
        context.Notifications.Add(new Notification
        {
            Id = Guid.NewGuid(),
            RecipientId = recipientId,
            Type = type,
            Title = title,
            Body = body,
            RelatedId = relatedId,
            RelatedType = relatedType,
            IsRead = false,
            CreatedAt = DateTime.UtcNow
        });
        await context.SaveChangesAsync();
    }

    private bool TryGetUserId(out Guid userId)
    {
        var value = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.TryParse(value, out userId);
    }

    private static NotificationDto MapToDto(Notification n) => new()
    {
        Id = n.Id.ToString(),
        Type = n.Type,
        Title = n.Title,
        Body = n.Body,
        RelatedId = n.RelatedId,
        RelatedType = n.RelatedType,
        IsRead = n.IsRead,
        CreatedAt = n.CreatedAt.ToString("o"),
        ReadAt = n.ReadAt?.ToString("o")
    };
}
