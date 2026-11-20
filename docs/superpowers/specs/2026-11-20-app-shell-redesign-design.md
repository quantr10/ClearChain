# App shell redesign

**Date:** 2026-11-20 · **Branch:** `ui/shell` · **Status:** draft for approval

Covers the frame every signed-in screen sits in, in the Android app (`Mobile/`):

- the bottom bar for all three roles
- the headers: the three dashboards, Profile, the tab screens and the title row on detail screens
- snackbars
- empty, error and loading states

Before screenshots are in `D:\ClearChain-screens\before\2-grocery` to `5-admin`. The approved mockups
are in `2026-11-20-app-shell-redesign/` next to this file. They're fragments for the brainstorm viewer,
and also open in a plain browser (they load Google Fonts and Material Icons):

- `bottom-bar.html`: the bar (option A was chosen)
- `headers.html`: the dashboard and Profile headers (option A was chosen)
- `tab-titles.html`: tab screens and detail titles (option A was chosen)
- `states.html`: snackbars, empty and loading states (approved as shown)

## Intent

The sign-in redesign gave the first run one look. Once signed in, the app still reads as several apps:

- **Three dashboard gradients.** Grocery runs teal to green corner to corner, NGO runs green to teal,
  and admin builds its gradient from `primary`. That makes it flat teal in light and light teal in dark.
- **Different headers elsewhere.** Profile has a separate 248dp gradient hero. Most tab screens have no
  title at all, while Stats has an oversized one.
- **A bottom bar that fails in Vietnamese.** It has six tabs for NGOs, and "Trang chủ" wraps and is
  cut off at the screen edge.
- **"… refreshed" snackbars.** They cover content after every pull.

This work gives the signed-in frame the same family as the sign-in screens, built from the design
system's tokens and shared components. It fixes findings 3, 4 and 7 from
`D:\ClearChain-screens\before\README.md`, and the problems found while exploring that sit in the same
pieces (see *Findings map*).

## Goals

- **One gradient frame** for the three dashboards and Profile, the same one Login uses.
- **One bottom bar** with five tabs for every role.
- **One way to show a screen's title.**
- **One snackbar host.**
- **One pattern for list states**, which keeps loading, error and empty states inside the
  pull-to-refresh area.
- **Fix the three findings:**
  - **3:** the clipped Vietnamese tab bar.
  - **4:** the "… refreshed" snackbars.
  - **7:** the inconsistent headers.
- **Work everywhere:** light and dark, English and Vietnamese (Vietnamese runs about 30% longer), a
  720×1280 (360×640dp) screen, and font scales up to 200%.
- **Screen-reader labels say what each control does.**

## Non-goals

- **Findings from other sections:** 2, 5, 6, 8–11 and 16–23.
  - That includes the English count line ("0 listings", finding 2/20) and the hard-coded
    "org/orgs selected" on Verify. Both sit in the list headers this work touches, but their strings
    belong to finding 2.
- **TODO.md items.** None is about how these screens look. Two touch the same code and stay where they
  are:
  - the admin dashboard reloading alerts on events that can't change them
  - the session-expired toast after sign-out
- **Placeholder (skeleton) loading.** The three loading kinds in `LoadingIndicators.kt` stay.
- **Emoji in notification titles** ("🆕 New Food Available!"). They come from the server.
- **Snackbar actions.** No snackbar has one today, and none is added.
- **The design system's known contrast shortfalls** (12sp `primary` text, `outline-variant` borders).
  This work adds no new instance where an alternative exists.

## Anti-goals

The work fails, even if it looks right, if any of these happen:

- **Tab navigation breaks.** Watch:
  - each tab keeping its state and scroll when you switch away and back
  - Back from a tab root
  - Browse → Cart → Request pickup → Back → Back
  - the deep links `clearchain://browse`, `clearchain://ngo/inventory`, `clearchain://ngo/requests`,
    `clearchain://admin/*` and `clearchain://profile`
- **Status-bar icons on the wrong background.** Light icons end up on a settled plain screen, or dark
  icons on the gradient. During a crossfade between the two, the icons switch once, at the midpoint
  (see *Status bar*).
- **Login looks different.** It must look exactly as it does on `main`.
- **Text clips or truncates** in the bar, a header, a title or an empty state, in Vietnamese or at 200%.
- **Pull-to-refresh stops working** anywhere it works today.
- **A failure snackbar disappears.** Only the "refreshed" success messages go.
- **dp, colour or alpha literals reappear** in the changed components and lines.

## Constraints

- **UI only, with four exceptions:**
  - **Finding 4** deletes the "refreshed" snackbar sends in 13 ViewModels (listed under
    *Snackbars*). It needs nothing else from them.
  - **Browse reloads its cart** when it comes back on screen. That's one new event in
    `BrowseListingsEvent` that calls the existing `loadCart()` (see *Cart moves under Browse*). The
    user asked for it.
  - **`BrowseListingsState.cartItemCount`** goes if nothing reads it once the badge counts items (see
    *Calls I made*). It's a computed property.
  - **`NavGraph`** changes which routes pad the status bar, hides the bar on Cart, and passes a few new
    callbacks.

  Every other ViewModel, `*State`, `*Event`, use case, API call and DTO stays as it is. New buttons
  send existing events or navigate.
- **Tokens.** Use `ui/theme` and `presentation/components`.
  - New and rewritten components contain no dp, colour or alpha literals.
  - In other files, the lines this work changes follow the same rule. Their other literals are out of
    scope.
  - A missing token goes in `Dimens.kt`, `Spacing.kt` or `Alpha.kt`, with KDoc naming its
    design-system token, and into the design system (see *Design system update*).
- **Strings.**
  - Every new or changed string goes in both `res/values/strings.xml` and `res/values-vi/strings.xml`.
  - Counts use `<plurals>`.
  - Keys that become unused are removed, after a search confirms nothing else uses them.
- **No automated tests.** Verify by build, `spotlessCheck`, `lint` and running the app (see
  *Verification*).
- **Live data.** `DATABASE_URL` is the live Supabase database.
- **Code style.** Star imports, no trailing commas, KDoc on declarations, and `// ── Title ───`
  dividers.
- **Docs.**
  - `CLAUDE.md`'s *System bars* paragraph changes (see *Status bar*).
  - The design-system mirror rule means the artifact gets the same changes.

## Approaches

1. **Extend `BrandScaffold` (chosen).**
   - Login's hero already does what the dashboards need: a gradient header that scrolls away, a sheet
     with rounded top corners, a teal strip pinned behind the status bar, and light status-bar icons.
   - The hero mode gains a header slot and optional pull-to-refresh. Login passes its logo, each
     dashboard its greeting, and Profile its avatar block.
   - The bar, snackbar host, list states and title row become shared pieces in `presentation/components`,
     then the screens move onto them.
   - Trade-off: it touches the Login path finished on `main`, so Login needs a re-check.
2. **A new `GradientHeaderScaffold` beside `BrandScaffold`, sharing its private helpers.** Rejected:
   two scaffolds would lay out nearly the same frame, which is how finding 7 happened.
3. **Restyle `DashboardWelcomeHeader` and `ProfileHero` in place.** Rejected: it keeps three separate
   gradient builds.

## Decisions made with the user

- **Five tabs for NGOs.** Cart leaves the bar and is reached from the cart button on Browse.
- **Bar option A.**
  - The selected tab's pill is `primary` at `tile-tint` with a `primary` icon.
  - The selected label is `on-surface` SemiBold.
  - The bar is plain `surface` with a hairline top edge, 64dp tall.
- **Header option A.**
  - **Gradient:** the dashboards and Profile use the brand gradient behind the status bar.
  - **Sheet:** the content sits on a sheet with rounded top corners.
  - **What sits where:** only the greeting, bell and avatar sit on the gradient. The role eyebrow and
    subtitle move to the top of the sheet.
- **Tab-title option A.**
  - **List tabs:** no visible title. The search row is the header, and a pane title names the screen
    for screen readers.
  - **Stats:** it gets the standard title row.
- **Snackbars, empty states and loading** were approved as shown in `states.html`.
- **Approach 1** (extend `BrandScaffold`).
- **Browse reloads its cart** when it comes back on screen, a small ViewModel change.
- **Verification is left out of the plan.** The user checks the screens and captures the after set.

## Design

### Bottom bar (finding 3)

`presentation/navigation/BottomNavBar.kt`, reworked. Material's `NavigationBar` has an 80dp minimum
height and a fixed 64×32 indicator, so the builder will likely need a custom row. Navigation
behaviour stays exactly as today: `popUpTo(start) { saveState = true }`, `launchSingleTop`,
`restoreState`, the admin Stats tab's `navRoute`, and Profile staying selected on its sub-routes.

- **Tabs.**
  - **Grocery:** Home, Listings, Add, Requests, Profile. Unchanged.
  - **NGO:** Home, Inventory, Browse, Requests, Profile. Cart is removed.
  - **Admin:** Home, Verify, Stats, History, Profile. Unchanged.
- **Bar.**
  - **Fill and edge:** `surface`, with a `BorderWidth` top edge in `outlineVariant`. There's no tonal
    elevation, so the teal wash goes. That lifts the unselected labels from 4.4:1 to 4.8:1 in light.
  - **Height:** at least the new `BottomBarHeight` (64dp). It grows when labels wrap.
  - **Width:** tabs share it equally, with no gaps between them.
  - **System inset:** the bar still fills the navigation-bar inset below it, as today.
- **Tab.**
  - **Layout:** the icon in its indicator, then the label below, centred.
  - **Icon:** `ControlIconSize` (18dp). The selected tab shows the Filled icon, the others Outlined
    icons, as today.
  - **Indicator:** a pill of `NavIndicatorWidth` × `NavIndicatorHeight` (48 × 26dp, new tokens). When
    selected, it's filled with `primary` at `Alpha.TileTint` over `surface`, and the icon is `primary`.
    When unselected, it has no fill and the icon is `onSurfaceVariant`.
  - **Label:** `labelSmall`. Selected labels are `onSurface` SemiBold. Unselected labels are
    `onSurfaceVariant`.
- **Label size rule.**
  - All labels in a bar share one size, the user's scaled `labelSmall`, as long as every label fits in
    at most two lines at its tab's width.
  - If one doesn't fit, all of them use the largest size at which every label fits. They never go
    below `labelSmall`'s unscaled size.
  - Labels break only between words, are centred, and are never cut off or ellipsized.
  - At 200%, Vietnamese stays at full size on two lines. English settles near 15sp, because
    "Inventory" and "Requests" are single words.
- **Semantics.**
  - The icon has no content description.
  - Each tab is one node: the label, role Tab, its selected state, and its position ("Home, tab, 1 of
    5, selected").
  - The tabs are in a `selectableGroup`. Today each tab is read twice, because the icon and the label
    both say "Home".

### Cart moves under Browse

- **`NavGraph`.**
  - The `Cart` route calls `onShowBottomBar(false, OrganizationType.NGO)`, as `CartPickup` does.
  - `CartScreen` gains an `onNavigateBack`.
  - The route and its arguments don't change.
- **`CartScreen`.**
  - It gets a `ScreenTitleRow` titled "Cart" (`nav_cart`) with a back button, above its search row.
  - Request pickup → Back returns to Cart, and Back again returns to Browse.
- **Browse's cart button.**
  - **Badge:** it shows the number of listings in the cart, `state.cartItemsByListingId.size`, capped
    at "99+" like the bell. Today it adds up requested quantities, so 12 kg of one item shows "12".
  - **Name:** today it's a hard-coded English "Cart". It becomes "Open cart" when the cart is empty
    (`cd_open_cart`), and "Open cart, 3 items" otherwise (plurals `cd_open_cart_items`). The badge is
    hidden from accessibility, because the name carries the count.
- **Browse reloads its cart when it comes back on screen.**
  - **Today:** `BrowseListingsViewModel` loads the cart only at init and on live listing events. After
    Browse → Cart → remove an item → Back, the badge, its spoken name and the cart steppers on the
    listing cards still show the old cart. Now that Cart is reached only through Browse, this would be
    noticed.
  - **Change:** add one event, for example `BrowseListingsEvent.ReloadCart`, handled by calling the
    existing `loadCart()`. `BrowseListingsScreen` sends it when the screen resumes. That covers Back
    from Cart, from listing detail, and the app returning from the background.
  - **Don't double the first load.** Skip the resume that follows the first composition, because init
    has just loaded the cart.

### Gradient frame: `BrandScaffold` hero (finding 7)

`presentation/components/BrandScaffold.kt`. The hero mode becomes general. The API is the builder's
call; one option is `BrandHeader.Hero(content)`, with Login passing a `LogoHero()` that holds today's
disc and wordmark.

- **The scaffold still owns:**
  - the `BrandGradient` behind the status bar
  - `statusBarsPadding` inside the gradient
  - the sheet (`shape-extra-large` top corners, `background` fill, `SheetOverlap`, sheet padding,
    `ListGap` stacking)
  - the `heightIn` that keeps gradient from showing under short content
  - the `BrandTeal` strip pinned behind the status bar
  - `LightStatusBarIcons()`
  - the snackbar host (now `ClearChainSnackbarHost`, see below)
- **The slot owns** its own content, alignment and inner padding. Login's slot keeps its spacing
  exactly. The dashboard and Profile slots use the compact header's rhythm: `ScreenVPadding` above,
  and `ScreenHPadding` at the sides and below, before the overlap.
- **Pull-to-refresh.**
  - New optional `isRefreshing` / `onRefresh` (and an `enabled` flag, see *List states*) wrap the
    hero's scroll in `HapticPullToRefreshBox`.
  - The indicator appears below the status bar, not under it.
  - Login passes none.
- **Insets.** With the bottom bar showing, the outer `Scaffold` has already consumed the
  navigation-bar inset. The existing `KeyboardOrNavigationBar` padding then adds nothing, and the sheet
  ends at the bar.

#### Dashboards

`GroceryDashboardScreen`, `NgoDashboardScreen` and `AdminDashboardScreen` move onto the hero.
`DashboardWelcomeHeader` and its `gradientColors` parameter are removed. `AvatarHeader.kt` keeps
`AvatarImage` and `OverlayAvatar`.

- **Hero slot.** This is a new `DashboardGreeting` in `presentation/components`. It's one row:
  - **Greeting:** "Hi, %1$s" (`greeting_hi`), `headlineSmall` Bold in `OnBrandGradient`. It takes the
    remaining width (`weight(1f)`), wraps onto as many lines as it needs, and is a `heading()`. Long
    names break across lines rather than pushing anything off screen. This fixes the Vietnamese NGO
    header, where "Xin chào, AmericanRedCross" pushes the bell and avatar off screen today.
  - **Bell:**
    - It's a `BackButtonSize` (36dp) disc in `OnBrandGradient` at `Alpha.LogoDisc`, with the bell in
      `OnBrandGradient`. Today the bell is a 24dp disc at `0.18f`.
    - It keeps its error badge, capped at "99+".
    - Its name is "Open notifications" with no unread, and "Open notifications, 5 unread" otherwise
      (plurals `cd_open_notifications_unread`). The badge is hidden from accessibility.
    - `NotificationBell` gains whatever parameters the gradient style needs, or the greeting builds
      its own around `NotificationBellViewModel`. That's the builder's choice.
  - **Avatar:**
    - It's `AvatarImage` at the new `HeaderAvatarSize` (40dp, today 48), with a `AvatarRingWidth`
      (2dp, new token) ring in `OnBrandGradient`.
    - It still opens Account details, and its name says so: "Open account details"
      (`cd_open_account_details`). Today it reads "Profile picture of …", or just the initial.
  - The bell and avatar sit side by side, `CardPadding` apart. Today they're stacked.
- **Top of the sheet:**
  - the role eyebrow (`role_grocery`, `role_ngo`, `role_admin`) in `EyebrowStyle`, uppercase,
    `onSurfaceVariant`
  - the subtitle (`grocery_dashboard_subtitle`, `ngo_dashboard_subtitle`, `system_health`) in
    `bodyMedium`, `onSurfaceVariant`
  - then the sections, as today

  Both texts move off the gradient, where they reached only 2.5–3.0:1.
- **All three roles** get the same gradient. NGO's reversed gradient and admin's `primary` gradient
  are gone.
- **Loading.** The hero draws at once. On the first load, the area spinner sits in the sheet under the
  eyebrow and subtitle, in place of today's `LoadingState(…padding(vertical = 64.dp))`.
- **Refresh.** Pull-to-refresh works as today, through the scaffold, and is disabled during the first
  load.
- **Pane title:** "Home" (`nav_home`).

#### Profile

`ProfileScreen` moves onto the hero. The private `ProfileHero` becomes the slot content.

- **Layout:** centred, wrapping its content. The fixed 248dp height goes.
- **Avatar tile:** `ProfileAvatarSize` (64dp, new token; today 84), `MaterialTheme.shapes.large`
  corners, `OnBrandGradient` at `Alpha.LogoDisc`. Its camera badge is `ActionIconButtonSize`, and its
  click and label behave as today.
- **Name:** `headlineSmall` Bold in `OnBrandGradient`.
- **Chips:** the type chip and the Verified chip stay as today, built with tokens.
- **The sheet** holds the action cards as today.
- **Loading.** While the user loads, the frame shows with an empty hero and the area spinner in the
  sheet. The error state shows in the sheet the same way.
- **Pane title:** "Profile" (`nav_profile`).

#### Status bar

Today every signed-in route uses `insetComposable`, which applies `statusBarsPadding()` and
`navigationBarsPadding()`.

- The four gradient routes (`GroceryDashboard`, `NgoDashboard`, `AdminDashboard`, `Profile`) stop
  padding the status bar, so the gradient runs behind it. They keep the navigation-bar padding.
- Every other signed-in route looks exactly as it does now.
- Nothing may jump during tab switches or crossfades.
- **Icons during a crossfade.** Navigation's default 700ms fade stays.
  - Today `LightStatusBarIcons` claims light icons as soon as a screen composes and releases them when
    it's disposed.
  - Listings → Home would turn the icons light while Listings is still almost fully visible.
    Home → Listings would keep them light over the incoming plain screen for the second half of the
    fade.
  - **Requirement:** between a gradient screen and a plain one, the icons switch once, at the fade's
    midpoint. Between two gradient screens (or two sign-in screens), they stay light throughout, as
    the counting already ensures.
  - One mechanism: the destination passes its `AnimatedContentScope` transition to
    `LightStatusBarIcons`. The light claim then counts only while that screen is the more visible one.
    The builder picks.
- **`CLAUDE.md` *System bars*:**
  - the seven sign-in routes draw behind both bars
  - the four gradient routes draw behind the status bar only
  - all other routes pad both

### Tab screens and titles

- **List tabs** (`MyListingsScreen`, `ManageRequestsScreen`, `BrowseListingsScreen`, `InventoryScreen`,
  `MyRequestsScreen`, `VerificationQueueScreen`, `TransactionsScreen`) get no visible title. Their
  search row stays the first row.
- **Pane titles.** Each tab root sets `semantics { paneTitle = … }` to its tab's label: Listings,
  Requests, Browse, Inventory, Verify, History, Stats, Home and Profile. TalkBack then announces the
  screen when the tab opens. Reusing the `nav_*` strings means the spoken name matches the tab the user
  tapped.
- **Stats** (`AdminAnalyticsScreen`).
  - The private `AnalyticsHeader` (`titleLarge` Bold, inside the list) is replaced by
    `ScreenTitleRow(title = stats_analytics_title, onBack = null)`, with the PDF export button in its
    `actions` slot.
  - The row sits above the loading, error and content states, so the title shows in all of them.
  - The export button keeps its disabled-while-exporting behaviour.

#### `ScreenTitleRow`

- **Title:**
  - `titleMedium` SemiBold, as today.
  - It no longer has `maxLines = 1` or an ellipsis, and it wraps onto as many lines as it needs.
  - It's a `heading()`.
- **Back button:**
  - It stays aligned with the title's first line when the title wraps.
  - Its label stays "Back".
  - It stays a `BackButtonSize` circle in `surfaceVariant` with a 48dp touch area.
  - Its arrow uses the new `TitleIconSize` (20dp, the design system's screen-title icon).
- **Spacing:**
  - `CardPadding` between the back button and the title.
  - Vertical padding from a token. The builder names it if none fits.
- **Gutter.** Callers place it inside the `ScreenHPadding` gutter. `NotificationInboxScreen` passes
  8dp today, so its back button sits closer to the edge than on every other screen.
- **Notifications action row.** "Mark all as read" and "Clear all notifications" move into a `FlowRow`
  aligned to the end, so they wrap at large text or in Vietnamese instead of running off the edge.

### Snackbars (finding 4)

- **Remove the "refreshed" success snackbars.** The pull indicator already shows when a refresh is
  done. Delete the send in each of these places:
  - `PublicProfileScreen.kt:96`
  - `MyListingsViewModel.kt:187`
  - `ManageRequestsViewModel.kt:135`
  - `GroceryDashboardViewModel.kt:84`
  - `NgoDashboardViewModel.kt:115`
  - `MyRequestsViewModel.kt:166`
  - `InventoryViewModel.kt:198`
  - `BrowseListingsViewModel.kt:318`
  - `AnalyticsScreen.kt:78`
  - `VerificationQueueViewModel.kt:249`
  - `AdminDashboardViewModel.kt:153`
  - `TransactionsViewModel.kt:213`
  - `AdminAnalyticsViewModel.kt:108`

  Tidy any `ok` or `isRefresh` value that becomes unused. Failure snackbars stay, including the
  dashboards' partial-failure message. The live-update snackbars stay too (new organization,
  completed transaction, new dispute, system alert).

  The code sends these only after a pull. The screenshots that show them "on open" most likely caught
  a pull triggered while scrolling back to the top, since one NGO screenshot shows the pull spinner.
  Either way, the fix is the same.
- **`ClearChainSnackbarHost`** (new, `presentation/components`).
  - It wraps `SnackbarHost` with a `Snackbar` in `inverseSurface` / `inverseOnSurface` on
    `MaterialTheme.shapes.extraSmall`.
  - Messages wrap onto as many lines as they need.
  - It replaces all 25 `SnackbarHost(…)` calls, including `BrandScaffold`'s two and `FabScaffold`'s.
  - Placement doesn't change: it sits above the bottom bar, and covers the FAB on FAB screens.
  - Durations stay as each call site sets them.

### List states

#### One pattern for loading, error, empty and list

`VerificationQueueScreen` already does this right. Its loading and empty states are `LazyColumn` items
with `fillParentMaxSize()`, inside the pull box. Everywhere else, these states sit outside the pull
box, so an empty or failed list can't be pulled to refresh.

- **The rule.** A list screen's loading, error and empty states sit inside the same pull-to-refresh
  area as its list. They fill that area, and they scroll when their content is taller than the area,
  so pull works and large text doesn't clip.
- **Shared wrapper.** A wrapper in `presentation/components` owns this. Name and API are the builder's.
- **Where it applies:**
  - `MyListingsScreen`
  - `ManageRequestsScreen`
  - `BrowseListingsScreen` (list view; the map view is unchanged)
  - `InventoryScreen`
  - `MyRequestsScreen`
  - `TransactionsScreen`
  - `VerificationQueueScreen` (moves onto the wrapper)
- **Pull during the first load.** Pull is disabled while the first-load spinner shows (the area
  spinner, before any data). `HapticPullToRefreshBox` gains `enabled`. This also applies to the
  dashboards. It removes the trigger TODO.md names for the dashboard activity race, where a pull
  overlaps the first load. The `FeedPager` fix itself stays in TODO.md.
- **Error states can't be pulled.** Pull is disabled on them, as during the first load, and their
  Retry button sends the existing load event, as today. The refresh functions don't handle `error`
  the way a pull from an error would need:
  - On Inventory, MyRequests and Browse, `refresh…()` clears `error` at the start and never sets it
    again. A pull would briefly show the "nothing at all" empty state, and keep showing it if the
    refresh failed.
  - On MyListings and ManageRequests, refresh never clears it.

  Changing those ViewModels is out of scope. Empty states and lists stay pullable.
- **Cart** has no refresh today and gets none. Its states only fill and scroll.
- **Notifications** keeps its layout. Its empty state gets the scrolling `EmptyState`.

#### Count and sort row

`ResultsCountAndSort` is hidden while the visible list is empty, including during the first load.
Search and the filter chips stay, so the user can still change what they're looking at. This applies
on MyListings, ManageRequests, Browse, Inventory, MyRequests, Verify, Transactions and Cart.

#### `EmptyState`

- **Look:** unchanged: `StatusDisc`, `titleMedium` SemiBold title, `bodyMedium` subtitle and an
  optional filled button.
- **Tokens:**
  - The 40dp padding becomes `EmptyStatePadding` (new).
  - The 160dp minimum button width becomes `EmptyActionMinWidth` (new).
  - The 16dp and 8dp gaps use spacing tokens. The builder adds one if none fits.
- **Scrolling:** it scrolls when its content is taller than its area, and is centred otherwise.
- **A next step where one is obvious.** Each is a button on the empty state that does something the app
  already does:
  - **Browse, nothing nearby** (`allListings` empty, not saved-only): "Change location"
    (`empty_action_change_location`). It opens the existing location sheet.
  - **Inventory, nothing at all** (not a filter result): "Browse food" (`empty_action_browse_food`).
    It goes to the Browse tab with the bar's navigation options. `InventoryScreen` gains an
    `onBrowseFood` callback from `NavGraph`.
  - **NGO Requests, nothing at all:** "Browse food", wired the same way through `MyRequestsScreen`.
  - **Everything else gets no button.** Filter-empty states have the chips right above. Grocery
    Listings has its create button, and Cart has its back button.

#### Loading

- **The three kinds stay** (`InlineSpinner`, `LoadingState`, `UpdatingBar`), with their rules in
  `LoadingIndicators.kt`.
- **Tokens:**
  - The `LoadingDefaults` sizes move to `Dimens.kt` as `SpinnerInlineSize` (16dp),
    `SpinnerInlineStroke` (2dp), `SpinnerAreaSize` (40dp) and `UpdatingBarHeight` (2dp).
  - `LoadingState`'s 12dp and 16dp become `CardPadding` and `ScreenHPadding`.
  - `LoadingDefaults` goes, or forwards to the tokens. That's the builder's choice.
- **Delay.** `LoadingState` draws nothing for its first 300ms, so a fast load from the Room cache
  doesn't flash a spinner. It still takes its space, so nothing jumps.
- **Spoken name:** "Loading" (`cd_loading`).
- **Retry label.** "Retry" (`retry`) everywhere. The two analytics screens use `action_retry` ("Try
  again") today. `action_retry` is removed if nothing else uses it.

### Tokens to add

| Kotlin | Value | Design-system token | Used by |
|---|---|---|---|
| `BottomBarHeight` | 64dp (minimum) | `bottom-bar-height` | Bottom bar |
| `NavIndicatorWidth`, `NavIndicatorHeight` | 48, 26dp | `nav-indicator-width`, `nav-indicator-height` | Selected-tab pill |
| `HeaderAvatarSize` | 40dp | `header-avatar` | Dashboard greeting avatar |
| `AvatarRingWidth` | 2dp | `avatar-ring` | Avatar ring on the gradient and on photos |
| `ProfileAvatarSize` | 64dp | `profile-avatar` | Profile hero tile |
| `TitleIconSize` | 20dp | `title-icon` | `ScreenTitleRow` back arrow |
| `SpinnerInlineSize`, `SpinnerInlineStroke` | 16, 2dp | `spinner-inline`, `spinner-inline-stroke` | `InlineSpinner` |
| `SpinnerAreaSize` | 40dp | `spinner-area` | `LoadingState` |
| `UpdatingBarHeight` | 2dp | `updating-bar` | `UpdatingBar` |
| `EmptyStatePadding` | 40dp | `empty-state-padding` | `EmptyState` |
| `EmptyActionMinWidth` | 160dp | `empty-action-min-width` | `EmptyState` button |

KDoc that widens:

- **`BackButtonSize`:** also the bell disc on the gradient header.
- **`Alpha.TileTint`:** also the selected tab's indicator.
- **`Alpha.LogoDisc`:** also the bell disc and the Profile avatar tile.

`HeaderPadding` is removed if it ends up unused. The builder may add a spacing token for a gap that no
token fits, following the same rule.

### Strings

New keys, in both files:

| Key | en | vi |
|---|---|---|
| `cd_open_notifications_unread` (plurals) | one: Open notifications, %d unread · other: Open notifications, %d unread | other: Mở thông báo, %d chưa đọc |
| `cd_open_account_details` | Open account details | Mở chi tiết tài khoản |
| `cd_open_cart` | Open cart | Mở giỏ hàng |
| `cd_open_cart_items` (plurals) | one: Open cart, %d item · other: Open cart, %d items | other: Mở giỏ hàng, %d mặt hàng |
| `empty_action_change_location` | Change location | Thay đổi vị trí |
| `empty_action_browse_food` | Browse food | Duyệt thực phẩm |
| `cd_loading` | Loading | Đang tải |

`cd_open_notifications` stays, for zero unread. The pane titles and the Cart title reuse `nav_*`.

Remove, after a search confirms nothing else uses each one:

- `snack_listings_refreshed`, `snack_inventory_refreshed`, `snack_stats_refreshed`,
  `snack_dashboard_refreshed`, `snack_profile_refreshed`, `snack_orgs_refreshed`,
  `snack_transactions_refreshed`, `snack_requests_refreshed`
- `action_retry`

### Findings map

| Finding | Fixed by |
|---|---|
| 3 Clipped Vietnamese tab bar | Five tabs for NGOs, 64dp bar, shared label size that wraps to two lines and shrinks only as far as needed |
| 4 "… refreshed" snackbars | The 13 sends removed, with their strings |
| 7 Inconsistent headers | One `BrandScaffold` hero for the three dashboards and Profile, same gradient behind the status bar, `gradientColors` removed |

Also fixed, found while exploring:

- **The Vietnamese NGO header** pushes the bell and avatar off screen.
- **Tabs are read twice** by screen readers.
- **The bell and avatar names** don't say what they do. The avatar opens Account details.
- **Browse's cart button** is called "Cart" in English, and its badge counts quantities.
- **Empty and error lists can't be pulled** to refresh.
- **Detail titles are cut off** with "…".
- **Notifications' action row** can run off the edge, and its back button sits off the gutter.
- **"Retry" and "Try again"** label the same button.

## Verification

The implementation plan leaves verification out, at the user's request. The user runs these checks
and captures the after set. The plan's tasks stop at a clean build, `spotlessCheck` and `lint`, and
never sign in to the live database.

- **Build and lint.**
  - From `Mobile/`, `./gradlew assembleDebug spotlessCheck lint`, with no new lint warnings.
  - A search of the changed components finds no `.dp` literals, `Color(` / `Color.White`, or `alpha =`
    with a number. The same goes for the changed lines elsewhere.
- **Visual matrix.** Use the 720×1280 Android 16 emulator from the before set. In dark-en, light-en and
  light-vi, check:
  - the three dashboards
  - Profile
  - every tab screen
  - Cart
  - one detail screen per role
  - an empty list and an error list
  - a snackbar

  At font scale 1.3 and 2.0, in English and Vietnamese, check the bar for all three roles, the NGO
  dashboard, Profile, Stats and one empty state. Nothing clips, overlaps or truncates.
- **Login.** It looks the same as on `main` in all three variants, including the pinned strip after
  scrolling.
- **Status bar.**
  - Light icons on the dashboards and Profile, theme icons everywhere else.
  - Switch between Home and Listings, and between Profile and Requests, in both directions. The icons
    switch once, about halfway through the fade.
- **TalkBack.**
  - A tab reads "Home, tab, 1 of 5, selected", once.
  - Opening a list tab announces its name.
  - The bell says "Open notifications, 5 unread".
  - The avatar says "Open account details".
  - The Browse cart button says "Open cart, 2 items".
  - Titles are headings.
  - The area spinner says "Loading".
- **Pull-to-refresh.**
  - It works on every screen that has it today, and on empty lists.
  - It's off during the first-load spinner and on error states, where Retry still works.
  - No "… refreshed" snackbar appears.
  - A refresh that fails still shows its message.
- **Navigation.**
  - Each tab keeps its state when you leave it and come back.
  - Browse → Cart → Request pickup → Back → Back returns to Browse.
  - Browse → Cart → remove an item → Back: the badge, its spoken name and the card steppers show the
    new cart.
  - "Browse food" and "Change location" do what they say.
  - Each deep link listed under *Anti-goals* opens the right tab with the bar showing.
- **After set.** Capture `D:\ClearChain-screens\after\2-grocery` to `5-admin` with the same file names
  as the before set. Cart's files keep their names, though it's no longer a tab.

## Design system update

The theme mirrors the design system, so the artifact
(`https://claude.ai/artifact/W5MSxyo1uFx8jmaEF2iCuS`) gets these changes, published only once the user
OKs it:

- **`tokens.json`:** the tokens in the table above, and the widened usage notes for `back-button`,
  `tile-tint` and `logo-disc`.
- **`BottomNavBar` README:**
  - five tabs per role
  - the new anatomy and colours
  - the label size rule
  - semantics

  The three "Watch for" items are resolved.
- **`DashboardHeader` README:** it becomes the greeting slot on the brand frame, with the eyebrow and
  subtitle on the sheet. The flat admin header and the subtitle contrast items are resolved.
- **`ScreenTitleRow` README:** the title wraps, it's a heading, and the gutter rule.
- **`EmptyState` README:** scrolling, the inside-the-pull-area rule, and the next-step buttons.
- **New pages:** `ClearChainSnackbarHost`, and the list-state wrapper.
- **`README.md`:**
  - *Colour, The gradient:* one gradient, vertical, behind the status bar on the dashboards and
    Profile. The admin exception is removed.
  - *Colour, Selection:* the bar uses the teal tint, so the "blue and teal at once" line goes.
  - *Spacing and layout:* dashboards, the bottom bar and lists, including the empty-list rules.
  - *States:* the spinner delay and the retry label.

## Left to the builder

- `BrandScaffold`'s hero API, and how Login's logo block moves into it.
- Whether the bar wraps Material's `NavigationBar` or is a custom row, as long as the spec above holds.
- How the label size rule measures, for example with `TextMeasurer`.
- The list-state wrapper's name and API.
- How `NotificationBell` takes the gradient style.
- The `NavGraph` mechanism for the four gradient routes, as long as nothing jumps.
- Names of any extra spacing tokens.
- The exact spinner delay, about 300ms.

## Calls I made

These weren't discussed with the user. Check them while reading.

1. **The bell grows to a 36dp disc and the avatar shrinks to 40dp**, side by side, so they match the
   back circle on the sign-in headers and leave the greeting more width.
2. **Pane titles reuse the tab labels**, so the screen reader says "Verify", not "Verification queue".
3. **Cart hides the bottom bar** like other screens opened from a tab, and gets a back button.
4. **The cart badge counts listings, not quantities.** `cartItemCount` (the quantity sum) is removed
   if nothing else reads it.
5. **Pull-to-refresh is disabled during the first load.** That's new behaviour, but it keeps the
   moved loading states from creating a second load in flight.
6. **Stats' title stays visible while loading and on error**, because the title row moves above the
   states.
7. **Profile shows its frame while the user loads**, instead of a plain full-screen spinner, so
   nothing jumps when the user arrives.
8. **Notifications' action row wraps**, and its title row moves to the standard gutter.
9. **"Retry" wins over "Try again"**, because 13 screens already say it.
10. **The spinner waits about 300ms** before drawing.
11. **The count and sort row hides during the first load too**, since the list is empty then.
12. **No "Browse food" on an empty Cart.** Its back button already goes to Browse.

These came from the reviewer's read of the design:

13. **Error states aren't pullable.** The mockup said empty and error states both move inside the
    pull area. Empty states do. Error states keep Retry, with pull disabled, because the existing
    refresh functions would show a false "nothing at all" state, and fixing that means changing five
    ViewModels.
14. **Status-bar icons switch at the crossfade's midpoint**, and the 700ms fade stays.
