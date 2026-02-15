namespace ClearChain.API.DTOs.Admin;

public class OrganizationListResponse
{
    public string Message { get; set; } = string.Empty;
    public List<OrganizationData> Data { get; set; } = new();
}

public class OrganizationData
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Type { get; set; } = string.Empty;
    public string Phone { get; set; } = string.Empty;
    public string Address { get; set; } = string.Empty;
    public string Location { get; set; } = string.Empty;
    public bool Verified { get; set; }
    public string VerificationStatus { get; set; } = string.Empty;
    public string CreatedAt { get; set; } = string.Empty;
}