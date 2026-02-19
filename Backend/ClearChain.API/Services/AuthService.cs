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
    private readonly IConfiguration _configuration;

    public AuthService(
        ApplicationDbContext context,
        IJwtService jwtService,
        IConfiguration configuration)
    {
        _context = context;
        _jwtService = jwtService;
        _configuration = configuration;
    }

    public async Task<(bool Success, string Message, AuthResponse? Response)> RegisterAsync(RegisterRequest request)
    {
        // Check if email already exists
        if (await _context.Organizations.AnyAsync(o => o.Email.ToLower() == request.Email.ToLower()))
        {
            return (false, "Email already registered", null);
        }

        // Hash password
        var passwordHash = BCrypt.Net.BCrypt.HashPassword(request.Password);

        // Create organization
        var organization = new Organization
        {
            Id = Guid.NewGuid(),
            Name = request.Name,
            Type = request.Type.ToLower(),
            Email = request.Email.ToLower(),
            PasswordHash = passwordHash,
            Phone = request.Phone,
            Address = request.Address,
            Location = request.Location,
            Hours = request.Hours,
            Verified = false,
            VerificationStatus = "pending",
            AuthProvider = "local",
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.Organizations.Add(organization);
        await _context.SaveChangesAsync();

        // Generate tokens
        var accessToken = _jwtService.GenerateAccessToken(organization);
        var refreshToken = _jwtService.GenerateRefreshToken();

        // Save refresh token
        var refreshTokenEntity = new RefreshToken
        {
            Id = Guid.NewGuid(),
            OrganizationId = organization.Id,
            Token = refreshToken,
            ExpiresAt = DateTime.UtcNow.AddDays(
                int.Parse(_configuration["REFRESH_TOKEN_EXPIRY_DAYS"] ?? "7")
            ),
            CreatedAt = DateTime.UtcNow,
            IsRevoked = false
        };

        _context.RefreshTokens.Add(refreshTokenEntity);
        await _context.SaveChangesAsync();

        var response = new AuthResponse
        {
            AccessToken = accessToken,
            RefreshToken = refreshToken,
            TokenType = "Bearer",
            ExpiresIn = int.Parse(_configuration["JWT_EXPIRY_MINUTES"] ?? "60") * 60,
            User = MapToDto(organization)
        };

        return (true, "Registration successful. Awaiting admin verification.", response);
    }

    public async Task<(bool Success, string Message, AuthResponse? Response)> LoginAsync(LoginRequest request)
    {
        // Find user by email
        var user = await _context.Organizations
            .FirstOrDefaultAsync(o => o.Email.ToLower() == request.Email.ToLower());

        if (user == null || user.PasswordHash == null)
        {
            return (false, "Invalid email or password", null);
        }

        // Verify password
        if (!BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash))
        {
            return (false, "Invalid email or password", null);
        }

        // Check if verified (optional - can allow login even if not verified)
        // if (!user.Verified)
        // {
        //     return (false, "Account pending verification", null);
        // }

        // Generate tokens
        var accessToken = _jwtService.GenerateAccessToken(user);
        var refreshToken = _jwtService.GenerateRefreshToken();

        // Save refresh token
        var refreshTokenEntity = new RefreshToken
        {
            Id = Guid.NewGuid(),
            OrganizationId = user.Id,
            Token = refreshToken,
            ExpiresAt = DateTime.UtcNow.AddDays(
                int.Parse(_configuration["REFRESH_TOKEN_EXPIRY_DAYS"] ?? "7")
            ),
            CreatedAt = DateTime.UtcNow,
            IsRevoked = false
        };

        _context.RefreshTokens.Add(refreshTokenEntity);
        await _context.SaveChangesAsync();

        var response = new AuthResponse
        {
            AccessToken = accessToken,
            RefreshToken = refreshToken,
            TokenType = "Bearer",
            ExpiresIn = int.Parse(_configuration["JWT_EXPIRY_MINUTES"] ?? "60") * 60,
            User = MapToDto(user)
        };

        return (true, "Login successful", response);
    }

    public async Task<(bool Success, string Message, AuthResponse? Response)> RefreshTokenAsync(string refreshToken)
    {
        var storedToken = await _context.RefreshTokens
            .Include(rt => rt.Organization)
            .FirstOrDefaultAsync(rt => rt.Token == refreshToken);

        if (storedToken == null)
        {
            return (false, "Invalid refresh token", null);
        }

        if (storedToken.IsRevoked)
        {
            return (false, "Token has been revoked", null);
        }

        if (storedToken.ExpiresAt < DateTime.UtcNow)
        {
            return (false, "Token has expired", null);
        }

        var user = await _context.Organizations.FindAsync(storedToken.OrganizationId);
        if (user == null)
        {
            return (false, "User not found", null);
        }

        // Generate new tokens
        var newAccessToken = _jwtService.GenerateAccessToken(user);
        var newRefreshToken = _jwtService.GenerateRefreshToken();

        // Revoke old refresh token
        storedToken.IsRevoked = true;
        storedToken.RevokedAt = DateTime.UtcNow;

        // Save new refresh token
        var newRefreshTokenEntity = new RefreshToken
        {
            Id = Guid.NewGuid(),
            OrganizationId = user.Id,
            Token = newRefreshToken,
            ExpiresAt = DateTime.UtcNow.AddDays(
                int.Parse(_configuration["REFRESH_TOKEN_EXPIRY_DAYS"] ?? "7")
            ),
            CreatedAt = DateTime.UtcNow,
            IsRevoked = false
        };

        _context.RefreshTokens.Add(newRefreshTokenEntity);
        await _context.SaveChangesAsync();

        var response = new AuthResponse
        {
            AccessToken = newAccessToken,
            RefreshToken = newRefreshToken,
            TokenType = "Bearer",
            ExpiresIn = int.Parse(_configuration["JWT_EXPIRY_MINUTES"] ?? "60") * 60,
            User = MapToDto(user)
        };

        return (true, "Token refreshed successfully", response);
    }

    public async Task<bool> RevokeRefreshTokenAsync(string refreshToken)
    {
        var storedToken = await _context.RefreshTokens
            .FirstOrDefaultAsync(rt => rt.Token == refreshToken);

        if (storedToken == null || storedToken.IsRevoked)
        {
            return false;
        }

        storedToken.IsRevoked = true;
        storedToken.RevokedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<bool> ChangePasswordAsync(Guid userId, ChangePasswordRequest request)
    {
        var user = await _context.Organizations.FindAsync(userId);
        if (user == null || user.PasswordHash == null)
        {
            return false;
        }

        // Verify current password
        if (!BCrypt.Net.BCrypt.Verify(request.CurrentPassword, user.PasswordHash))
        {
            return false;
        }

        // Hash new password
        user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);
        user.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return true;
    }

    private static OrganizationDto MapToDto(Organization org)
    {
        return new OrganizationDto
        {
            Id = org.Id,
            Name = org.Name,
            Type = org.Type,
            Email = org.Email,
            Phone = org.Phone,
            Address = org.Address,
            Location = org.Location,
            Verified = org.Verified,
            VerificationStatus = org.VerificationStatus,
            Hours = org.Hours,
            ProfilePictureUrl = org.ProfilePictureUrl,
            CreatedAt = org.CreatedAt.ToString("o")
        };
    }
}