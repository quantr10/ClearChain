using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
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
    private readonly ILogger<InventoryController> _logger;
    private readonly IInventoryNotificationService _inventoryNotificationService;
    private readonly IStorageService _storageService;

    public InventoryController(
        ApplicationDbContext context,
        ILogger<InventoryController> logger,
        IInventoryNotificationService inventoryNotificationService,
        IStorageService storageService)
    {
        _context = context;
        _logger = logger;
        _inventoryNotificationService = inventoryNotificationService;
        _storageService = storageService;
    }

    // ✅ NEW: Helper method to map entity to DTO
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
            BeneficiaryCount = item.BeneficiaryCount,
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
            if (!string.IsNullOrEmpty(status) && Enum.TryParse<InventoryStatus>(status, ignoreCase: true, out var statusEnum))
            {
                query = query.Where(i => i.Status == statusEnum);
            }

            var items = await query
                .OrderByDescending(i => i.ReceivedAt)
                .ToListAsync();

            // ✅ Use helper method for mapping
            var responseData = items.Select(MapToDto).ToList();

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

            if (item.Status != InventoryStatus.Active)
            {
                return BadRequest(new { message = "Can only distribute active items" });
            }

            item.Status = InventoryStatus.Distributed;
            item.DistributedAt = DateTime.UtcNow;
            item.UpdatedAt = DateTime.UtcNow;

            await _context.SaveChangesAsync();

            // ✅ Map to DTO using helper method
            var responseData = MapToDto(item);

            // ✅ ADD: Send real-time notification
            await _inventoryNotificationService.NotifyInventoryItemDistributed(
                item.Id.ToString(),
                item.NgoId
            );

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
                           i.Status == InventoryStatus.Active &&
                           i.ExpiryDate < DateTime.UtcNow.Date)
                .ToListAsync();

            foreach (var item in expiredItems)
            {
                item.Status = InventoryStatus.Expired;
                item.UpdatedAt = DateTime.UtcNow;
            }

            await _context.SaveChangesAsync();

            // ✅ ADD: Send notifications for each expired item
            foreach (var item in expiredItems)
            {
                var itemDto = MapToDto(item);
                await _inventoryNotificationService.NotifyInventoryItemExpired(itemDto);
            }

            return Ok(new { 
                message = $"{expiredItems.Count} items marked as expired",
                count = expiredItems.Count
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating expired items");
            return StatusCode(500, new { message = "An error occurred while updating expired items" });
        }
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<InventoryItemResponse>> GetInventoryItemById(Guid id)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
                return Unauthorized(new { message = "User not authenticated" });

            var item = await _context.Inventories
                .FirstOrDefaultAsync(i => i.Id == id);

            if (item == null)
                return NotFound(new { message = "Inventory item not found" });

            return Ok(new InventoryItemResponse
            {
                Message = "Inventory item retrieved successfully",
                Data = MapToDto(item)
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting inventory item by id");
            return StatusCode(500, new { message = "An error occurred while retrieving the inventory item" });
        }
    }

    // PUT: api/inventory/{id} — Edit an inventory item
    [HttpPut("{id}")]
    public async Task<ActionResult<InventoryItemResponse>> UpdateInventoryItem(
        Guid id, [FromBody] UpdateInventoryItemRequest request)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
                return Unauthorized(new { message = "User not authenticated" });

            var item = await _context.Inventories
                .FirstOrDefaultAsync(i => i.Id == id && i.NgoId.ToString() == userId);

            if (item == null)
                return NotFound(new { message = "Inventory item not found" });

            if (request.Quantity.HasValue && request.Quantity.Value > 0)
                item.Quantity = request.Quantity.Value;
            if (request.ExpiryDate.HasValue)
                item.ExpiryDate = request.ExpiryDate.Value;
            if (request.Notes != null)
                item.Notes = request.Notes;

            item.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();

            return Ok(new InventoryItemResponse
            {
                Message = "Inventory item updated",
                Data = MapToDto(item)
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating inventory item");
            return StatusCode(500, new { message = "An error occurred" });
        }
    }

    // PUT: api/inventory/{id}/distribute — now accepts beneficiary count
    [HttpPut("{id}/distribute-with-count")]
    public async Task<ActionResult<InventoryItemResponse>> DistributeItemWithCount(
        Guid id, [FromBody] DistributeWithCountRequest request)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
                return Unauthorized(new { message = "User not authenticated" });

            var item = await _context.Inventories
                .FirstOrDefaultAsync(i => i.Id == id && i.NgoId.ToString() == userId);

            if (item == null) return NotFound(new { message = "Inventory item not found" });
            if (item.Status != InventoryStatus.Active)
                return BadRequest(new { message = "Can only distribute active items" });

            item.Status = InventoryStatus.Distributed;
            item.DistributedAt = DateTime.UtcNow;
            item.BeneficiaryCount = request.BeneficiaryCount;
            item.UpdatedAt = DateTime.UtcNow;

            await _context.SaveChangesAsync();

            await _inventoryNotificationService.NotifyInventoryItemDistributed(
                item.Id.ToString(), item.NgoId);

            return Ok(new InventoryItemResponse
            {
                Message = "Item marked as distributed",
                Data = MapToDto(item)
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error distributing item with count");
            return StatusCode(500, new { message = "An error occurred" });
        }
    }

    // POST: api/inventory/manual — Manually add an inventory item (not from pickup)
    [HttpPost("manual")]
    public async Task<ActionResult<InventoryItemResponse>> AddManualItem(
        [FromBody] AddManualInventoryItemRequest request)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId) || !Guid.TryParse(userId, out var userGuid))
                return Unauthorized(new { message = "User not authenticated" });

            if (!DateTime.TryParse(request.ExpiryDate, out var expiry))
                return BadRequest(new { message = "Invalid expiry date format" });

            var item = new Domain.Entities.Inventory
            {
                Id = Guid.NewGuid(),
                NgoId = userGuid,
                PickupRequestId = Guid.Empty,  // no linked request
                ProductName = request.ProductName,
                Category = request.Category,
                Quantity = request.Quantity,
                Unit = request.Unit,
                ExpiryDate = DateTime.SpecifyKind(expiry, DateTimeKind.Utc),
                Status = InventoryStatus.Active,
                Notes = request.Notes,
                IsManuallyAdded = true,
                ReceivedAt = DateTime.UtcNow,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            _context.Inventories.Add(item);
            await _context.SaveChangesAsync();

            return Ok(new InventoryItemResponse
            {
                Message = "Inventory item added",
                Data = MapToDto(item)
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error adding manual inventory item");
            return StatusCode(500, new { message = "An error occurred" });
        }
    }

    // GET: api/inventory/export — Export inventory as CSV
    [HttpGet("export")]
    public async Task<IActionResult> ExportInventoryCsv([FromQuery] string? status = null)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
                return Unauthorized(new { message = "User not authenticated" });

            var query = _context.Inventories.Where(i => i.NgoId.ToString() == userId);

            if (!string.IsNullOrEmpty(status) && Enum.TryParse<InventoryStatus>(status, ignoreCase: true, out var statusEnum))
                query = query.Where(i => i.Status == statusEnum);

            var items = await query.OrderByDescending(i => i.ReceivedAt).ToListAsync();

            var csv = new System.Text.StringBuilder();
            csv.AppendLine("Product Name,Category,Quantity,Unit,Expiry Date,Status,Received At,Distributed At,Beneficiary Count,Manual,Notes");

            foreach (var item in items)
            {
                csv.AppendLine(string.Join(",",
                    $"\"{item.ProductName}\"",
                    $"\"{item.Category}\"",
                    item.Quantity,
                    $"\"{item.Unit}\"",
                    item.ExpiryDate.ToString("yyyy-MM-dd"),
                    item.Status.ToString().ToLower(),
                    item.ReceivedAt.ToString("yyyy-MM-dd"),
                    item.DistributedAt?.ToString("yyyy-MM-dd") ?? "",
                    item.BeneficiaryCount,
                    item.IsManuallyAdded ? "yes" : "no",
                    $"\"{item.Notes ?? ""}\""
                ));
            }

            var bytes = System.Text.Encoding.UTF8.GetBytes(csv.ToString());
            return File(bytes, "text/csv", $"inventory_{DateTime.UtcNow:yyyyMMdd}.csv");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error exporting inventory");
            return StatusCode(500, new { message = "An error occurred during export" });
        }
    }

    // GET: api/inventory/stats — Category breakdown and expiry alerts
    [HttpGet("stats")]
    public async Task<IActionResult> GetInventoryStats()
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
                return Unauthorized(new { message = "User not authenticated" });

            var allActive = await _context.Inventories
                .Where(i => i.NgoId.ToString() == userId && i.Status == InventoryStatus.Active)
                .ToListAsync();

            var categoryBreakdown = allActive
                .GroupBy(i => i.Category)
                .Select(g => new { category = g.Key, count = g.Count(), quantity = g.Sum(i => (double)i.Quantity) })
                .OrderByDescending(g => g.count)
                .ToList();

            var expiringSoon = allActive
                .Where(i => i.ExpiryDate <= DateTime.UtcNow.AddHours(48))
                .Count();

            var totalBeneficiariesServed = await _context.Inventories
                .Where(i => i.NgoId.ToString() == userId && i.Status == InventoryStatus.Distributed)
                .SumAsync(i => i.BeneficiaryCount);

            return Ok(new
            {
                data = new
                {
                    categoryBreakdown,
                    expiringSoonCount = expiringSoon,
                    totalBeneficiariesServed,
                    totalActiveItems = allActive.Count
                }
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting inventory stats");
            return StatusCode(500, new { message = "An error occurred" });
        }
    }

    // POST: api/inventory/{id}/photo — Attach a photo to an inventory item
    [HttpPost("{id}/photo")]
    [Consumes("multipart/form-data")]
    public async Task<ActionResult<InventoryItemResponse>> UploadItemPhoto(
        Guid id, IFormFile photo)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
                return Unauthorized(new { message = "User not authenticated" });

            if (photo == null || photo.Length == 0)
                return BadRequest(new { message = "No photo provided" });

            var item = await _context.Inventories
                .FirstOrDefaultAsync(i => i.Id == id && i.NgoId.ToString() == userId);

            if (item == null)
                return NotFound(new { message = "Inventory item not found" });

            using var stream = photo.OpenReadStream();
            var url = await _storageService.UploadFileAsync(
                stream, photo.FileName, photo.ContentType, "inventory-photos");

            item.PhotoUrl = url;
            item.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();

            return Ok(new InventoryItemResponse
            {
                Message = "Photo uploaded",
                Data = MapToDto(item)
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error uploading inventory photo");
            return StatusCode(500, new { message = "An error occurred" });
        }
    }

    // GET: api/inventory/{id}/history — Lifecycle timeline for one item
    [HttpGet("{id}/history")]
    public async Task<IActionResult> GetItemHistory(Guid id)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
                return Unauthorized(new { message = "User not authenticated" });

            var item = await _context.Inventories
                .FirstOrDefaultAsync(i => i.Id == id && i.NgoId.ToString() == userId);

            if (item == null)
                return NotFound(new { message = "Inventory item not found" });

            // Build synthetic timeline from known timestamps
            var events = new List<object>();

            if (item.IsManuallyAdded)
            {
                events.Add(new { status = "manual_add", label = "Added Manually", timestamp = item.ReceivedAt.ToString("o"), note = item.Notes ?? "" });
            }
            else
            {
                // Source pickup request info
                var srcId = item.SourcePickupRequestId ?? item.PickupRequestId.ToString();
                PickupRequest? pr = null;
                if (Guid.TryParse(srcId, out var prGuid))
                    pr = await _context.PickupRequests
                        .Include(r => r.Grocery)
                        .FirstOrDefaultAsync(r => r.Id == prGuid);

                events.Add(new
                {
                    status = "pickup_completed",
                    label = "Received from Pickup",
                    timestamp = item.ReceivedAt.ToString("o"),
                    note = pr != null ? $"From {pr.Grocery?.Name ?? "grocery"}" : "From pickup request"
                });
            }

            events.Add(new { status = "stored", label = "Stored in Inventory", timestamp = item.ReceivedAt.ToString("o"), note = $"{item.Quantity} {item.Unit} of {item.ProductName}" });

            if (item.DistributedAt.HasValue)
            {
                events.Add(new
                {
                    status = "distributed",
                    label = "Distributed",
                    timestamp = item.DistributedAt.Value.ToString("o"),
                    note = item.BeneficiaryCount > 0
                        ? $"{item.BeneficiaryCount} beneficiaries served"
                        : "Distributed"
                });
            }

            return Ok(new { data = new { itemId = item.Id, events } });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting inventory history");
            return StatusCode(500, new { message = "An error occurred" });
        }
    }
}

public class UpdateInventoryItemRequest
{
    public decimal? Quantity { get; set; }
    public DateTime? ExpiryDate { get; set; }
    public string? Notes { get; set; }
}

public class DistributeWithCountRequest
{
    public int BeneficiaryCount { get; set; }
}

public class AddManualInventoryItemRequest
{
    public string ProductName { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public decimal Quantity { get; set; }
    public string Unit { get; set; } = string.Empty;
    public string ExpiryDate { get; set; } = string.Empty;
    public string? Notes { get; set; }
}