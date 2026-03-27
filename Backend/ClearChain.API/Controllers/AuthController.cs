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
    private readonly IPushNotificationService _pushNotificationService;

    public AuthController(
        IAuthService authService,
        ApplicationDbContext context,
        IAdminNotificationService adminNotificationService,
        IPushNotificationService pushNotificationService)
    {
        _authService = authService;
        _context = context;
        _adminNotificationService = adminNotificationService;
        _pushNotificationService = pushNotificationService;
    }

    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        var (success, message, response) = await _authService.RegisterAsync(request);

        if (!success)
            return BadRequest(new { message });

        if (response?.User != null)
        {
            try
            {
                var orgData = new ClearChain.API.DTOs.Admin.OrganizationData
                {
                    Id = response.User.Id.ToString(),
                    Name = response.User.Name,
                    Email = response.User.Email,
                    Type = response.User.Type,
                    Phone = response.User.Phone,
                    Address = response.User.Address,
                    Location = response.User.Location ?? "",
                    Verified = response.User.Verified,
                    VerificationStatus = response.User.VerificationStatus,
                    CreatedAt = response.User.CreatedAt
                };

                await _pushNotificationService.SendNewRegistrationAlertToAdmins(orgData);
                await _pushNotificationService.SendWelcomeNotification(orgData);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Failed to send admin alert: {ex.Message}");
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

    // ═══ UPDATED: /me now includes new fields (Part 1) ═══
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
                Id = user.Id,
                Name = user.Name,
                Type = user.Type,
                Email = user.Email,
                Phone = user.Phone ?? "",
                Address = user.Address ?? "",
                Location = user.Location ?? "",
                Verified = user.Verified,
                VerificationStatus = user.VerificationStatus ?? "pending",
                Hours = user.Hours,
                ProfilePictureUrl = user.ProfilePictureUrl,
                CreatedAt = user.CreatedAt.ToString("o"),
                // ═══ NEW FIELDS (Part 1) ═══
                Latitude = user.Latitude,
                Longitude = user.Longitude,
                ContactPerson = user.ContactPerson,
                PickupInstructions = user.PickupInstructions,
                Description = user.Description
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
            var existingTokensWithSameToken = await _context.FCMTokens
                .Where(t => t.Token == request.FcmToken).ToListAsync();
            if (existingTokensWithSameToken.Any())
                _context.FCMTokens.RemoveRange(existingTokensWithSameToken);

            var oldTokensForThisUser = await _context.FCMTokens
                .Where(t => t.OrganizationId == userGuid && t.Token != request.FcmToken).ToListAsync();
            if (oldTokensForThisUser.Any())
                _context.FCMTokens.RemoveRange(oldTokensForThisUser);

            _context.FCMTokens.Add(new FCMToken
            {
                Id = Guid.NewGuid(),
                OrganizationId = userGuid,
                Token = request.FcmToken,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            });
            await _context.SaveChangesAsync();

            return Ok(new { message = "FCM token registered successfully" });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { message = "Failed to register FCM token", error = ex.Message });
        }
    }
}