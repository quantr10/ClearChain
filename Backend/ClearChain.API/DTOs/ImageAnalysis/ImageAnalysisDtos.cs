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
    /// Store-style product copy: what it is, variety, pack size, typical use (for Description field)
    /// Example: "Sliced whole wheat sandwich bread, 500 g loaf. Soft and hearty, ideal for toast and sandwiches."
    /// </summary>
    public string Description { get; set; } = string.Empty;

    /// <summary>
    /// Food category detected by AI (for Category field)
    /// Must match: FRUITS|VEGETABLES|DAIRY|BAKERY|MEAT|SEAFOOD|PACKAGED|BEVERAGES|OTHER
    /// </summary>
    public string Category { get; set; } = string.Empty;

    /// <summary>
    /// AI-estimated expiry date (for ExpiryDate field)
    /// Format: yyyy-MM-dd
    /// </summary>
    public string ExpiryDate { get; set; } = string.Empty;

    /// <summary>
    /// Visible condition and storage advice (for Notes field)
    /// Example: "Fresh loaf with an intact crust. Store in an airtight bag at room temperature."
    /// </summary>
    public string Notes { get; set; } = string.Empty;

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
}

public class UploadImageResponse
{
    public bool Success { get; set; }
    public string Message { get; set; } = string.Empty;
    public string? ImageUrl { get; set; }
}
