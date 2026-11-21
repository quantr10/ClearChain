# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

ClearChain connects grocery stores (post surplus as listings) with NGOs (claim it as pickup requests, then
track it as inventory); admins verify organizations and resolve disputes. Two apps share one repo:
`Backend/` (ASP.NET Core 8 API + SignalR + Hangfire, PostgreSQL on Supabase) and `Mobile/` (Kotlin, Jetpack
Compose, Hilt). The README holds setup, every config variable, the hub/event table, the job schedule and
the route list. Read it rather than re-deriving those, and keep its tables up to date when you change them.

## Commands

Backend (run from `Backend/ClearChain.API`, because `.env` and `firebase-adminsdk.json` are read from the
current directory):

```bash
dotnet run                                   # http profile, port 5000, Development (Swagger at /swagger)
dotnet build ../ClearChain.sln
dotnet build ClearChain.API.csproj -o <scratch-dir>   # compile-check while the API is running (it locks bin/*.dll)
dotnet format ../ClearChain.sln --verify-no-changes   # style per .editorconfig
```

Mobile (from `Mobile/`; on PowerShell use `.\gradlew.bat`):

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew spotlessCheck      # ktlint 1.0.1 via Spotless; spotlessApply to fix
./gradlew lint
```

There are **no automated tests**. The test projects were removed on purpose (commit `f560fe2`), so
verify changes by building, running `spotlessCheck`/`lint`, and exercising the flow. There are also
**no EF migrations**, and `DATABASE_URL` is normally a live Supabase database, so running the API or the
app writes real rows.

## Backend architecture

Three projects: `ClearChain.Domain` (entities and enums, no dependencies), `ClearChain.Infrastructure`
(only `ApplicationDbContext`), and `ClearChain.API` (everything else). `Program.cs` holds all DI, auth,
the Hangfire job registrations and the hub routes.

- **Required config fails fast.** `Program.cs` throws at startup if a key in `requiredConfigKeys` is
  missing. A new required variable goes in that list, in `.env.example` and in the README's table.
- **Data access is mostly in controllers.** Most controllers query `ApplicationDbContext` directly. Auth
  and organization logic sits in `AuthService` and `OrganizationService`. The two flows that move stock,
  `CartService` and `PickupRequestService`, return a result record with an error enum
  (`CartServiceResult`/`CartServiceError`, `PickupRequestServiceResult`/`PickupRequestServiceError`) that
  the controller maps to an HTTP status.
- **Response shape.** Every error carries a top-level `message`. Successes wrap the payload in `data`,
  often alongside a `message`. Paged lists add `total`, `page`, `pageSize` and `totalPages`, and
  `Common/PageRequest.cs` does the clamping. The app reads `message` from error bodies
  (`util/ApiErrorUtils.kt`), so server error text is what users see, and it is English only.
  `ErrorHandlingMiddleware` maps `ArgumentException` to 400, `KeyNotFoundException` to 404 and
  `UnauthorizedAccessException` to 401.
- **Identity and roles.** Use `this.TryGetUserId(out var userId)` (`Common/ControllerBaseExtensions.cs`).
  The role is the string `Organization.Type` (`grocery` / `ngo` / `admin`), also issued as the JWT role
  claim. Endpoints that create activity carry `[RequireVerifiedOrganization]`, which reads the DB on
  every call. `DeletedAccountTokenCheck` runs on token validation and caches its answer for one minute.
- **Schema conventions.** Table names and column names are all lowercase (the column name is the
  property name lowercased, in a loop at the end of `OnModelCreating`). Enums are stored as lowercase
  strings. Because there are no migrations, adding, dropping or renaming a column also needs the matching
  SQL run against the database by hand.
- **Stock accounting.** A `ListingGroup` holds the totals for one posted item (`TotalAvailable`,
  `TotalReserved`, `TotalCompleted`, `TotalRemoved`). Reserving part of a listing splits off a child
  `ClearanceListing` in the same group with `Status = Reserved` (`CartService.ReserveListing`). Cart
  checkout is per grocery and creates one `PickupRequest` with many `PickupRequestItem`s, each holding a
  snapshot of the listing's title, unit and photo. `ClearanceListing` and `ListingGroup` use Postgres
  `xmin` as an optimistic concurrency token. Any code that changes quantities must keep the group totals
  consistent and handle `DbUpdateConcurrencyException`, usually by returning a Conflict.
- **Notifications have two paths, and a state change usually uses both:**
  1. `I{Pickup,Listing,Inventory,Admin}NotificationService` broadcast data events over SignalR to hub
     groups (`user_{id}`, `all_listings`, `listing_{id}`, `pickup_{id}`, `item_{id}`, `admins`). These keep
     open screens live.
  2. `PushNotificationService.SendNotificationAsync` sends user-facing notifications. It saves an inbox
     row, broadcasts it on `NotificationHub`, then pushes it over FCM. Each step can fail without
     stopping the others, and the client uses the inbox row's id to drop duplicates. With no
     `firebase-adminsdk.json`, push is skipped and the other two steps still run.

  `ListingsController.CreateListing` is a compact example of the full sequence.
- **Background jobs** live in `Jobs/NotificationJobs.cs` and are registered with cron expressions in
  `Program.cs`. A new job also needs an entry in the hard-coded `/api/health/jobs` list and in the README.

## Mobile architecture

The package is `com.clearchain.app`, split into `data` / `domain` / `presentation` layers wired with Hilt
(`di/`).

- **Two data-access styles.** The core entities (auth, listings, pickup requests, inventory,
  notifications, organizations) go through `domain/repository` interfaces and use cases. Their
  `*RepositoryImpl` writes API results to Room and falls back to the Room cache when the network fails.
  The other features (cart, disputes, messages, reviews, saved listings, admin, image analysis) inject the
  Retrofit `*Api` straight into the ViewModel.
- **Screen pattern.** Each feature folder has `XScreen`, `XViewModel`, `XState` and `XEvent`. `XState` is
  an immutable data class held in a `MutableStateFlow` and changed with `_state.update { it.copy(...) }`.
  `XEvent` is a sealed class handled by `onEvent()`. One-shot effects go through
  `Channel<UiEvent>` (`util/UiEvent.kt`: snackbar, navigate, navigate up, share file). Server-paged feeds
  use `util/FeedPager.kt`.
- **List screens.** A list screen's states go through `ListArea`/`ListAreaState`, which keeps loading,
  error and empty states inside the pull area. Pull is off during the first load and on errors.
- **Realtime.** `data/remote/signalr/SignalRService.kt` is a singleton that holds connections to all five
  hubs and exposes one `SharedFlow` per server event. ViewModels collect those flows.
  `RealtimeLifecycleObserver` connects when the app comes to the foreground and disconnects when it goes
  to the background. Joined rooms are replayed on reconnect.
- **Networking.** `util/Constants.kt` derives both `BASE_URL` and the SignalR root from the
  `API_BASE_URL` build config field. `AuthInterceptor` attaches the token and `TokenAuthenticator`
  refreshes it on a 401. `Json { ignoreUnknownKeys = true }`, so extra server fields are harmless, but a
  missing non-nullable field fails to parse.
- **Room** (`data/local/database/ClearChainDatabase.kt`) uses `fallbackToDestructiveMigration`. Changing
  an entity means bumping `version`, which wipes the cache, including the stored auth token, so every
  user is signed out once.
- **Strings.** Every user-visible string must exist in both `res/values/strings.xml` (en) and
  `res/values-vi/strings.xml` (vi). Counts use `<plurals>`. The language switches at runtime from
  Settings, which is why the bundle's language split is disabled.
- **Theme tokens.** `ui/theme` mirrors the ClearChain design system: `Color.kt` (with `ExpiryColors`
  and `OnBrandGradient`), `Type.kt` (plus the wordmark and eyebrow styles), `Shape.kt`, `Spacing.kt`,
  `Dimens.kt` (component sizes) and `Alpha.kt` (opacities). Each declaration's KDoc names its design
  system token. Shared components use these names rather than repeating dp, alpha or colour literals,
  so a change to a token is one edit.
- **System bars.** `MainActivity`'s outer `Scaffold` applies no system-bar insets. In `NavGraph`, ordinary
  routes use `insetComposable`, which pads for the status and navigation bars. The seven sign-in and
  first-run routes (Splash, Login, Register, ForgotPassword, EmailVerification, Onboarding,
  PendingReview) use plain `composable` and draw behind both bars.
- **Sign-in and first-run screens** are built on `BrandScaffold` (`presentation/components`). It takes
  either `BrandHeader.Hero`, the logo hero (Login only), or `BrandHeader.Compact`: a title, an optional
  back circle and onboarding's step bars. Content sits on a sheet with rounded top corners, and there's
  an optional fixed `bottomBar`. While a brand screen is shown, `LightStatusBarIcons` keeps the
  status-bar icons light. It counts its callers, so two brand screens overlapping in a crossfade don't
  fight over the icons. Splash uses `BrandGradient` and `LightStatusBarIcons` directly.
- **Form pieces.**
  - **Clear buttons.** `ClearChainTextField`, `TimePickerField` and `DatePickerField` name their clear
    button "Clear <label>". The label comes from `clearLabel`, then the field's own `label`, then the
    enclosing `FieldCard` (through `LocalFieldLabel`). When none of those exists, the button says
    "Clear text", "Clear time" or "Clear date". Pass `clearLabel` when two fields share one card, as the
    opening and closing hours do.
  - **Heights.** Buttons and fields have a 32dp minimum height (`heightIn(min = …)`), so they grow
    with large text. Don't put a fixed height back on them.
  - **One-time codes** use `CodeInputField`. It's a single text field drawn as cells, so screen
    readers hear one field.
  - **The status picture** (the icon in a disc inside two rings) is `StatusDisc`. `EmptyState` uses it.
  - **Address entry.** `AddressSuggestionField` shows its suggestions in the layout under the field,
    only while the field has focus, and only US addresses.
- **Launcher icon and splash.** The adaptive icon (`res/drawable/ic_launcher_*`) and the Android 12+
  system splash (`res/values-v31/themes.xml`, `res/drawable/splash_mark.xml`) use colours from
  `res/values/colors.xml`. Those colours copy `BrandTeal`, `BrandGreen`, and `OnBrandGradient` at
  `Alpha.LogoDisc`, so change them together with `Color.kt`. `splash_mark` draws the app splash's
  96dp disc with its 56dp leaf (`LogoDiscSplashSize`, `LogoMarkSplashSize`) at the screen's centre,
  so the system splash hands over to the app splash without a jump. Change the two together.

## Contracts that span both apps

- An API change is end to end. Update the backend DTO, the mobile DTO in `data/remote/dto`, the domain
  model, and the Room entity if the data is cached. Recent commits remove unused fields from all of these
  layers together.
- SignalR event names are plain strings, `SendAsync("ListingCreated", ...)` on the server and a matching
  handler in `SignalRService`. Rename or add them on both sides and in the README's hub table.
- Dispute reason keys in `Backend/.../DTOs/Disputes/DisputeReasons.cs` must match `NgoDisputeReason` in
  `Mobile/.../presentation/dispute/DisputeComponents.kt`. The server stores and returns keys and the app
  translates them. Nothing checks that the two lists match.
- Upload size and type limits in `Common/StorageBucketPolicy.cs` must match the Supabase bucket settings.

## Code style

The rules come from `.editorconfig` and `.gitattributes`. Files use LF line endings and 4-space indents.
Lines should stay under 120 columns, but this isn't enforced for Kotlin.

- **C#.** Use file-scoped namespaces, opening braces on their own line, `_camelCase` private fields, and
  an `I` prefix on interfaces.
- **Kotlin.** Star imports are the agreed style, there are no trailing commas, and `@Composable` functions
  are PascalCase. A file can hold several composables. The ktlint rule overrides appear in both
  `.editorconfig` and `Mobile/build.gradle.kts`, so change both together.
- **Comments** (commit `ff17ee3`):
  - Document declarations with doc comments (KDoc or XML `<summary>`) and keep `//` for notes inside
    method bodies.
  - Section dividers look like `// ── Title ───`, padded to column 80, in sentence case.
  - Don't write change-log comments ("NEW", "used to") or comments that repeat what the code says.
