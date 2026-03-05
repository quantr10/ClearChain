using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using ClearChain.API.Services;
using ClearChain.API.DTOs.Auth;
using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly IAuthService _authService;
    private readonly ApplicationDbContext _context;
    private readonly IAdminNotificationService _adminNotificationService;

    public AuthController(
        IAuthService authService, 
        ApplicationDbContext context,
        IAdminNotificationService adminNotificationService)
    {
        _authService = authService;
        _context = context;
        _adminNotificationService = adminNotificationService;
    }

    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        var (success, message, response) = await _authService.RegisterAsync(request);

        if (!success)
            return BadRequest(new { message });

        // Notify admins of new organization
        if (response?.User != null)
        {
            try
            {
                var orgNotification = new OrganizationRegisteredNotification
                {
                    OrganizationId = response.User.Id.ToString(),
                    Name = response.User.Name,
                    Type = response.User.Type,
                    Email = response.User.Email,
                    Location = response.User.Location ?? "",
                    RegisteredAt = DateTime.Parse(response.User.CreatedAt)
                };
                await _adminNotificationService.NotifyNewOrganizationRegistered(orgNotification);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Failed to send admin notification: {ex.Message}");
            }
        }

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

        var user = await _context.Organizations.FindAsync(userGuid);
        if (user == null)
            return NotFound(new { message = "User not found" });

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

    // ✅ NEW: Register FCM Token endpoint
    [HttpPost("fcm-token")]
    [Authorize]
    public async Task<IActionResult> RegisterFCMToken([FromBody] RegisterFCMTokenRequest request)
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;

        if (userId == null || !Guid.TryParse(userId, out var userGuid))
            return Unauthorized(new { message = "Invalid token" });

        try
        {
            // Delete old tokens for this user
            var oldTokens = await _context.FCMTokens
                .Where(t => t.OrganizationId == userGuid)
                .ToListAsync();
            _context.FCMTokens.RemoveRange(oldTokens);

            // Save new token
            var fcmToken = new FCMToken
            {
                Id = Guid.NewGuid(),
                OrganizationId = userGuid,
                Token = request.FcmToken,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            _context.FCMTokens.Add(fcmToken);
            await _context.SaveChangesAsync();

            return Ok(new { message = "FCM token registered successfully" });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { message = "Failed to register FCM token", error = ex.Message });
        }
    }
}