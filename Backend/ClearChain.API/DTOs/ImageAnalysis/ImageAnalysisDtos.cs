namespace ClearChain.API.DTOs.ImageAnalysis;

/// <summary>Response of the analyze endpoint: auto-fill data for a CreateListingRequest.</summary>
public class AnalyzeImageResponse
{
    public bool Success { get; set; }
    public string Message { get; set; } = string.Empty;
    public FoodAnalysisData? Data { get; set; }
}

/// <summary>What the model read off the photo, field by field, for the create-listing form.</summary>
public class FoodAnalysisData
{
    /// <summary>Product name; fills Title.</summary>
    public string Title { get; set; } = string.Empty;

    /// <summary>
    /// Store-style product copy (what it is, variety, pack size, typical use); fills Description.
    /// Example: "Sliced whole wheat sandwich bread, 500 g loaf. Soft and hearty, ideal for toast and sandwiches."
    /// </summary>
    public string Description { get; set; } = string.Empty;

    /// <summary>
    /// Food category; fills Category. One of
    /// FRUITS|VEGETABLES|DAIRY|BAKERY|MEAT|SEAFOOD|PACKAGED|BEVERAGES|OTHER.
    /// </summary>
    public string Category { get; set; } = string.Empty;

    /// <summary>Expiry date read off the label or estimated, as yyyy-MM-dd; fills ExpiryDate.</summary>
    public string ExpiryDate { get; set; } = string.Empty;

    /// <summary>
    /// Visible condition and storage advice; fills Notes.
    /// Example: "Fresh loaf with an intact crust. Store in an airtight bag at room temperature."
    /// </summary>
    public string Notes { get; set; } = string.Empty;

    /// <summary>The model's confidence in its reading, 0.0-1.0.</summary>
    public double Confidence { get; set; }

    /// <summary>Visible freshness, 0-100.</summary>
    public double FreshnessScore { get; set; }

    /// <summary>Quality grade: A, B, C or D.</summary>
    public string QualityGrade { get; set; } = string.Empty;
}

public class UploadImageResponse
{
    public bool Success { get; set; }
    public string Message { get; set; } = string.Empty;
    public string? ImageUrl { get; set; }
}
