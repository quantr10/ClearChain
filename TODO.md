# TODO

Open work found while reviewing commit `4d43afb` (account closure, paged dashboard feeds, admin
analytics), by the format and lint run after the cleanup that followed it, and by reviews of
`Backend/ClearChain.API/Services/`, `Backend/ClearChain.API/Controllers/` and the app's `data/`
and `presentation/` packages after that commit's bugs were fixed. Line numbers were checked on
2026-11-10.

## Bugs

### Live stats updates overwrite the admin dashboard's all-time figures

The admin `StatsUpdated` event is built in three places:
[`NotificationJobs.BroadcastPlatformStats`](Backend/ClearChain.API/Jobs/NotificationJobs.cs#L258)
sends it every hour,
[`PickupRequestService.MarkPickedUpAsync`](Backend/ClearChain.API/Services/PickupRequestService.cs#L486)
after every pickup, and [`AuthController`](Backend/ClearChain.API/Controllers/AuthController.cs#L91)
after every registration.
[`AdminDashboardViewModel.kt:90`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/dashboard/AdminDashboardViewModel.kt#L90)
merges it into the stats loaded from
[`AdminController.GetStatisticsOverview`](Backend/ClearChain.API/Controllers/AdminController.cs#L130),
but two of its figures count something different:

- **Completed requests:** the event sends `CompletedToday`, the completed requests whose
  `RequestedAt` is today. The app writes that into `completedRequests`, which the REST load
  fills with the all-time count, so after the first update the dashboard's completion meter and
  Completed bar fall to that small number. `CompletedToday` is also counted by request date
  rather than completion date, but nothing in the app shows a figure for today.
- **Organizations:** the event counts every grocery and NGO, including deleted accounts, while
  the REST overview leaves out `IsDeleted`.
- **Fix:**
  - send the all-time completed count and drop `CompletedToday`;
  - leave out deleted accounts;
  - build the payload in one helper that all three senders call, so it can't drift from the
    overview again;
  - update `PlatformStatsNotification` in
    [`SignalRService.kt`](Mobile/app/src/main/java/com/clearchain/app/data/remote/signalr/SignalRService.kt#L594)
    to match.

### Stock put back on offer isn't broadcast to listing screens

When a reservation is released,
[`ReleaseReservationAsync`](Backend/ClearChain.API/Services/PickupRequestService.cs#L733) either
reopens the reserved listing or merges it into an Open sibling, and nothing sends a listing event
for it. Three things release a reservation: an NGO cancelling a pending request, the stale-request
job cancelling one, and an NGO closing its account.

- **Impact:** NGOs with a browse or listing-detail screen open (`all_listings`, `listing_{id}`)
  keep seeing the old quantity, and don't see a reopened listing, until they refresh.
- **Fix:** have the release report which listings it reopened, merged into or removed. After
  the save, broadcast them from `CancelAsync` (the job goes through it too) and from
  `NotifyAccountClosedAsync`. This needs the listing DTO built the way `ListingsController`
  builds it.
- **Why deferred:** it adds notify calls to three paths, well beyond the reviewed diff.

### A reserved listing with no group is stranded on release

[`ReleaseReservationAsync`](Backend/ClearChain.API/Services/PickupRequestService.cs#L751) only
releases a reserved listing that has a `Group`, but it then clears every item's
`ReservedListingId` anyway. A reserved listing with no `GroupId` stays Reserved with no request
pointing at it, so it never goes back on offer. The branch for requests that predate cart items
([`:772`](Backend/ClearChain.API/Services/PickupRequestService.cs#L772)) does the same with
`pr.ListingId`.

- **First:** check whether any exist:
  `select count(*) from clearancelistings where status = 'reserved' and groupid is null;`
- **Fix:** reopen such a listing where it is, or keep the reference when the listing can't be
  released.
- **Why deferred:** it changes how legacy data behaves, and nobody has checked whether such
  listings exist.

### Other admins aren't told when a dispute is resolved

[`DisputesController.ResolveDispute`](Backend/ClearChain.API/Controllers/DisputesController.cs#L176)
pushes the outcome to the NGO and the grocery, but sends nothing on the admin hub, which has
`NewDisputeCreated` and no event for a resolution.

- **Impact:** another admin's dashboard keeps the dispute in its alert feed, which lists only
  open disputes, until they reload. If they try to resolve it they now get a 409. The analytics
  screen's dispute outcomes don't update either.
- **Fix:** add a `DisputeResolved` event end to end:
  - on the server, a method on `IAdminNotificationService`, called from `ResolveDispute`;
  - in the app, a handler and flow in `SignalRService.kt`, an alert-feed reload in
    `AdminDashboardViewModel`, and the event in `AdminAnalyticsViewModel`'s live-update merge;
  - in the README, the `/hubs/admin` row of the hub table.
- **Why deferred:** a new SignalR event on both sides, well outside the reviewed folder.

### Radius search never shows groceries without coordinates

When a radius is given,
[`ListingsController.GetAllListings`](Backend/ClearChain.API/Controllers/ListingsController.cs#L91)
first filters to a bounding box in the database, which needs the grocery's latitude and
longitude. Afterwards it appends the listings with no distance after the in-radius ones
([`:135`](Backend/ClearChain.API/Controllers/ListingsController.cs#L135)), but that list is always
empty because the box has already removed them.

- **Impact:** coordinates are optional for organizations, and the browse screen sends a radius
  whenever the NGO has set a location
  ([`BrowseListingsViewModel.kt:287`](Mobile/app/src/main/java/com/clearchain/app/presentation/ngo/browselistings/BrowseListingsViewModel.kt#L287)).
  Those NGOs never see listings from a grocery that hasn't set its location.
- **Decision:** should those listings appear in radius results, after the nearby ones?
  - **Yes:** let the bounding box also pass groceries with no coordinates.
  - **No:** delete the `noCoordinates` branch, since it never adds anything.

### Logout shouldn't need an access token

[`AuthController.Logout`](Backend/ClearChain.API/Controllers/AuthController.cs#L195) is
`[Authorize]`, but it never uses the caller's identity. It finds the session by the refresh token
in the body, and removes the push token only for the organization that refresh token belongs to.
Requiring an access token is the only reason logout can get a 401, and the app has two
workarounds for that:

- `TokenAuthenticator` checks for the logout path, so that a refresh that fails during sign-out
  isn't reported as an expired session
  ([`:52`](Mobile/app/src/main/java/com/clearchain/app/data/remote/interceptor/TokenAuthenticator.kt#L52)).
- `AuthRepositoryImpl.logout` sends the logout a second time, because the authenticator's refresh
  rotates the refresh token and its retry still names the old one
  ([`:140`](Mobile/app/src/main/java/com/clearchain/app/data/repository/AuthRepositoryImpl.kt#L140)).

It also leaves a gap. When both tokens have expired, the refresh fails and the logout never reaches
the server, so the device's push token stays registered to the account that signed out. The device
keeps getting that account's notifications until someone signs in on it, because
`RegisterFCMToken` moves the token to the new account, or until the 60-day stale-token prune.

- **Fix:** make `Logout` `[AllowAnonymous]`, then remove both workarounds.
  `RevokeRefreshTokenAsync` accepts an expired refresh token as long as it isn't revoked, so this
  also closes the gap for expired sessions.
- **Also consider:** remove the push token when the refresh token is found but already revoked,
  for example after a password reset signed the device out. Today that case returns before the
  removal.
- **Why deferred:** backend code, outside the reviewed `data/` package.

### A sign-out can still end on "session expired"

`TokenAuthenticator` holds back the "session expired" message only for the logout request itself
([`:84`](Mobile/app/src/main/java/com/clearchain/app/data/remote/interceptor/TokenAuthenticator.kt#L84)).
If another request's refresh fails first, it calls `notifySessionExpired()`, and
[`MainActivity`](Mobile/app/src/main/java/com/clearchain/app/MainActivity.kt#L112) shows the toast
to someone who just signed out. For example, a profile reload can still be in flight when the user
taps sign-out on a dead session.

- **Fix:** have the logout flow tell `SessionManager` that a sign-out is in progress, and skip
  `notifySessionExpired()` while it is. This is still needed after the fix above: logout itself
  would stop getting a 401, but other requests in flight still can.
- **Why deferred:** a timing edge case whose fix reaches beyond `data/`.

### Admin screens wait for pickup events that admins never receive

`PickupNotificationService` sends `PickupRequestCreated`, `PickupRequestStatusChanged` and
`PickupRequestCancelled` only to the grocery's and the NGO's own groups
([`PickupNotificationService.cs:25`](Backend/ClearChain.API/Services/PickupNotificationService.cs#L25)),
never to `admins`. Two admin screens listen for them anyway:

- [`AdminAnalyticsViewModel.kt:62`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/analytics/AdminAnalyticsViewModel.kt#L62)
  reloads on `pickupRequestCreated` and `pickupRequestCancelled`, so its request-status card stays
  stale until a manual refresh. Its comment about a burst of cancellations when an account closes
  describes events it never gets.
- [`TransactionsViewModel.kt:44`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/transactions/TransactionsViewModel.kt#L44),
  the admin list of every pickup request, reloads on all three, so only completions
  (`TransactionCompleted`) reach it.

Verification decisions don't reach `admins` either. The verify and unverify endpoints
([`AdminController.cs:77`](Backend/ClearChain.API/Controllers/AdminController.cs#L77)) send no admin
event, so another admin's verification queue keeps showing an organization that has just been
handled. Dispute resolutions are covered in their own entry above.

- **Fix:** also send the pickup events to `admins`, or send one admin event for any request
  status change, and add an event for verification decisions. Each new event needs a handler in
  `SignalRService.kt` and a row in the README's hub table. Switching the screens to
  `statsUpdated` would only partly help, because it fires only after a pickup, after a
  registration and once an hour.
- **Why deferred:** backend events, outside the reviewed `presentation/` package.

### Dashboard activity can go back to an older page

On the NGO and grocery dashboards, `loadActivity` fetches the week's activity itself and only then
hands it to the pager with `refreshFrom`
([`NgoDashboardViewModel.kt:196`](Mobile/app/src/main/java/com/clearchain/app/presentation/ngo/NgoDashboardViewModel.kt#L196),
[`GroceryDashboardViewModel.kt:161`](Mobile/app/src/main/java/com/clearchain/app/presentation/grocery/GroceryDashboardViewModel.kt#L161)).
That fetch runs outside `FeedPager`'s lock, so when two loads overlap, a slow older response can
land after a newer one, in both the sparkline and the activity list. `refresh()` avoided this by
fetching inside the lock.

- **When:** two `loadAll` calls at once, the first load and a pull-to-refresh. The review judged
  this unlikely, on the grounds that a pull can't start while the first load's spinner is up.
  Nobody has checked that, and the pull-to-refresh box is active during the first load.
- **In the same code:**
  - Once the sheet has been read past 200 items, `refreshFrom` falls back to `reload()`, which
    downloads again the first page the caller has just fetched
    ([`FeedPager.kt:71`](Mobile/app/src/main/java/com/clearchain/app/util/FeedPager.kt#L71)).
  - The two `loadActivity` functions are identical apart from the log tag.
- **Fix:** give `FeedPager` a refresh that takes the first-page fetch, runs it inside the lock,
  hands the page back for the sparkline, and carries on from page 2 when the feed was read
  deeper. One shared `loadActivity` helper next to `activityFeedPager` can then serve both
  dashboards.
- **Why deferred:** an API change in `util/FeedPager.kt`, outside the reviewed package.

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
- **N+1 queries:** each withdrawn request runs two queries of its own, one for its reserved
  listings and one for their groups' Open listings (`LoadOpenListingsAsync`). Load both once
  for the whole closure.
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

### Drop the statistics fields the app never reads

`Timing` and `Organizations.OldestPendingDays`
([`AdminController.cs:301`](Backend/ClearChain.API/Controllers/AdminController.cs#L301),
[`:368`](Backend/ClearChain.API/Controllers/AdminController.cs#L368)) are computed on every call,
but the app stopped reading them before `4d43afb`. Remove them end to end: the controller, the
backend DTO, and `AdminExtDto.kt`.

### Admin dashboard reloads alerts on events that can't change them

In [`AdminDashboardViewModel.kt`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/dashboard/AdminDashboardViewModel.kt#L66),
`newOrganizationRegistered` and `transactionCompleted` reload both the statistics and the alert
feed. The alert feed holds only open disputes, which neither event changes.

- **Fix:** reload only the statistics on those two events, but keep the spinner and the
  partial-failure snackbar that `loadAll` provides.

## Design decisions

### Dispute outcomes: bar or donut

`ShareBar` ([`AdminAnalyticsScreen.kt:344`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/analytics/AdminAnalyticsScreen.kt#L344))
rebuilds the split bar and legend that `DonutChart` in `components/AnalyticsCharts.kt` already
provides, and the Organizations card below it uses a donut.

- **If the bar is wanted:** move `ShareBar` into `AnalyticsCharts.kt` and share the legend row
  with `DonutLegend`.
- **Otherwise:** use `DonutChart`.

## Minor

- **Concurrent cache misses in `DeletedAccountTokenCheck`.** `GetOrCreateAsync` doesn't make
  concurrent callers share one lookup, so when the entry expires, parallel requests each run the
  same query. Caching the `Task<bool>` would fix this, but it must not cache a failed lookup,
  which lets the request through rather than giving an answer.
- **Dispute reason labels.** `NgoDisputeReason.fromKey(key)?.let { label } ?: key` is written out
  in four places, and one helper on `NgoDisputeReason` could serve all of them:
  - [`AdminAnalyticsScreen.kt:331`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/analytics/AdminAnalyticsScreen.kt#L331)
  - [`AdminAnalyticsViewModel.kt:264`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/analytics/AdminAnalyticsViewModel.kt#L264)
  - [`AdminDashboardViewModel.kt:82`](Mobile/app/src/main/java/com/clearchain/app/presentation/admin/dashboard/AdminDashboardViewModel.kt#L82)
  - [`AdminDisputeSection.kt:83`](Mobile/app/src/main/java/com/clearchain/app/presentation/shared/requestdetail/AdminDisputeSection.kt#L83)

## Format and lint

### Dependency and SDK version warnings

The 22 lint warnings that remain report newer versions or the target API level. They need a
decision rather than a code fix:

- **Newer versions available** (`GradleDependency`, `NewerVersionAvailable`,
  `AndroidGradlePluginVersion`): 21 warnings. AGP stays at 9.0 so the project syncs in Android
  Studio 2025.2 (commit `688ab1e`).
- **`OldTargetApi`:** `targetSdk` stays at 34 on purpose.
