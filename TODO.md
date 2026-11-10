# TODO

Open work found while reviewing commit `4d43afb` (account closure, paged dashboard feeds, admin
analytics), and by the format and lint run after the cleanup that followed it. Line numbers are
as of that cleanup.

## Bugs

### Account closure can split a listing group into several Open listings

[`PickupRequestService.WithdrawNgoRequestsAsync`](Backend/ClearChain.API/Services/PickupRequestService.cs#L173)
releases every open request's reservation into the same DbContext and saves once at the end.
[`SmartMergeOnCancel`](Backend/ClearChain.API/Services/PickupRequestService.cs#L766) looks for an
Open sibling to merge into with a database query, which can't see siblings that an earlier
release in the same loop has reopened but not yet saved.

- **Example:** an NGO holds two requests, A and B. Each has a reserved child listing, RA and RB,
  from the same ListingGroup, and the group has no Open listing. Releasing A reopens RA in memory
  only. Releasing B queries the database, still sees RA as Reserved, and reopens RB as well.
  After the save, the group has two Open listings for one item instead of one merged listing.
  `CancelAsync` saves after each request, so it doesn't hit this.
- **Fix:** have the sibling lookup check the change tracker as well (`_context.ClearanceListings.Local`),
  or save after each release, inside one transaction.

### Leaderboard rows for deleted accounts open a profile that returns 404

The admin leaderboards include deleted accounts on purpose: their pickups still count, under
the name they had. But every row opens the public profile
([`AdminAnalyticsScreen.kt:266`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/analytics/AdminAnalyticsScreen.kt#L266)),
and that endpoint filters out `IsDeleted`
([`OrganizationsController.cs:432`](Backend/ClearChain.API/Controllers/OrganizationsController.cs#L432)).
Tapping a deleted leader always ends on a "not found" screen.

- **Fix:** send an `isDeleted` flag with each leader and make those rows not clickable (or
  label them), or let admins read deleted profiles.

### A database error in the deleted-account check rejects every token with 401

[`DeletedAccountTokenCheck.RejectIfDeletedAsync`](Backend/ClearChain.API/Middleware/DeletedAccountTokenCheck.cs#L24)
runs inside JwtBearer's `OnTokenValidated`. If the `IsDeleted` lookup throws (a brief Supabase
connection drop, for example), nothing is cached and JwtBearer reports an authentication
failure.

- **Impact:** every request on every endpoint returns 401, not only deleted accounts. The app
  refreshes, which succeeds, then the retry fails the same way, so users can be sent back to
  Login over what should have been a 500 or a retry.
- **Fix:** catch the lookup failure and let the request through without caching an answer
  (verified-only actions still check the database through `RequireVerifiedOrganizationAttribute`),
  or return a 503 instead of a 401.

## Deferred cleanups

### Merge the activity feed in SQL

[`OrganizationsController.GetMyActivity`](Backend/ClearChain.API/Controllers/OrganizationsController.cs#L231)
reads `skip + size` rows from each of its two sources and merges and pages them in memory. So
page N reads N × size rows per source, and the tie-break has to copy Postgres's uuid ordering by
hand.

- **Main change:** project both sources to one row shape, `Concat` them (EF translates this to
  `UNION ALL`), order, skip and take in SQL, then map to `ActivityDto`. That also folds the
  duplicated status `switch`.
- **Smaller fixes in the same method:**
  - read the organization type from the role claim instead of a tracked `FindAsync`;
  - get both counts in one query;
  - project only the columns used, with `AsNoTracking`.
- **Why deferred:** a query rewrite that needs checking against a real Postgres.

### Batch the account-closure queries

These are in `CloseAccountAsync` ([`PickupRequestService.cs:89`](Backend/ClearChain.API/Services/PickupRequestService.cs#L89)).
It is a rare path, so the cost is low.

- **Both sides always run:** an NGO account pays for the grocery-side queries, which always
  come back empty. Pass the organization type in and run only the matching side.
- **Two scans of `PickupRequestItems`:** fold `groupsWithHistory` into the groups query as an
  `Any()`.
- **Lookups inside the group loop:** `List.Contains` and `Where` run per group. Use a `HashSet`
  and `ToLookup(GroupId)`.
- **N+1 queries:** each withdrawn request loads its reserved listings separately, plus one
  sibling query per listing in `SmartMergeOnCancel`. Batch-load these up front, which also
  helps with the first bug above.
- **Notifications:** `NotifyAccountClosedAsync` sends them one at a time inside the DELETE
  request, one `ListingDeleted` broadcast per listing. Use `Task.WhenAll` for the hub sends, or
  move the work to a Hangfire job.
- **Why deferred:** correctness-sensitive, and there are no tests.

### One listing-group removal helper

[`CloseGroceryAsync`](Backend/ClearChain.API/Services/PickupRequestService.cs#L112) and
[`ListingsController.DeleteListing`](Backend/ClearChain.API/Controllers/ListingsController.cs#L343)
each update the group totals by hand when a listing is removed: `TotalRemoved`,
`IsFullyConsumed`, `OriginalListingId`, and deleting the group when it has no history. Move that
into one helper, for example a method on `ListingGroup`, and call it from both places.

- **Why deferred:** it changes `ListingsController`, which was outside the reviewed diff.

### `TokenAuthenticator` should not refresh a failed logout

A logout that gets a 401 goes through
[`TokenAuthenticator`](Mobile/app/src/main/java/com/clearchain/app/data/remote/interceptor/TokenAuthenticator.kt),
for example a long-idle session whose refresh token has expired. The refresh fails, and the app
shows "session expired" when the user simply signed out.

- **Fix:** skip `auth/logout` the same way `auth/refresh` is already skipped.
- **Afterwards:** consider dropping the `revokeOnServer` flag, which today only spares the
  post-delete sign-out a request that is bound to fail.
- **Why deferred:** a bug fix outside the reviewed diff.

### Drop the statistics fields the app never reads

`Timing` and `Organizations.OldestPendingDays`
([`AdminController.cs:351`](Backend/ClearChain.API/Controllers/AdminController.cs#L351),
[`:363`](Backend/ClearChain.API/Controllers/AdminController.cs#L363)) are computed on every call,
but the app stopped reading them before `4d43afb`. Remove them end to end: the controller, the
backend DTO, and `AdminExtDto.kt`.

### Use `PageRequest` for the remaining paged endpoints

`ListingsController` ([`:142`](Backend/ClearChain.API/Controllers/ListingsController.cs#L142),
[`:189`](Backend/ClearChain.API/Controllers/ListingsController.cs#L189)) and
[`NotificationsController.cs:31`](Backend/ClearChain.API/Controllers/NotificationsController.cs#L31)
still clamp by hand and compute the offset as an `int`, which can overflow on a huge `page`
value. Switch them to
[`Common/PageRequest.cs`](Backend/ClearChain.API/Common/PageRequest.cs).

### Admin dashboard reloads alerts on events that can't change them

In [`AdminDashboardViewModel.kt`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/dashboard/AdminDashboardViewModel.kt#L66),
`newOrganizationRegistered` and `transactionCompleted` reload both the statistics and the alert
feed. The alert feed holds only open disputes, which neither event changes.

- **Fix:** reload only the statistics on those two events, but keep the spinner and the
  partial-failure snackbar that `loadAll` provides.

## Design decisions

### Dispute outcomes: bar or donut

`ShareBar` ([`AdminAnalyticsScreen.kt:324`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/analytics/AdminAnalyticsScreen.kt#L324))
rebuilds the split bar and legend that `DonutChart` in `components/AnalyticsCharts.kt` already
provides, and the Organizations card below it uses a donut.

- **If the bar is wanted:** move `ShareBar` into `AnalyticsCharts.kt` and share the legend row
  with `DonutLegend`.
- **Otherwise:** use `DonutChart`.

## Minor

- **Concurrent cache misses in `DeletedAccountTokenCheck`.** `GetOrCreateAsync` doesn't make
  concurrent callers share one lookup, so when the entry expires, parallel requests each run the
  same query. Caching the `Task<bool>` would fix this, but it must not cache a failed lookup
  (see the 401 bug above).
- **Dispute reason labels.** `NgoDisputeReason.fromKey(key)?.let { label } ?: key` is written out
  in four places, and one helper on `NgoDisputeReason` could serve all of them:
  - [`AdminAnalyticsScreen.kt:311`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/analytics/AdminAnalyticsScreen.kt#L311)
  - [`AdminAnalyticsViewModel.kt:254`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/analytics/AdminAnalyticsViewModel.kt#L254)
  - [`AdminDashboardViewModel.kt:82`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/dashboard/AdminDashboardViewModel.kt#L82)
  - [`AdminDisputeSection.kt:83`](Mobile/app/src/main/java/com/clearchain/app/presentation/shared/requestdetail/AdminDisputeSection.kt#L83)

## Format and lint

### `dotnet format` fails on drift older than this work

`dotnet format Backend/ClearChain.sln --verify-no-changes` reports 49 findings in 32 files. A
clean copy of `HEAD` reports more, so none of them came from the cleanup.

- **`IMPORTS` (26 files):** `using` directives out of order.
- **`IDE1006` (22):** naming. Mostly `private static readonly` fields without the `_camelCase`
  prefix that `.editorconfig` asks for. In these files:
  - `Services/AuthService.cs` (5)
  - `Services/AzureOpenAIFoodAnalysisService.cs` (3)
  - 2 each in `Controllers/AdminController.cs`, `Controllers/ImageAnalysisController.cs`,
    `Jobs/NotificationJobs.cs`, `Services/PushNotificationService.cs` and
    `Services/SupabaseStorageService.cs`
  - 1 each in `Common/StorageBucketPolicy.cs`, `Controllers/DisputesController.cs`,
    `DTOs/Disputes/DisputeReasons.cs` and `Middleware/DeletedAccountTokenCheck.cs`
- **`IDE0065` (1):** a `using` inside the namespace in `DTOs/Listings/UpdateListingQuantityRequest.cs`.

To fix, run `dotnet format Backend/ClearChain.sln` with the API stopped and the affected files
closed in the editor. Re-run with `--verify-no-changes` and rename whatever naming findings it
leaves by hand.

### Dependency and SDK version warnings

The 22 lint warnings that remain report newer versions or the target API level. They need a
decision rather than a code fix:

- **Newer versions available** (`GradleDependency`, `NewerVersionAvailable`,
  `AndroidGradlePluginVersion`): 21 warnings. AGP stays at 9.0 so the project syncs in Android
  Studio 2025.2 (commit `688ab1e`).
- **`OldTargetApi`:** `targetSdk` stays at 34 on purpose.
