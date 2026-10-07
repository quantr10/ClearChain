using System.Text.Json;

namespace ClearChain.API.Common;

/// <summary>
/// Parsers for listing and organization fields that are stored as free-form strings.
/// </summary>
public static class ListingFields
{
    /// <summary>
    /// An organization's opening hours ("08:00 - 18:00", en/em dashes allowed) as a time
    /// window, or null when the text is missing, unparsable or runs backwards.
    /// </summary>
    public static (TimeSpan Start, TimeSpan End)? ParseHoursWindow(string? hours)
    {
        if (string.IsNullOrWhiteSpace(hours)) return null;

        var parts = hours
            .Replace("–", "-")
            .Replace("—", "-")
            .Split('-', StringSplitOptions.TrimEntries | StringSplitOptions.RemoveEmptyEntries);

        if (parts.Length < 2) return null;
        if (!TimeSpan.TryParse(parts[0], out var start)) return null;
        if (!TimeSpan.TryParse(parts[1], out var end)) return null;

        return start <= end ? (start, end) : null;
    }

    /// <summary>
    /// The first image of a listing's PhotoUrl, which holds either a single URL or a JSON
    /// array of URLs.
    /// </summary>
    public static string? FirstImageUrl(string? photoUrl)
    {
        if (string.IsNullOrWhiteSpace(photoUrl))
            return null;

        var trimmed = photoUrl.Trim();
        if (!trimmed.StartsWith("["))
            return trimmed;

        try
        {
            return JsonSerializer.Deserialize<List<string>>(trimmed)
                ?.FirstOrDefault(url => !string.IsNullOrWhiteSpace(url));
        }
        catch
        {
            return null;
        }
    }
}
