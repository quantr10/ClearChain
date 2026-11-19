# Sign-in and First-Run Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the launcher icon, splash, Login, Forgot password, Register, Verify email, Onboarding 1–3 and Pending review on one brand header with a curved sheet. Fix findings 1, 12, 13, 14 and 15 along the way.

**Architecture:**
- Shared pieces come first. Three new components: `BrandScaffold`, `CodeInputField` and `StatusDisc`. Fixes to the shared fields and buttons. A per-destination status-bar inset in `NavGraph`.
- Each screen then moves onto those pieces, one task per screen.
- Logic doesn't change. ViewModels, state, events, APIs and DTOs stay as they are.

**Tech Stack:** Kotlin 2.4.20, Jetpack Compose (BOM 2026.06.01) with Material 3, Navigation Compose 2.9.8, Hilt. `minSdk` 26, `targetSdk` 34, `compileSdk` 36.

**Spec:** `docs/superpowers/specs/2026-11-19-sign-in-first-run-redesign-design.md`. The approved mockups are in `docs/superpowers/specs/2026-11-19-sign-in-first-run-redesign/`.

**Path shorthand:** `K/` is `Mobile/app/src/main/java/com/clearchain/app/`, and `R/` is `Mobile/app/src/main/res/`.

**Not in this plan:**
- **Tests, builds, lint, screenshots and reviews.** The repo has no automated tests, and the user asked for these steps to be left out. The spec's *Verification* section lists the checks for later.
- **The design-system artifact update.** It's published separately, after implementation, with the user's OK.
- **Real links for Register's terms text.** That's a follow-up.

## Global Constraints

- **UI only.** No change to any ViewModel, `*State`, `*Event`, use case, repository, API or DTO.
- **No literals.** The ten screens, `BrandScaffold`, `CodeInputField` and `StatusDisc` contain no dp, colour or alpha literals.
  - In existing shared components, every line a task changes follows the same rule.
  - A new token goes in `K/ui/theme/Dimens.kt` or `K/ui/theme/Alpha.kt`, with a KDoc naming its design-system token in backticks, as the existing ones do.
- **Strings.**
  - Every new or changed string goes in both `R/values/strings.xml` and `R/values-vi/strings.xml`, with the exact text from the spec's *Strings* tables.
  - Counts use `<plurals>`.
  - A key that a task leaves unused is deleted from both files, after a repo-wide search confirms nothing else uses it.
- **Code style.**
  - Star imports, no trailing commas, KDoc on every new declaration.
  - Section dividers look like `// ── Title ───`, padded to column 80, in sentence case.
  - LF line endings, 4-space indents, lines under 120 columns.
  - No change-log comments.
- **Every screen** must work in light and dark, in English and Vietnamese (Vietnamese text runs about 30% longer), at 360×640dp, and at font scale 2.0.
- **Screen-reader labels** say what the control does.
- **Commits.**
  - Each task ends with one commit on `ui/sign-in`.
  - The message is in the repo's style: a sentence-case, imperative subject, then a short body.
  - It ends with:
    ```
    Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
    ```

## Review Focus

The user asked for no test steps. So each line below is written as an explicit requirement inside the task that owns the code.

1. **Font scale 2.0 in Vietnamese at 360dp.**
   - Header titles wrap (Task 10).
   - Buttons and fields grow, not clip (Tasks 2, 6).
   - Role cards wrap and stay equal height (Task 14).
   - The six code cells still fit (Task 9).
   - Pending review's Help and Log out stack (Task 20).
2. **Keyboard open on a compact screen.**
   - The focused field and the bottom bar stay visible (Task 10).
   - Address suggestions appearing near the keyboard scroll into view (Task 8).
3. **Stale address lookups.**
   - A result for an older query is dropped.
   - So is one that arrives after the field lost focus (Task 8).
4. **Pasted codes with separators or extra digits.**
   - "482 913", "482-913" and "4829135" all become "482913" (Task 9).
5. **Status-bar icons across navigation.**
   - Login → Register → back keeps light icons on both screens.
   - Leaving to a non-brand screen restores the theme's icons.
   - Switching system dark mode while on a brand screen keeps them light.
   - Nothing jumps vertically during transitions (Task 10).

---

### Task 1: `FieldCard` shares its label

**Files:**
- Modify: `K/presentation/components/FieldCard.kt:22-47`

**Interfaces:**
- Produces:
  ```kotlin
  /** The label of the [FieldCard] around a field, or null outside one. */
  val LocalFieldLabel: ProvidableCompositionLocal<String?>
  ```
  It's a `staticCompositionLocalOf<String?> { null }`, declared in `FieldCard.kt`.

- [ ] **Step 1: Add `LocalFieldLabel` and provide it in `FieldCard`**

  Wrap the `content()` call in `CompositionLocalProvider(LocalFieldLabel provides …)`. The value is the same text the card shows: `label.replace("*", "").trim()`. Nothing changes visually.

- [ ] **Step 2: Commit**

  Subject: `Let a FieldCard's fields read its label`.

### Task 2: `ClearChainTextField`: named clear button and growing height

**Files:**
- Modify: `K/presentation/components/ClearChainTextField.kt`
- Modify: `K/ui/theme/Dimens.kt`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `LocalFieldLabel` (Task 1).
- Produces:
  - A new last parameter on `ClearChainTextField`: `clearLabel: String? = null`.
  - `@Composable internal fun clearButtonLabel(clearLabel: String?, label: String, @StringRes fallback: Int): String`, declared in `ClearChainTextField.kt`. Tasks 3 and 5 use it.
  - Tokens `FieldEndButtonSize` (24.dp, `field-end-button`) and `BorderWidth` (1.dp, `border-width`).
  - Strings `cd_clear_field` and `cd_clear_text`.

- [ ] **Step 1: Add the tokens and strings**

  - Add `FieldEndButtonSize` and `BorderWidth` to `Dimens.kt`.
  - Add `cd_clear_field` ("Clear %1$s" / "Xóa %1$s") and `cd_clear_text` ("Clear text" / "Xóa nội dung") to both strings files.

- [ ] **Step 2: Implement `clearButtonLabel`**

  It returns the first of these that exists:
  1. `clearLabel`
  2. `stringResource(R.string.cd_clear_field, label)` when `label` isn't blank
  3. `stringResource(R.string.cd_clear_field, it)` with `LocalFieldLabel.current`, when that isn't blank
  4. `stringResource(fallback)`

- [ ] **Step 3: Use it for the clear button**

  - The clear `IconButton`'s description (today `cd_clear_search`, line 164) becomes `clearButtonLabel(clearLabel, label, R.string.cd_clear_text)`.
  - The clear button and the password toggle use `FieldEndButtonSize` instead of `24.dp` (lines 144 and 162).
  - The border uses `BorderWidth`.

- [ ] **Step 4: Let single-line fields grow**

  - Replace `.height(fieldHeight)` with `.heightIn(min = fieldHeight)`.
  - The decoration `Row` keeps its content vertically centred at `fieldHeight` when the text fits, and grows with it at large font scales.
  - Nothing changes at font scale 1.0.

- [ ] **Step 5: Commit**

  Subject: `Name the field a text field's clear button clears`.

### Task 3: `TimePickerField`: named clear button and growing height

**Files:**
- Modify: `K/presentation/components/TimePickerField.kt:27-110`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `clearButtonLabel` (Task 2), `LocalFieldLabel` (Task 1), `FieldEndButtonSize` and `BorderWidth` (Task 2).
- Produces:
  - A new last parameter on `TimePickerField`: `clearLabel: String? = null`.
  - The string `cd_clear_time`.

- [ ] **Step 1: Add `cd_clear_time`** ("Clear time" / "Xóa giờ") to both strings files.

- [ ] **Step 2: Change the clear button and the field height**

  - The clear button's description becomes `clearButtonLabel(clearLabel, label, R.string.cd_clear_time)`.
  - Its `IconButton` uses `FieldEndButtonSize`.
  - The field's `.height(fieldHeight)` becomes `.heightIn(min = fieldHeight)`.
  - The border uses `BorderWidth`.

- [ ] **Step 3: Commit**

  Subject: `Name the field a time field's clear button clears`.

### Task 4: Account details: label the paired hours clear buttons

**Files:**
- Modify: `K/presentation/profile/AccountDetailScreen.kt:337-357`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: the `TimePickerField(clearLabel = …)` parameter (Task 3).
- Produces: the strings `cd_clear_opening_time` and `cd_clear_closing_time`. Task 18 uses them too.

- [ ] **Step 1: Add the strings**

  - `cd_clear_opening_time`: "Clear opening time" / "Xóa giờ mở cửa".
  - `cd_clear_closing_time`: "Clear closing time" / "Xóa giờ đóng cửa".

- [ ] **Step 2: Pass the labels**

  The open-time `TimePickerField` passes `clearLabel = stringResource(R.string.cd_clear_opening_time)`, and the close-time one passes `cd_clear_closing_time`. Nothing else on the screen changes.

- [ ] **Step 3: Commit**

  Subject: `Tell the two hours clear buttons apart in account details`.

### Task 5: `DatePickerField`: named clear button and growing height

**Files:**
- Modify: `K/presentation/components/DatePickerField.kt:36-112`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `clearButtonLabel` (Task 2), `FieldEndButtonSize` and `BorderWidth` (Task 2).
- Produces:
  - A new last parameter on `DatePickerField`: `clearLabel: String? = null`.
  - The string `cd_clear_date`.

- [ ] **Step 1: Add `cd_clear_date`** ("Clear date" / "Xóa ngày") to both strings files.

- [ ] **Step 2: Change the clear button and the field height**

  - The clear button's description becomes `clearButtonLabel(clearLabel, label, R.string.cd_clear_date)`.
  - Its `IconButton` uses `FieldEndButtonSize`.
  - `.height(ControlHeight)` becomes `.heightIn(min = ControlHeight)`.
  - `borderWidth` uses `BorderWidth`.
  - No call site changes. Create listing, Edit listing and Cart pickup get their card's label through `LocalFieldLabel`. The filter sheets fall back to "Clear date".

- [ ] **Step 3: Commit**

  Subject: `Name the field a date field's clear button clears`.

### Task 6: `ClearChainButton`: grow with large text

**Files:**
- Modify: `K/presentation/components/ClearChainButton.kt:55-160`

**Interfaces:**
- Produces: no signature changes. `ClearChainButton` and `ClearChainOutlinedButton` are at least `ClearChainButtonDefaults.Height` tall and grow to fit a wrapped label.

- [ ] **Step 1: Replace the fixed height in both buttons**

  - `.requiredHeight(ClearChainButtonDefaults.Height)` (lines 77 and 136) becomes `.heightIn(min = ClearChainButtonDefaults.Height)`.
  - The label `Text` gets `textAlign = TextAlign.Center` and no line limit, so a long label at font scale 2.0 wraps instead of clipping. Pending review's "Edit profile and documents" and Onboarding's half-width "View status" are examples.
  - At font scale 1.0 the buttons stay exactly 32dp. Material's 40dp `defaultMinSize` doesn't apply once a min height is set.

- [ ] **Step 2: Commit**

  Subject: `Let buttons grow when large text needs room`.

### Task 7: `StatusDisc`, extracted from `EmptyState`

**Files:**
- Create: `K/presentation/components/StatusDisc.kt`
- Modify: `K/presentation/components/EmptyState.kt:38-66`
- Modify: `K/ui/theme/Dimens.kt`, `K/ui/theme/Alpha.kt`

**Interfaces:**
- Produces:
  ```kotlin
  /** The design system's status picture: an icon on a primary-container disc inside two rings. Decorative. */
  @Composable
  fun StatusDisc(icon: ImageVector, modifier: Modifier = Modifier)
  ```
  It adds the tokens `StatusDiscSize` (56.dp, `status-disc`), `StatusDiscIconSize` (28.dp, `status-disc-icon`) and `StatusRingsSize` (100.dp, `status-rings`). It also adds `Alpha.RingOuter` (0.08f, `ring-outer`) and `Alpha.RingInner` (0.12f, `ring-inner`).

- [ ] **Step 1: Add the five tokens.**

- [ ] **Step 2: Create `StatusDisc`**

  It's the picture `EmptyState` draws today, at the same geometry:
  - A `StatusRingsSize` canvas with an outer ring of `primary` at `Alpha.RingOuter`, radius `width / 2`.
  - An inner ring of `primary` at `Alpha.RingInner`, radius `width / 2.8f`.
  - A `StatusDiscSize` `primaryContainer` circle holding the icon, at `StatusDiscIconSize`, in `onPrimaryContainer`, with `contentDescription = null`.

- [ ] **Step 3: Use it in `EmptyState`**

  Replace the inline `Box` at lines 38–66 with `StatusDisc(icon)`. Nothing changes visually.

- [ ] **Step 4: Commit**

  Subject: `Share the empty-state picture as StatusDisc`.

### Task 8: `AddressSuggestionField`: inline, focus-bound, US-only (finding 14)

**Files:**
- Modify: `K/presentation/components/AddressSuggestionField.kt`
- Modify: `K/ui/theme/Dimens.kt`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `InlineSpinner` (`LoadingIndicators.kt`) and `ClearChainCard`.
- Produces:
  - No signature change. `AddressSuggestion` and the `AddressSuggestionField(...)` parameters stay the same, so Onboarding and Account details keep their call sites.
  - The token `MinTouchTarget` (48.dp, `touch-target`).
  - The strings `address_searching`, `address_no_matches` and `cd_use_address`.

- [ ] **Step 1: Add the token and strings**

  - `MinTouchTarget` goes in `Dimens.kt`.
  - `address_searching`: "Searching addresses…" / "Đang tìm địa chỉ…".
  - `address_no_matches`: "No matching addresses. Fill in the fields below." / "Không tìm thấy địa chỉ phù hợp. Hãy điền các ô bên dưới."
  - `cd_use_address`: "Use this address" / "Dùng địa chỉ này".

- [ ] **Step 2: Replace the popup with an inline list**

  - Model the list as a private `sealed interface SuggestionState` with `Idle`, `Searching`, `Results(list)` and `NoMatches`.
  - Render it in a `Column` under the field, so it pushes the next field down. Delete the `Popup`, `IntOffset`, `zIndex` and `LocalDensity` code.
  - **Searching:** an `InlineSpinner` with `address_searching`.
  - **Results:** a `ClearChainCard` of rows.
  - **NoMatches:** `address_no_matches` in `bodySmall` and `onSurfaceVariant`.
  - **Lookup error:** go back to `Idle`.

- [ ] **Step 3: Bind the lookup to focus and to the latest query**

  - Track `hasFocus` with `Modifier.onFocusChanged` on the wrapping `Column`.
  - Start a lookup only while focused, with the same 300ms debounce and three-character minimum.
  - Set `Searching` when the debounce ends.
  - Losing focus cancels the job and returns to `Idle`.
  - **(Review Focus 3)** Apply a result only if the job is still current and the field still has focus. A result for an older query, or one that lands after focus left, is dropped.

- [ ] **Step 4: Restrict the results to the US and lay out the rows**

  - Call `geocoder.getFromLocationName(query, 10)`, keep the results whose `countryCode == "US"`, and take five.
  - **First line:** `streetAddress`.
  - **Second line:** `city`, then `state` and `zipCode` joined with a space, then the two parts joined with ", ", skipping blanks. That gives "Seattle, Washington 98109". `state` stays the geocoder's `adminArea`, unchanged.
  - **Click:** each row uses `Modifier.clickable(onClickLabel = stringResource(R.string.cd_use_address))` and `heightIn(min = MinTouchTarget)`. The place icon is `primary`, at `ControlIconSize`.
  - **Dividers:** inset by `ControlPadding + ControlIconSize + IconGap`.
  - **(Review Focus 2)** When the list appears, bring it into view with a `BringIntoViewRequester`, so rows under the keyboard scroll up.

- [ ] **Step 5: Commit**

  Subject: `Show address suggestions in the form and only while typing there`.

### Task 9: `CodeInputField`

**Files:**
- Create: `K/presentation/components/CodeInputField.kt`
- Modify: `K/ui/theme/Dimens.kt`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `BorderWidth` (Task 2). The error line copies `ClearChainTextField`'s error styling.
- Produces:
  ```kotlin
  /** A one-time code entered as [length] cells; one text field to accessibility services. */
  @Composable
  fun CodeInputField(
      value: String,
      onValueChange: (String) -> Unit,
      label: String,
      modifier: Modifier = Modifier,
      length: Int = 6,
      showLabel: Boolean = true,
      isError: Boolean = false,
      errorMessage: String? = null,
      enabled: Boolean = true,
      imeAction: ImeAction = ImeAction.Done,
      onImeAction: () -> Unit = {}
  )
  ```
  It adds the token `CodeCellMinHeight` (52.dp, `code-cell`) and the plurals `cd_code_field` and `cd_code_digits_entered`.

- [ ] **Step 1: Add the token and the plurals**

  - `cd_code_field`: en `other` "%1$s, %2$d digits", with `one` "%1$s, %2$d digit". vi `other` "%1$s, %2$d chữ số".
  - `cd_code_digits_entered`: en `one` "%d digit entered", `other` "%d digits entered". vi `other` "Đã nhập %d chữ số".

- [ ] **Step 2: Implement the field**

  - **Text field.** One `BasicTextField`, with `KeyboardType.Number`, the given `imeAction`, and `onAny = { onImeAction() }`.
  - **(Review Focus 4)** `onValueChange` passes on `new.filter(Char::isDigit).take(length)`. So "482 913", "482-913" and "4829135" all become "482913".
  - **Cells.**
    - The `decorationBox` draws `length` cells in a `Row`, each with `weight(1f)` and `ListGap` between them, and keeps `innerTextField()` composed but invisible.
    - Each cell is at least `CodeCellMinHeight` tall and grows with the font scale. It has `ButtonShape` corners and a `surface` fill.
    - Digits are `headlineSmall` with `FontWeight.Bold`, in `onSurface`, centred.
  - **Borders.** `BorderWidth` in `outlineVariant`. The cell at index `value.length` uses `primary` with a caret while focused. Every cell uses `error` when `isError`. Everything drops to `Alpha.Disabled` when disabled.
  - **(Review Focus 1)** Six cells fit in 328dp at font scale 2.0, so the row never scrolls or clips.

- [ ] **Step 3: Add the label, the error line and the semantics**

  - **Visible label.** When `showLabel` is set, `label` goes above the cells in `ClearChainTextField`'s label style.
  - **Error line.** `errorMessage` goes below when `isError`.
  - **Semantics,** on the text field:
    - `contentDescription` is `pluralStringResource(R.plurals.cd_code_field, length, label, length)`.
    - `stateDescription` is `pluralStringResource(R.plurals.cd_code_digits_entered, value.length, value.length)`.
    - `error(errorMessage)` when `isError`.
    - `contentType = ContentType.SmsOtpCode`.
    - The cells add no semantics of their own.

- [ ] **Step 4: Commit**

  Subject: `Add a six-cell code input`.

### Task 10: `BrandScaffold` and the status-bar inset

**Files:**
- Create: `K/presentation/components/BrandScaffold.kt`
- Modify: `K/MainActivity.kt:127-139`
- Modify: `K/presentation/navigation/NavGraph.kt`
- Modify: `K/ui/theme/Dimens.kt`, `K/ui/theme/Alpha.kt` (the `LogoDisc` KDoc only)

**Interfaces:**
- Produces:
  ```kotlin
  /** Design system brand gradient: `brand-teal` at the top into `brand-green`. */
  val BrandGradient: Brush

  /** Keeps the status-bar icons light while composed; restores the theme's choice when the last user leaves. */
  @Composable
  fun LightStatusBarIcons()

  /** What the gradient header shows. */
  sealed interface BrandHeader {
      /** Login's logo hero, which scrolls away with the content. */
      data object Hero : BrandHeader

      /** A fixed header: a title, an optional back circle, optional step bars. */
      data class Compact(
          val title: String,
          val onBack: (() -> Unit)? = null,
          val backLabel: String? = null,
          val backEnabled: Boolean = true,
          val step: Int? = null,
          val totalSteps: Int? = null
      ) : BrandHeader
  }

  /** The sign-in and first-run frame: gradient header, curved sheet, optional bottom bar. */
  @Composable
  fun BrandScaffold(
      header: BrandHeader,
      modifier: Modifier = Modifier,
      snackbarHostState: SnackbarHostState? = null,
      bottomBar: (@Composable () -> Unit)? = null,
      content: @Composable ColumnScope.() -> Unit
  )
  ```
  It adds the tokens `LogoDiscHeroSize` (64.dp, `logo-disc-hero`), `LogoMarkHeroSize` (38.dp, `logo-mark-hero`) and `StepBarHeight` (4.dp, `step-bar`).

  In `NavGraph.kt` it adds:
  ```kotlin
  /** A destination that sits below the status bar, as every screen outside the brand flow does. */
  private fun NavGraphBuilder.insetComposable(
      route: String,
      arguments: List<NamedNavArgument> = emptyList(),
      deepLinks: List<NavDeepLink> = emptyList(),
      content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
  )
  ```
  Tasks 12–20 each switch their own route from `insetComposable` back to `composable`.

- [ ] **Step 1: Add the tokens and widen the `Alpha.LogoDisc` KDoc**

  The KDoc becomes: "white at this alpha on the brand gradient: the logo disc, the header back circle and the step-bar track".

- [ ] **Step 2: Move the status-bar inset from the outer scaffold to each destination**

  - In `MainActivity`, give the outer `Scaffold` `contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)`.
  - In `NavGraph`, add `insetComposable`. It wraps `content` in `Box(Modifier.fillMaxSize().statusBarsPadding())`.
  - Replace every `composable(` call with `insetComposable(`, including the seven brand routes for now, and keep each call's `arguments` and `deepLinks`.
  - Nothing moves on any screen after this step.

- [ ] **Step 3: Add `BrandGradient` and `LightStatusBarIcons`**

  - `BrandGradient` is `Brush.verticalGradient(listOf(BrandTeal, BrandGreen))`.
  - `LightStatusBarIcons` sets `isAppearanceLightStatusBars = false` on the window's insets controller, as `ClearChainTheme` does today. It also reapplies that after every composition.
  - It keeps a process-wide count of active callers. When the count drops to zero, it restores `!isDark`, where `isDark` is `MaterialTheme.colorScheme.background.luminance() < 0.5f`.
  - **(Review Focus 5)** Going Login → Register → back keeps light icons throughout, even though both screens are composed during the crossfade. A system dark-mode switch while on a brand screen keeps them light, even after `ClearChainTheme`'s `SideEffect` runs. Leaving to a non-brand screen restores the theme's icons.

- [ ] **Step 4: Implement the compact header**

  - **Layout.** The `BrandGradient` area includes the status bar (`statusBarsPadding` inside it), with `ScreenHPadding` at the sides and `ScreenVPadding` at the top.
  - **Back circle.** When `onBack` is set, an `IconButton` of `BackButtonSize`, filled with `OnBrandGradient` at `Alpha.LogoDisc`, `CircleShape`. It shows `Icons.AutoMirrored.Filled.ArrowBack` in `OnBrandGradient`, with `contentDescription = backLabel` and `enabled = backEnabled`. It sits `CardPadding` from the title.
  - **Title.** `headlineSmall`, which is Bold, in `OnBrandGradient`, with `Modifier.semantics { heading() }`.
    - **(Review Focus 1)** No line limit, so it wraps at font scale 2.0 in Vietnamese.
  - **Step bars,** when `step` and `totalSteps` are set:
    - One bar per step, `StepBarHeight` tall, `IconGap` apart, with `CircleShape` ends.
    - The track is `OnBrandGradient` at `Alpha.LogoDisc`. The fill is `OnBrandGradient`, at fraction 1 for bars up to `step` and 0 after it.
    - The fill animates with `tween(500, easing = FastOutSlowInEasing)`.
    - The bars are wrapped in `clearAndSetSemantics {}`.

- [ ] **Step 5: Implement the sheet and the slots**

  - **Corners.** The sheet's top corners take the radius of `MaterialTheme.shapes.extraLarge`, and its bottom corners are square. It's filled with `colorScheme.background`.
  - **Overlap.** It overlaps the gradient by `ScreenHPadding`, so the gradient shows in both corners.
  - **Content.** `content` goes in a `Column` with `verticalScroll`, padded `ScreenHPadding` at the sides and the top, with `Arrangement.spacedBy(ListGap)`.
  - **Bottom bar.** `bottomBar` is fixed below the scroll, inside the sheet, with `ScreenPadding`.
  - **Header.** For `Compact`, the header stays fixed and the sheet takes the rest of the height.
  - **(Review Focus 2)** The sheet has `imePadding()`, since the outer scaffold has already consumed the navigation-bar inset. With the keyboard open, the bottom bar sits above it and the focused field scrolls into view.
  - **Snackbar.** It sits at the bottom, above the bottom bar.
  - **Status bar.** `BrandScaffold` calls `LightStatusBarIcons()`.

- [ ] **Step 6: Implement the hero**

  - The `BrandGradient` area includes the status bar.
  - **Logo.** Centred: a `LogoDiscHeroSize` circle of `OnBrandGradient` at `Alpha.LogoDisc`, holding `Icons.Default.Eco` at `LogoMarkHeroSize` in `OnBrandGradient`, with `contentDescription = null`.
  - **Wordmark.** `stringResource(R.string.app_name)` in `WordmarkHeaderStyle` and `OnBrandGradient`, `ListGap` below the disc.
  - **Scrolling.** The hero and the sheet scroll together in one `verticalScroll`. The sheet is at least as tall as the space left under the hero, so the gradient never shows below short content.
  - **Pinned strip.** A `BrandTeal` strip of `WindowInsets.statusBars` height (`windowInsetsTopHeight`) stays pinned on top, so scrolled content never sits under the white icons.

- [ ] **Step 7: Commit**

  Subject: `Add the brand scaffold and give each screen its own status-bar inset`.

### Task 11: Launcher icon and system splash (finding 1)

**Files:**
- Modify: `R/values/colors.xml`, `R/values/themes.xml`
- Create: `R/values-v31/themes.xml`, `R/drawable/splash_mark.xml`
- Modify: `R/drawable/ic_launcher_background.xml`, `R/drawable/ic_launcher_foreground.xml`
- Keep: `R/mipmap-anydpi/ic_launcher.xml`, `ic_launcher_round.xml`. They already point the background, foreground and monochrome layers at these two drawables.
- Delete: `R/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.webp` and `ic_launcher_round.webp`, ten files.

**Interfaces:**
- Produces: the colours `brand_teal`, `brand_green` and `on_brand_disc`, and the drawable `splash_mark`.

- [ ] **Step 1: Add the colours**

  - `brand_teal` is `#FF0D9488`, `brand_green` is `#FF16A34A`, and `on_brand_disc` is `#33FFFFFF`.
  - Each gets a comment saying it mirrors `BrandTeal`, `BrandGreen`, or `OnBrandGradient` at `Alpha.LogoDisc`, as appropriate.

- [ ] **Step 2: Redraw the adaptive icon**

  - **Background.** A 108×108dp vector filled with a vertical linear gradient from `@color/brand_teal` at the top to `@color/brand_green` at the bottom.
  - **Foreground.** A 108×108dp vector with one `group` using `scaleX`/`scaleY` 2.75 and `translateX`/`translateY` 21. It holds Material's `eco` path, filled `@android:color/white`:
    ```
    M6.05,8.05c-2.73,2.73 -2.73,7.15 -0.02,9.88c1.47,-3.4 4.09,-6.24 7.36,-7.93c-2.77,2.34 -4.71,5.61 -5.39,9.32c2.6,1.23 5.8,0.78 7.95,-1.37C19.43,14.47 20,4 20,4S9.53,4.57 6.05,8.05z
    ```
    Check it against `Icons.Filled.Eco` in `material-icons-extended`, and copy that one exactly if they differ.
  - **Monochrome.** The existing `<monochrome>` entry already reuses the foreground.
  - **Robot.** Delete the ten robot `.webp` files.

- [ ] **Step 3: Create `splash_mark.xml`**

  A 288×288dp vector containing:
  - a circle of radius 48, centred at (144, 144), filled `@color/on_brand_disc`
  - the same `eco` path in a `group` with scale 2.3333 and translate 116, filled white

- [ ] **Step 4: Set the window and splash themes**

  - In `values/themes.xml`, `Theme.ClearChain` gains `<item name="android:windowBackground">@color/brand_teal</item>`.
  - Create `values-v31/themes.xml` with the same style (same parent, same `windowBackground`). Add:
    - `android:windowSplashScreenBackground` as `@color/brand_teal`
    - `android:windowSplashScreenAnimatedIcon` as `@drawable/splash_mark`
  - Leave out `windowSplashScreenIconBackgroundColor`.

- [ ] **Step 5: Commit**

  Subject: `Replace the placeholder launcher icon and colour the system splash`.

### Task 12: Splash screen

**Files:**
- Modify: `K/presentation/splash/SplashScreen.kt:144-186`
- Modify: `K/presentation/navigation/NavGraph.kt`. Only the Splash route changes.
- Modify: `K/ui/theme/Dimens.kt`

**Interfaces:**
- Consumes: `BrandGradient` and `LightStatusBarIcons()` (Task 10), and `LoadingDefaults.AreaSize`.
- Produces: the tokens `LogoDiscSplashSize` (96.dp, `logo-disc-splash`) and `LogoMarkSplashSize` (56.dp, `logo-mark-splash`).

- [ ] **Step 1: Add the two tokens.**

- [ ] **Step 2: Rebuild the layout**

  - **Background.** A full-screen `Box` with `BrandGradient`, and a call to `LightStatusBarIcons()`.
  - **Disc.** The `LogoDiscSplashSize` disc (`OnBrandGradient` at `Alpha.LogoDisc`), holding `Icons.Default.Eco` at `LogoMarkSplashSize`. The disc's centre is exactly the screen's centre, where `splash_mark` draws it.
  - **Text.** The wordmark (`WordmarkStyle`) and the tagline (`bodyMedium`, `OnBrandGradient` at `Alpha.Subtitle`) stack below the disc, `ListGap` apart, and keep their 700ms fade-in.
  - **Animation.** The disc's scale-in spring is removed, along with its `scale` state.
  - **Spinner.** The bottom spinner is a `CircularProgressIndicator` of `LoadingDefaults.AreaSize`, in `OnBrandGradient`, `HeaderPadding` above the bottom.
  - **Unchanged.** The 1600ms delay and all routing in the `LaunchedEffect`s stay as they are.

- [ ] **Step 3: Draw the splash behind the status bar**

  In `NavGraph`, change the Splash route from `insetComposable` to `composable`.

- [ ] **Step 4: Commit**

  Subject: `Hand the system splash over to the app splash without a jump`.

### Task 13: Login

**Files:**
- Modify: `K/presentation/auth/login/LoginScreen.kt:85-189`
- Modify: `K/presentation/navigation/NavGraph.kt`. Only the Login route changes.

**Interfaces:**
- Consumes: `BrandScaffold` and `BrandHeader.Hero` (Task 10), and `MinTouchTarget` (Task 8).

- [ ] **Step 1: Move the screen onto `BrandScaffold(BrandHeader.Hero, snackbarHostState = …)`**

  - Remove the `Scaffold`, the scroll `Column` and `AuthHeader`.
  - The `AnimatedVisibility` fade-in stays around the content.

- [ ] **Step 2: Lay out the sheet**

  The sheet holds, in order:
  1. `welcome_back` in `titleMedium`.
  2. `sign_in_to_account` in `bodyMedium` and `onSurfaceVariant`.
  3. The email and password fields, unchanged.
  4. The Remember me row: one `Row` with `Modifier.toggleable(value = state.rememberMe, enabled = !state.isLoading, role = Role.Checkbox, onValueChange = { viewModel.onEvent(LoginEvent.ToggleRememberMe) })`. It holds a `Checkbox(checked = state.rememberMe, onCheckedChange = null)` and the label.
  5. "Forgot password?" at the row's end, as a `TextButton` with the `forgot_password` label in `labelSmall` and `primary`, at least `MinTouchTarget` tall. Its `onClick` stays as today.
  6. Sign In, `AuthDivider()` and Create New Account, unchanged.

  Spacing uses `ListGap`. Lockout, the snackbar effect and the reset-email `LaunchedEffect` stay as they are.

- [ ] **Step 3: Draw Login behind the status bar**

  In `NavGraph`, change the Login route from `insetComposable` to `composable`.

- [ ] **Step 4: Commit**

  Subject: `Move the login screen onto the brand scaffold`.

### Task 14: Register

**Files:**
- Modify: `K/presentation/auth/register/RegisterScreen.kt`
- Modify: `K/presentation/navigation/NavGraph.kt`. Only the Register route changes.
- Modify: `K/ui/theme/Dimens.kt`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `BrandScaffold` and `BrandHeader.Compact` (Task 10), `clearButtonLabel` or `cd_clear_field` (Task 2), `FieldEndButtonSize` and `BorderWidth` (Task 2), and `StepBarHeight` (Task 10).
- Produces:
  - The token `TileIconSize` (24.dp, `tile-icon`).
  - The strings `cd_back_to_sign_in`, `register_subtitle` and `register_role_label`. Task 15 reuses `cd_back_to_sign_in`.

- [ ] **Step 1: Add the token and strings, and delete the unused key**

  - `TileIconSize` goes in `Dimens.kt`.
  - `cd_back_to_sign_in`: "Back to sign in" / "Quay lại đăng nhập".
  - `register_subtitle`: "Help reduce food waste in your community". In vi, use the same text as the existing `ngo_dashboard_subtitle` vi value.
  - `register_role_label`: "I'm signing up as" / "Tôi đăng ký với tư cách".
  - Delete `join_clearchain`.

- [ ] **Step 2: Move the screen onto the scaffold**

  Use `BrandScaffold(BrandHeader.Compact(title = stringResource(R.string.create_your_account), onBack = { navController.navigateUp() }, backLabel = stringResource(R.string.cd_back_to_sign_in), backEnabled = !state.isLoading), snackbarHostState = …)`.

  Remove:
  - the `Scaffold`
  - `AuthHeader`
  - the no-op `AnimatedVisibility(visible = true)`
  - the "Join ClearChain" title
  - the `HorizontalDivider`

- [ ] **Step 3: Lay out the sheet**

  The sheet holds, in order:
  1. `register_subtitle` in `bodyMedium` and `onSurfaceVariant`.
  2. `register_role_label` as a field label (`OptionalFieldLabel`, `labelSmall`, Medium).
  3. The role row.
  4. The fields, as today.
  5. The terms row.
  6. Create Account, `AuthDivider()` and Sign In, as today.

  Spacing uses `ListGap`.

- [ ] **Step 4: Restyle the role cards**

  - **Row.** `Modifier.selectableGroup().height(IntrinsicSize.Max)`, with `spacedBy(ListGap)`.
  - **Cards.** Each `RoleCard` has `weight(1f).fillMaxHeight()`, so both are the same height. **(Review Focus 1)** Their text wraps freely in Vietnamese at font scale 2.0.
  - **Selection.** `Modifier.selectable(selected, enabled, role = Role.RadioButton, onClick)` replaces `clickable`.
  - **Removed.** `heightIn(min = 118.dp)`.
  - **Size and style.** `CardPadding` inside, `CardShape` corners, a `BorderWidth` border, and an icon at `TileIconSize`.
  - **Selected:** `primaryContainer` fill, a `primary` border, and the icon, title and subtitle in `onPrimaryContainer`.
  - **Unselected:** `surface` fill, an `outlineVariant` border, the icon in `onSurfaceVariant`, the title in `onSurface` and the subtitle in `onSurfaceVariant`.
  - **Text.** The title is `labelMedium` SemiBold and the subtitle `bodySmall`, both centred.

- [ ] **Step 5: Fix the email clear button, the terms row and the strength meter**

  - **Email clear button.** Its description is `stringResource(R.string.cd_clear_field, stringResource(R.string.email_address))`. The `IconButton` is `FieldEndButtonSize` and the icon `ControlIconSize`. `RegisterEvent.ClearEmail` stays.
  - **Terms row.**
    - It becomes `Modifier.toggleable(value = state.tosAccepted, enabled = !state.isLoading, role = Role.Checkbox, onValueChange = { viewModel.onEvent(RegisterEvent.ToggleTos) })`.
    - The `Checkbox` gets `onCheckedChange = null`. The annotated text stays as it is today.
    - The error line's start padding becomes `FieldEndButtonSize + ListGap`.
  - **`PasswordStrengthMeter`.**
    - The gap is `IconGap` and the bar height is `StepBarHeight`, with `CircleShape` ends.
    - The fixed `width(40.dp)` on the label goes, so "Trung bình" isn't clipped.
    - The row's horizontal padding is removed.

- [ ] **Step 6: Draw Register behind the status bar**

  In `NavGraph`, change the Register route from `insetComposable` to `composable`.

- [ ] **Step 7: Commit**

  Subject: `Move the register screen onto the brand scaffold`.

### Task 15: Forgot password

**Files:**
- Modify: `K/presentation/auth/forgot/ForgotPasswordScreen.kt`
- Modify: `K/presentation/auth/AuthComponents.kt`, to delete `AuthHeader`
- Modify: `K/presentation/navigation/NavGraph.kt`. Only the ForgotPassword route changes.

**Interfaces:**
- Consumes: `BrandScaffold` and `BrandHeader.Compact` (Task 10), `CodeInputField` (Task 9), and `cd_back_to_sign_in` (Task 14).

- [ ] **Step 1: Move the screen onto the scaffold**

  Use `BrandScaffold(BrandHeader.Compact(title = stringResource(R.string.reset_password_title), onBack = { navController.navigateUp() }, backLabel = stringResource(R.string.cd_back_to_sign_in), backEnabled = !state.isBusy), snackbarHostState = …)`.

  Remove the `Scaffold`, `AuthHeader` with its `navigationIcon`, and the body title `Text`. `EmailStep` and `ResetStep` become the sheet's content.

- [ ] **Step 2: Swap the reset code field for `CodeInputField`**

  - In `ResetStep`, the code `ClearChainTextField` becomes `CodeInputField(value = state.code, onValueChange = { onEvent(ForgotPasswordEvent.CodeChanged(it)) }, label = stringResource(R.string.reset_password_code_label), showLabel = true, isError = state.codeError != null, errorMessage = state.codeError, enabled = !state.isBusy, imeAction = ImeAction.Next, onImeAction = { focusManager.moveFocus(FocusDirection.Next) })`.
  - `focusManager` is `LocalFocusManager.current`, so Next moves to New password.
  - Everything else in both steps stays as it is.

- [ ] **Step 3: Delete `AuthHeader` and its imports**

  It now has no callers in `AuthComponents.kt`. `AuthDivider` stays.

- [ ] **Step 4: Draw Forgot password behind the status bar**

  In `NavGraph`, change the ForgotPassword route from `insetComposable` to `composable`.

- [ ] **Step 5: Commit**

  Subject: `Move forgot password onto the brand scaffold and the code input`.

### Task 16: Verify email

**Files:**
- Modify: `K/presentation/auth/verify/EmailVerificationScreen.kt:54-128`
- Modify: `K/presentation/navigation/NavGraph.kt`. Only the EmailVerification route changes.
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `BrandScaffold` and `BrandHeader.Compact` (Task 10), `CodeInputField` (Task 9), and `StatusDisc` (Task 7).
- Produces: the strings `verify_code_label`, `action_verify` and `verify_code_hint`.

- [ ] **Step 1: Add the strings and delete `submit`**

  - `verify_code_label`: "Verification code" / "Mã xác minh".
  - `action_verify`: "Verify" / "Xác minh".
  - `verify_code_hint`: "Didn't get it? Check your spam folder." / "Chưa nhận được? Hãy kiểm tra thư mục thư rác."
  - Delete `submit`, since this screen was its only user.

- [ ] **Step 2: Rebuild the layout**

  Use `BrandScaffold(BrandHeader.Compact(title = stringResource(R.string.email_verify_title)), snackbarHostState = …)`, with no back circle. The sheet holds, centred horizontally:
  1. `StatusDisc(Icons.Default.MarkEmailUnread)`.
  2. `email_verify_sent` with `state.email`, in `bodyMedium` and `onSurfaceVariant`, centred.
  3. `CodeInputField(value = state.code, onValueChange = { viewModel.onEvent(EmailVerificationEvent.CodeChanged(it)) }, label = stringResource(R.string.verify_code_label), showLabel = false, isError = state.codeError != null, errorMessage = state.codeError, enabled = !state.isLoading, imeAction = ImeAction.Done, onImeAction = { if (state.code.length == 6 && !state.isResending) viewModel.onEvent(EmailVerificationEvent.Verify) })`.
  4. `ClearChainButton(text = stringResource(R.string.action_verify), …)`, with the same `onClick`, `enabled` and `loading` as today.
  5. `verify_code_hint` in `bodySmall` and `onSurfaceVariant`, centred.
  6. The Resend `ClearChainOutlinedButton`, unchanged.

  Remove the `OutlinedTextField`, the bare `Icon` and the `sp`/`dp` literals.

- [ ] **Step 3: Draw Verify email behind the status bar**

  In `NavGraph`, change the EmailVerification route from `insetComposable` to `composable`.

- [ ] **Step 4: Commit**

  Subject: `Move email verification onto the brand scaffold and the code input`.

### Task 17: Onboarding frame and step 1

**Files:**
- Modify: `K/presentation/onboarding/OnboardingScreen.kt:62-280` (the screen, `OnboardingHeader` and `Step1Content`)
- Modify: `K/presentation/navigation/NavGraph.kt`. Only the Onboarding route changes.
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `BrandScaffold` and `BrandHeader.Compact` (Task 10).
- Produces: `@Composable private fun StepIntro(step: Int, totalSteps: Int)` in `OnboardingScreen.kt`. Steps 2 and 3 (Tasks 18–19) call it first.

- [ ] **Step 1: Add, change and delete strings**

  - **Add:**
    - `onboarding_step_label_named`: "Step %1$d of %2$d · %3$s" / "Bước %1$d/%2$d · %3$s".
    - `onboarding_step_contact`: "Contact details" / "Thông tin liên hệ".
    - `onboarding_step_location`: "Location and hours" / "Địa điểm và giờ mở cửa".
    - `onboarding_step_submitted`: "Submitted" / "Đã gửi".
    - `onboarding_step1_subtitle`: "How other organizations and our team can reach you" / "Cách các tổ chức khác và đội ngũ của chúng tôi liên hệ với bạn".
    - `onboarding_step2_subtitle`: "Where you're based, and when you're open" / "Bạn ở đâu và mở cửa lúc nào".
  - **Change:**
    - `onboarding_setup_profile`: "Set up your profile" / "Thiết lập hồ sơ của bạn".
    - `hint_phone_profile`: "(206) 555-0100" in both files.
  - **Delete:** `onboarding_welcome` and `onboarding_step_label`.

- [ ] **Step 2: Replace the frame**

  - Use `BrandScaffold(BrandHeader.Compact(title = stringResource(R.string.onboarding_setup_profile), step = state.currentStep, totalSteps = state.totalSteps), snackbarHostState = …, bottomBar = { … })`.
  - The bottom bar holds today's per-step buttons, with their rows spaced `spacedBy(ListGap)` and the old `24.dp`/`16.dp` padding gone.
  - The `AnimatedContent` keeps its transitions and becomes the sheet's content, without its own `weight` and `verticalScroll`.
  - Delete `OnboardingHeader`.

- [ ] **Step 3: Add `StepIntro`**

  - **Eyebrow.** `stringResource(R.string.onboarding_step_label_named, step, totalSteps, name).uppercase(Locale.getDefault())`, in `EyebrowStyle` and `onSurfaceVariant`.
  - **Step names.** `name` is `onboarding_step_contact`, `onboarding_step_location` or `onboarding_step_submitted` for steps 1, 2 and 3.
  - **Subtitle.** For steps 1 and 2, `onboarding_step1_subtitle` or `onboarding_step2_subtitle` follows in `bodyMedium` and `onSurfaceVariant`.

- [ ] **Step 4: Update step 1**

  `Step1Content` starts with `StepIntro(1, state.totalSteps)`. Its padding goes, because the sheet pads, and its spacing becomes `ListGap`. The fields stay unchanged.

- [ ] **Step 5: Draw Onboarding behind the status bar**

  In `NavGraph`, change the Onboarding route from `insetComposable` to `composable`.

- [ ] **Step 6: Commit**

  Subject: `Move onboarding onto the brand scaffold and name each step`.

### Task 18: Onboarding step 2 (findings 12, 13 and 14)

**Files:**
- Modify: `K/presentation/onboarding/OnboardingScreen.kt` (`Step2Content`)
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes:
  - `StepIntro` (Task 17)
  - `TimePickerField(clearLabel = …)` (Task 3)
  - `cd_clear_opening_time` and `cd_clear_closing_time` (Task 4)
  - the reworked `AddressSuggestionField` (Task 8)
  - `BorderWidth` and `FieldEndButtonSize` (Task 2)

- [ ] **Step 1: Add, change and delete strings**

  - **Add:**
    - `onboarding_opens_label`: "Opens" / "Mở cửa".
    - `onboarding_closes_label`: "Closes" / "Đóng cửa".
    - `cd_remove_document`: "Remove document" / "Xóa tài liệu".
  - **Change, in both files:**
    - `onboarding_city_placeholder`: "Seattle".
    - `onboarding_state_placeholder`: "Washington".
    - `onboarding_zip_placeholder`: "98109".
  - **Change, vi only:**
    - `onboarding_state_label`: "Tiểu bang".
    - `onboarding_zip_label`: "Mã ZIP".
  - **Delete:** `onboarding_remove_icon`.

- [ ] **Step 2: Rework the location fields**

  - `Step2Content` starts with `StepIntro(2, state.totalSteps)`. Its padding goes, and its spacing becomes `ListGap`.
  - **City.** Its `leadingIcon` becomes `Icons.Default.LocationCity`.
  - **State and ZIP code.** They lose their `leadingIcon`s, so the envelope goes. Their `Row` is spaced `ListGap`.
  - The address field call stays unchanged.

- [ ] **Step 3: Rework the hours**

  - The "Operating Hours" `OptionalFieldLabel` and its wrapping `Column` go.
  - A `Row` spaced `ListGap` holds two `TimePickerField`s, each `Modifier.weight(1f)`, without the `Box` wrappers:
    - label `onboarding_opens_label`, `clearLabel = stringResource(R.string.cd_clear_opening_time)`
    - label `onboarding_closes_label`, `clearLabel = stringResource(R.string.cd_clear_closing_time)`
  - The events and errors stay as they are.

- [ ] **Step 4: Restyle the document row**

  - **Container.** A `Surface` filled `primaryContainer`, with `ShapeMedium` corners and a `BorderStroke(BorderWidth, outlineVariant)`.
  - **Row.** At least `ControlHeight` tall, padded `ControlPadding` at the sides, with `IconGap` spacing.
  - **Icons and text.** `ControlIconSize` icons. The file name or uploading text is in `onPrimaryContainer`, with `maxLines = 1` and an ellipsis.
  - **Remove button.** An `IconButton` of `FieldEndButtonSize` with an icon at `ControlIconSize`, described as `cd_remove_document`.
  - The upload button and the error line stay unchanged.

- [ ] **Step 5: Commit**

  Subject: `Rework onboarding step 2's location, hours and document fields`.

### Task 19: Onboarding step 3

**Files:**
- Modify: `K/presentation/onboarding/OnboardingScreen.kt` (`Step3Content`)
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `StepIntro` (Task 17) and `StatusDisc` (Task 7).

- [ ] **Step 1: Change and delete strings**

  - **Change** `onboarding_submitted_subtitle`:
    - en: "We'll review your details within 1–3 business days and notify you when you're approved."
    - vi: "Chúng tôi sẽ xem xét thông tin của bạn trong 1–3 ngày làm việc và thông báo khi bạn được duyệt."
  - **Delete** `onboarding_submitted_step_profile`, `onboarding_submitted_step_review` and `onboarding_submitted_step_notify`.

- [ ] **Step 2: Rebuild `Step3Content`**

  - **Order:** `StepIntro(3, state.totalSteps)`, then `StatusDisc(Icons.Default.TaskAlt)`, then `onboarding_submitted_title` in `titleMedium`, then `onboarding_submitted_subtitle` in `bodyMedium` and `onSurfaceVariant`. Centre all of them and space them `ListGap`.
  - **Animation.** Keep the 600ms alpha reveal and drop the scale spring.
  - **Removed.** The gradient discs and the three `DashboardActionCard`s.
  - **Signature.** `Step3Content` gains a `totalSteps: Int` parameter for `StepIntro`.
  - **Imports.** Remove the ones that are now unused: `BrandTeal`, `BrandGreen`, `Brush`, `Color`.

- [ ] **Step 3: Commit**

  Subject: `Turn onboarding step 3 into a short confirmation`.

### Task 20: Pending review (finding 15)

**Files:**
- Modify: `K/presentation/pendingreview/PendingReviewScreen.kt`
- Modify: `K/presentation/navigation/NavGraph.kt`. Only the PendingReview route changes.
- Modify: `K/ui/theme/Dimens.kt`
- Modify: `R/values/strings.xml`, `R/values-vi/strings.xml`

**Interfaces:**
- Consumes: `BrandScaffold` and `BrandHeader.Compact` (Task 10), and the existing `SectionCard` in `SharedDetailComponents.kt`.
- Produces: the token `DetailIconSize` (14.dp, `detail-icon`).

- [ ] **Step 1: Add the token and change the strings**

  - `DetailIconSize` goes in `Dimens.kt`.
  - `pending_review_pending_title`: "Under review" / "Đang xét duyệt".
  - `pending_review_pending_subtitle`: "We're checking the details for %1$s. This usually takes 1–3 business days." / "Chúng tôi đang kiểm tra thông tin của %1$s. Việc này thường mất 1–3 ngày làm việc."
  - `pending_review_rejected_title`: "Changes needed" / "Cần chỉnh sửa".
  - `pending_review_rejected_subtitle`: "Our team couldn't verify %1$s yet. Fix the details below and send them again." / "Đội ngũ của chúng tôi chưa thể xác minh %1$s. Hãy sửa thông tin bên dưới và gửi lại."
  - Search for other callers of the two subtitles, since they gain `%1$s`.

- [ ] **Step 2: Move the screen onto the scaffold**

  - Use `BrandScaffold(BrandHeader.Compact(title = stringResource(if (rejected) R.string.pending_review_rejected_title else R.string.pending_review_pending_title)), snackbarHostState = …)`.
  - The hourglass hero `Surface` goes.
  - `BackHandler`, the `ON_RESUME` refresh and the event collection stay as they are.

- [ ] **Step 3: Lay out the sheet**

  The sheet holds, in order:
  1. **Subtitle.** The subtitle with `user?.name.orEmpty()`, in `bodyMedium` and `onSurfaceVariant`.
  2. **Reason card,** rejected with notes only. A `Surface` filled `errorContainer`, with `ShapeMedium` corners and `CardPadding`. `pending_review_reason_label` is `labelMedium` SemiBold and the notes are `bodySmall`, both in `onErrorContainer`.
  3. **Steps card,** pending only.
     - `SectionCard(title = "")` with the three existing step strings.
     - The icons are `ControlIconSize`: `CheckCircle` in `BrandGreen`, `Schedule` in `primary`, and `Notifications` in `onSurfaceVariant`.
     - The text is `bodySmall`, `IconGap` from the icon.
  4. **Details card.** `SectionCard(title = stringResource(R.string.pending_review_submitted_label))` with these rows: name, type, email, phone (if any), location (if any), and document attached or missing. Each row has an icon at `DetailIconSize` in `onSurfaceVariant`, `IconGap` from its `bodySmall` text.
  5. **Actions** (Step 4).

- [ ] **Step 4: Fix the type row and order the actions**

  - **Type row (finding 15).** It uses a private `@Composable fun organizationTypeLabel(type: OrganizationType): Pair<String, ImageVector>`:
    - `GROCERY` gives `role_grocery_title` with `Icons.Default.Store`.
    - `NGO` gives `role_ngo_title` with `Icons.Default.VolunteerActivism`.
    - `ADMIN` gives `role_admin` with `Icons.Default.AdminPanelSettings`.
    - It replaces `user.type.name.lowercase()…`.
  - **Pending actions.** Check status (`ClearChainButton`, refresh icon), then Edit profile and documents (`ClearChainOutlinedButton`, edit icon, `fillMaxWidth = true`).
  - **Rejected actions.** Fix and resubmit (`ClearChainButton`, edit icon, `onClick = onEditProfile`), then Check status (`ClearChainOutlinedButton`, refresh icon, `fillMaxWidth = true`).
  - **Help and Log out.** These come last in both states. Log out keeps `contentColor = error`.
    - **(Review Focus 1)** They sit side by side, each half the width, when both labels fit on one line. Otherwise they stack at full width.
    - **How to decide.** Wrap them in `BoxWithConstraints`. Measure each label with `rememberTextMeasurer()` in the button text style, then add `ControlIconSize`, `IconGap` and twice `ClearChainButtonDefaults.HorizontalPadding`. If either total is wider than `(maxWidth - ListGap) / 2`, use a `Column` spaced `ListGap`. Otherwise use a `Row` spaced `ListGap` with `weight(1f)` on each button.
  - Every `onClick`, `enabled` and `loading` stays as it is today.

- [ ] **Step 5: Draw Pending review behind the status bar**

  In `NavGraph`, change the PendingReview route from `insetComposable` to `composable`. It's the last brand route, so all seven now draw behind the status bar.

- [ ] **Step 6: Commit**

  Subject: `Move pending review onto the brand scaffold and translate the type`.
