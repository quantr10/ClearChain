using Microsoft.Azure.CognitiveServices.Vision.ComputerVision;
using Microsoft.Azure.CognitiveServices.Vision.ComputerVision.Models;
using ClearChain.API.DTOs.ImageAnalysis;
using ClearChain.Domain.Entities;
using ClearChain.Infrastructure.Data;
using Microsoft.EntityFrameworkCore;

namespace ClearChain.API.Services;

/// <summary>
/// 🤖 ENHANCED Azure Computer Vision AI service for precise food analysis
/// Multi-factor detection with weighted scoring for accuracy
/// </summary>
public class AzureVisionService : IImageAnalysisService
{
    private readonly ComputerVisionClient _visionClient;
    private readonly IStorageService _storageService;
    private readonly ApplicationDbContext _context;
    private readonly ILogger<AzureVisionService> _logger;

    // ====================================================================
    // 🔍 ENHANCED CATEGORY MAPPING - More Specific
    // ====================================================================
    private static readonly Dictionary<string, (string Category, int Weight)> CategoryKeywords = new()
    {
        // FRUITS - High specificity
        { "apple", ("FRUITS", 100) },
        { "banana", ("FRUITS", 100) },
        { "orange", ("FRUITS", 100) },
        { "grape", ("FRUITS", 100) },
        { "strawberry", ("FRUITS", 100) },
        { "berry", ("FRUITS", 95) },
        { "watermelon", ("FRUITS", 100) },
        { "melon", ("FRUITS", 95) },
        { "pear", ("FRUITS", 100) },
        { "peach", ("FRUITS", 100) },
        { "mango", ("FRUITS", 100) },
        { "kiwi", ("FRUITS", 100) },
        { "pineapple", ("FRUITS", 100) },
        { "lemon", ("FRUITS", 100) },
        { "lime", ("FRUITS", 100) },
        { "citrus", ("FRUITS", 90) },
        { "fruit", ("FRUITS", 70) },  // General term - lower weight
        
        // VEGETABLES - High specificity
        { "carrot", ("VEGETABLES", 100) },
        { "broccoli", ("VEGETABLES", 100) },
        { "lettuce", ("VEGETABLES", 100) },
        { "tomato", ("VEGETABLES", 100) },
        { "potato", ("VEGETABLES", 100) },
        { "onion", ("VEGETABLES", 100) },
        { "cabbage", ("VEGETABLES", 100) },
        { "spinach", ("VEGETABLES", 100) },
        { "cucumber", ("VEGETABLES", 100) },
        { "pepper", ("VEGETABLES", 95) },
        { "bell pepper", ("VEGETABLES", 100) },
        { "zucchini", ("VEGETABLES", 100) },
        { "eggplant", ("VEGETABLES", 100) },
        { "cauliflower", ("VEGETABLES", 100) },
        { "celery", ("VEGETABLES", 100) },
        { "radish", ("VEGETABLES", 100) },
        { "vegetable", ("VEGETABLES", 70) },
        
        // DAIRY - High specificity
        { "milk", ("DAIRY", 100) },
        { "cheese", ("DAIRY", 100) },
        { "yogurt", ("DAIRY", 100) },
        { "butter", ("DAIRY", 100) },
        { "cream", ("DAIRY", 100) },
        { "ice cream", ("DAIRY", 95) },
        { "dairy", ("DAIRY", 80) },
        
        // BAKERY - High specificity
        { "bread", ("BAKERY", 100) },
        { "baguette", ("BAKERY", 100) },
        { "bagel", ("BAKERY", 100) },
        { "croissant", ("BAKERY", 100) },
        { "bun", ("BAKERY", 95) },
        { "roll", ("BAKERY", 95) },
        { "loaf", ("BAKERY", 100) },
        { "pastry", ("BAKERY", 95) },
        { "cake", ("BAKERY", 90) },
        { "cookie", ("BAKERY", 90) },
        { "muffin", ("BAKERY", 95) },
        { "donut", ("BAKERY", 95) },
        { "bakery", ("BAKERY", 75) },
        
        // MEAT - High specificity
        { "beef", ("MEAT", 100) },
        { "pork", ("MEAT", 100) },
        { "chicken", ("MEAT", 100) },
        { "turkey", ("MEAT", 100) },
        { "lamb", ("MEAT", 100) },
        { "steak", ("MEAT", 100) },
        { "sausage", ("MEAT", 95) },
        { "bacon", ("MEAT", 95) },
        { "ham", ("MEAT", 95) },
        { "poultry", ("MEAT", 90) },
        { "meat", ("MEAT", 80) },
        
        // SEAFOOD - High specificity
        { "fish", ("SEAFOOD", 100) },
        { "salmon", ("SEAFOOD", 100) },
        { "tuna", ("SEAFOOD", 100) },
        { "shrimp", ("SEAFOOD", 100) },
        { "crab", ("SEAFOOD", 100) },
        { "lobster", ("SEAFOOD", 100) },
        { "oyster", ("SEAFOOD", 100) },
        { "mussel", ("SEAFOOD", 100) },
        { "seafood", ("SEAFOOD", 85) },
        
        // PACKAGED (previously CANNED_GOODS)
        { "can", ("PACKAGED", 95) },
        { "canned", ("PACKAGED", 100) },
        { "jar", ("PACKAGED", 90) },
        { "package", ("PACKAGED", 85) },
        { "box", ("PACKAGED", 80) },
        
        // BEVERAGES - High specificity
        { "juice", ("BEVERAGES", 100) },
        { "soda", ("BEVERAGES", 100) },
        { "water", ("BEVERAGES", 100) },
        { "coffee", ("BEVERAGES", 100) },
        { "tea", ("BEVERAGES", 100) },
        { "beer", ("BEVERAGES", 100) },
        { "wine", ("BEVERAGES", 100) },
        { "bottle", ("BEVERAGES", 85) },
        { "drink", ("BEVERAGES", 80) },
        { "beverage", ("BEVERAGES", 75) }
    };

    // Expiry estimates with more granular categories
    private static readonly Dictionary<string, int> ExpiryDaysMapping = new()
    {
        { "FRUITS", 5 },
        { "VEGETABLES", 7 },
        { "DAIRY", 7 },
        { "BAKERY", 3 },
        { "MEAT", 2 },
        { "SEAFOOD", 1 },
        { "PACKAGED", 180 },  // Renamed from CANNED_GOODS
        { "BEVERAGES", 30 },
        { "OTHER", 7 }
    };

    public AzureVisionService(
        IConfiguration config,
        IStorageService storageService,
        ApplicationDbContext context,
        ILogger<AzureVisionService> logger)
    {
        var endpoint = config["AZURE_VISION_ENDPOINT"]
            ?? throw new InvalidOperationException("AZURE_VISION_ENDPOINT not configured");
        var key = config["AZURE_VISION_KEY"]
            ?? throw new InvalidOperationException("AZURE_VISION_KEY not configured");

        _visionClient = new ComputerVisionClient(new ApiKeyServiceClientCredentials(key))
        {
            Endpoint = endpoint
        };

        _storageService = storageService;
        _context = context;
        _logger = logger;
    }

public async Task<FoodAnalysisData> AnalyzeFoodImageAsync(IFormFile image, Guid groceryId)
{
    try
    {
        _logger.LogInformation($"🔍 Starting ENHANCED AI analysis for grocery {groceryId}");

        // ❌ REMOVED: Upload to Supabase here
        // var imageUrl = await _storageService.UploadFoodImageAsync(stream, image.FileName, groceryId);
        
        // ✅ NEW: Analyze directly from uploaded stream
        using var stream = image.OpenReadStream();
        
        var features = new List<VisualFeatureTypes?>
        {
            VisualFeatureTypes.Tags,
            VisualFeatureTypes.Description,
            VisualFeatureTypes.Objects,
            VisualFeatureTypes.Color,
            VisualFeatureTypes.ImageType
        };

        var analysis = await _visionClient.AnalyzeImageInStreamAsync(stream, features);
        _logger.LogInformation($"✅ Azure AI complete - {analysis.Tags.Count} tags, {analysis.Objects.Count} objects");

        // ✅ NEW: Process without imageUrl
        var result = ProcessAzureResultsEnhanced(analysis, string.Empty, groceryId);

        _logger.LogInformation($"✅ ENHANCED Analysis: {result.Title} ({result.Category}) - {result.QualityGrade} grade, {result.FreshnessScore}% fresh");

        return result;
    }
    catch (ComputerVisionErrorResponseException ex)
    {
        _logger.LogError(ex, $"❌ Azure API error: {ex.Response?.Content}");
        throw new InvalidOperationException($"AI analysis failed: {ex.Response?.Content ?? ex.Message}", ex);
    }
    catch (Exception ex)
    {
        _logger.LogError(ex, "❌ Analysis error");
        throw;
    }
}
// ====================================================================
// 💾 NEW: SAVE ANALYSIS (Called only when listing created)
// ====================================================================
public async Task SaveAnalysisAsync(FoodAnalysisData data, Guid groceryId)
{
    try
    {
        _logger.LogInformation($"💾 Saving analysis to DB: {data.Title} for grocery {groceryId}");
        
        await SaveAnalysisToDatabase(data, groceryId);
        
        _logger.LogInformation($"✅ Analysis saved successfully");
    }
    catch (Exception ex)
    {
        // Don't fail listing creation if analysis save fails
        _logger.LogError(ex, "⚠️ Failed to save analysis (non-critical)");
    }
}

// ====================================================================
// Keep private method as-is
// ====================================================================
private async Task SaveAnalysisToDatabase(FoodAnalysisData data, Guid groceryId)
{
    DateTime? expiryDateUtc = null;
    if (!string.IsNullOrEmpty(data.ExpiryDate))
    {
        var parsedDate = DateTime.Parse(data.ExpiryDate);
        expiryDateUtc = DateTime.SpecifyKind(parsedDate, DateTimeKind.Utc);
    }

    var entity = new FoodImageAnalysis
    {
        Id = Guid.NewGuid(),
        GroceryId = groceryId,
        ImageUrl = data.ImageUrl,
        DetectedName = data.Title,
        DetectedCategory = data.Category,
        EstimatedExpiryDate = expiryDateUtc,
        Notes = data.Notes,
        Confidence = data.Confidence,
        FreshnessScore = data.FreshnessScore,
        QualityGrade = data.QualityGrade,
        DetectedItems = System.Text.Json.JsonSerializer.Serialize(data.DetectedItems),
        AnalyzedAt = DateTime.SpecifyKind(data.AnalyzedAt, DateTimeKind.Utc),
        CreatedAt = DateTime.UtcNow
    };

    _context.FoodImageAnalyses.Add(entity);
    await _context.SaveChangesAsync();
}

// ... rest of code unchanged ...
    // ====================================================================
    // 🚀 ENHANCED PROCESSING - Multi-factor Analysis
    // ====================================================================
    private FoodAnalysisData ProcessAzureResultsEnhanced(ImageAnalysis analysis, string imageUrl, Guid groceryId)
    {
        // Extract high-confidence tags
        var tags = analysis.Tags
            .Where(t => t.Confidence > 0.5)
            .OrderByDescending(t => t.Confidence)
            .ToList();

        // Get description for context
        var description = analysis.Description?.Captions?.FirstOrDefault()?.Text ?? "";

        // Get objects for specific detection
        // ✅ ĐÚNG - Convert IList → List
        var objects = analysis.Objects?.ToList() ?? new List<DetectedObject>();
        _logger.LogInformation($"📊 Tags: {tags.Count}, Objects: {objects.Count}, Description: '{description}'");

        // 🎯 STEP 1: Multi-factor category detection (weighted scoring)
        var (category, categoryConfidence) = DetectCategoryEnhanced(tags, objects, description);

        // 🎯 STEP 2: Generate specific product name
        var productName = GenerateProductNameEnhanced(tags, objects, description, category);

        // 🎯 STEP 3: Calculate freshness with visual analysis
        var freshnessScore = CalculateFreshnessScoreEnhanced(analysis, tags, objects);

        // 🎯 STEP 4: Quality grade
        var qualityGrade = GetQualityGrade(freshnessScore);

        // 🎯 STEP 5: Smart expiry estimation
        var expiryDays = EstimateExpiryDaysEnhanced(category, freshnessScore, tags);
        var expiryDate = DateTime.UtcNow.AddDays(expiryDays);

        // 🎯 STEP 6: Detailed condition notes with storage tips
        var notes = GenerateDetailedNotes(tags, objects, description, freshnessScore, qualityGrade, category);

        // 🎯 STEP 7: Top detected items
        var detectedItems = BuildDetectedItemsList(tags, objects);

        return new FoodAnalysisData
        {
            Title = productName,
            Category = category,
            ExpiryDate = expiryDate.ToString("yyyy-MM-dd"),
            Notes = notes,
            ImageUrl = imageUrl,
            Confidence = categoryConfidence,
            FreshnessScore = freshnessScore,
            QualityGrade = qualityGrade,
            DetectedItems = detectedItems,
            AnalyzedAt = DateTime.UtcNow
        };
    }

    // ====================================================================
    // 🎯 ENHANCED CATEGORY DETECTION - Weighted Scoring System
    // ====================================================================
    private (string category, double confidence) DetectCategoryEnhanced(
        List<ImageTag> tags,
        List<DetectedObject> objects,
        string description)
    {
        var categoryScores = new Dictionary<string, double>();

        // Score from tags (primary source)
        foreach (var tag in tags)
        {
            var tagLower = tag.Name.ToLower();

            foreach (var keyword in CategoryKeywords)
            {
                if (tagLower.Contains(keyword.Key))
                {
                    var category = keyword.Value.Category;
                    var weight = keyword.Value.Weight;
                    var score = tag.Confidence * (weight / 100.0);

                    if (!categoryScores.ContainsKey(category))
                        categoryScores[category] = 0;

                    categoryScores[category] += score;

                    _logger.LogDebug($"  Tag '{tag.Name}' → {category} (+{score:F2})");
                }
            }
        }

        // Boost from objects (secondary source)
        foreach (var obj in objects)
        {
            var objLower = obj.ObjectProperty.ToLower();

            foreach (var keyword in CategoryKeywords)
            {
                if (objLower.Contains(keyword.Key))
                {
                    var category = keyword.Value.Category;
                    var boost = obj.Confidence * 0.5; // Objects get 50% weight of tags

                    if (!categoryScores.ContainsKey(category))
                        categoryScores[category] = 0;

                    categoryScores[category] += boost;

                    _logger.LogDebug($"  Object '{obj.ObjectProperty}' → {category} (+{boost:F2})");
                }
            }
        }

        // Boost from description (tertiary source)
        var descLower = description.ToLower();
        foreach (var keyword in CategoryKeywords)
        {
            if (descLower.Contains(keyword.Key))
            {
                var category = keyword.Value.Category;
                var boost = 0.3; // Description gets fixed boost

                if (!categoryScores.ContainsKey(category))
                    categoryScores[category] = 0;

                categoryScores[category] += boost;

                _logger.LogDebug($"  Description '{keyword.Key}' → {category} (+{boost:F2})");
            }
        }

        // Get winner
        if (categoryScores.Any())
        {
            var winner = categoryScores.OrderByDescending(x => x.Value).First();
            var totalScore = categoryScores.Values.Sum();
            var confidence = Math.Min(1.0, winner.Value / totalScore);

            _logger.LogInformation($"🏆 Category: {winner.Key} (score: {winner.Value:F2}, confidence: {confidence:F2})");

            return (winner.Key, confidence);
        }

        // Fallback
        _logger.LogWarning("⚠️ No category detected, defaulting to OTHER");
        return ("OTHER", 0.5);
    }

    // ====================================================================
    // 🏷️ ENHANCED PRODUCT NAMING - Specific & Descriptive
    // ====================================================================
    private string GenerateProductNameEnhanced(
        List<ImageTag> tags,
        List<DetectedObject> objects,
        string description,
        string category)
    {
        // Priority 1: Use specific object detection
        var specificObject = objects
            .Where(o => o.Confidence > 0.7)
            .OrderByDescending(o => o.Confidence)
            .FirstOrDefault();

        if (specificObject != null)
        {
            var name = CapitalizeWords(specificObject.ObjectProperty);
            _logger.LogDebug($"  Product name from object: '{name}'");
            return name;
        }

        // Priority 2: Use high-confidence specific tags
        var specificTags = new[] { "apple", "banana", "orange", "bread", "milk", "chicken", "salmon" };
        var specificTag = tags
            .FirstOrDefault(t => specificTags.Any(st => t.Name.ToLower().Contains(st)) && t.Confidence > 0.7);

        if (specificTag != null)
        {
            var name = $"Fresh {CapitalizeWords(specificTag.Name)}";
            _logger.LogDebug($"  Product name from tag: '{name}'");
            return name;
        }

        // Priority 3: Use description caption
        if (!string.IsNullOrEmpty(description))
        {
            var words = description.Split(' ');
            if (words.Length >= 2)
            {
                var name = string.Join(" ", words.Take(3));
                _logger.LogDebug($"  Product name from description: '{CapitalizeWords(name)}'");
                return CapitalizeWords(name);
            }
        }

        // Priority 4: Category-based fallback with quality adjective
        var qualityAdjective = tags.Any(t => t.Name.ToLower().Contains("fresh")) ? "Fresh" : "Quality";

        var fallbackName = category switch
        {
            "FRUITS" => $"{qualityAdjective} Fruits",
            "VEGETABLES" => $"{qualityAdjective} Vegetables",
            "DAIRY" => "Dairy Product",
            "BAKERY" => $"{qualityAdjective} Bread",
            "MEAT" => "Meat Product",
            "SEAFOOD" => "Fresh Seafood",
            "PACKAGED" => "Packaged Food",
            "BEVERAGES" => "Beverage",
            _ => "Food Item"
        };

        _logger.LogDebug($"  Product name fallback: '{fallbackName}'");
        return fallbackName;
    }

    // ====================================================================
    // 📊 ENHANCED FRESHNESS SCORING - Visual Quality Analysis
    // ====================================================================
    private double CalculateFreshnessScoreEnhanced(
        ImageAnalysis analysis,
        List<ImageTag> tags,
        List<DetectedObject> objects)
    {
        double score = 70; // Base score
        var reasons = new List<string>();

        // FACTOR 1: Color Analysis (±20 points)
        var colors = analysis.Color?.DominantColors ?? new List<string>();

        if (colors.Any(c => c.Contains("Green") || c.Contains("Red") || c.Contains("Yellow") || c.Contains("Orange")))
        {
            score += 15;
            reasons.Add("+15 vibrant colors");
        }

        if (colors.Any(c => c.Contains("Brown") || c.Contains("Grey")))
        {
            score -= 10;
            reasons.Add("-10 dull colors");
        }

        if (colors.Any(c => c.Contains("Black")))
        {
            score -= 15;
            reasons.Add("-15 dark/spoiled colors");
        }

        // FACTOR 2: Quality Keywords (±30 points)
        var qualityKeywords = new Dictionary<string, int>
        {
            // Positive indicators
            { "fresh", 20 },
            { "ripe", 15 },
            { "bright", 10 },
            { "vibrant", 10 },
            { "clean", 10 },
            { "shiny", 8 },
            { "green", 5 },
            { "colorful", 5 },
            
            // Negative indicators
            { "moldy", -40 },
            { "rotten", -45 },
            { "spoiled", -40 },
            { "wilted", -25 },
            { "bruised", -20 },
            { "damaged", -15 },
            { "brown", -10 },
            { "old", -15 }
        };

        foreach (var tag in tags)
        {
            foreach (var keyword in qualityKeywords)
            {
                if (tag.Name.ToLower().Contains(keyword.Key))
                {
                    var impact = keyword.Value * tag.Confidence;
                    score += impact;
                    reasons.Add($"{(impact > 0 ? "+" : "")}{impact:F0} {keyword.Key}");
                }
            }
        }

        // FACTOR 3: Visual Clarity (±10 points)
        if (analysis.Color?.IsBWImg == false) // Color image
        {
            score += 5;
            reasons.Add("+5 color image");
        }

        // FACTOR 4: Object Detection Confidence (±10 points)
        var avgObjectConfidence = objects.Any()
            ? objects.Average(o => o.Confidence)
            : 0.5;

        if (avgObjectConfidence > 0.8)
        {
            score += 10;
            reasons.Add("+10 clear objects");
        }
        else if (avgObjectConfidence < 0.5)
        {
            score -= 5;
            reasons.Add("-5 unclear objects");
        }

        // Final clamping
        score = Math.Max(0, Math.Min(100, score));

        _logger.LogInformation($"🌟 Freshness: {score:F0}% [{string.Join(", ", reasons)}]");

        return Math.Round(score, 1);
    }

    // ====================================================================
    // 📅 ENHANCED EXPIRY ESTIMATION - Context-Aware
    // ====================================================================
    private int EstimateExpiryDaysEnhanced(string category, double freshnessScore, List<ImageTag> tags)
    {
        var baseDays = ExpiryDaysMapping.TryGetValue(category, out var days) ? days : 7;

        // Adjustment factor based on freshness (0.5x to 1.5x)
        var adjustmentFactor = 0.5 + (freshnessScore / 100.0);

        // Extra penalty for visible damage
        if (tags.Any(t => t.Name.ToLower().Contains("damaged") ||
                         t.Name.ToLower().Contains("bruised")))
        {
            adjustmentFactor *= 0.7;
        }

        // Bonus for packaged/sealed items
        if (tags.Any(t => t.Name.ToLower().Contains("sealed") ||
                         t.Name.ToLower().Contains("packaged")))
        {
            adjustmentFactor *= 1.2;
        }

        var adjustedDays = (int)(baseDays * adjustmentFactor);
        var finalDays = Math.Max(1, adjustedDays);

        _logger.LogDebug($"  Expiry: {baseDays}d × {adjustmentFactor:F2} = {finalDays}d");

        return finalDays;
    }

    // ====================================================================
    // 📝 ENHANCED NOTES - Detailed & Helpful
    // ====================================================================
    private string GenerateDetailedNotes(
        List<ImageTag> tags,
        List<DetectedObject> objects,
        string description,
        double freshnessScore,
        string qualityGrade,
        string category)
    {
        var notes = new List<string>();

        // 1. Quality assessment
        var qualityDesc = qualityGrade switch
        {
            "A" => "Excellent quality",
            "B" => "Good quality",
            "C" => "Fair quality",
            _ => "Quality inspection recommended"
        };
        notes.Add(qualityDesc);

        // 2. Freshness description
        if (freshnessScore >= 85)
            notes.Add("very fresh and pristine condition");
        else if (freshnessScore >= 70)
            notes.Add("fresh and good condition");
        else if (freshnessScore >= 50)
            notes.Add("acceptable condition, use soon");
        else
            notes.Add("condition check recommended");

        // 3. Specific observations from tags
        var positiveTerms = tags
            .Where(t =>
                t.Name.ToLower().Contains("fresh") ||
                t.Name.ToLower().Contains("ripe") ||
                t.Name.ToLower().Contains("bright") ||
                t.Name.ToLower().Contains("vibrant"))
            .Select(t => t.Name.ToLower())
            .Distinct()
            .Take(2);

        if (positiveTerms.Any())
            notes.Add(string.Join(" and ", positiveTerms));

        // 4. Storage recommendations
        var storageRec = category switch
        {
            "FRUITS" or "VEGETABLES" => "Store in cool, dry place or refrigerate",
            "DAIRY" => "Keep refrigerated at all times",
            "BAKERY" => "Store at room temperature in airtight container",
            "MEAT" or "SEAFOOD" => "Keep refrigerated or frozen immediately",
            "PACKAGED" => "Store in cool, dry place away from sunlight",
            "BEVERAGES" => "Store at room temperature or refrigerate after opening",
            _ => "Follow standard food storage guidelines"
        };
        notes.Add(storageRec);

        // 5. Warning if needed
        if (freshnessScore < 50)
        {
            notes.Add("⚠️ Distribute quickly to avoid spoilage");
        }

        return string.Join(". ", notes) + ".";
    }

    // ====================================================================
    // 📋 BUILD DETECTED ITEMS LIST
    // ====================================================================
    private List<DetectedItem> BuildDetectedItemsList(List<ImageTag> tags, List<DetectedObject> objects)
    {
        var items = new List<DetectedItem>();

        // Add objects first (higher priority)
        foreach (var obj in objects.Take(3))
        {
            items.Add(new DetectedItem
            {
                Name = CapitalizeWords(obj.ObjectProperty),
                Confidence = obj.Confidence
            });
        }

        // Add remaining tags
        var remainingSlots = 5 - items.Count;
        foreach (var tag in tags.Take(remainingSlots))
        {
            if (!items.Any(i => i.Name.ToLower() == tag.Name.ToLower()))
            {
                items.Add(new DetectedItem
                {
                    Name = CapitalizeWords(tag.Name),
                    Confidence = tag.Confidence
                });
            }
        }

        return items;
    }

    // ====================================================================
    // 🛠️ UTILITY METHODS
    // ====================================================================
    private string GetQualityGrade(double freshnessScore)
    {
        return freshnessScore switch
        {
            >= 80 => "A",
            >= 60 => "B",
            >= 40 => "C",
            _ => "D"
        };
    }

    private string CapitalizeWords(string text)
    {
        if (string.IsNullOrWhiteSpace(text)) return text;

        var words = text.Split(' ');
        for (int i = 0; i < words.Length; i++)
        {
            if (words[i].Length > 0)
            {
                words[i] = char.ToUpper(words[i][0]) + words[i][1..].ToLower();
            }
        }
        return string.Join(" ", words);
    }

    public async Task<List<FoodAnalysisData>> GetAnalysisHistoryAsync(Guid groceryId, int limit = 10)
    {
        var analyses = await _context.FoodImageAnalyses
            .Where(a => a.GroceryId == groceryId)
            .OrderByDescending(a => a.AnalyzedAt)
            .Take(limit)
            .ToListAsync();

        return analyses.Select(a => new FoodAnalysisData
        {
            Title = a.DetectedName,
            Category = a.DetectedCategory,
            ExpiryDate = a.EstimatedExpiryDate?.ToString("yyyy-MM-dd") ?? "",
            Notes = a.Notes,
            ImageUrl = a.ImageUrl,
            Confidence = a.Confidence,
            FreshnessScore = a.FreshnessScore,
            QualityGrade = a.QualityGrade,
            DetectedItems = System.Text.Json.JsonSerializer.Deserialize<List<DetectedItem>>(a.DetectedItems) ?? new(),
            AnalyzedAt = a.AnalyzedAt
        }).ToList();
    }
}

public class ApiKeyServiceClientCredentials : Microsoft.Rest.ServiceClientCredentials
{
    private readonly string _apiKey;

    public ApiKeyServiceClientCredentials(string apiKey)
    {
        _apiKey = apiKey;
    }

    public override Task ProcessHttpRequestAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        request.Headers.Add("Ocp-Apim-Subscription-Key", _apiKey);
        return Task.CompletedTask;
    }
}