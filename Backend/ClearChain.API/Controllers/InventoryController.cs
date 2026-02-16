using ClearChain.Infrastructure.Data;
using ClearChain.API.DTOs.Inventory;
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
    private readonly ILogger<InventoryController> _logger;

    public InventoryController(
        ApplicationDbContext context,
        ILogger<InventoryController> logger)
    {
        _context = context;
        _logger = logger;
    }

    // GET: api/inventory/my
    [HttpGet("my")]
    public async Task<ActionResult<InventoryListResponse>> GetMyInventory(
        [FromQuery] string? status = null)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var query = _context.Inventories
                .Where(i => i.NgoId.ToString() == userId);

            // Filter by status if provided
            if (!string.IsNullOrEmpty(status))
            {
                query = query.Where(i => i.Status.ToLower() == status.ToLower());
            }

            var items = await query
                .OrderByDescending(i => i.ReceivedAt)
                .ToListAsync();

            var responseData = items.Select(i => new InventoryItemData
            {
                Id = i.Id.ToString(),
                ProductName = i.ProductName,
                Category = i.Category,
                Quantity = i.Quantity,
                Unit = i.Unit,
                ExpiryDate = i.ExpiryDate.ToString("yyyy-MM-dd"),
                Status = i.Status,
                ReceivedAt = i.ReceivedAt.ToString("o"),
                DistributedAt = i.DistributedAt?.ToString("o")
            }).ToList();

            return Ok(new InventoryListResponse
            {
                Message = "Inventory retrieved successfully",
                Data = responseData
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting inventory");
            return StatusCode(500, new { message = "An error occurred while retrieving inventory" });
        }
    }

    // PUT: api/inventory/{id}/distribute
    [HttpPut("{id}/distribute")]
    public async Task<ActionResult<InventoryItemResponse>> DistributeItem(Guid id)
    {
        try
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

            if (item.Status != "active")
            {
                return BadRequest(new { message = "Can only distribute active items" });
            }

            item.Status = "distributed";
            item.DistributedAt = DateTime.UtcNow;
            item.UpdatedAt = DateTime.UtcNow;

            await _context.SaveChangesAsync();

            var responseData = new InventoryItemData
            {
                Id = item.Id.ToString(),
                ProductName = item.ProductName,
                Category = item.Category,
                Quantity = item.Quantity,
                Unit = item.Unit,
                ExpiryDate = item.ExpiryDate.ToString("yyyy-MM-dd"),
                Status = item.Status,
                ReceivedAt = item.ReceivedAt.ToString("o"),
                DistributedAt = item.DistributedAt?.ToString("o")
            };

            return Ok(new InventoryItemResponse
            {
                Message = "Item marked as distributed",
                Data = responseData
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error distributing item");
            return StatusCode(500, new { message = "An error occurred while distributing the item" });
        }
    }

    // POST: api/inventory/update-expired
    [HttpPost("update-expired")]
    public async Task<ActionResult> UpdateExpiredItems()
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User not authenticated" });
            }

            var expiredItems = await _context.Inventories
                .Where(i => i.NgoId.ToString() == userId && 
                           i.Status == "active" && 
                           i.ExpiryDate < DateTime.UtcNow.Date)
                .ToListAsync();

            foreach (var item in expiredItems)
            {
                item.Status = "expired";
                item.UpdatedAt = DateTime.UtcNow;
            }

            await _context.SaveChangesAsync();

            return Ok(new { message = $"{expiredItems.Count} items marked as expired" });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating expired items");
            return StatusCode(500, new { message = "An error occurred while updating expired items" });
        }
    }
}