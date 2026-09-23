using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
using ClearChain.API.Common;
using ClearChain.API.DTOs.Inventory;
using ClearChain.API.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class InventoryController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly IInventoryNotificationService _inventoryNotificationService;

    public InventoryController(
        ApplicationDbContext context,
        IInventoryNotificationService inventoryNotificationService)
    {
        _context = context;
        _inventoryNotificationService = inventoryNotificationService;
    }

    private InventoryItemData MapToDto(Domain.Entities.Inventory item)
    {
        return new InventoryItemData
        {
            Id = item.Id.ToString(),
            NgoId = item.NgoId.ToString(),
            ProductName = item.ProductName,
            Category = item.Category,
            Quantity = item.Quantity,
            Unit = item.Unit,
            ExpiryDate = item.ExpiryDate.ToString("yyyy-MM-dd"),
            Status = item.Status.ToString().ToLower(),
            ReceivedAt = item.ReceivedAt.ToString("o"),
            DistributedAt = item.DistributedAt?.ToString("o"),
            PickupRequestId = item.PickupRequestId.ToString(),
            IsManuallyAdded = item.IsManuallyAdded,
            SourcePickupRequestId = item.SourcePickupRequestId,
            PhotoUrl = item.PhotoUrl,
            Notes = item.Notes
        };
    }

    // GET: api/inventory/my
    [HttpGet("my")]
    public async Task<ActionResult<InventoryListResponse>> GetMyInventory(
        [FromQuery] string? status = null)
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return Unauthorized(new { message = "User not authenticated" });
        }

        var query = _context.Inventories
            .Where(i => i.NgoId.ToString() == userId);

        // Filter by status if provided
        if (!string.IsNullOrEmpty(status) && Enum.TryParse<InventoryStatus>(status, ignoreCase: true, out var statusEnum))
        {
            query = query.Where(i => i.Status == statusEnum);
        }

        var items = await query
            .OrderByDescending(i => i.ReceivedAt)
            .ToListAsync();

        var responseData = items.Select(MapToDto).ToList();

        return Ok(new InventoryListResponse
        {
            Message = "Inventory retrieved successfully",
            Data = responseData
        });
    }

    // PUT: api/inventory/{id}/distribute
    [HttpPut("{id}/distribute")]
    public async Task<ActionResult<InventoryItemResponse>> DistributeItem(Guid id)
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return Unauthorized(new { message = "User not authenticated" });
        }

        var item = await _context.Inventories
            .FirstOrDefaultAsync(i => i.Id == id && i.NgoId.ToString() == userId);

        if (item == null)
        {
            return NotFound(new { message = "Inventory item not found" });
        }

        if (item.Status != InventoryStatus.Active)
        {
            return BadRequest(new { message = "Can only distribute active items" });
        }

        item.Status = InventoryStatus.Distributed;
        item.DistributedAt = DateTime.UtcNow;
        item.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        var responseData = MapToDto(item);

        await _inventoryNotificationService.NotifyInventoryItemDistributedAsync(
            item.Id.ToString(),
            item.NgoId
        );

        return Ok(new InventoryItemResponse
        {
            Message = "Item marked as distributed",
            Data = responseData
        });
    }

    // POST: api/inventory/update-expired
    [HttpPost("update-expired")]
    public async Task<ActionResult> UpdateExpiredItems()
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return Unauthorized(new { message = "User not authenticated" });
        }

        var expiredItems = await _context.Inventories
            .Where(i => i.NgoId.ToString() == userId &&
                       i.Status == InventoryStatus.Active &&
                       i.ExpiryDate < DateTime.UtcNow.Date)
            .ToListAsync();

        foreach (var item in expiredItems)
        {
            item.Status = InventoryStatus.Expired;
            item.UpdatedAt = DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();

        foreach (var item in expiredItems)
        {
            var itemDto = MapToDto(item);
            await _inventoryNotificationService.NotifyInventoryItemExpiredAsync(itemDto);
        }

        return Ok(new
        {
            message = $"{expiredItems.Count} items marked as expired",
            count = expiredItems.Count
        });
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<InventoryItemResponse>> GetInventoryItemById(Guid id)
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
            return Unauthorized(new { message = "User not authenticated" });

        var item = await _context.Inventories
            .FirstOrDefaultAsync(i => i.Id == id && i.NgoId.ToString() == userId);

        if (item == null)
            return NotFound(new { message = "Inventory item not found" });

        return Ok(new InventoryItemResponse
        {
            Message = "Inventory item retrieved successfully",
            Data = MapToDto(item)
        });
    }

}
