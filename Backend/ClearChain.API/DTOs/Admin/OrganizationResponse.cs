namespace ClearChain.API.DTOs.Admin;

public class OrganizationResponse
{
    public string Message { get; set; } = string.Empty;
    public OrganizationData Data { get; set; } = new();
}