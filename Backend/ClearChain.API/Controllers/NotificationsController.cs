using ClearChain.API.Common;
using ClearChain.API.DTOs.Notifications;
using ClearChain.Domain.Entities;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

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
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

        var clampedPage = Math.Max(1, page);
        var clampedSize = Math.Clamp(pageSize, 1, 50);

        // The nightly sweep is what normally bounds this table, but it can be behind — a
        // missed run, or a restore from backup. Applying the same cutoff here keeps the
        // endpoint's window exactly the one it reports, whatever state the rows are in.
        var cutoff = NotificationRetentionPolicy.Cutoff(DateTime.UtcNow);

        var query = _context.Notifications
            .Where(n => n.RecipientId == userId && n.CreatedAt >= cutoff);

        if (unreadOnly)
            query = query.Where(n => !n.IsRead);

        var total = await query.CountAsync();
        var unreadCount = await _context.Notifications
            .CountAsync(n => n.RecipientId == userId && n.CreatedAt >= cutoff && !n.IsRead);

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
            TotalPages = (int)Math.Ceiling((double)total / clampedSize),
            RetentionDays = NotificationRetentionPolicy.RetentionDays
        });
    }

    // PUT api/notifications/{id}/read
    [HttpPut("{id}/read")]
    public async Task<IActionResult> MarkAsRead(Guid id)
    {
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

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
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

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

    // DELETE api/notifications — clears the caller's whole inbox
    [HttpDelete]
    public async Task<IActionResult> DeleteAllNotifications()
    {
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

        var deleted = await _context.Notifications
            .Where(n => n.RecipientId == userId)
            .ExecuteDeleteAsync();

        return Ok(new { message = $"{deleted} notifications deleted" });
    }

    // Notifications are written by IPushNotificationService, never directly. Persisting a row
    // on its own would produce an inbox entry that was never pushed or broadcast — visible on
    // next launch, silent at the moment it mattered.

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
