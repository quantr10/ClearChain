namespace ClearChain.API.DTOs.Disputes;

/// <summary>
/// The reasons an NGO can pick when opening a dispute. The app sends the key, which is what gets
/// stored; the English label is only used in human-readable text such as push notifications.
/// </summary>
public static class DisputeReasons
{
    private static readonly IReadOnlyDictionary<string, string> NgoLabels = new Dictionary<string, string>
    {
        ["poor_condition"] = "Poor food condition",
        ["wrong_items"] = "Wrong items received",
        ["quantity_mismatch"] = "Quantity mismatch",
        ["expired"] = "Items already expired",
        ["not_available"] = "Items not available at pickup",
        ["other"] = "Other",
    };

    public static bool IsValidNgoReason(string? key) => key is not null && NgoLabels.ContainsKey(key);

    /// <summary>
    /// English label for a stored reason key. A value that isn't a known key is returned unchanged,
    /// so rows written before keys were introduced still read sensibly.
    /// </summary>
    public static string Label(string reason) => NgoLabels.TryGetValue(reason, out var label) ? label : reason;
}
