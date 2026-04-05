using ClearChain.API.DTOs.PickupRequests;
using ClearChain.API.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class PickupRequestsController : ControllerBase
{
    private readonly IPickupRequestService _service;

    public PickupRequestsController(IPickupRequestService service)
    {
        _service = service;
    }

    // ── POST api/pickuprequests ───────────────────────────────────────────────

    [HttpPost]
    public async Task<ActionResult<PickupRequestResponse>> CreatePickupRequest(
        [FromBody] CreatePickupRequestRequest request)
    {
        if (!TryGetUserId(out var userId))
            return Unauthorized(new { message = "User not authenticated" });

        var result = await _service.CreateAsync(userId, request);
        if (!result.Success) return MapError(result);

        return CreatedAtAction(nameof(GetPickupRequestById), new { id = result.Data!.Id },
            new PickupRequestResponse { Message = "Pickup request created successfully", Data = result.Data!});
    }

    // ── DELETE api/pickuprequests/{id} ────────────────────────────────────────

    [HttpDelete("{id}")]
    public async Task<ActionResult> CancelPickupRequest(Guid id)
    {
        if (!TryGetUserId(out var userId))
            return Unauthorized(new { message = "User not authenticated" });

        var result = await _service.CancelAsync(id, userId);
        if (!result.Success) return MapError(result);

        return Ok(new PickupRequestResponse { Message = "Pickup request cancelled successfully", Data = result.Data!});
    }

    // ── PUT api/pickuprequests/{id}/picked-up ─────────────────────────────────

    [HttpPut("{id}/picked-up")]
    [Consumes("multipart/form-data")]
    public async Task<ActionResult<PickupRequestResponse>> MarkPickedUp(
        Guid id, IFormFile proofPhoto)
    {
        if (!TryGetUserId(out var userId))
            return Unauthorized(new { message = "User not authenticated" });

        if (proofPhoto == null || proofPhoto.Length == 0)
            return BadRequest(new { message = "Proof photo is required to confirm pickup" });

        var allowedExtensions = new[] { ".jpg", ".jpeg", ".png", ".webp" };
        var extension = Path.GetExtension(proofPhoto.FileName).ToLowerInvariant();
        if (!allowedExtensions.Contains(extension))
            return BadRequest(new { message = "Invalid file type. Allowed: jpg, jpeg, png, webp" });

        using var stream = proofPhoto.OpenReadStream();
        var result = await _service.MarkPickedUpAsync(id, userId, stream, proofPhoto.FileName);
        if (!result.Success) return MapError(result);

        var response = new PickupRequestResponse { Message = "Pickup confirmed successfully", Data = result.Data!};
        return Ok(response);
    }

    // ── GET api/pickuprequests/ngo/my ─────────────────────────────────────────

    [HttpGet("ngo/my")]
    public async Task<ActionResult<PickupRequestsResponse>> GetMyPickupRequests(
        [FromQuery] int page = 1, [FromQuery] int pageSize = 20)
    {
        if (!TryGetUserId(out var userId))
            return Unauthorized(new { message = "User not authenticated" });

        var result = await _service.GetNgoRequestsAsync(userId, page, pageSize);
        if (!result.Success) return MapError(result);

        return Ok(result.ListData);
    }

    // ── GET api/pickuprequests/grocery/my ────────────────────────────────────

    [HttpGet("grocery/my")]
    public async Task<ActionResult<PickupRequestsResponse>> GetGroceryPickupRequests(
        [FromQuery] int page = 1, [FromQuery] int pageSize = 20)
    {
        if (!TryGetUserId(out var userId))
            return Unauthorized(new { message = "User not authenticated" });

        var result = await _service.GetGroceryRequestsAsync(userId, page, pageSize);
        if (!result.Success) return MapError(result);

        return Ok(result.ListData);
    }

    // ── GET api/pickuprequests/{id} ───────────────────────────────────────────

    [HttpGet("{id}")]
    public async Task<ActionResult<PickupRequestResponse>> GetPickupRequestById(Guid id)
    {
        var result = await _service.GetByIdAsync(id);
        if (!result.Success) return MapError(result);

        return Ok(new PickupRequestResponse { Message = "Pickup request retrieved successfully", Data = result.Data!});
    }

    // ── PUT api/pickuprequests/{id}/approve ───────────────────────────────────

    [HttpPut("{id}/approve")]
    public async Task<ActionResult<PickupRequestResponse>> ApprovePickupRequest(Guid id)
    {
        if (!TryGetUserId(out var userId))
            return Unauthorized(new { message = "User not authenticated" });

        var result = await _service.ApproveAsync(id, userId);
        if (!result.Success) return MapError(result);

        return Ok(new PickupRequestResponse { Message = "Pickup request approved successfully", Data = result.Data!});
    }

    // ── PUT api/pickuprequests/{id}/ready ─────────────────────────────────────

    [HttpPut("{id}/ready")]
    public async Task<ActionResult<PickupRequestResponse>> MarkReadyForPickup(Guid id)
    {
        if (!TryGetUserId(out var userId))
            return Unauthorized(new { message = "User not authenticated" });

        var result = await _service.MarkReadyAsync(id, userId);
        if (!result.Success) return MapError(result);

        return Ok(new PickupRequestResponse { Message = "Pickup request marked as ready", Data = result.Data!});
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private bool TryGetUserId(out Guid userId)
    {
        var value = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.TryParse(value, out userId);
    }

    private ActionResult MapError(PickupRequestServiceResult result) => result.Error switch
    {
        PickupRequestServiceError.NotFound    => NotFound(new { message = result.ErrorMessage }),
        PickupRequestServiceError.Forbidden   => Forbid(),
        PickupRequestServiceError.InvalidStatus
            or PickupRequestServiceError.InvalidInput
            or PickupRequestServiceError.StorageError => BadRequest(new { message = result.ErrorMessage }),
        _                                    => StatusCode(500, new { message = result.ErrorMessage })
    };
}
