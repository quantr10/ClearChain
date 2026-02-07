using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Organizations;

public class VerifyOrganizationRequest
{
    [Required]
    public Guid OrganizationId { get; set; }

    [Required]
    [RegularExpression("^(approved|rejected)$", ErrorMessage = "Action must be 'approved' or 'rejected'")]
    public string Action { get; set; } = string.Empty;

    public string? Notes { get; set; }
}