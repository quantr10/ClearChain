using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using ClearChain.API.DTOs.Organizations;
using ClearChain.API.DTOs.Auth;

namespace ClearChain.API.Services;

public interface IOrganizationService
{
    Task<List<OrganizationDto>> GetPendingVerificationsAsync(string? type = null);
    Task<List<OrganizationDto>> GetVerifiedOrganizationsAsync(string? type = null);
    Task<(bool Success, string Message)> VerifyOrganizationAsync(Guid organizationId, string action, string? notes = null);
    Task<OrganizationDto?> GetOrganizationByIdAsync(Guid id);
    Task<(bool Success, string Message)> UpdateProfileAsync(Guid userId, UpdateProfileRequest request);
}

public class OrganizationService : IOrganizationService
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<OrganizationService> _logger;
    private readonly IPushNotificationService _pushNotificationService;
    private readonly IEmailService _emailService;

    public OrganizationService(
        ApplicationDbContext context,
        ILogger<OrganizationService> logger,
        IPushNotificationService pushNotificationService,
        IEmailService emailService)
    {
        _context = context;
        _logger = logger;
        _pushNotificationService = pushNotificationService;
        _emailService = emailService;
    }

    public async Task<List<OrganizationDto>> GetPendingVerificationsAsync(string? type = null)
    {
        var query = _context.Organizations
            .Where(o => !o.Verified && o.VerificationStatus == "pending");

        if (!string.IsNullOrEmpty(type))
            query = query.Where(o => o.Type == type.ToLower());

        var organizations = await query.OrderBy(o => o.CreatedAt).ToListAsync();
        return organizations.Select(MapToDto).ToList();
    }

    public async Task<List<OrganizationDto>> GetVerifiedOrganizationsAsync(string? type = null)
    {
        var query = _context.Organizations
            .Where(o => o.Verified && o.VerificationStatus == "approved");

        if (!string.IsNullOrEmpty(type))
            query = query.Where(o => o.Type == type.ToLower());

        var organizations = await query.OrderBy(o => o.Name).ToListAsync();
        return organizations.Select(MapToDto).ToList();
    }

    public async Task<(bool Success, string Message)> VerifyOrganizationAsync(
        Guid organizationId, string action, string? notes = null)
    {
        var organization = await _context.Organizations.FindAsync(organizationId);
        if (organization == null)
            return (false, "Organization not found");
        if (organization.Verified)
            return (false, "Organization already verified");

        if (action.ToLower() == "approved")
        {
            organization.Verified = true;
            organization.VerificationStatus = "approved";
        }
        else if (action.ToLower() == "rejected")
        {
            organization.Verified = false;
            organization.VerificationStatus = "rejected";
        }
        else
            return (false, "Invalid action. Must be 'approved' or 'rejected'");

        organization.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();
        return (true, $"Organization {action} successfully");
    }

    public async Task<OrganizationDto?> GetOrganizationByIdAsync(Guid id)
    {
        var organization = await _context.Organizations.FindAsync(id);
        return organization == null ? null : MapToDto(organization);
    }

    // ── handles new fields ───────────────────────────────────────────────────
    public async Task<(bool Success, string Message)> UpdateProfileAsync(
        Guid userId, UpdateProfileRequest request)
    {
        var user = await _context.Organizations.FindAsync(userId);
        if (user == null)
            return (false, "User not found");

        var (emailOk, emailError, emailChanged) = await TryApplyEmailChangeAsync(user, request.Email);
        if (!emailOk)
            return (false, emailError);

        // Update only provided fields (existing)
        if (!string.IsNullOrEmpty(request.Name)) user.Name = request.Name;
        if (!string.IsNullOrEmpty(request.Phone)) user.Phone = request.Phone;
        if (!string.IsNullOrEmpty(request.Address)) user.Address = request.Address;
        if (!string.IsNullOrEmpty(request.Location)) user.Location = request.Location;
        if (request.State != null) user.State = request.State;
        if (request.ZipCode != null) user.ZipCode = request.ZipCode;
        if (request.Hours != null) user.Hours = request.Hours;

        if (request.Latitude.HasValue) user.Latitude = request.Latitude;
        if (request.Longitude.HasValue) user.Longitude = request.Longitude;
        if (request.ContactPerson != null) user.ContactPerson = request.ContactPerson;
        if (request.PickupInstructions != null) user.PickupInstructions = request.PickupInstructions;
        if (request.Description != null) user.Description = request.Description;

        var resubmitted = ResubmitIfRejected(user);

        user.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        if (resubmitted)
            await NotifyResubmissionAsync(user);

        if (emailChanged)
        {
            var emailSent = await SendEmailVerificationAsync(user);
            return (true, emailSent
                ? "Profile updated. Check your new email for a verification code — you will need it to sign in again."
                : "Profile updated, but we couldn't send the verification code to your new email. Use \"resend\" to try again.");
        }

        return (true, resubmitted ? "Profile updated and resubmitted for review" : "Profile updated successfully");
    }

    /// <summary>
    /// Applies a requested email change to <paramref name="user"/> without saving.
    /// </summary>
    /// <remarks>
    /// The address is the login identifier and carries a unique index, so a collision
    /// is rejected here rather than left to surface as a database error. Clearing
    /// EmailVerified is what makes the change safe: an address nobody has confirmed
    /// must not keep the access the old one had, which is the same rule registration
    /// applies.
    /// </remarks>
    private async Task<(bool Success, string Error, bool Changed)> TryApplyEmailChangeAsync(
        Organization user, string? requestedEmail)
    {
        if (string.IsNullOrWhiteSpace(requestedEmail))
            return (true, string.Empty, false);

        var email = requestedEmail.Trim().ToLower();
        if (email == user.Email.ToLower())
            return (true, string.Empty, false);

        var taken = await _context.Organizations
            .AnyAsync(o => o.Id != user.Id && o.Email.ToLower() == email && !o.IsDeleted);
        if (taken)
            return (false, "That email address is already in use", false);

        user.Email = email;
        user.EmailVerified = false;
        return (true, string.Empty, true);
    }

    /// <summary>
    /// Issues a fresh verification code for the user's current address, matching the
    /// code shape and 15-minute lifetime used at registration.
    /// </summary>
    private async Task<bool> SendEmailVerificationAsync(Organization user)
    {
        var code = VerificationCodeGenerator.Generate();
        user.EmailVerificationToken = BCrypt.Net.BCrypt.HashPassword(code);
        user.EmailVerificationTokenExpiry = DateTime.UtcNow.AddMinutes(15);
        user.EmailVerificationAttempts = 0;
        await _context.SaveChangesAsync();

        try
        {
            await _emailService.SendVerificationEmailAsync(user.Email, user.Name, code);
            return true;
        }
        catch (Exception ex)
        {
            // The token/expiry above are already saved, so the user can still verify via
            // "resend" — a failed send here must not surface as a 500 on profile update.
            _logger.LogError(ex, "Failed to send verification email to {Email}", user.Email);
            return false;
        }
    }

    /// <summary>
    /// A rejected org that edits its own profile or re-uploads a document is treated as
    /// having addressed the feedback — flip it back to "pending" so it reappears in the
    /// admin's review queue instead of staying stuck as rejected forever.
    /// Does not save changes; caller is expected to call SaveChangesAsync.
    /// </summary>
    public static bool ResubmitIfRejected(Organization org)
    {
        if (org.VerificationStatus != "rejected")
            return false;

        org.VerificationStatus = "pending";
        org.VerificationNotes = null;
        return true;
    }

    private async Task NotifyResubmissionAsync(Organization org)
    {
        try
        {
            await _pushNotificationService.SendResubmissionAlertToAdmins(new DTOs.Admin.OrganizationData
            {
                Id = org.Id.ToString(),
                Name = org.Name,
                Email = org.Email,
                Type = org.Type,
                Phone = org.Phone ?? "",
                Address = org.Address ?? "",
                Location = org.Location ?? "",
                Verified = org.Verified,
                VerificationStatus = org.VerificationStatus,
                CreatedAt = org.CreatedAt.ToString("o")
            });
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to send resubmission alert for {OrgId}", org.Id);
        }
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
