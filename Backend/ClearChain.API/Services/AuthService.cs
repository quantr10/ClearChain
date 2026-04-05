using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using ClearChain.API.DTOs.Auth;
using BCrypt.Net;

namespace ClearChain.API.Services;

public interface IAuthService
{
    Task<(bool Success, string Message, AuthResponse? Response)> RegisterAsync(RegisterRequest request);
    Task<(bool Success, string Message, AuthResponse? Response)> LoginAsync(LoginRequest request);
    Task<(bool Success, string Message, AuthResponse? Response)> RefreshTokenAsync(string refreshToken);
    Task<bool> RevokeRefreshTokenAsync(string refreshToken);
    Task<bool> ChangePasswordAsync(Guid userId, ChangePasswordRequest request);
}

public class AuthService : IAuthService
{
    private readonly ApplicationDbContext _context;
    private readonly IJwtService _jwtService;
    private readonly int _refreshTokenExpiryDays;
    private readonly int _jwtExpirySeconds;

    public AuthService(
        ApplicationDbContext context,
        IJwtService jwtService,
        IConfiguration configuration)
    {
        _context = context;
        _jwtService = jwtService;
        _refreshTokenExpiryDays = int.Parse(configuration["REFRESH_TOKEN_EXPIRY_DAYS"] ?? "7");
        _jwtExpirySeconds = int.Parse(configuration["JWT_EXPIRY_MINUTES"] ?? "60") * 60;
    }

    public async Task<(bool Success, string Message, AuthResponse? Response)> RegisterAsync(RegisterRequest request)
    {
        if (await _context.Organizations.AnyAsync(o => o.Email.ToLower() == request.Email.ToLower()))
            return (false, "Email already registered", null);

        var passwordHash = BCrypt.Net.BCrypt.HashPassword(request.Password);

        var organization = new Organization
        {
            Id = Guid.NewGuid(),
            Name = request.Name,
            Type = request.Type.ToLower(),
            Email = request.Email.ToLower(),
            PasswordHash = passwordHash,
            Phone = null,
            Address = null,
            Location = null,
            Hours = null,
            Verified = true,
            VerificationStatus = "approved",
            AuthProvider = "local",
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.Organizations.Add(organization);
        await _context.SaveChangesAsync();

        if (!string.IsNullOrEmpty(request.FcmToken))
        {
            var existingTokens = await _context.FCMTokens
                .Where(t => t.Token == request.FcmToken).ToListAsync();
            if (existingTokens.Any())
            {
                _context.FCMTokens.RemoveRange(existingTokens);
                await _context.SaveChangesAsync();
            }

            _context.FCMTokens.Add(new FCMToken
            {
                Id = Guid.NewGuid(),
                OrganizationId = organization.Id,
                Token = request.FcmToken,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            });
            await _context.SaveChangesAsync();
        }

        var accessToken = _jwtService.GenerateAccessToken(organization);
        var refreshToken = _jwtService.GenerateRefreshToken();

        _context.RefreshTokens.Add(new RefreshToken
        {
            Id = Guid.NewGuid(),
            OrganizationId = organization.Id,
            Token = refreshToken,
            ExpiresAt = DateTime.UtcNow.AddDays(_refreshTokenExpiryDays),
            CreatedAt = DateTime.UtcNow,
            IsRevoked = false
        });
        await _context.SaveChangesAsync();

        return (true, "Registration successful. Please complete your profile!", new AuthResponse
        {
            AccessToken = accessToken,
            RefreshToken = refreshToken,
            TokenType = "Bearer",
            ExpiresIn = _jwtExpirySeconds,
            User = MapToDto(organization)
        });
    }

    public async Task<(bool Success, string Message, AuthResponse? Response)> LoginAsync(LoginRequest request)
    {
        var user = await _context.Organizations
            .FirstOrDefaultAsync(o => o.Email.ToLower() == request.Email.ToLower());

        if (user == null || user.PasswordHash == null)
            return (false, "Invalid email or password", null);

        if (!BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash))
            return (false, "Invalid email or password", null);

        var accessToken = _jwtService.GenerateAccessToken(user);
        var refreshToken = _jwtService.GenerateRefreshToken();

        _context.RefreshTokens.Add(new RefreshToken
        {
            Id = Guid.NewGuid(),
            OrganizationId = user.Id,
            Token = refreshToken,
            ExpiresAt = DateTime.UtcNow.AddDays(_refreshTokenExpiryDays),
            CreatedAt = DateTime.UtcNow,
            IsRevoked = false
        });
        await _context.SaveChangesAsync();

        return (true, "Login successful", new AuthResponse
        {
            AccessToken = accessToken,
            RefreshToken = refreshToken,
            TokenType = "Bearer",
            ExpiresIn = _jwtExpirySeconds,
            User = MapToDto(user)
        });
    }

    public async Task<(bool Success, string Message, AuthResponse? Response)> RefreshTokenAsync(string refreshToken)
    {
        var storedToken = await _context.RefreshTokens
            .Include(rt => rt.Organization)
            .FirstOrDefaultAsync(rt => rt.Token == refreshToken);

        if (storedToken == null) return (false, "Invalid refresh token", null);
        if (storedToken.IsRevoked) return (false, "Token has been revoked", null);
        if (storedToken.ExpiresAt < DateTime.UtcNow) return (false, "Token has expired", null);

        var user = await _context.Organizations.FindAsync(storedToken.OrganizationId);
        if (user == null) return (false, "User not found", null);

        var newAccessToken = _jwtService.GenerateAccessToken(user);
        var newRefreshToken = _jwtService.GenerateRefreshToken();

        storedToken.IsRevoked = true;
        storedToken.RevokedAt = DateTime.UtcNow;

        _context.RefreshTokens.Add(new RefreshToken
        {
            Id = Guid.NewGuid(),
            OrganizationId = user.Id,
            Token = newRefreshToken,
            ExpiresAt = DateTime.UtcNow.AddDays(_refreshTokenExpiryDays),
            CreatedAt = DateTime.UtcNow,
            IsRevoked = false
        });
        await _context.SaveChangesAsync();

        return (true, "Token refreshed successfully", new AuthResponse
        {
            AccessToken = newAccessToken,
            RefreshToken = newRefreshToken,
            TokenType = "Bearer",
            ExpiresIn = _jwtExpirySeconds,
            User = MapToDto(user)
        });
    }

    public async Task<bool> RevokeRefreshTokenAsync(string refreshToken)
    {
        var storedToken = await _context.RefreshTokens.FirstOrDefaultAsync(rt => rt.Token == refreshToken);
        if (storedToken == null || storedToken.IsRevoked) return false;

        storedToken.IsRevoked = true;
        storedToken.RevokedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<bool> ChangePasswordAsync(Guid userId, ChangePasswordRequest request)
    {
        var user = await _context.Organizations.FindAsync(userId);
        if (user == null || user.PasswordHash == null) return false;
        if (!BCrypt.Net.BCrypt.Verify(request.CurrentPassword, user.PasswordHash)) return false;

        user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);
        user.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();
        return true;
    }

    // ═══ UPDATED: includes new fields (Part 1) ═══
    private static OrganizationDto MapToDto(Organization org)
    {
        return new OrganizationDto
        {
            Id = org.Id,
            Name = org.Name,
            Type = org.Type,
            Email = org.Email,
            Phone = org.Phone ?? "",
            Address = org.Address ?? "",
            Location = org.Location ?? "",
            Verified = org.Verified,
            VerificationStatus = org.VerificationStatus,
            Hours = org.Hours,
            ProfilePictureUrl = org.ProfilePictureUrl,
            CreatedAt = org.CreatedAt.ToString("o"),
            // NEW
            Latitude = org.Latitude,
            Longitude = org.Longitude,
            ContactPerson = org.ContactPerson,
            PickupInstructions = org.PickupInstructions,
            Description = org.Description
        };
    }
}