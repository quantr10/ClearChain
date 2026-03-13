namespace ClearChain.API.DTOs.ImageAnalysis;

/// <summary>
/// Response for analyze food image endpoint
/// Contains auto-fill data for CreateListingRequest
/// </summary>
public class AnalyzeImageResponse
{
    public bool Success { get; set; }
    public string Message { get; set; } = string.Empty;
    public FoodAnalysisData? Data { get; set; }
}

/// <summary>
/// AI-analyzed food data ready to auto-fill CreateListingRequest
/// </summary>
public class FoodAnalysisData
{
    /// <summary>
    /// Product name detected by AI (for Title field)
    /// </summary>
    public string Title { get; set; } = string.Empty;

    /// <summary>
    /// Food category detected by AI (for Category field)
    /// Must match: FRUITS|VEGETABLES|DAIRY|BAKERY|MEAT|SEAFOOD|CANNED_GOODS|BEVERAGES|FROZEN_FOODS|GRAINS
    /// </summary>
    public string Category { get; set; } = string.Empty;

    /// <summary>
    /// AI-estimated expiry date (for ExpiryDate field)
    /// Format: yyyy-MM-dd
    /// </summary>
    public string ExpiryDate { get; set; } = string.Empty;

    /// <summary>
    /// Food condition notes from AI analysis (for Description field)
    /// Example: "Fresh bread, golden brown crust, good condition"
    /// </summary>
    public string Notes { get; set; } = string.Empty;

    /// <summary>
    /// Uploaded image URL in Supabase (for ImageUrl field)
    /// </summary>
    public string ImageUrl { get; set; } = string.Empty;

    /// <summary>
    /// AI confidence score (0.0 - 1.0)
    /// </summary>
    public double Confidence { get; set; }

    /// <summary>
    /// Quality score (0-100)
    /// </summary>
    public double FreshnessScore { get; set; }

    /// <summary>
    /// Quality grade (A/B/C/D)
    /// </summary>
    public string QualityGrade { get; set; } = string.Empty;

    /// <summary>
    /// All items detected in image
    /// </summary>
    public List<DetectedItem> DetectedItems { get; set; } = new();

    /// <summary>
    /// Timestamp of analysis
    /// </summary>
    public DateTime AnalyzedAt { get; set; }
}

/// <summary>
/// Individual detected item in image
/// </summary>
public class DetectedItem
{
    public string Name { get; set; } = string.Empty;
    public double Confidence { get; set; }
}

/// <summary>
/// Response for analysis history
/// </summary>
public class AnalysisHistoryResponse
{
    public bool Success { get; set; }
    public string Message { get; set; } = string.Empty;
    public int Count { get; set; }
    public List<FoodAnalysisData> Analyses { get; set; } = new();
}

public class UploadImageResponse
{
    public bool Success { get; set; }
    public string Message { get; set; } = string.Empty;
    public string? ImageUrl { get; set; }
}

