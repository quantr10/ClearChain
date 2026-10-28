using ClearChain.API.DTOs.ImageAnalysis;
using OpenAI;
using OpenAI.Chat;
using System.ClientModel;
using System.ClientModel.Primitives;
using System.Globalization;
using System.Text.Json;

namespace ClearChain.API.Services;

/// <summary>
/// Food image analysis on a vision-capable model deployed in Azure OpenAI (Microsoft Foundry).
/// The model looks at the photo and fills in the listing fields itself, constrained by a strict
/// JSON schema; this class only builds the request and checks the answer.
/// </summary>
public class AzureOpenAIFoodAnalysisService : IImageAnalysisService
{
    private const string SystemPrompt = """
        You inspect photos that grocery stores take of surplus food they are about to list for
        donation to NGOs. Look only at what is visible in the photo and never invent text you
        cannot read.

        Fill in the fields as follows:
        - isFood: false when the photo shows no food or drink product, or is too blurry or dark
          to tell. Fill in the other fields as best you can anyway.
        - title: a short product name as a store clerk would write it, with brand and size when
          they are legible (for example "Ripe bananas" or "Vinamilk fresh milk 1L").
        - description: one or two short sentences of product copy, as a store would print on a
          shelf tag or product page: what the product is, its variety or flavor, pack size, and
          what it is typically used for (for example "Vinamilk fresh pasteurized milk in a 1L
          carton. Smooth and creamy, good for drinking or cooking."). Describe the product, not
          the photo: never write "the image shows", "in the photo" or similar, and leave out
          its current condition and storage, which belong in notes. Use general knowledge of
          the product type, but only name a brand or size you can read. Plain text, no emoji.
        - category: FRUITS, VEGETABLES, DAIRY (milk, yogurt, cheese, eggs), BAKERY, MEAT,
          SEAFOOD, PACKAGED (canned, dry or other shelf-stable packaged goods), BEVERAGES
          (drinks other than milk) or OTHER.
        - printedExpiryDate: the expiry, use-by or best-before date printed on the product
          (labels such as HSD, EXP, Use by, Best before, BB), as yyyy-MM-dd. Vietnamese and
          European labels put the day first. Never use the manufacture date (NSX, MFG). If only
          a month and year are printed, use the last day of that month. Null when no such date
          is legible.
        - estimatedShelfLifeDays: how many days from today the food stays safe to eat under
          normal storage, judging by its type and visible condition. Always give a number.
        - freshnessScore: 0 to 100, from visible condition only: bruises, mold, wilting,
          discoloration, leaks or damaged packaging. For sealed packaged goods, rate the
          packaging.
        - qualityGrade: A excellent, B good with minor flaws, C fair and should be used soon,
          D poor and should not be distributed.
        - confidence: 0 to 1, how sure you are of the title and category. Lower it for blurry
          photos, partial views or several different products.
        - notes: one to three short sentences on the visible condition and how to store it.
          Mention it when the expiry date was read from the label. Plain text, no emoji.
        """;

    private static readonly BinaryData ResponseSchema = BinaryData.FromString("""
        {
          "type": "object",
          "properties": {
            "isFood": { "type": "boolean" },
            "title": { "type": "string" },
            "description": { "type": "string" },
            "category": {
              "type": "string",
              "enum": ["FRUITS", "VEGETABLES", "DAIRY", "BAKERY", "MEAT", "SEAFOOD", "PACKAGED", "BEVERAGES", "OTHER"]
            },
            "printedExpiryDate": { "type": ["string", "null"] },
            "estimatedShelfLifeDays": { "type": "integer" },
            "freshnessScore": { "type": "integer" },
            "qualityGrade": { "type": "string", "enum": ["A", "B", "C", "D"] },
            "confidence": { "type": "number" },
            "notes": { "type": "string" }
          },
          "required": [
            "isFood", "title", "description", "category", "printedExpiryDate", "estimatedShelfLifeDays",
            "freshnessScore", "qualityGrade", "confidence", "notes"
          ],
          "additionalProperties": false
        }
        """);

    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    private readonly ChatClient _chatClient;
    private readonly ChatCompletionOptions _completionOptions;
    private readonly ILogger<AzureOpenAIFoodAnalysisService> _logger;

    public AzureOpenAIFoodAnalysisService(
        IConfiguration config,
        ILogger<AzureOpenAIFoodAnalysisService> logger)
    {
        var endpoint = config["AZURE_OPENAI_ENDPOINT"]
            ?? throw new InvalidOperationException("AZURE_OPENAI_ENDPOINT not configured");
        var key = config["AZURE_OPENAI_KEY"]
            ?? throw new InvalidOperationException("AZURE_OPENAI_KEY not configured");
        var deployment = config["AZURE_OPENAI_DEPLOYMENT"]
            ?? throw new InvalidOperationException("AZURE_OPENAI_DEPLOYMENT not configured");

        _chatClient = new ChatClient(
            model: deployment,
            credential: new ApiKeyCredential(key),
            options: new OpenAIClientOptions
            {
                Endpoint = ToV1Endpoint(endpoint),
                // The app gives up on this request after 30 s (OkHttp readTimeout), so answer
                // within that rather than retrying for a client that has already left.
                NetworkTimeout = TimeSpan.FromSeconds(25),
                RetryPolicy = new ClientRetryPolicy(maxRetries: 0)
            });

        _completionOptions = new ChatCompletionOptions
        {
            ResponseFormat = ChatResponseFormat.CreateJsonSchemaFormat(
                "food_listing", ResponseSchema, jsonSchemaIsStrict: true)
        };

        // Only reasoning models accept this; leave it unset for the others or they reject the call.
        // The SDK still marks the setting experimental (OPENAI001), hence the pragma.
        var reasoningEffort = config["AZURE_OPENAI_REASONING_EFFORT"];
        if (!string.IsNullOrWhiteSpace(reasoningEffort))
        {
#pragma warning disable OPENAI001
            _completionOptions.ReasoningEffortLevel = new ChatReasoningEffortLevel(reasoningEffort);
#pragma warning restore OPENAI001
        }

        _logger = logger;
    }

    public async Task<FoodAnalysisData> AnalyzeFoodImageAsync(IFormFile image, Guid groceryId, string language)
    {
        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var outputLanguage = language == "vi" ? "Vietnamese" : "English";

        BinaryData imageData;
        using (var stream = image.OpenReadStream())
        {
            imageData = await BinaryData.FromStreamAsync(stream);
        }

        var messages = new List<ChatMessage>
        {
            new SystemChatMessage(SystemPrompt),
            new UserChatMessage(
                ChatMessageContentPart.CreateTextPart(
                    $"Today is {today:yyyy-MM-dd}. Write title, description and notes in {outputLanguage}."),
                // High detail so small print such as expiry dates stays legible.
                ChatMessageContentPart.CreateImagePart(imageData, image.ContentType, ChatImageDetailLevel.High))
        };

        ChatCompletion completion;
        try
        {
            completion = await _chatClient.CompleteChatAsync(messages, _completionOptions);
        }
        catch (ClientResultException ex)
        {
            _logger.LogError(ex, "Azure OpenAI call failed with status {Status}", ex.Status);
            throw new InvalidOperationException("AI analysis is unavailable right now. Please try again.", ex);
        }
        catch (OperationCanceledException ex)
        {
            _logger.LogWarning(ex, "Azure OpenAI call timed out for grocery {GroceryId}", groceryId);
            throw new InvalidOperationException("AI analysis took too long. Please try again.", ex);
        }

        if (completion.FinishReason != ChatFinishReason.Stop || !string.IsNullOrEmpty(completion.Refusal))
        {
            _logger.LogWarning(
                "Azure OpenAI gave no usable answer for grocery {GroceryId}: finish reason {FinishReason}, refusal {Refusal}",
                groceryId, completion.FinishReason, completion.Refusal);
            throw new InvalidOperationException("AI analysis couldn't read this photo. Please try another one.");
        }

        var answer = JsonSerializer.Deserialize<ModelAnswer>(completion.Content[0].Text, JsonOptions)
            ?? throw new InvalidOperationException("AI analysis returned an empty answer. Please try again.");

        if (!answer.IsFood)
        {
            throw new InvalidOperationException(
                "The photo doesn't seem to show a food product. Please take a clearer photo of the item.");
        }

        // A date read off the label beats any estimate; the schema can't enforce the date format.
        var expiryDate = DateOnly.TryParseExact(
            answer.PrintedExpiryDate, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var printed)
            ? printed
            : today.AddDays(Math.Max(1, answer.EstimatedShelfLifeDays));

        _logger.LogInformation(
            "Analyzed image for grocery {GroceryId}: {Title} ({Category}), grade {Grade}, {Tokens} tokens",
            groceryId, answer.Title, answer.Category, answer.QualityGrade, completion.Usage?.TotalTokenCount);

        return new FoodAnalysisData
        {
            Title = answer.Title,
            Description = answer.Description,
            Category = answer.Category,
            ExpiryDate = expiryDate.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture),
            Notes = answer.Notes,
            Confidence = Math.Clamp(answer.Confidence, 0, 1),
            FreshnessScore = Math.Clamp(answer.FreshnessScore, 0, 100),
            QualityGrade = answer.QualityGrade
        };
    }

    // The v1 API lives under /openai/v1/ on the resource endpoint and needs no api-version.
    private static Uri ToV1Endpoint(string endpoint)
    {
        var trimmed = endpoint.TrimEnd('/');
        if (!trimmed.EndsWith("/openai/v1", StringComparison.OrdinalIgnoreCase))
            trimmed += "/openai/v1";
        return new Uri(trimmed + "/");
    }

    private sealed record ModelAnswer(
        bool IsFood,
        string Title,
        string Description,
        string Category,
        string? PrintedExpiryDate,
        int EstimatedShelfLifeDays,
        int FreshnessScore,
        string QualityGrade,
        double Confidence,
        string Notes);
}
