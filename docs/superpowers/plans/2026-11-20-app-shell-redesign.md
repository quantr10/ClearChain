# App Shell Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the signed-in frame of the app: the bottom bar, the dashboard and Profile headers, the
tab and detail titles, snackbars, and the empty, error and loading states. Fix findings 3, 4 and 7
along the way.

**Architecture:**
- **Each task is one shared component or one screen.**
- **Components come first** (Tasks 1–12): list states, the snackbar host, the title row, the bottom
  bar, a general hero mode in `BrandScaffold`, the `NavGraph` destination helper, and the dashboard
  greeting.
  - A component task also makes the one-for-one swaps it needs at every call site: the snackbar host,
    the title row's gutter token, `LoadingDefaults`, and Login's hero call. That's so the app keeps
    compiling.
  - A component task also updates the `CLAUDE.md` paragraph that describes it.
- **Screens follow** (Tasks 13–28). Each screen task changes the screen, its route in `NavGraph`, and
  its ViewModel's "refreshed" snackbar line.

**Tech Stack:** Kotlin 2.4.20, Jetpack Compose (BOM 2026.06.01) with Material 3 1.4.0, Navigation Compose
2.9.8, Hilt. `minSdk` 26, `targetSdk` 34, `compileSdk` 36.

**Spec:** `docs/superpowers/specs/2026-11-20-app-shell-redesign-design.md`. The approved mockups are in
`docs/superpowers/specs/2026-11-20-app-shell-redesign/`.

**Path shorthand:** `K/` is `Mobile/app/src/main/java/com/clearchain/app/`, and `R/` is
`Mobile/app/src/main/res/`.

**Not in this plan:**
- **Tests, builds, lint, screenshots and reviews.** The repo has no automated tests, and the user asked
  for these steps to be left out. The spec's *Verification* section lists the checks for later.
- **The design-system artifact update.** It's published separately, with the user's OK.

## Global Constraints

- **UI only.** No change to any ViewModel, `*State`, `*Event`, use case, repository, API or DTO, except:
  - each screen task deleting its ViewModel's "refreshed" snackbar send (finding 4)
  - Task 22 adding `BrowseListingsEvent.ReloadCart` and its handler, and removing
    `BrowseListingsState.cartItemCount`
- **No literals.** New and rewritten components contain no dp, colour or alpha literals.
  - Every other line a task changes follows the same rule.
  - A new token goes in `K/ui/theme/Dimens.kt` or `K/ui/theme/Alpha.kt`, with a KDoc naming its
    design-system token in backticks, as the existing ones do.
- **Strings.**
  - Every new string goes in both `R/values/strings.xml` and `R/values-vi/strings.xml`, with the exact
    text given in the task, which is the spec's *Strings* table.
  - Counts use `<plurals>`.
  - A key a task leaves unused is deleted from both files, after a repo-wide search confirms nothing
    else uses it. Each task names the keys it should expect to delete.
- **Code style.**
  - Star imports, no trailing commas, KDoc on every new declaration.
  - Section dividers look like `// ── Title ───`, padded to column 80, in sentence case.
  - LF line endings, 4-space indents, lines under 120 columns.
  - No change-log comments.
- **Every screen** works in light and dark, in English and Vietnamese (about 30% longer), at 360×640dp,
  and at font scale 2.0.
- **Screen-reader labels** say what the control does.
- **Commits.**
  - Each task ends with one commit on `ui/shell`.
  - The subject is a sentence-case imperative, followed by a short body.
  - It ends with:
    ```
    Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
    ```

## Shared recipes

Screen tasks point to these by name.

### Refresh snackbar removal (finding 4)

- In the screen's ViewModel, delete the `_uiEvent.send(UiEvent.ShowSnackbar(… snack_*_refreshed …))`
  line.
- Where that leaves `val ok = …` unused, call the loader without the `val`.
- Failure sends and live-update sends stay.

### List screen on `ListArea`

1. **Map the screen's content `when` to a `ListAreaState` (Task 4), in this order.** Each branch
   keeps its existing icon and strings.
   1. **First load** (`isLoading` and nothing loaded): `Loading`.
   2. **Error and nothing loaded:** `Error(stringResource(R.string.error_generic), state.error, { <the existing load event> })`.
   3. **Filtered list empty:** `Empty`, with the existing icon, title and subtitle for "nothing at all"
      or "nothing matches".
   4. **Otherwise:** `Content`.
2. **Render** `ListArea(state, isRefreshing = state.isRefreshing, onRefresh = { <the existing refresh event> })`.
   Its `content` is the existing `LazyColumn` with its items, unchanged.
3. **Count and sort row.** `ResultsCountAndSort` shows only when the state is `Content`.
4. **Pane title.** The screen's root gets `Modifier.semantics { paneTitle = <the task's nav_* string> }`.
5. **(Review Focus 5)** The screen keeps one `ListArea` across all four states. Don't branch around it,
   so a refresh that empties the list, or fills it, keeps its indicator.

### Dashboard on the brand frame (finding 7)

1. **Replace** the `Scaffold` + `HapticPullToRefreshBox` + scrolling `Column` +
   `DashboardWelcomeHeader` with:
   ```
   BrandScaffold(
       header = BrandHeader.Hero { DashboardGreeting(…) },
       snackbarHostState = …,
       isRefreshing = …,
       onRefresh = …,
       refreshEnabled = <false during the first load>,
       modifier = Modifier.semantics { paneTitle = <nav_home> }
   )
   ```
   (Tasks 8 and 12.) `onAvatarClick` keeps today's target, `Screen.AccountDetail`. The bell keeps
   `Screen.NotificationInbox`.
2. **The sheet's content** starts with `DashboardIntro(role, subtitle)` (Task 12), then the sections
   as today. Drop their inner `padding(ScreenPadding)` and `spacedBy(8.dp)` wrapper, because the sheet
   supplies both.
3. **First load:** `LoadingState(Modifier.fillMaxWidth())`, in place of today's
   `LoadingState(…padding(vertical = 64.dp))`.
4. **Unchanged:** every bottom sheet, dialog and navigation call.
5. **Route:** in `NavGraph`, the route uses `SystemBarInsets.NavigationBar` (Task 9).
6. **Snackbar:** apply *Refresh snackbar removal* to the ViewModel.

## Review Focus

The user asked for no test steps. So each line below is written as an explicit requirement inside the
task that owns the code, marked **(Review Focus N)**.

1. **Font scale 2.0 in Vietnamese at 360dp.**
   - Bar labels follow the size rule (Task 7).
   - The greeting wraps, and the bell and avatar stay on screen (Task 12).
   - Titles wrap (Task 6).
   - Empty states scroll (Task 2).
   - The Profile hero grows (Task 16).
2. **`EmptyState` inside a parent that already scrolls.** The grocery and NGO Analytics screens put it
   in a `verticalScroll` column. It must not scroll itself or set a minimum height there, or Compose
   crashes on an infinite height (Task 2).
3. **Returning to Browse by its tab after changing the cart.** Browse's ViewModel is restored, not
   recreated, so it must reload the cart. A fresh Browse must not load it twice (Task 22).
4. **Status-bar icons across crossfades.** Check Home ↔ Listings, Profile ↔ Requests, Login → Home,
   Splash → Home, and Pending review → Help. The icons switch once, at the midpoint, or stay light
   between two gradient screens (Task 8).
5. **Pull-to-refresh across state changes.**
   - Pull is off during the first load and on error states.
   - A refresh that empties the list, or fills an empty one, keeps one pull area and its indicator
     (Tasks 3, 4 and the list screens).

---

## Shared components

### Task 1: `LoadingIndicators`

**Files:**
- Modify: `K/presentation/components/LoadingIndicators.kt`
- Modify: `K/presentation/splash/SplashScreen.kt:183` (the one `LoadingDefaults` reference outside the
  file)
- Modify: `K/ui/theme/Dimens.kt`, `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Signatures that don't change: `InlineSpinner`, `LoadingState(modifier, message)` and `UpdatingBar`.
- `LoadingDefaults` is deleted.
- Produces tokens `SpinnerInlineSize` (16.dp, `spinner-inline`), `SpinnerInlineStroke` (2.dp,
  `spinner-inline-stroke`), `SpinnerAreaSize` (40.dp, `spinner-area`) and `UpdatingBarHeight` (2.dp,
  `updating-bar`).
- Produces the string `cd_loading` ("Loading" / "Đang tải").

- [ ] **Step 1: Add the four tokens and `cd_loading`.**

- [ ] **Step 2: Rework the file**
  - **Tokens.** `InlineSpinner` uses `SpinnerInlineSize` and `SpinnerInlineStroke`, `LoadingState`
    uses `SpinnerAreaSize`, and `UpdatingBar` uses `UpdatingBarHeight`. Delete `LoadingDefaults`, and
    point `SplashScreen` at `SpinnerAreaSize`.
  - **Spacing.** `LoadingState`'s 12.dp gap becomes `CardPadding` and its 16.dp padding
    `ScreenHPadding`.
  - **Delay.** `LoadingState` draws its spinner and message at alpha 0 for the first 300ms (a private
    `const val AREA_SPINNER_DELAY_MS = 300L`). It still measures and takes its space, so nothing
    jumps when they appear.
  - **Spoken name.** Once visible, the spinner has the content description
    `stringResource(R.string.cd_loading)`.
  - The KDoc block keeps the three kinds and their rules.

- [ ] **Step 3: Commit.** Subject: `Delay the area spinner and give it a spoken name`.

### Task 2: `EmptyState`

**Files:**
- Modify: `K/presentation/components/EmptyState.kt`
- Modify: `K/ui/theme/Dimens.kt`

**Interfaces:**
- The signature doesn't change.
- Produces tokens `EmptyStatePadding` (40.dp, `empty-state-padding`) and `EmptyActionMinWidth` (160.dp,
  `empty-action-min-width`).

- [ ] **Step 1: Add the two tokens.**

- [ ] **Step 2: Rework the layout**
  - **Tokens.** The 40.dp padding becomes `EmptyStatePadding`, and the button's 160.dp minimum width
    becomes `EmptyActionMinWidth`. The 16.dp and 8.dp gaps become `ScreenHPadding` and `ListGap`.
  - **Bounded height.** Wrap the column in a `BoxWithConstraints`. When the height is bounded, the
    column gets `verticalScroll` and `heightIn(min = maxHeight)`, with its content centred. It's
    centred when it fits and scrolls when it doesn't.
  - **(Review Focus 1)** At font scale 2.0 in Vietnamese, the title, subtitle and button scroll rather
    than clip.
  - **(Review Focus 2)** When the height is unbounded, as inside `AnalyticsScreen`'s scrolling column,
    it gets neither, and lays out as today.

- [ ] **Step 3: Commit.** Subject: `Let empty states scroll when they don't fit`.

### Task 3: `HapticPullToRefreshBox`

**Files:**
- Modify: `K/presentation/components/HapticPullToRefresh.kt`

**Interfaces:**
- Produces two new parameters. Existing callers don't change.
  ```kotlin
  @Composable
  fun HapticPullToRefreshBox(
      isRefreshing: Boolean,
      onRefresh: () -> Unit,
      modifier: Modifier = Modifier,
      enabled: Boolean = true,
      indicatorPadding: PaddingValues = PaddingValues(),
      content: @Composable BoxScope.() -> Unit
  )
  ```

- [ ] **Step 1: Rebuild it on `Modifier.pullToRefresh`**
  - Material 3 1.4.0's `PullToRefreshBox` has no `enabled`. So it becomes a `Box` with
    `Modifier.pullToRefresh(isRefreshing, state, enabled) { haptic; onRefresh() }`.
  - `content` comes first, then `PullToRefreshDefaults.Indicator(state = state, isRefreshing = isRefreshing)`,
    aligned `TopCenter` and padded by `indicatorPadding`.
  - The haptic stays.

- [ ] **Step 2: Commit.** Subject: `Let pull-to-refresh be switched off and its indicator inset`.

### Task 4: `ListArea`

**Files:**
- Create: `K/presentation/components/ListArea.kt`
- Modify: `CLAUDE.md` (*Mobile architecture*)

**Interfaces:**
- Consumes: `LoadingState` (Task 1), `EmptyState` (Task 2), `HapticPullToRefreshBox(enabled = …)`
  (Task 3).
- Produces:
  ```kotlin
  /** What a list area shows. */
  sealed interface ListAreaState {
      /** Nothing to show yet: the area spinner. Pull is off. */
      data object Loading : ListAreaState
      /** The first load failed: an error [EmptyState] with Retry. Pull is off. */
      data class Error(val title: String, val message: String?, val onRetry: () -> Unit) : ListAreaState
      /** Nothing to list. Pullable. */
      data class Empty(
          val icon: ImageVector,
          val title: String,
          val subtitle: String? = null,
          val actionLabel: String? = null,
          val onAction: (() -> Unit)? = null
      ) : ListAreaState
      /** The list itself. Pullable. */
      data object Content : ListAreaState
  }

  /** A list screen's content area: its states and its list share one pull-to-refresh area. */
  @Composable
  fun ListArea(
      state: ListAreaState,
      isRefreshing: Boolean,
      onRefresh: (() -> Unit)?,
      modifier: Modifier = Modifier,
      content: @Composable () -> Unit
  )
  ```

- [ ] **Step 1: Implement it**
  - **Pull area.** When `onRefresh` is non-null, everything sits in one `HapticPullToRefreshBox`. It's
    `enabled` only for `Empty` and `Content`. When `onRefresh` is null, there's no pull box.
  - **(Review Focus 5)** The box is the same node across state changes.
  - **The three states.** `Loading`, `Error` and `Empty` each render as one `LazyColumn` item with
    `Modifier.fillParentMaxSize()`, as `VerificationQueueScreen` does today, so a pull reaches the
    pull area:
    - `Loading` is a `LoadingState`.
    - `Error` is an `EmptyState` with `Icons.Default.ErrorOutline`, `title`, `message`, and
      `stringResource(R.string.retry)` sending `onRetry`.
    - `Empty` is an `EmptyState` with its own fields.
  - **`Content`** calls `content()`.

- [ ] **Step 2: Add a list-screens bullet to `CLAUDE.md`.** It goes under *Mobile architecture*: "A
  list screen's states go through `ListArea`/`ListAreaState`, which keeps loading, error and empty
  states inside the pull area. Pull is off during the first load and on errors."

- [ ] **Step 3: Commit.** Subject: `Add a list area that keeps its states inside the pull area`.

### Task 5: `ClearChainSnackbarHost`

**Files:**
- Create: `K/presentation/components/ClearChainSnackbarHost.kt`
- Modify: every file with `SnackbarHost(`. That's 22 screens, `BrandScaffold.kt` (two calls) and
  `FabScaffold.kt`; `rg "SnackbarHost\(" Mobile/app/src/main/java` lists all 25.
- Modify: `CLAUDE.md`

**Interfaces:**
- Produces:
  ```kotlin
  /** The app's snackbar: `inverse-surface` on `shape-extra-small`, wrapping long messages. */
  @Composable
  fun ClearChainSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier)
  ```

- [ ] **Step 1: Implement it**
  - It's a `SnackbarHost(hostState, modifier)`. Its `snackbar` slot is a `Snackbar(data, …)` with
    `shape = MaterialTheme.shapes.extraSmall`, `containerColor = colorScheme.inverseSurface` and
    `contentColor = colorScheme.inverseOnSurface`.
  - The message has no line limit.

- [ ] **Step 2: Swap every call.** Each `SnackbarHost(…)` becomes `ClearChainSnackbarHost(…)`, with the
  same state, modifier and placement. `FabScaffold` keeps its bottom padding and its draw order over the
  FAB. No other line in those files changes.

- [ ] **Step 3: Add to `CLAUDE.md`'s list-screens bullet:** "Snackbars go through
  `ClearChainSnackbarHost`."

- [ ] **Step 4: Commit.** Subject: `Show every snackbar through one host`.

### Task 6: `ScreenTitleRow`

**Files:**
- Modify: `K/presentation/components/ScreenTitleRow.kt`
- Modify: every `ScreenTitleRow(` caller that pads it with `16.dp` or `8.dp`. Swap the literal for
  `ScreenHPadding`; `rg "ScreenTitleRow\(" Mobile/app/src/main/java` lists them.
- Modify: `K/ui/theme/Dimens.kt`

**Interfaces:**
- The signature doesn't change.
- Produces the token `TitleIconSize` (20.dp, `title-icon`).

- [ ] **Step 1: Add `TitleIconSize`.**

- [ ] **Step 2: Rework the row**
  - **Title:**
    - Drop `maxLines` and `overflow`.
    - Add `Modifier.semantics { heading() }`.
    - **(Review Focus 1)** It wraps onto as many lines as it needs.
  - **Row layout:**
    - The row is `verticalAlignment = Alignment.Top`.
    - The title is padded at the top by half the difference between `BackButtonSize` and its line
      height, so a one-line title sits centred on the circle.
    - When the title wraps, the back button stays beside its first line.
  - **Tokens:**
    - The arrow uses `TitleIconSize`.
    - The 12.dp gap becomes `CardPadding`.
    - The 6.dp vertical padding becomes a token. Add one, with KDoc naming it, if none fits.
  - The back label stays `R.string.back`.

- [ ] **Step 3: Put every caller on the gutter.** Each literal horizontal padding becomes
  `ScreenHPadding`. `NotificationInboxScreen` changes from 8.dp. Nothing else in those files changes.

- [ ] **Step 4: Commit.** Subject: `Let screen titles wrap and read as headings`.

### Task 7: `BottomNavBar` (finding 3)

**Files:**
- Modify: `K/presentation/navigation/BottomNavBar.kt`
- Modify: `K/ui/theme/Dimens.kt`, `K/ui/theme/Alpha.kt` (the `TileTint` KDoc only)
- Modify: `CLAUDE.md`

**Interfaces:**
- `BottomNavBar(navController, userType)` keeps its signature.
- Produces:
  ```kotlin
  /** Opens a bottom-bar destination the way its tab does: one copy, with each tab's state saved and restored. */
  fun NavController.navigateToTab(route: String)
  ```
  It goes in `BottomNavBar.kt`. Tasks 23 and 24 use it.
- Produces tokens:
  - `BottomBarHeight` (64.dp, `bottom-bar-height`): the minimum height
  - `NavIndicatorWidth` (48.dp, `nav-indicator-width`) and `NavIndicatorHeight` (26.dp,
    `nav-indicator-height`)
  - `NavTabGap` (4.dp, `nav-tab-gap`): between a tab's indicator and its label, and at its sides
- `Alpha.TileTint`'s KDoc becomes "the stat-tile fill, and the selected tab's indicator, as primary at
  this alpha".

- [ ] **Step 1: Add the tokens and widen the KDoc.**

- [ ] **Step 2: Five tabs for NGOs.**
  - Remove the Cart item from `getNgoNavigationItems()`.
  - Move today's `navigate(item.navRoute) { popUpTo(start) { saveState = true }; launchSingleTop; restoreState }`
    into `navigateToTab`, and call it from the tabs.
  - The `currentRoute != item.route` guard, the admin Stats `navRoute` and `profileRoutes` stay.

- [ ] **Step 3: Replace `NavigationBar` with a custom bar.** Material's has an 80dp minimum and a 64×32
  indicator.
  - **Bar:**
    - a `Column` with a `surface` background
    - a `BorderWidth` top line in `outlineVariant`
    - no tonal elevation
    - `windowInsetsPadding(WindowInsets.navigationBars)` below the tabs, so the surface fills the
      system inset as today
  - **Tab row:** a `Row` of at least `BottomBarHeight`, with `Modifier.selectableGroup()` and
    `semantics { collectionInfo = CollectionInfo(rowCount = 1, columnCount = items.size) }`.
  - **Each tab:**
    - `weight(1f)`, padded `ScreenVPadding` top and bottom and `NavTabGap` at the sides
    - `Modifier.selectable(selected, onClick = { navController.navigateToTab(…) }, role = Role.Tab)`,
      plus `collectionItemInfo = CollectionItemInfo(0, 1, index, 1)`
    - the indicator above the label, `NavTabGap` apart, centred
  - **Indicator:** a `NavIndicatorWidth` × `NavIndicatorHeight` pill (`CircleShape`). When selected,
    it's filled with `colorScheme.primary.copy(alpha = Alpha.TileTint).compositeOver(colorScheme.surface)`
    and the Filled icon is in `primary`. When unselected, it has no fill and the Outlined icon is in
    `onSurfaceVariant`.
  - **Icon:** `ControlIconSize`, with `contentDescription = null`.
  - **Label:** `labelSmall`, centred. Selected labels are `onSurface` with `FontWeight.SemiBold`.
    Unselected labels are `onSurfaceVariant`.

- [ ] **Step 4: Implement the label size rule**

  **(Review Focus 1)** One `fontSize` for every label in the bar:
  - **Measure.** Use a `BoxWithConstraints` to get the tab width, minus `NavTabGap` on both sides,
    and `rememberTextMeasurer()`. Measure in SemiBold.
  - **Candidates.** Start at `labelSmall.fontSize`, already scaled by the user's font scale. Step down
    0.5sp at a time to the floor, `with(density) { labelSmall.fontSize.value.dp.toSp() }`, which is
    the size at font scale 1.0.
  - **Pick** the first candidate where every label passes both checks:
    - Each word, measured on one line with `softWrap = false`, is no wider than the tab.
    - The whole label, wrapped to the tab width, has at most two lines.
  - **Floor.** If none passes, use the floor and let the labels wrap.
  - **No cutting.** Labels have no `maxLines` and no ellipsis.
  - **Cache.** `remember` the result on the labels, the tab width and the density.

- [ ] **Step 5: Add a bottom-bar line to `CLAUDE.md`** (*Mobile architecture*): "The bottom bar has
  five tabs for every role. NGOs reach Cart from Browse. `NavController.navigateToTab` opens a tab the
  way the bar does."

- [ ] **Step 6: Commit.** Subject: `Give every role a five-tab bar that fits its labels`.

### Task 8: `BrandScaffold`

**Files:**
- Modify: `K/presentation/components/BrandScaffold.kt`
- Modify: `K/presentation/auth/login/LoginScreen.kt:80-83` (the hero call only)
- Modify: `CLAUDE.md` (the *Sign-in and first-run screens* paragraph)

**Interfaces:**
- Consumes: `HapticPullToRefreshBox(enabled, indicatorPadding)` (Task 3).
- Produces:
  ```kotlin
  /** The enter/exit transition of the navigation destination being composed, or null outside one. */
  val LocalDestinationTransition: ProvidableCompositionLocal<Transition<EnterExitState>?>  // staticCompositionLocalOf { null }

  sealed interface BrandHeader {
      /** A gradient header that scrolls away with the content: Login's logo, a dashboard greeting, Profile's avatar. */
      data class Hero(val content: @Composable ColumnScope.() -> Unit) : BrandHeader
      data class Compact(/* unchanged */) : BrandHeader
  }

  /** Login's hero: the logo disc and the wordmark, centred. */
  @Composable
  fun LogoHero()

  @Composable
  fun BrandScaffold(
      header: BrandHeader,
      modifier: Modifier = Modifier,
      snackbarHostState: SnackbarHostState? = null,
      isRefreshing: Boolean = false,
      onRefresh: (() -> Unit)? = null,
      refreshEnabled: Boolean = true,
      bottomBar: (@Composable () -> Unit)? = null,
      content: @Composable ColumnScope.() -> Unit
  )
  ```
- `LightStatusBarIcons()` keeps its signature.

- [ ] **Step 1: Generalize the hero**
  - **Login's look moves into `LogoHero()`.** The disc and wordmark go in a full-width `Column`,
    centred, `ListGap` apart, with `HeaderPadding` above and below. That's what `HeroLayout` applies
    today.
  - **`HeroLayout`'s gradient column keeps:** `BrandGradient`, `statusBarsPadding()`, the
    `onSizeChanged` height capture and a bottom padding of `SheetOverlap`. Its alignment and
    `HeaderPadding` go into `LogoHero`.
  - **Then it calls** `header.content`.
  - **Login** passes `BrandHeader.Hero { LogoHero() }`. It must look exactly as on `main`.

- [ ] **Step 2: Add pull-to-refresh to the hero.**
  - When `onRefresh` is non-null, `HeroLayout`'s scrolling column sits in
    `HapticPullToRefreshBox(isRefreshing, onRefresh, enabled = refreshEnabled, indicatorPadding = WindowInsets.statusBars.asPaddingValues())`.
  - The pinned `BrandTeal` strip and the snackbar host stay above it.
  - `Compact` ignores these parameters.

- [ ] **Step 3: Make `LightStatusBarIcons` follow the transition**
  - **Read** `LocalDestinationTransition.current`.
  - **No transition** (null): claim while composed, as today.
  - **With a transition, claim light icons only while this screen is the more visible one:**
    - while it's entering and the transition is at least half done
    - while it's settled
    - while it's exiting and the transition is less than half done

    Measure progress as `playTimeNanos / totalDurationNanos` while `isRunning`. Navigation's default
    fade is 700ms.
  - **Keep the counting:** a claim increments the count, releasing it decrements, and the theme's
    `!isDark` is restored when the count reaches zero. Key the effect on whether the screen is
    claiming.
  - **(Review Focus 4)** Light icons switch to the theme's at the midpoint of Home → Listings. They
    switch back at the midpoint of Listings → Home. Between two gradient screens (Login → Home,
    Splash → Home), the release and the claim fall in the same frame, so the icons never flicker
    dark. Pending review → Help switches at the midpoint.

- [ ] **Step 4: Update `CLAUDE.md`.**
  - The *Sign-in and first-run screens* paragraph becomes "Brand screens".
  - `BrandHeader.Hero(content)` holds Login's `LogoHero`, a dashboard's `DashboardGreeting`, or
    Profile's avatar block. It scrolls away, can pull to refresh, and keeps the teal strip pinned
    behind the status bar.
  - `LightStatusBarIcons` switches at a crossfade's midpoint through `LocalDestinationTransition`.

- [ ] **Step 5: Commit.** Subject: `Let the brand hero take any header and switch status-bar icons mid-fade`.

### Task 9: `NavGraph` destination helper

**Files:**
- Modify: `K/presentation/navigation/NavGraph.kt`
- Modify: `CLAUDE.md` (the *System bars* paragraph)

**Interfaces:**
- Consumes: `LocalDestinationTransition` (Task 8).
- Produces, private to `NavGraph.kt`:
  ```kotlin
  /** Which system bars a destination pads for; the rest it draws behind. */
  private enum class SystemBarInsets { None, NavigationBar, Both }

  private fun NavGraphBuilder.destination(
      route: String,
      insets: SystemBarInsets,
      arguments: List<NamedNavArgument> = emptyList(),
      deepLinks: List<NavDeepLink> = emptyList(),
      content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
  )
  ```
  Tasks 13–16 switch their routes to `SystemBarInsets.NavigationBar`.

- [ ] **Step 1: Implement `destination`**
  - It calls `composable(route, arguments, deepLinks)`.
  - It provides `LocalDestinationTransition provides transition`, the `AnimatedContentScope`'s
    transition.
  - It wraps `content` in `Box(Modifier.fillMaxSize())`, padded by `insets`:
    - `None`: nothing
    - `NavigationBar`: `navigationBarsPadding()`
    - `Both`: `statusBarsPadding().navigationBarsPadding()`

- [ ] **Step 2: Replace every registration**
  - The seven sign-in `composable(` calls (Splash, Login, Register, EmailVerification,
    ForgotPassword, Onboarding, PendingReview) become `destination(…, SystemBarInsets.None)`.
  - Every `insetComposable(` call becomes `destination(…, SystemBarInsets.Both)`.
  - Each keeps its arguments, deep links and body.
  - Delete `insetComposable`.
  - Nothing moves on any screen.

- [ ] **Step 3: Rewrite `CLAUDE.md`'s *System bars* paragraph.** `NavGraph` registers every route
  through `destination(…)` with `SystemBarInsets`:
  - `None` for the seven sign-in and first-run routes, which draw behind both bars
  - `NavigationBar` for the three dashboards and Profile, whose gradient runs behind the status bar
  - `Both` for everything else

- [ ] **Step 4: Commit.** Subject: `Register every route through one destination helper`.

### Task 10: `NotificationBell`

**Files:**
- Modify: `K/presentation/notifications/NotificationBell.kt`
- Modify: `K/ui/theme/Dimens.kt` (the `BackButtonSize` KDoc), `K/ui/theme/Alpha.kt` (the `LogoDisc`
  KDoc)
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- `NotificationBell(onClick, modifier, viewModel)` keeps its signature. It's only used on the gradient
  header.
- Produces plurals `cd_open_notifications_unread`: en `one` and `other` "Open notifications, %d
  unread"; vi `other` "Mở thông báo, %d chưa đọc".
- KDocs:
  - `BackButtonSize` becomes "the circular back button beside a screen title, and the bell disc on the
    gradient header".
  - `Alpha.LogoDisc` adds "the bell disc and the Profile avatar tile".

- [ ] **Step 1: Add the plurals and widen the two KDocs.**

- [ ] **Step 2: Restyle the bell**
  - **Button:** an `IconButton` of `BackButtonSize` with
    `IconButtonDefaults.iconButtonColors(containerColor = OnBrandGradient.copy(alpha = Alpha.LogoDisc), contentColor = OnBrandGradient)`.
    That's the header back circle's style in `BrandScaffold`.
  - **Badge:** inside the `BadgedBox`, keeps the error colours and "99+", with
    `Modifier.clearAndSetSemantics {}`.
  - **Name:** `cd_open_notifications` at zero unread, otherwise
    `pluralStringResource(R.plurals.cd_open_notifications_unread, unreadCount, unreadCount)`.
  - **Cleanup:** the `Color.White` and `0.18f` literals go.

- [ ] **Step 3: Commit.** Subject: `Put the bell on the header style and say how many are unread`.

### Task 11: `AvatarImage`

**Files:**
- Modify: `K/presentation/components/AvatarHeader.kt` (`AvatarImage` only)
- Modify: `K/ui/theme/Dimens.kt`

**Interfaces:**
- Produces:
  - A new last parameter on `AvatarImage`: `clickLabel: String? = null`.
  - A `Dp` overload,
    `AvatarImage(imageUrl: String?, name: String, size: Dp, modifier: Modifier = Modifier, backgroundColor, textColor, borderColor: Color? = null, borderWidth: Dp = AvatarRingWidth, onClick: (() -> Unit)? = null, clickLabel: String? = null)`.
    The existing `size: Int` version forwards to it, so no caller changes.
  - The token `AvatarRingWidth` (2.dp, `avatar-ring`).

- [ ] **Step 1: Add `AvatarRingWidth`. Add the overload, and make `borderWidth` default to the token.**

- [ ] **Step 2: Name a clickable avatar by what it does.** When `clickLabel` and `onClick` are both set,
  the avatar gets
  `Modifier.clearAndSetSemantics { contentDescription = clickLabel; role = Role.Button; onClick { onClick(); true } }`.
  Then neither the photo's "Profile picture of …" nor the initial is read.

- [ ] **Step 3: Commit.** Subject: `Let an avatar button say what it opens`.

### Task 12: `DashboardGreeting` and `DashboardIntro`

**Files:**
- Create: `K/presentation/components/DashboardGreeting.kt`
- Modify: `K/ui/theme/Dimens.kt`, `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `NotificationBell` (Task 10), and `AvatarImage(size: Dp, …, clickLabel)` (Task 11).
- Produces:
  ```kotlin
  /** The dashboard's gradient row: "Hi, <name>", the bell and the avatar. Goes in BrandHeader.Hero. */
  @Composable
  fun DashboardGreeting(
      userName: String,
      profilePictureUrl: String?,
      onAvatarClick: () -> Unit,
      onNotificationsClick: () -> Unit,
      modifier: Modifier = Modifier
  )

  /** The top of a dashboard's sheet: the role eyebrow and the subtitle. */
  @Composable
  fun DashboardIntro(role: String, subtitle: String, modifier: Modifier = Modifier)
  ```
- Produces the token `HeaderAvatarSize` (40.dp, `header-avatar`) and the string
  `cd_open_account_details` ("Open account details" / "Mở chi tiết tài khoản").

- [ ] **Step 1: Add the token and the string.**

- [ ] **Step 2: Implement `DashboardGreeting`**
  - **Layout:** a `Row`, with `verticalAlignment = CenterVertically` and the parts `CardPadding`
    apart. It's padded `ScreenVPadding` at the top and `ScreenHPadding` at the sides and bottom.
  - **Greeting:**
    - `stringResource(R.string.greeting_hi, userName)`
    - `headlineSmall` with `FontWeight.Bold`, in `OnBrandGradient`
    - `Modifier.weight(1f).semantics { heading() }`
    - no line limit
  - **Then** `NotificationBell(onNotificationsClick)`.
  - **Then the avatar:**
    `AvatarImage(profilePictureUrl, userName, size = HeaderAvatarSize, borderColor = OnBrandGradient, onClick = onAvatarClick, clickLabel = stringResource(R.string.cd_open_account_details))`.
  - **(Review Focus 1)** At font scale 2.0 in Vietnamese, "Xin chào, AmericanRedCross" wraps over two
    or three lines. The bell and avatar keep their size at the end of the row.

- [ ] **Step 3: Implement `DashboardIntro`.**
  - A `Column`, `ListGap` apart.
  - The eyebrow: `role.uppercase()` in `EyebrowStyle`, `onSurfaceVariant`.
  - The subtitle: `bodyMedium`, `onSurfaceVariant`.

- [ ] **Step 4: Commit.** Subject: `Add the dashboard greeting and intro`.

## Screens

### Task 13: Grocery dashboard

**Files:**
- Modify: `K/presentation/grocery/GroceryDashboardScreen.kt:43-90`
- Modify: `K/presentation/grocery/GroceryDashboardViewModel.kt:84`
- Modify: `K/presentation/navigation/NavGraph.kt` (the `GroceryDashboard` route)

**Interfaces:**
- Consumes: `BrandScaffold`, `BrandHeader.Hero` (Task 8), `SystemBarInsets` (Task 9),
  `DashboardGreeting`, `DashboardIntro` (Task 12), `LoadingState` (Task 1).

- [ ] **Step 1: Apply *Dashboard on the brand frame*.**
  - `DashboardIntro(stringResource(R.string.role_grocery), stringResource(R.string.grocery_dashboard_subtitle))`.
  - `refreshEnabled = !state.isLoading`.
  - Remove the now-unused `BrandTeal` and `BrandGreen` imports.

- [ ] **Step 2: Commit.** Subject: `Put the grocery dashboard on the brand frame`.

### Task 14: NGO dashboard

**Files:**
- Modify: `K/presentation/ngo/NgoDashboardScreen.kt:52-90`
- Modify: `K/presentation/ngo/NgoDashboardViewModel.kt:115`
- Modify: `K/presentation/navigation/NavGraph.kt` (the `NgoDashboard` route)

**Interfaces:**
- Consumes: the same as Task 13.

- [ ] **Step 1: Apply *Dashboard on the brand frame*.**
  - `DashboardIntro(stringResource(R.string.role_ngo), stringResource(R.string.ngo_dashboard_subtitle))`.
  - `refreshEnabled = !state.isLoading`.
  - The reversed `listOf(BrandGreen, BrandTeal)` goes with the old header.

- [ ] **Step 2: Commit.** Subject: `Put the NGO dashboard on the brand frame`.

### Task 15: Admin dashboard

**Files:**
- Modify: `K/presentation/admin/dashboard/AdminDashboardScreen.kt:80-115`
- Modify: `K/presentation/admin/dashboard/AdminDashboardViewModel.kt:153`
- Modify: `K/presentation/components/AvatarHeader.kt` (delete `DashboardWelcomeHeader`)
- Modify: `K/presentation/navigation/NavGraph.kt` (the `AdminDashboard` route)
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: the same as Task 13.

- [ ] **Step 1: Apply *Dashboard on the brand frame*.**
  - `DashboardIntro(stringResource(R.string.role_admin), stringResource(R.string.system_health))`.
  - `refreshEnabled = !(state.isLoading && state.stats == null)`.
  - The `gradientColors = listOf(colorScheme.primary, BrandTeal)` goes.
  - The screen's user collection (`userName`, `profilePictureUrl`) feeds `DashboardGreeting` as today.

- [ ] **Step 2: Delete `DashboardWelcomeHeader`,** now that nothing calls it, along with its imports
  that become unused. Delete `snack_dashboard_refreshed` from both strings files, since this is its
  last use.

- [ ] **Step 3: Commit.** Subject: `Put the admin dashboard on the brand frame`.

### Task 16: Profile

**Files:**
- Modify: `K/presentation/profile/ProfileScreen.kt:72-104, 149-360`
- Modify: `K/presentation/navigation/NavGraph.kt` (the `Profile` route)
- Modify: `K/ui/theme/Dimens.kt` (and `Alpha.kt` if the upload overlay needs a token)

**Interfaces:**
- Consumes: `BrandScaffold`, `BrandHeader.Hero` (Task 8), `SystemBarInsets` (Task 9),
  `LoadingState` (Task 1), `EmptyState` (Task 2).
- Produces tokens `ProfileAvatarSize` (64.dp, `profile-avatar`) and `ProfileAvatarIconSize` (32.dp,
  `profile-avatar-icon`).

- [ ] **Step 1: Move the screen onto the scaffold**
  - `ProfileScreen` renders
    `BrandScaffold(header = BrandHeader.Hero { state.user?.let { ProfileHero(it, …) } }, snackbarHostState, modifier = Modifier.semantics { paneTitle = <nav_profile> })`.
    There's no refresh.
  - **The sheet holds:**
    - while the user loads, `LoadingState(Modifier.fillMaxWidth())`
    - when there's no user, today's error `EmptyState` with Retry
    - otherwise, `ProfileViewContent`'s action cards, without its own scroll, `Column` or padding,
      because the sheet supplies them
  - The dialogs and `BlockBackWhile` stay as they are.

- [ ] **Step 2: Rework `ProfileHero` as slot content**
  - **Layout:**
    - a full-width `Column`, centred and `ListGap` apart
    - padded `ScreenVPadding` at the top and `ScreenHPadding` at the sides and bottom
    - **(Review Focus 1)** no fixed height, so it grows with large text
  - **Tile:**
    - `ProfileAvatarSize`, clipped to `MaterialTheme.shapes.large`
    - filled with `OnBrandGradient.copy(alpha = Alpha.LogoDisc)`
    - the role icon at `ProfileAvatarIconSize` in `OnBrandGradient`
  - **Camera badge:** an `ActionIconButtonSize` circle in `OnBrandGradient`, with the camera at
    `DetailIconSize` in `primary`.
  - **Upload overlay:** `colorScheme.scrim` at an alpha token. Add `Alpha.Scrim` with KDoc if none
    fits.
  - **Name:** `headlineSmall` Bold in `OnBrandGradient`.
  - **Chips:** the type and Verified `StatusBadge`s use `OnBrandGradient.copy(alpha = Alpha.LogoDisc)`
    and `OnBrandGradient`.
  - **Behaviour stays:** the click, the labels and the uploading behaviour don't change.
  - Every literal in `ProfileHero` goes.

- [ ] **Step 3: Draw behind the status bar.** The `Profile` route uses `SystemBarInsets.NavigationBar`.

- [ ] **Step 4: Commit.** Subject: `Put Profile on the brand frame`.

### Task 17: My listings (grocery)

**Files:**
- Modify: `K/presentation/grocery/mylistings/MyListingsScreen.kt:160-270`
- Modify: `K/presentation/grocery/mylistings/MyListingsViewModel.kt:187`

**Interfaces:**
- Consumes: `ListArea`, `ListAreaState` (Task 4).

- [ ] **Step 1: Apply *List screen on `ListArea`*.** Load: `MyListingsEvent.LoadListings`. Refresh:
  `MyListingsEvent.RefreshListings`. Pane title: `nav_listings`.

- [ ] **Step 2: Apply *Refresh snackbar removal*.** `snack_listings_refreshed` stays until Task 22.

- [ ] **Step 3: Commit.** Subject: `Keep My listings' states inside its pull area`.

### Task 18: Requests (grocery)

**Files:**
- Modify: `K/presentation/grocery/managerequests/ManageRequestsScreen.kt:50-135`
- Modify: `K/presentation/grocery/managerequests/ManageRequestsViewModel.kt:135`

**Interfaces:**
- Consumes: `ListArea`, `ListAreaState` (Task 4).

- [ ] **Step 1: Apply *List screen on `ListArea`*.** Use the screen's existing load and refresh events.
  Pane title: `nav_requests`.

- [ ] **Step 2: Apply *Refresh snackbar removal*.** `snack_requests_refreshed` stays until Task 24.

- [ ] **Step 3: Commit.** Subject: `Keep the grocery requests list's states inside its pull area`.

### Task 19: History (admin)

**Files:**
- Modify: `K/presentation/admin/transactions/TransactionsScreen.kt:53-130`
- Modify: `K/presentation/admin/transactions/TransactionsViewModel.kt:213`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `ListArea`, `ListAreaState` (Task 4).

- [ ] **Step 1: Apply *List screen on `ListArea`*.**
  - The screen has no error branch, so it never maps to `Error`.
  - Refresh: `TransactionsEvent.RefreshTransactions`.
  - Pane title: `nav_history`.
  - `ResultsCountAndSort` sits outside `ListScreenHeader` today. Move it into the header, so it
    follows the same rhythm and hide rule as the other lists.

- [ ] **Step 2: Apply *Refresh snackbar removal*,** and delete `snack_transactions_refreshed`.

- [ ] **Step 3: Commit.** Subject: `Keep History's states inside its pull area`.

### Task 20: Verify (admin)

**Files:**
- Modify: `K/presentation/admin/verification/VerificationQueueScreen.kt:125-240`
- Modify: `K/presentation/admin/verification/VerificationQueueViewModel.kt:249`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `ListArea`, `ListAreaState` (Task 4).

- [ ] **Step 1: Apply *List screen on `ListArea`*.**
  - The `LazyColumn` state items (loading, none, none in this category) move into `ListArea`'s
    states, keeping their icons and strings.
  - Refresh: `VerificationQueueEvent.RefreshOrganizations`.
  - Pane title: `nav_verify`.

- [ ] **Step 2: Apply *Refresh snackbar removal*,** and delete `snack_orgs_refreshed`.

- [ ] **Step 3: Commit.** Subject: `Move Verify's states onto the list area`.

### Task 21: Stats (admin)

**Files:**
- Modify: `K/presentation/admin/analytics/AdminAnalyticsScreen.kt:48-160`
- Modify: `K/presentation/admin/analytics/AdminAnalyticsViewModel.kt:107-109`

**Interfaces:**
- Consumes: `ScreenTitleRow` (Task 6).

- [ ] **Step 1: Give Stats the standard title row**
  - The private `AnalyticsHeader` and its `LazyColumn` item go.
  - The `Scaffold` body becomes a `Column`. First comes
    `ScreenTitleRow(title = stringResource(R.string.stats_analytics_title), onBack = null, modifier = Modifier.padding(horizontal = ScreenHPadding))`.
    Its `actions` slot holds the existing PDF-export `ClearChainActionIconButton`, disabled while
    `isExporting`.
  - The existing `when` follows, in a `Box(Modifier.weight(1f))`, so the title shows while loading
    and on error too.
  - The root gets `Modifier.semantics { paneTitle = <nav_stats> }`.

- [ ] **Step 2: Fix the section index.** The list loses its header item, so `FIRST_SECTION_INDEX`
  (line 50) becomes 0. Its KDoc and the comment at line 127 change to match. The admin home's deep
  links, through `animateScrollToItem(FIRST_SECTION_INDEX + section.ordinal)`, still land on their
  section.

- [ ] **Step 3: Use one retry label.** The error state's button uses `R.string.retry` instead of
  `action_retry`. `action_retry` stays until Task 27.

- [ ] **Step 4: Remove the refresh snackbar.** In `AdminAnalyticsViewModel.fetch`, delete the whole
  `if (isRefresh) { … snack_stats_refreshed … }` block. `isRefresh` stays, because it still sets
  `isRefreshing`. `snack_stats_refreshed` stays until Task 27.

- [ ] **Step 5: Commit.** Subject: `Give Stats the standard title row`.

### Task 22: Browse (NGO)

**Files:**
- Modify: `K/presentation/ngo/browselistings/BrowseListingsScreen.kt:110-260`
- Modify: `K/presentation/ngo/browselistings/BrowseListingsEvent.kt`
- Modify: `K/presentation/ngo/browselistings/BrowseListingsViewModel.kt` (`onEvent` and line 318)
- Modify: `K/presentation/ngo/browselistings/BrowseListingsState.kt:64`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `ListArea`, `ListAreaState` (Task 4).
- Produces:
  - `object ReloadCart : BrowseListingsEvent()`, handled by the existing private `loadCart()`
  - strings `cd_open_cart` ("Open cart" / "Mở giỏ hàng") and
    `empty_action_change_location` ("Change location" / "Thay đổi vị trí")
  - plurals `cd_open_cart_items`: en `one` "Open cart, %d item", `other` "Open cart, %d items"; vi
    `other` "Mở giỏ hàng, %d mặt hàng"

- [ ] **Step 1: Add the strings.**

- [ ] **Step 2: Apply *List screen on `ListArea`* to the list view**
  - **What moves:** the first-load and error branches (lines 190–200) move into
    `ListingsListView`'s `ListArea`. In map view, the top-level `when` keeps them as today, ahead of
    `GroceryMapView`.
  - **Events:** load `BrowseListingsEvent.LoadListings`, refresh `BrowseListingsEvent.RefreshListings`.
  - **Pane title:** `nav_browse`.
  - **Nothing nearby:** the `Empty` for this case (`allListings` empty, `!showFavoritesOnly`) gets
    `actionLabel = stringResource(R.string.empty_action_change_location)` and
    `onAction = { showLocationSheet = true }`.
  - The count and sort row is list-only, as today.

- [ ] **Step 3: The cart button**
  - **Count:** `val cartCount = state.cartItemsByListingId.size`.
  - **Badge:** shows `cartCount`, or "99+" above 99, with `Modifier.clearAndSetSemantics {}`.
  - **Name:** `cd_open_cart` when `cartCount == 0`, otherwise
    `pluralStringResource(R.plurals.cd_open_cart_items, cartCount, cartCount)`. The hard-coded "Cart"
    goes.
  - **Cleanup:** delete `cartItemCount` from `BrowseListingsState` if a search finds no other reader.

- [ ] **Step 4: Reload the cart when Browse comes back on screen**
  - Add `ReloadCart` and handle it in `onEvent` with `loadCart()`.
  - In the screen, `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` sends `ReloadCart`, except on
    the first resume of a new ViewModel.
  - Track that with `var skipNextResume by rememberSaveable { mutableStateOf(true) }`. The first
    resume clears it and sends nothing.
  - **(Review Focus 3)** `rememberSaveable` is restored with the tab's saved state, as the ViewModel
    is. So returning by the tab, or by Back from Cart or listing detail, reloads the cart. A
    brand-new Browse doesn't load it twice.

- [ ] **Step 5: Apply *Refresh snackbar removal*,** and delete `snack_listings_refreshed`.

- [ ] **Step 6: Commit.** Subject: `Keep Browse's cart current and its states inside the pull area`.
  The body notes the one event added to `BrowseListingsViewModel`, at the user's request.

### Task 23: Inventory (NGO)

**Files:**
- Modify: `K/presentation/ngo/inventory/InventoryScreen.kt:30-200`
- Modify: `K/presentation/ngo/inventory/InventoryViewModel.kt:198`
- Modify: `K/presentation/navigation/NavGraph.kt` (the `Inventory` route)
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `ListArea`, `ListAreaState` (Task 4), and `NavController.navigateToTab` (Task 7).
- Produces:
  - `InventoryScreen(onNavigateToItemDetail: (String) -> Unit, onBrowseFood: () -> Unit, viewModel …)`
  - the string `empty_action_browse_food` ("Browse food" / "Duyệt thực phẩm")

- [ ] **Step 1: Add the string.**

- [ ] **Step 2: Apply *List screen on `ListArea`*.**
  - Load: `InventoryEvent.LoadInventory`. Refresh: `InventoryEvent.RefreshInventory`. Pane title:
    `nav_inventory`.
  - The `Empty` for "nothing at all" (`allItems` empty) gets
    `actionLabel = stringResource(R.string.empty_action_browse_food)` and `onAction = onBrowseFood`.
  - Selection mode's count text and select-all stay in `ResultsCountAndSort`. The row still shows
    only when the state is `Content`.

- [ ] **Step 3: Wire it.** In `NavGraph`, the `Inventory` route passes
  `onBrowseFood = { navController.navigateToTab(Screen.BrowseListings.route) }`.

- [ ] **Step 4: Apply *Refresh snackbar removal*,** and delete `snack_inventory_refreshed`.

- [ ] **Step 5: Commit.** Subject: `Keep Inventory's states inside its pull area and offer Browse food`.

### Task 24: Requests (NGO)

**Files:**
- Modify: `K/presentation/ngo/myrequests/MyRequestsScreen.kt:30-185`
- Modify: `K/presentation/ngo/myrequests/MyRequestsViewModel.kt:166`
- Modify: `K/presentation/navigation/NavGraph.kt` (the `MyRequests` route)
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `ListArea`, `ListAreaState` (Task 4), `NavController.navigateToTab` (Task 7), and
  `empty_action_browse_food` (Task 23).
- Produces:
  `MyRequestsScreen(onNavigateToRequestDetail: (String) -> Unit, onNavigateToRoute: (String) -> Unit, onBrowseFood: () -> Unit, viewModel …)`

- [ ] **Step 1: Apply *List screen on `ListArea`*.**
  - Load: `MyRequestsEvent.LoadRequests`. Refresh: `MyRequestsEvent.RefreshRequests`. Pane title:
    `nav_requests`.
  - The `Empty` for "nothing at all" (`allRequests` empty) gets `empty_action_browse_food` sending
    `onBrowseFood`.
  - The upload-error banner above the header stays.

- [ ] **Step 2: Wire it.** In `NavGraph`, the `MyRequests` route passes
  `onBrowseFood = { navController.navigateToTab(Screen.BrowseListings.route) }`.

- [ ] **Step 3: Apply *Refresh snackbar removal*,** and delete `snack_requests_refreshed`.

- [ ] **Step 4: Commit.** Subject: `Keep the NGO requests list's states inside its pull area`.

### Task 25: Cart (NGO)

**Files:**
- Modify: `K/presentation/ngo/cart/CartScreen.kt:30-120`
- Modify: `K/presentation/navigation/NavGraph.kt` (the `Cart` route)

**Interfaces:**
- Consumes: `ScreenTitleRow` (Task 6) and `ListArea`, `ListAreaState` (Task 4).
- Produces:
  `CartScreen(onNavigate: (String) -> Unit, onNavigateBack: () -> Unit, viewModel: CartViewModel = hiltViewModel())`

- [ ] **Step 1: Hide the bar on Cart.**
  - The `Cart` route calls `onShowBottomBar(false, OrganizationType.NGO)`, as `CartPickup` does.
  - It passes `onNavigateBack = { navController.navigateUp() }`.

- [ ] **Step 2: Add the title row.**
  - `ScreenTitleRow(title = stringResource(R.string.nav_cart), onBack = onNavigateBack, modifier = Modifier.padding(horizontal = ScreenHPadding))`
    goes first, above `ListScreenHeader`.
  - Request pickup → Back lands on Cart, and Back again lands on Browse.

- [ ] **Step 3: Move the states onto `ListArea`.**
  - The content `when` becomes `ListArea(…, isRefreshing = false, onRefresh = null)`.
  - The error state comes first, as today: `Error(error_generic, state.error, { onEvent(CartEvent.LoadCart) })`.
  - Then `Loading`, then `Empty` (the existing cart-empty strings, no action), then `Content`.
  - `ResultsCountAndSort` shows only for `Content`.

- [ ] **Step 4: Commit.** Subject: `Open Cart from Browse with a title and back button`.

### Task 26: Notifications

**Files:**
- Modify: `K/presentation/notifications/NotificationInboxScreen.kt:69-84`

- [ ] **Step 1: Let the action row wrap.**
  - The `Row` holding "Mark all as read" and "Clear all notifications" becomes a `FlowRow`, `ListGap`
    apart in both directions and arranged to the end, on `ScreenHPadding`.
  - The buttons and their actions don't change.

- [ ] **Step 2: Commit.** Subject: `Let the notifications actions wrap`.

### Task 27: Analytics (grocery and NGO)

**Files:**
- Modify: `K/presentation/analytics/AnalyticsScreen.kt:78, 178`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

- [ ] **Step 1: Use one retry label.** The error `EmptyState` at line 178 uses `R.string.retry`.
  Delete `action_retry` from both files, since this is its last use.

- [ ] **Step 2: Apply *Refresh snackbar removal*** to `AnalyticsViewModel.refresh` (line 78), and
  delete `snack_stats_refreshed`.

- [ ] **Step 3: Commit.** Subject: `Stop announcing analytics refreshes and say Retry`.

### Task 28: Public profile

**Files:**
- Modify: `K/presentation/publicprofile/PublicProfileScreen.kt:94-96`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

- [ ] **Step 1: Apply *Refresh snackbar removal*** to `PublicProfileViewModel.refresh`, and delete
  `snack_profile_refreshed`.

- [ ] **Step 2: Commit.** Subject: `Stop announcing public profile refreshes`.
