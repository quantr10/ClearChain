using ClearChain.API.Common;
using ClearChain.API.DTOs.Cart;
using ClearChain.API.DTOs.PickupRequests;
using ClearChain.API.Middleware;
using ClearChain.API.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/cart")]
[Authorize]
public class CartController : ControllerBase
{
    private readonly ICartService _service;

    public CartController(ICartService service)
    {
        _service = service;
    }

    [HttpGet]
    public async Task<ActionResult<CartResponse>> GetCart()
    {
        if (!this.TryGetUserId(out var userId))
            return Unauthorized(new { message = "User not authenticated" });

        var result = await _service.GetCartAsync(userId);
        if (!result.Success) return MapError(result);

        return Ok(new CartResponse { Message = "Cart retrieved successfully", Data = result.Cart ?? new() });
    }

    [HttpPost("items")]
    public async Task<ActionResult<CartResponse>> AddItem([FromBody] AddCartItemRequest request)
    {
        if (!this.TryGetUserId(out var userId))
            return Unauthorized(new { message = "User not authenticated" });

        var result = await _service.AddItemAsync(userId, request);
        if (!result.Success) return MapError(result);

        return Ok(new CartResponse { Message = "Cart updated successfully", Data = result.Cart ?? new() });
    }

    [HttpPut("items/{itemId:guid}")]
    public async Task<ActionResult<CartResponse>> UpdateItem(Guid itemId, [FromBody] UpdateCartItemRequest request)
    {
        if (!this.TryGetUserId(out var userId))
            return Unauthorized(new { message = "User not authenticated" });

        var result = await _service.UpdateItemAsync(userId, itemId, request);
        if (!result.Success) return MapError(result);

        return Ok(new CartResponse { Message = "Cart updated successfully", Data = result.Cart ?? new() });
    }

    [HttpPost("checkout")]
    [RequireVerifiedOrganization]
    public async Task<ActionResult<PickupRequestResponse>> Checkout([FromBody] CheckoutCartGroupRequest request)
    {
        if (!this.TryGetUserId(out var userId))
            return Unauthorized(new { message = "User not authenticated" });

        var result = await _service.CheckoutGroupAsync(userId, request);
        if (!result.Success) return MapError(result);

        return CreatedAtAction(
            "GetPickupRequestById",
            "PickupRequests",
            new { id = result.PickupRequest!.Id },
            new PickupRequestResponse { Message = "Pickup request created successfully", Data = result.PickupRequest! });
    }

    private ActionResult MapError(CartServiceResult result) => result.Error switch
    {
        CartServiceError.NotFound => NotFound(new { message = result.ErrorMessage }),
        CartServiceError.Forbidden => Forbid(),
        CartServiceError.InvalidInput or CartServiceError.InvalidStatus => BadRequest(new { message = result.ErrorMessage }),
        _ => StatusCode(500, new { message = result.ErrorMessage })
    };
}
