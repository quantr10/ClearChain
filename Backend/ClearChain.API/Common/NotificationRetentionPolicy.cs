namespace ClearChain.API.Common;

/// <summary>
/// How long the Notifications table keeps a row. The nightly
/// <c>cleanup-old-notifications</c> job sweeps against this, so it also describes exactly what
/// an inbox request can return — a client asking for "everything" is asking for this window,
/// and the list endpoint reports it so the app can say so rather than guess.
///
/// One window for every notification, read or not: a split policy meant the same notification
/// disappeared at a different age depending on whether it had been opened, which is not
/// something a user can predict.
/// </summary>
public static class NotificationRetentionPolicy
{
    /// <summary>Nothing survives past this many days from when it was created.</summary>
    public const int RetentionDays = 30;

    /// <summary>Oldest CreatedAt a notification can still have in the table.</summary>
    public static DateTime Cutoff(DateTime utcNow) => utcNow.AddDays(-RetentionDays);
}
