namespace ClearChain.API.DTOs.Admin;

public class AdminStatsOverviewResponse
{
    public string Message { get; set; } = string.Empty;
    public AdminStatsOverviewData Data { get; set; } = new();
}

public class AdminStatsOverviewData
{
    public int TotalOrganizations { get; set; }
    public int TotalGroceries { get; set; }
    public int TotalNgos { get; set; }

    public int ActiveListings { get; set; }
    public int ReservedListings { get; set; }
    public int ExpiredListings { get; set; }

    public int TotalPickupRequests { get; set; }
    public int PendingRequests { get; set; }
    public int ApprovedRequests { get; set; }
    public int ReadyRequests { get; set; }
    public int CompletedRequests { get; set; }
    public int CancelledRequests { get; set; }
}
