using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using ClearChain.API.Services;
using ClearChain.API.DTOs.Auth;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly IAuthService _authService;

    public AuthController(IAuthService authService)
    {
        _authService = authService;
    }

    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterRequest request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        var (success, message, response) = await _authService.RegisterAsync(request);

        if (!success)
        {
            return BadRequest(new { message });
        }

        return Ok(new
        {
            message,
            data = response
        });
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequest request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        var (success, message, response) = await _authService.LoginAsync(request);

        if (!success)
        {
            return Unauthorized(new { message });
        }

        return Ok(new
        {
            message,
            data = response
        });
    }

    [HttpPost("refresh")]
    public async Task<IActionResult> RefreshToken([FromBody] RefreshTokenRequest request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        var (success, message, response) = await _authService.RefreshTokenAsync(request.RefreshToken);

        if (!success)
        {
            return Unauthorized(new { message });
        }

        return Ok(new
        {
            message,
            data = response
        });
    }

    [Authorize]
    [HttpPost("logout")]
    public async Task<IActionResult> Logout([FromBody] RefreshTokenRequest request)
    {
        var success = await _authService.RevokeRefreshTokenAsync(request.RefreshToken);

        if (!success)
        {
            return BadRequest(new { message = "Invalid refresh token" });
        }

        return Ok(new { message = "Logged out successfully" });
    }

   [Authorize]
    [HttpGet("me")]
    public IActionResult GetCurrentUser()  // Remove 'async Task'
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                ?? User.FindFirst("sub")?.Value;

        if (userId == null)
        {
            return Unauthorized(new { message = "Invalid token" });
        }

        // You can fetch full user details from database here
        var userInfo = new
        {
            id = userId,
            email = User.FindFirst(ClaimTypes.Email)?.Value,
            name = User.FindFirst(ClaimTypes.Name)?.Value,
            type = User.FindFirst("type")?.Value,
            verified = User.FindFirst("verified")?.Value
        };

        return Ok(new { data = userInfo });
    }

    [Authorize]
    [HttpPost("change-password")]
    public async Task<IActionResult> ChangePassword([FromBody] ChangePasswordRequest request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;

        if (userId == null || !Guid.TryParse(userId, out var userGuid))
        {
            return Unauthorized(new { message = "Invalid token" });
        }

        var success = await _authService.ChangePasswordAsync(userGuid, request);

        if (!success)
        {
            return BadRequest(new { message = "Current password is incorrect" });
        }

        return Ok(new { message = "Password changed successfully" });
    }
}