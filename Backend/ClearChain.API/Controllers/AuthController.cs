using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using ClearChain.API.Services;
using ClearChain.API.DTOs.Auth;
using ClearChain.Infrastructure.Data;
using Microsoft.EntityFrameworkCore;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly IAuthService _authService;
    private readonly ApplicationDbContext _context;

    public AuthController(IAuthService authService, ApplicationDbContext context)
    {
        _authService = authService;
        _context = context;
    }

    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        var (success, message, response) = await _authService.RegisterAsync(request);

        if (!success)
            return BadRequest(new { message });

        return Ok(new { message, data = response });
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        var (success, message, response) = await _authService.LoginAsync(request);

        if (!success)
            return Unauthorized(new { message });

        return Ok(new { message, data = response });
    }

    [HttpPost("refresh")]
    public async Task<IActionResult> RefreshToken([FromBody] RefreshTokenRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        var (success, message, response) = await _authService.RefreshTokenAsync(request.RefreshToken);

        if (!success)
            return Unauthorized(new { message });

        return Ok(new { message, data = response });
    }

    [Authorize]
    [HttpPost("logout")]
    public async Task<IActionResult> Logout([FromBody] RefreshTokenRequest request)
    {
        var success = await _authService.RevokeRefreshTokenAsync(request.RefreshToken);

        if (!success)
            return BadRequest(new { message = "Invalid refresh token" });

        return Ok(new { message = "Logged out successfully" });
    }

    [Authorize]
    [HttpGet("me")]
    public async Task<IActionResult> GetCurrentUser()
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;

        if (userId == null || !Guid.TryParse(userId, out var userGuid))
            return Unauthorized(new { message = "Invalid token" });

        // Query full user from DB
        var user = await _context.Organizations.FindAsync(userGuid);
        if (user == null)
            return NotFound(new { message = "User not found" });

        // Android's AuthApi expects AuthResponse shape: { data: AuthData }
        var responseData = new
        {
            accessToken = "",
            refreshToken = "",
            tokenType = "Bearer",
            expiresIn = 0,
            user = new OrganizationDto
            {
                Id                 = user.Id,
                Name               = user.Name,
                Type               = user.Type,
                Email              = user.Email,
                Phone              = user.Phone ?? "",
                Address            = user.Address ?? "",
                Location           = user.Location ?? "",
                Verified           = user.Verified,
                VerificationStatus = user.VerificationStatus ?? "pending",
                Hours              = user.Hours,
                ProfilePictureUrl  = user.ProfilePictureUrl,
                CreatedAt          = user.CreatedAt.ToString("o")
            }
        };

        return Ok(new { data = responseData });
    }

    [Authorize]
    [HttpPost("change-password")]
    public async Task<IActionResult> ChangePassword([FromBody] ChangePasswordRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;

        if (userId == null || !Guid.TryParse(userId, out var userGuid))
            return Unauthorized(new { message = "Invalid token" });

        var success = await _authService.ChangePasswordAsync(userGuid, request);

        if (!success)
            return BadRequest(new { message = "Current password is incorrect" });

        return Ok(new { message = "Password changed successfully" });
    }
}