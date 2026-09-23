using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using ClearChain.API.DTOs.Auth;
using BCrypt.Net;

namespace ClearChain.API.Services;

public interface IAuthService
{
    Task<(bool Success, string Message, Organization? NewOrg)> RegisterAsync(RegisterRequest request);
    Task<(bool Success, string Message, AuthResponse? Response)> LoginAsync(LoginRequest request);
    Task<(bool Success, string Message, AuthResponse? Response)> RefreshTokenAsync(string refreshToken);
    Task<bool> RevokeRefreshTokenAsync(string refreshToken, string? fcmToken = null);
    Task<bool> ChangePasswordAsync(Guid userId, ChangePasswordRequest request);
    Task<(bool Available, string Message)> CheckEmailAvailableAsync(string email);
    Task<(bool Success, string Message)> DeleteAccountAsync(Guid userId, string password);
    Task<(bool Success, string Message, AuthResponse? Response)> VerifyEmailAsync(string email, string code);
    Task<(bool Success, string Message)> ResendVerificationAsync(string email);
}

public class AuthService : IAuthService
{
    private const int MaxEmailVerificationAttempts = 5;

    private readonly ApplicationDbContext _context;
    private readonly IJwtService _jwtService;
    private readonly IEmailService _emailService;
    private readonly IPushNotificationService _pushNotificationService;
    private readonly ILogger<AuthService> _logger;
    private readonly int _refreshTokenExpiryDays;
    private readonly int _jwtExpirySeconds;

    public AuthService(
        ApplicationDbContext context,
        IJwtService jwtService,
        IEmailService emailService,
        IPushNotificationService pushNotificationService,
        ILogger<AuthService> logger,
        IConfiguration configuration)
    {
        _context = context;
        _jwtService = jwtService;
        _emailService = emailService;
        _pushNotificationService = pushNotificationService;
        _logger = logger;
        _refreshTokenExpiryDays = int.Parse(configuration["REFRESH_TOKEN_EXPIRY_DAYS"] ?? "7");
        _jwtExpirySeconds = int.Parse(configuration["JWT_EXPIRY_MINUTES"] ?? "60") * 60;
    }

    /// <summary>
    /// Sends the verification email without letting a transient SMTP failure surface as an
    /// unhandled 500 — the account row is already committed by the time this runs, so a
    /// failed send just means the user needs to use "resend", not that registration failed.
    /// </summary>
    private async Task<bool> TrySendVerificationEmailAsync(string email, string name, string code)
    {
        try
        {
            await _emailService.SendVerificationEmailAsync(email, name, code);
            return true;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send verification email to {Email}", email);
            return false;
        }
    }

    public async Task<(bool Success, string Message, Organization? NewOrg)> RegisterAsync(RegisterRequest request)
    {
        if (await _context.Organizations.AnyAsync(o => o.Email.ToLower() == request.Email.ToLower()))
            return (false, "Email already registered", null);

        // Public self-registration only ever creates grocery/NGO accounts pending
        // verification. Checked again here (not just via the DTO's [RegularExpression])
        // so this invariant holds even for a caller that builds the request directly —
        // admin accounts must never be reachable through this path.
        var type = request.Type.ToLower();
        if (type != "grocery" && type != "ngo")
            return (false, "Type must be 'grocery' or 'ngo'", null);

        var verificationCode = VerificationCodeGenerator.Generate();

        var organization = new Organization
        {
            Id = Guid.NewGuid(),
            Name = request.Name,
            Type = type,
            Email = request.Email.ToLower(),
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password),
            AuthProvider = "local",
            EmailVerified = false,
            EmailVerificationToken = BCrypt.Net.BCrypt.HashPassword(verificationCode),
            EmailVerificationTokenExpiry = DateTime.UtcNow.AddMinutes(15),
            Verified = false,
            VerificationStatus = "pending",
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

        var emailSent = await TrySendVerificationEmailAsync(organization.Email, organization.Name, verificationCode);

        return (true, emailSent
            ? "Verification email sent. Please check your inbox."
            : "Account created, but we couldn't send the verification email. Use \"resend\" to try again.",
            organization);
    }

    public async Task<(bool Success, string Message, AuthResponse? Response)> VerifyEmailAsync(string email, string code)
    {
        var user = await _context.Organizations
            .FirstOrDefaultAsync(o => o.Email.ToLower() == email.ToLower() && !o.IsDeleted);

        if (user == null)
            return (false, "Account not found", null);

        if (user.EmailVerified)
            return (false, "Email already verified. Please log in.", null);

        if (user.EmailVerificationToken == null || user.EmailVerificationTokenExpiry == null)
            return (false, "No verification pending. Request a new code.", null);

        if (user.EmailVerificationTokenExpiry < DateTime.UtcNow)
            return (false, "Verification code has expired. Please request a new one.", null);

        if (!BCrypt.Net.BCrypt.Verify(code, user.EmailVerificationToken))
        {
            user.EmailVerificationAttempts++;
            if (user.EmailVerificationAttempts >= MaxEmailVerificationAttempts)
            {
                // Too many wrong guesses — invalidate the code outright instead of leaving
                // it brute-forceable for the rest of its 15-minute window.
                user.EmailVerificationToken = null;
                user.EmailVerificationTokenExpiry = null;
                user.EmailVerificationAttempts = 0;
                user.UpdatedAt = DateTime.UtcNow;
                await _context.SaveChangesAsync();
                return (false, "Too many incorrect attempts. Request a new code.", null);
            }

            user.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
            return (false, "Invalid verification code.", null);
        }

        user.EmailVerified = true;
        user.EmailVerificationToken = null;
        user.EmailVerificationTokenExpiry = null;
        user.EmailVerificationAttempts = 0;
        user.UpdatedAt = DateTime.UtcNow;

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

        return (true, "Email verified successfully!", new AuthResponse
        {
            AccessToken = accessToken,
            RefreshToken = refreshToken,
            TokenType = "Bearer",
            ExpiresIn = _jwtExpirySeconds,
            User = MapToDto(user)
        });
    }

    public async Task<(bool Success, string Message)> ResendVerificationAsync(string email)
    {
        var user = await _context.Organizations
            .FirstOrDefaultAsync(o => o.Email.ToLower() == email.ToLower() && !o.IsDeleted);

        if (user == null) return (false, "Account not found");
        if (user.EmailVerified) return (false, "Email already verified");

        var newCode = VerificationCodeGenerator.Generate();
        user.EmailVerificationToken = BCrypt.Net.BCrypt.HashPassword(newCode);
        user.EmailVerificationTokenExpiry = DateTime.UtcNow.AddMinutes(15);
        user.EmailVerificationAttempts = 0;
        user.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        var emailSent = await TrySendVerificationEmailAsync(user.Email, user.Name, newCode);
        return emailSent
            ? (true, "Verification email resent")
            : (false, "Couldn't send the verification email. Please try again shortly.");
    }

    public async Task<(bool Success, string Message, AuthResponse? Response)> LoginAsync(LoginRequest request)
    {
        var user = await _context.Organizations
            .FirstOrDefaultAsync(o => o.Email.ToLower() == request.Email.ToLower());

        if (user == null || user.PasswordHash == null)
            return (false, "Invalid email or password", null);

        if (user.IsDeleted)
            return (false, "This account has been deleted", null);

        // Check lockout
        if (user.LockoutUntil.HasValue && user.LockoutUntil.Value > DateTime.UtcNow)
        {
            var remaining = (int)(user.LockoutUntil.Value - DateTime.UtcNow).TotalMinutes + 1;
            return (false, $"Account locked. Try again in {remaining} minute(s).", null);
        }

        if (!BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash))
        {
            // Track failed attempts
            user.FailedLoginCount++;
            if (user.FailedLoginCount >= 5)
            {
                user.LockoutUntil = DateTime.UtcNow.AddMinutes(15);
                user.FailedLoginCount = 0;
            }
            user.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
            return (false, "Invalid email or password", null);
        }

        // Reset failed count on success
        user.FailedLoginCount = 0;
        user.LockoutUntil = null;
        user.UpdatedAt = DateTime.UtcNow;

        if (!user.EmailVerified)
            return (false, "Please verify your email address before logging in.", null);

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

    public async Task<bool> RevokeRefreshTokenAsync(string refreshToken, string? fcmToken = null)
    {
        var storedToken = await _context.RefreshTokens.FirstOrDefaultAsync(rt => rt.Token == refreshToken);
        if (storedToken == null || storedToken.IsRevoked) return false;

        storedToken.IsRevoked = true;
        storedToken.RevokedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        if (!string.IsNullOrEmpty(fcmToken))
            await _pushNotificationService.RemoveTokenAsync(storedToken.OrganizationId, fcmToken);

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

    public async Task<(bool Available, string Message)> CheckEmailAvailableAsync(string email)
    {
        var exists = await _context.Organizations
            .AnyAsync(o => o.Email.ToLower() == email.ToLower() && !o.IsDeleted);
        return exists
            ? (false, "Email is already registered")
            : (true, "Email is available");
    }

    public async Task<(bool Success, string Message)> DeleteAccountAsync(Guid userId, string password)
    {
        var user = await _context.Organizations.FindAsync(userId);
        if (user == null) return (false, "User not found");
        if (user.PasswordHash == null || !BCrypt.Net.BCrypt.Verify(password, user.PasswordHash))
            return (false, "Incorrect password");

        // Soft-delete: keep data for audit, mark as deleted
        user.IsDeleted = true;
        user.DeletedAt = DateTime.UtcNow;
        user.Email = $"deleted_{userId}@clearchain.deleted"; // free up email slot
        user.UpdatedAt = DateTime.UtcNow;

        // Revoke all refresh tokens
        var tokens = await _context.RefreshTokens
            .Where(t => t.OrganizationId == userId && !t.IsRevoked)
            .ToListAsync();
        foreach (var t in tokens) { t.IsRevoked = true; t.RevokedAt = DateTime.UtcNow; }

        await _context.SaveChangesAsync();
        return (true, "Account deleted successfully");
    }

    // ── includes new fields ──────────────────────────────────────────────────
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
            State = org.State ?? "",
            ZipCode = org.ZipCode ?? "",
            Verified = org.Verified,
            VerificationStatus = org.VerificationStatus,
            VerificationNotes = org.VerificationNotes,
            Hours = org.Hours,
            ProfilePictureUrl = org.ProfilePictureUrl,
            CreatedAt = org.CreatedAt.ToString("o"),
            DocumentUrl = org.DocumentUrl,
            // NEW
            Latitude = org.Latitude,
            Longitude = org.Longitude,
            ContactPerson = org.ContactPerson,
            PickupInstructions = org.PickupInstructions,
            Description = org.Description
        };
    }
}
