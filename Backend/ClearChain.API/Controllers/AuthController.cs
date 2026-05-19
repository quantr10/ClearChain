using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using ClearChain.API.Services;
using ClearChain.API.DTOs.Auth;
using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;
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
    private readonly ILogger<AuthController> _logger;

    public AuthController(
        IAuthService authService,
        ApplicationDbContext context,
        IAdminNotificationService adminNotificationService,
        IPushNotificationService pushNotificationService,
        ILogger<AuthController> logger)
    {
        _authService = authService;
        _context = context;
        _adminNotificationService = adminNotificationService;
        _pushNotificationService = pushNotificationService;
        _logger = logger;
    }

    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        var (success, message, newOrg) = await _authService.RegisterAsync(request);

        if (!success)
            return BadRequest(new { message });

        if (newOrg != null)
        {
            try
            {
                var orgData = new ClearChain.API.DTOs.Admin.OrganizationData
                {
                    Id = newOrg.Id.ToString(),
                    Name = newOrg.Name,
                    Email = newOrg.Email,
                    Type = newOrg.Type,
                    Phone = newOrg.Phone ?? "",
                    Address = newOrg.Address ?? "",
                    Location = newOrg.Location ?? "",
                    Verified = newOrg.Verified,
                    VerificationStatus = newOrg.VerificationStatus,
                    CreatedAt = newOrg.CreatedAt.ToString("o")
                };

                await _pushNotificationService.SendNewRegistrationAlertToAdmins(orgData);
                await _pushNotificationService.SendWelcomeNotification(orgData);

                await _adminNotificationService.NotifyNewOrganizationRegistered(new OrganizationRegisteredNotification
                {
                    OrganizationId = newOrg.Id.ToString(),
                    Name           = newOrg.Name,
                    Type           = newOrg.Type,
                    Email          = newOrg.Email,
                    Location       = newOrg.Location ?? newOrg.Address ?? "",
                    RegisteredAt   = newOrg.CreatedAt
                });

                var totalNgos      = await _context.Organizations.CountAsync(o => o.Type == "ngo");
                var totalGroceries = await _context.Organizations.CountAsync(o => o.Type == "grocery");
                var totalDonations = await _context.PickupRequests.CountAsync();
                var activeListings = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Open);
                var pendingReqs    = await _context.PickupRequests.CountAsync(r => r.Status == PickupRequestStatus.Pending);
                var today          = DateTime.UtcNow.Date;
                var completedToday = await _context.PickupRequests.CountAsync(r =>
                    r.Status == PickupRequestStatus.Completed && r.RequestedAt.Date == today);

                await _adminNotificationService.NotifyStatsUpdated(new PlatformStatsNotification
                {
                    TotalNGOs       = totalNgos,
                    TotalGroceries  = totalGroceries,
                    TotalDonations  = totalDonations,
                    ActiveListings  = activeListings,
                    PendingRequests = pendingReqs,
                    CompletedToday  = completedToday,
                    UpdatedAt       = DateTime.UtcNow
                });
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Failed to send registration notifications for {Email}", request.Email);
            }
        }

        return Ok(new { message });
    }

    [HttpPost("verify-email")]
    public async Task<IActionResult> VerifyEmail([FromBody] VerifyEmailRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        var (success, message, response) = await _authService.VerifyEmailAsync(request.Email, request.Code);

        if (!success)
            return BadRequest(new { message });

        return Ok(new { message, data = response });
    }

    [HttpPost("resend-verification")]
    public async Task<IActionResult> ResendVerification([FromBody] ResendVerificationRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        var (success, message) = await _authService.ResendVerificationAsync(request.Email);

        if (!success)
            return BadRequest(new { message });

        return Ok(new { message });
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

        var userDto = new OrganizationDto
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
            Latitude = user.Latitude,
            Longitude = user.Longitude,
            ContactPerson = user.ContactPerson,
            PickupInstructions = user.PickupInstructions,
            Description = user.Description
        };

        return Ok(new { data = new { user = userDto } });
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

    // GET api/auth/check-email?email=...
    [HttpGet("check-email")]
    public async Task<IActionResult> CheckEmailAvailable([FromQuery] string email)
    {
        if (string.IsNullOrWhiteSpace(email))
            return BadRequest(new { message = "Email is required" });

        var (available, message) = await _authService.CheckEmailAvailableAsync(email);
        return Ok(new { available, message });
    }

    // DELETE api/auth/account
    [Authorize]
    [HttpDelete("account")]
    public async Task<IActionResult> DeleteAccount([FromBody] DeleteAccountRequest request)
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;

        if (userId == null || !Guid.TryParse(userId, out var userGuid))
            return Unauthorized(new { message = "Invalid token" });

        var (success, message) = await _authService.DeleteAccountAsync(userGuid, request.Password);
        if (!success) return BadRequest(new { message });

        return Ok(new { message });
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