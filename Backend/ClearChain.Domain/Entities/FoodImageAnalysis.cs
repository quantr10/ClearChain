namespace ClearChain.Domain.Entities;

/// <summary>
/// Stores AI analysis results for food images
/// Tracks all analyzed images and their AI-detected properties
/// </summary>
public class FoodImageAnalysis
{
    public Guid Id { get; set; }
    
    /// <summary>
    /// Grocery that uploaded and analyzed the image
    /// </summary>
    public Guid GroceryId { get; set; }
    
    /// <summary>
    /// URL of the analyzed image in Supabase storage
    /// </summary>
    public string ImageUrl { get; set; } = string.Empty;
    
    /// <summary>
    /// AI-detected product name
    /// </summary>
    public string DetectedName { get; set; } = string.Empty;
    
    /// <summary>
    /// AI-detected food category
    /// </summary>
    public string DetectedCategory { get; set; } = string.Empty;
    
    /// <summary>
    /// AI-estimated expiry date
    /// </summary>
    public DateTime? EstimatedExpiryDate { get; set; }
    
    /// <summary>
    /// AI-generated condition notes
    /// </summary>
    public string Notes { get; set; } = string.Empty;
    
    /// <summary>
    /// Category detection confidence (0.0 - 1.0)
    /// </summary>
    public double Confidence { get; set; }
    
    /// <summary>
    /// Freshness score (0-100)
    /// </summary>
    public double FreshnessScore { get; set; }
    
    /// <summary>
    /// Quality grade (A/B/C/D)
    /// </summary>
    public string QualityGrade { get; set; } = string.Empty;
    
    /// <summary>
    /// JSON array of detected items with confidence scores
    /// </summary>
    public string DetectedItems { get; set; } = "[]";
    
    /// <summary>
    /// When the AI analysis was performed
    /// </summary>
    public DateTime AnalyzedAt { get; set; }
    
    /// <summary>
    /// When the record was created
    /// </summary>
    public DateTime CreatedAt { get; set; }
    
    // Navigation property
    public Organization? Grocery { get; set; }
}