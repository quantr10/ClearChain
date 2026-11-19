# Sign-in and first-run redesign

**Date:** 2026-11-19 · **Branch:** `ui/sign-in` · **Status:** draft for approval

Covers the launcher icon, the system and app splash, Login, Forgot password, Register, Verify email,
Onboarding steps 1–3 and Pending review in the Android app (`Mobile/`). Before screenshots are in
`D:\ClearChain-screens\before\1-sign-in`. The approved mockups are in
`2026-11-19-sign-in-first-run-redesign/` next to this file. Open them in a browser; they load Google
Fonts and Material Icons. `header-a-flow-and-icon.html` shows the header across the flow and the
icon options, `sheet-and-code-input.html` shows the curved sheet and the code input, and
`screen-bodies.html` shows onboarding steps 2–3 and Pending review.

## Intent

The first run is where a grocery or NGO decides whether to trust ClearChain with their organization.
Today it reads as four different apps:

- a 200dp gradient header on Login, Register and Forgot password
- a bare white Verify email screen
- a different gradient header in Onboarding
- a headerless Pending review with a blue hourglass

On top of that, the launcher still shows Android's robot and some Vietnamese text falls back to
English. The redesign gives the flow one look, built from the design system's tokens and shared
components. It also fixes findings 1, 12, 13, 14 and 15 from
`D:\ClearChain-screens\before\README.md`. The organizations that sign up are in the **US**, and
some of them use the app in Vietnamese.

## Goals

- One header system and one "status" picture across all ten screens.
- Fix the five findings:
  - **1:** placeholder launcher icon.
  - **12:** every clear button is announced as "Clear search".
  - **13:** example text mixes countries.
  - **14:** late address suggestions cover City; ZIP has an envelope icon.
  - **15:** Pending review shows the raw type "Grocery" in Vietnamese.
- Work in light and dark, in English and Vietnamese (Vietnamese text runs about 30% longer), on a
  720×1280 (360×640dp) screen, and at font scales up to 200%.
- Screen-reader labels say what each control does.

## Non-goals

- The findings from other sections (2–11, 16–23), including the dashboard header (finding 7).
- Changes to flow, validation or navigation logic. One exception is the inset plumbing in
  `MainActivity`/`NavGraph`, described under *Status bar*.
- The DS-wide contrast shortfalls the design system already documents: 12sp `primary` text and
  button labels, and `outline-variant` field borders. This work adds no new instance where an
  alternative exists.
- The notification icon (`ic_notification`, a bell), the Play Store listing icon, and the TODO.md
  sign-out bugs. Those bugs are in the data layer, even though Pending review has a Log out button.

## Anti-goals

The work fails, even if it looks right, if any of these happen:

- A flow breaks. Watch the splash gate (profile incomplete, then verification, then deep link,
  then dashboard), the verification gate on Pending review, lockout on Login, and the reset email
  that Forgot password hands back to Login.
- Text clips or overlaps in Vietnamese or at large font scales.
- The code input reads to a screen reader as six separate fields.
- A shared-component change visibly breaks a form outside this section.
- dp, colour or alpha literals reappear in the redesigned screens or the new components.

## Constraints

- **UI only.** ViewModels, `*State`, `*Event`, use cases, API calls and DTOs stay unchanged.
  Everything here works through existing state and events.
- **Tokens.** Use the theme tokens (`ui/theme`) and the shared components (`presentation/components`).
  - The ten screens and the new components (`BrandScaffold`, `StatusDisc`, `CodeInputField`) contain
    no dp, colour or alpha literals.
  - In existing shared components, lines this work changes follow the same rule. Their other
    literals are out of scope.
  - Where no token fits, add one to `Dimens.kt`, `Spacing.kt` or `Alpha.kt` with a KDoc naming its
    design-system token, and add it to the design system (see *Design system update*).
- **Strings.**
  - Every new or changed string goes in both `res/values/strings.xml` and
    `res/values-vi/strings.xml`.
  - Counts use `<plurals>`.
  - Keys that become unused are removed.
- **No automated tests.** Verification is by build, `spotlessCheck`, `lint` and running the flow
  (see *Verification*).
- **Live data.** `DATABASE_URL` is the live Supabase database, so registering creates real
  organizations.
- **Code style.** Star imports, no trailing commas, KDoc on declarations, and `// ── Title ───`
  section dividers.

## Approaches

1. **Shared pieces first, then the screens (chosen).**
   - Build `BrandScaffold`, `StatusDisc` and `CodeInputField`.
   - Fix the clear-button labels in `ClearChainTextField`, `TimePickerField` and `DatePickerField`.
   - Rework `AddressSuggestionField`.
   - Add the tokens.
   - Make the icon and splash a resource-only change.
   - Move the ten screens onto the shared pieces.
   - Trade-off: the finding 12 and 14 fixes also change forms elsewhere (account details, listing
     editor, cart pickup), so those need checking.
2. **The same, plus `androidx.core:core-splashscreen`.** Android 8–11 would get the same splash as
   12+. Rejected: it adds a dependency and a `MainActivity` change to improve under a second on
   older phones.
3. **Restyle each screen in place.** Rejected: it keeps three header implementations, the cause of
   the "four apps" look, and every later header change touches ten files.

## Decisions made with the user

- **Header A, with a curved sheet.**
  - Login keeps a logo hero, made shorter.
  - Every other screen gets one compact gradient header: an optional back circle, a 24sp Bold
    title, and step bars in onboarding.
  - Content sits on a sheet with rounded top corners that rises over the gradient.
  - The gradient runs behind the status bar.
- **Launcher icon:** the white `eco` leaf on the brand gradient. The system splash is flat brand
  teal with the leaf in its 20% white disc.
- **Code entry:** six boxes, used on Verify email and on the reset step of Forgot password.
- **US locale for examples:** every example is in Seattle.
- **The screen bodies** in `mockups/screen-bodies.html` were approved as shown, apart from the
  deviations listed under *Calls I made*.

## Design

### Shared pieces

#### `BrandScaffold` (new, `presentation/components/BrandScaffold.kt`)

It replaces `AuthHeader` in `auth/AuthComponents.kt` (`AuthDivider` stays), `OnboardingHeader`, the
plain `Scaffold`s of Verify email and Pending review. The same file exports `BrandGradient` (the
brush) and `LightStatusBarIcons()` (the status-bar effect below), which Splash uses for its own
full-screen layout. It has two header modes:

- **Hero, Login only.**
  - Content: the logo disc (64dp, `OnBrandGradient` at `Alpha.LogoDisc`) with the 38dp `eco` leaf,
    and the `WordmarkHeaderStyle` "ClearChain" below it, centred. There's no subtitle on the
    gradient.
  - The hero scrolls away with the content. A strip of `BrandTeal`, the height of the status bar,
    stays pinned behind it, so the light status-bar icons stay readable over scrolled content.
  - Height: the status bar, then the disc and wordmark with token spacing, then the sheet overlap.
    That's about 60dp shorter than today's 200dp header.
- **Compact, every other screen.**
  - **Back circle (optional):** a `BackButtonSize` (36dp) disc in `OnBrandGradient` at
    `Alpha.LogoDisc`, with the auto-mirrored back arrow in `OnBrandGradient`. Its label is
    "Back to sign in" (`cd_back_to_sign_in`) on Register and Forgot password, the two screens
    that use it, because that's what it does.
  - **Title:** `headlineSmall` (Bold 24sp) in `OnBrandGradient`. It wraps rather than truncates,
    and carries `heading()` semantics. 24sp Bold counts as large text, and white passes at 3.3:1
    on green and 3.7:1 on teal.
  - **Steps (optional):** onboarding's segment bars, one per step, at the new
    `StepBarHeight` (4dp). Bars are `IconGap` apart. The track is `OnBrandGradient` at
    `Alpha.LogoDisc` and the fill is `OnBrandGradient`. The fill keeps today's 500ms animation.
    The bars are hidden from accessibility, because the eyebrow text says the same thing.
  - The header stays fixed. The sheet scrolls under it.
- **Gradient.** `Brush.verticalGradient(BrandTeal, BrandGreen)` runs from the top of the status bar
  to the sheet's top edge, so it covers the same span as today's header. Below the sheet's top
  edge the screen is `background`.
- **Sheet.**
  - The top corners use the radius of `MaterialTheme.shapes.extraLarge` (28dp, `shape-extra-large`,
    the bottom-sheet corners). The bottom corners are square.
  - The fill is `MaterialTheme.colorScheme.background`.
  - It overlaps the gradient by `ScreenHPadding`, so the gradient shows in the corners.
  - Content padding is `ScreenHPadding` at the sides and `ScreenHPadding` at the top. Content is
    stacked with `ListGap`.
- **Slots.**
  - `content` goes in a vertical scroll inside the sheet.
  - `bottomBar` is optional, fixed at the bottom of the sheet with `ScreenPadding`. Onboarding uses
    it for Back, Continue and Complete.
  - `snackbarHost` is optional.
- **Insets and keyboard.** The scaffold applies `statusBarsPadding` inside the gradient. With the
  keyboard open, the focused field and the bottom bar must stay visible. Onboarding's bottom bar
  sits above the keyboard, as it does today.
- **Status bar icons** are light on these screens in both themes. The scaffold sets
  `isAppearanceLightStatusBars = false` while it's shown and restores the theme's value when it
  leaves. `ClearChainTheme` sets the value today as `!darkTheme`.
- The API is left to the builder. A sealed `BrandHeader` with `Hero` and
  `Compact(title, onBack, backLabel, step)` is one option.

#### Status bar (one change outside the screens)

`MainActivity`'s outer `Scaffold` passes its `paddingValues`, including the status-bar inset, to
the whole `NavGraph` ([MainActivity.kt:127](../../../Mobile/app/src/main/java/com/clearchain/app/MainActivity.kt#L127)).
That's why every screen sits under a grey strip today.

- The ten routes (`Splash`, `Login`, `Register`, `ForgotPassword`, `EmailVerification`,
  `Onboarding`, `PendingReview`) must get no top inset from the outer layer. `BrandScaffold` then
  draws behind the status bar.
- Every other route must look exactly as it does now.
- **Requirement:** nothing jumps during navigation transitions. One way is to stop applying the top
  inset around the `NavHost` and apply `statusBarsPadding` per destination for every non-brand
  route in `NavGraph`. Switching the outer padding by current route makes the outgoing screen shift
  by the inset during the crossfade. The builder picks the mechanism.

#### `StatusDisc` (new, extracted from `EmptyState`)

`EmptyState.kt` draws the design system's empty-state picture inline with literals: a 56dp
`primaryContainer` disc, a 28dp `onPrimaryContainer` icon, and rings of `primary` at 8% and 12%,
100dp across. That picture moves into `StatusDisc(icon)` with tokens:

- `StatusDiscSize` 56
- `StatusDiscIconSize` 28
- `StatusRingsSize` 100
- `Alpha.RingOuter` 0.08
- `Alpha.RingInner` 0.12

`EmptyState` uses `StatusDisc`, with no visible change. Verify email and Onboarding step 3 use it
too. It's decorative, so it has no content description.

#### `CodeInputField` (new, `presentation/components/CodeInputField.kt`)

- **Field.** One `BasicTextField`, drawn as `length` cells (default 6).
  - It accepts digits only, up to `length`. A paste keeps the first `length` digits and drops spaces
    and other characters.
  - Keyboard: `KeyboardType.Number`. The IME action is the caller's. On Verify email it's Done,
    which sends the existing Verify event. On Forgot password it's Next, which moves focus to New
    password. Submitting from the code field there would flag the two empty password fields.
  - It doesn't submit on its own when full, so behaviour stays as it is today.
- **Cells.**
  - Cells share the row width equally (`weight(1f)`), with `ListGap` between them. Six cells fit in
    360dp minus the screen padding at every font scale.
  - Height is at least the new `CodeCellMinHeight` (52dp), and it grows with the font scale.
  - Corners use `ButtonShape`. Fill is `surface`.
  - Borders: 1dp `outlineVariant` at rest, `primary` on the cell that takes the next digit (with a
    caret), and `error` on all cells when `isError` is set. Disabled cells use `Alpha.Disabled`.
  - Digits are `headlineSmall` Bold in `onSurface`, centred.
- **Label and error.** The field always takes a `label`. A visible copy above the cells, in
  `ClearChainTextField`'s label style, is optional.
  - Verify email passes "Verification code" (new `verify_code_label`) and hides it.
  - Forgot password passes "Reset code" (`reset_password_code_label`) and shows it.
  - An error line sits below, styled like `ClearChainTextField`'s.
- **Semantics.** The whole row is one text field, never six.
  - Content description: plurals `cd_code_field` ("%1$s, %2$d digits"), built from the label, so
    the spoken name contains the visible one ("Reset code, 6 digits").
  - State description: plurals `cd_code_digits_entered` ("%d digits entered").
  - Errors are announced with `error()`.
  - The builder adds one-time-code autofill if the Compose version supports it.

#### Clear-button labels (finding 12)

- **`ClearChainTextField`** gains `clearLabel: String? = null`. The clear button's description is
  the first of these that exists:
  1. `clearLabel`
  2. `"Clear %1$s"` (`cd_clear_field`) with the field's own `label`
  3. `"Clear %1$s"` with the label of the `FieldCard` around it (see below)
  4. "Clear text" (`cd_clear_text`)
- **`TimePickerField` and `DatePickerField`** gain the same parameter and chain. Their last
  fallbacks are "Clear time" (`cd_clear_time`) and "Clear date" (`cd_clear_date`).
- **`FieldCard`** provides its label to the fields inside it through a new `LocalFieldLabel`
  composition local. About 20 unlabelled fields in Account details, Create listing, Edit listing
  and Cart pickup sit in a `FieldCard`, so they name their field with no change to those screens.
- **Paired time fields** share one card label, so their callers pass `clearLabel`: "Clear opening
  time" and "Clear closing time". That covers Onboarding and the two hours fields in
  `AccountDetailScreen`.
- **`SearchBar`** keeps "Clear search", because there it's correct.
- **Register's custom email clear button** (`RegisterEvent.ClearEmail`) uses `cd_clear_field` with
  the email label.

#### Controls that grow with the text

At 200% font scale, Android's non-linear scaling turns 12sp into about 24sp, and the line height
reaches 32dp. Today the controls have a fixed 32dp height, so any wrapped label is cut off.

- `ClearChainButton` and `ClearChainOutlinedButton` change `requiredHeight(ControlHeight)` to
  `heightIn(min = ControlHeight)`, and their labels may wrap.
- `ClearChainTextField`, `TimePickerField` and `DatePickerField` change their single-line
  `height(...)` to `heightIn(min = ...)`.
- Nothing changes at normal text size. The design system's `control-height` becomes a minimum.

#### `AddressSuggestionField` (finding 14)

- **Inline list.** Suggestions render in the layout, directly under the field, inside a
  `ClearChainCard`-style card. They push the next field down. Today they're a `Popup` that covers
  it.
- **States while the field has focus:**
  - **Searching:** an `InlineSpinner` with "Searching addresses…" (`address_searching`). It shows
    as soon as the debounced lookup starts. Debounce stays at 300ms and the minimum stays at three
    characters.
  - **Results:** up to five rows. The first line is `streetAddress`. The second line is
    "City, State ZIP", built from the structured fields, so it never shows a country name in either
    language. The state is shown as the geocoder returns it ("Seattle, Washington 98109").
    Picking a row fills State with that same value, as it does today. Mapping it to a two-letter
    code would change what gets stored.
  - **No results:** "No matching addresses. Fill in the fields below." (`address_no_matches`).
  - **Lookup error:** the list hides, as it does today.
- **Focus.** The list shows only while the field has focus. Losing focus cancels the lookup and
  clears the list. A late answer can therefore no longer appear under a finger aimed at City.
- **US only.** Request about ten results and keep the first five whose `countryCode` is `US`.
- **Rows.**
  - Each row is a button whose click label is "Use this address" (`cd_use_address`).
  - Each row is at least 48dp tall.
  - The place icon is `primary`.
- `AccountDetailScreen` uses this field too and gets the same behaviour.

#### Tokens to add

| Kotlin | Value | Design-system token | Used by |
|---|---|---|---|
| `LogoDiscHeroSize`, `LogoMarkHeroSize` | 64, 38dp | `logo-disc-hero`, `logo-mark-hero` | Login hero |
| `LogoDiscSplashSize`, `LogoMarkSplashSize` | 96, 56dp | `logo-disc-splash`, `logo-mark-splash` | App and system splash |
| `StepBarHeight` | 4dp | `step-bar` | Onboarding header |
| `StatusDiscSize`, `StatusDiscIconSize`, `StatusRingsSize` | 56, 28, 100dp | `status-disc`, `status-disc-icon`, `status-rings` | `StatusDisc`, `EmptyState` |
| `CodeCellMinHeight` | 52dp | `code-cell` | `CodeInputField` |
| `Alpha.RingOuter`, `Alpha.RingInner` | 0.08, 0.12 | `ring-outer`, `ring-inner` | `StatusDisc` |
| `FieldEndButtonSize` | 24dp | `field-end-button` | Clear and visibility buttons at a field's end |
| `BorderWidth` | 1dp | `border-width` | Field, cell, role-card and document-row borders |
| `MinTouchTarget` | 48dp | `touch-target` | Address suggestion rows, the Login link |
| `TileIconSize` | 24dp | `tile-icon` | Register role cards (the size the design system gives action-card tiles) |
| `DetailIconSize` | 14dp | `detail-icon` | Pending review detail rows (the design system's card detail rows) |

`Alpha.LogoDisc`'s KDoc widens to "white at this alpha on the brand gradient: the logo disc, the
header back circle and the step-bar track". The builder may add a token for a gap that none of the
spacing tokens fits, following the same rule.

### Launcher icon and system splash (finding 1)

- **Colours.** Add `brand_teal` (#0D9488) and `brand_green` (#16A34A) to `res/values/colors.xml`,
  with a comment that they mirror `BrandTeal` and `BrandGreen`.
- **Adaptive icon** (`mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml`):
  - Background: a 108dp vector with a vertical linear gradient from `@color/brand_teal` to
    `@color/brand_green`.
  - Foreground: Material's `eco` path in white, unmodified, centred and about 44dp wide, inside the
    66dp safe zone.
  - Monochrome: the same leaf, for Android 13 themed icons.
- **Robot files.** Delete the robot `ic_launcher*.webp` files in `mipmap-*dpi`. With `minSdk` 26
  the adaptive icon covers every device.
- **Android 12 and later** (`res/values-v31/themes.xml`, with the same parent as
  `values/themes.xml`):
  - `windowSplashScreenBackground` is `@color/brand_teal`.
  - `windowSplashScreenAnimatedIcon` is a new `splash_mark` vector: 288dp, with a 96dp disc of
    white at 20% and the 56dp white leaf, centred. That's the app splash's disc at its final size.
  - Status-bar icons are light.
- **Android 8–11:** the cold-start window background is `@color/brand_teal`. After the first frame,
  no teal shows anywhere it didn't before, including during keyboard animations. If that can't be
  guaranteed, drop this part and keep the default window.

### Screens

Each screen keeps its ViewModel, state, events and navigation exactly. Only the layout, copy and
semantics change.

**Splash**
- `BrandGradient` full screen, behind the status bar, with `LightStatusBarIcons()`.
- The 96dp disc with the 56dp leaf sits at the **centre of the screen**, where the system splash
  drew it. It stays still: the scale-in goes.
- The wordmark (`WordmarkStyle`) and the tagline (`OnBrandGradient` at `Alpha.Subtitle`) sit below
  the disc and keep their fade-in.
- The bottom spinner uses the shared area spinner in `OnBrandGradient`.
- The tagline's contrast is below AA. It's decorative, it shows for 1.6s, and the design system
  restricts it to this screen. Noted, not changed.
- The 1.6s delay and all routing are unchanged.

**Login**
- `BrandScaffold` with the hero.
- The sheet holds:
  - "Welcome back" (`welcome_back`, `titleMedium`) and "Sign in to your account"
    (`sign_in_to_account`, `bodyMedium`, `onSurfaceVariant`).
  - The email and password fields.
  - A row with "Remember me" and "Forgot password?". The checkbox and its text become one
    toggleable row with `Role.Checkbox`. "Forgot password?" changes from an outlined button to a
    `primary` text link with a 48dp touch target.
  - Sign In, the "or" divider and Create New Account, as today.
- The form's fade-in, the lockout snackbar and the reset-email handoff are unchanged.

**Forgot password**
- Compact header with the back circle and "Reset your password" (`reset_password_title`). Today's
  header subtitle ("Forgot password?") and the duplicate body title go.
- **Email step:** the intro (`reset_password_email_intro`) under the header, then the email field
  and Send code.
- **Reset step:**
  - the intro (`reset_password_code_intro`)
  - `CodeInputField` with the visible label "Reset code", IME action Next moving to New password
  - new password and confirm
  - Reset, then Resend (outlined, with the cooldown), then the existing text button, all as today

**Register**
- Compact header with the back circle and "Create your account". Below it sits "Help reduce food
  waste in your community" (new `register_subtitle`). "Join ClearChain" goes.
- **Role cards:**
  - "I'm signing up as" (`register_role_label`) labels the two cards.
  - The cards keep today's icons (`Store`, `VolunteerActivism`) and strings, and are restyled with
    tokens.
  - Each card is a `Role.RadioButton` with `selected` state, inside a `selectableGroup`.
  - In Vietnamese the text wraps to as many lines as it needs, and both cards stay the same height.
- The fields, strength meter and email-availability indicator stay as today.
- The terms row becomes one toggleable row with `Role.Checkbox`. "Terms of Service" and "Privacy
  Policy" stay styled text, as today, and tapping anywhere in the row toggles the checkbox. Making
  them real links would be new behaviour, so it's a follow-up.
- Create Account, the "or" divider and Sign In stay as today.

**Verify email**
- Compact header, no back circle, "Verify your email".
- The sheet holds:
  - `StatusDisc(MarkEmailUnread)`
  - `email_verify_sent` with the address, centred
  - `CodeInputField` labelled "Verification code" (`verify_code_label`), label hidden, IME action
    Done sending Verify
  - **Verify** (new `action_verify`, replacing "Submit"), filled
  - "Didn't get it? Check your spam folder." (`verify_code_hint`, `bodySmall`, `onSurfaceVariant`)
  - Resend code, outlined with the cooldown, as today
- The Material `OutlinedTextField` and its hard-coded sizes go.

**Onboarding**
- **Header:** compact, no back circle, titled "Set up your profile" (`onboarding_setup_profile`,
  changed value), with three step bars. "Welcome to ClearChain" goes.
- **Under the header, per step:**
  - an eyebrow in `EyebrowStyle`, uppercase, in `onSurfaceVariant`: "Step %1$d of %2$d · %3$s",
    with the step names Contact details, Location and hours, and Submitted
  - a `bodyMedium` subtitle on steps 1 and 2
- **Bottom bar:** in `BrandScaffold`'s slot, with buttons as today.
- **Step transitions:** the `AnimatedContent` slide stays.
- **Step 1:** fields as today. The phone placeholder becomes "(206) 555-0100".
- **Step 2:**
  - **Address:** the reworked `AddressSuggestionField`.
  - **City:** the `LocationCity` icon, placeholder "Seattle".
  - **State and ZIP code:** half width, **no leading icons** (this removes the envelope), with
    placeholders "Washington" and "98109". "Washington" matches what a picked suggestion fills in.
  - **Hours:** two `TimePickerField`s labelled **Opens** and **Closes**, with `clearLabel` "Clear
    opening time" and "Clear closing time". The "Operating Hours" group label goes.
  - **Pickup instructions:** groceries only, as today.
  - **Verification document:** restyled with tokens. The remove button reads "Remove document"
    (`cd_remove_document`).
- **Step 3:**
  - `StatusDisc(TaskAlt)` and "Submitted for review"
  - "We'll review your details within 1–3 business days and notify you when you're approved."
    (changed `onboarding_submitted_subtitle`)
  - The three `DashboardActionCard`s go. That list now lives only on Pending review.
  - Back and View status, as today.

**Pending review**
- **Header:** compact, no back circle. The title is "Under review" when pending and "Changes
  needed" when rejected.
- **The sheet, in order:**
  1. **Subtitle:** pending reads "We're checking the details for %1$s. This usually takes 1–3
     business days." Rejected reads "Our team couldn't verify %1$s yet. Fix the details below and
     send them again." `%1$s` is `user.name`.
  2. **Reason card, rejected only:** `errorContainer` fill, with the label and the notes in
     `onErrorContainer` (6.8:1). It replaces today's 40% tint with `error` text.
  3. **Steps card, pending only:** a `SectionCard`. Submitted is `CheckCircle` in `BrandGreen`.
     In review is `Schedule` in **`primary`** (today it's `secondary` blue). Notify is
     `Notifications` in `onSurfaceVariant`. The strings stay as today.
  4. **"What you submitted" card:** name, then the **type**, then email, phone, location and
     document.
     - The type fixes finding 15. It shows the Register wording with the Register icon:
       `role_grocery_title` with `Store`, or `role_ngo_title` with `VolunteerActivism`, and
       `role_admin` as a fallback. It replaces `type.name` formatting.
  5. **Actions.**
     - **Pending:** Check status (filled), Edit profile and documents (outlined, full width), then
       Help and Log out (outlined, Log out in `error`).
     - **Rejected:** Fix and resubmit (filled), Check status (outlined, full width), then Help and
       Log out.
     - Help and Log out share a row and stack when they don't fit (`FlowRow`).
     - Every button sends the same event or callback as today.
- **Unchanged:** the hourglass hero goes, but `BackHandler`, the refresh on resume and logout work
  as today.

### Strings

New keys. All of them go in both files.

| Key | en | vi |
|---|---|---|
| `cd_back_to_sign_in` | Back to sign in | Quay lại đăng nhập |
| `cd_clear_field` | Clear %1$s | Xóa %1$s |
| `cd_clear_text` | Clear text | Xóa nội dung |
| `cd_clear_time` | Clear time | Xóa giờ |
| `cd_clear_date` | Clear date | Xóa ngày |
| `cd_clear_opening_time` | Clear opening time | Xóa giờ mở cửa |
| `cd_clear_closing_time` | Clear closing time | Xóa giờ đóng cửa |
| `cd_remove_document` | Remove document | Xóa tài liệu |
| `cd_use_address` | Use this address | Dùng địa chỉ này |
| `cd_code_field` (plurals) | %1$s, %2$d digits | %1$s, %2$d chữ số |
| `verify_code_label` | Verification code | Mã xác minh |
| `cd_code_digits_entered` (plurals) | one: %d digit entered · other: %d digits entered | other: Đã nhập %d chữ số |
| `address_searching` | Searching addresses… | Đang tìm địa chỉ… |
| `address_no_matches` | No matching addresses. Fill in the fields below. | Không tìm thấy địa chỉ phù hợp. Hãy điền các ô bên dưới. |
| `register_subtitle` | Help reduce food waste in your community | Chung tay giảm lãng phí thực phẩm tại cộng đồng của bạn |
| `register_role_label` | I'm signing up as | Tôi đăng ký với tư cách |
| `action_verify` | Verify | Xác minh |
| `verify_code_hint` | Didn't get it? Check your spam folder. | Chưa nhận được? Hãy kiểm tra thư mục thư rác. |
| `onboarding_step_label_named` | Step %1$d of %2$d · %3$s | Bước %1$d/%2$d · %3$s |
| `onboarding_step_contact` | Contact details | Thông tin liên hệ |
| `onboarding_step_location` | Location and hours | Địa điểm và giờ mở cửa |
| `onboarding_step_submitted` | Submitted | Đã gửi |
| `onboarding_step1_subtitle` | How other organizations and our team can reach you | Cách các tổ chức khác và đội ngũ của chúng tôi liên hệ với bạn |
| `onboarding_step2_subtitle` | Where you're based, and when you're open | Bạn ở đâu và mở cửa lúc nào |
| `onboarding_opens_label` | Opens | Mở cửa |
| `onboarding_closes_label` | Closes | Đóng cửa |

For `register_subtitle`, match the existing Vietnamese of `ngo_dashboard_subtitle` if it reads
better.

Changed values, in both files:

| Key | en | vi |
|---|---|---|
| `hint_phone_profile` | (206) 555-0100 | (206) 555-0100 |
| `onboarding_city_placeholder` | Seattle | Seattle |
| `onboarding_state_placeholder` | Washington | Washington |
| `onboarding_zip_placeholder` | 98109 | 98109 |
| `onboarding_state_label` | State (unchanged) | Tiểu bang |
| `onboarding_zip_label` | ZIP Code (unchanged) | Mã ZIP |
| `onboarding_setup_profile` | Set up your profile | Thiết lập hồ sơ của bạn |
| `onboarding_submitted_subtitle` | We'll review your details within 1–3 business days and notify you when you're approved. | Chúng tôi sẽ xem xét thông tin của bạn trong 1–3 ngày làm việc và thông báo khi bạn được duyệt. |
| `pending_review_pending_title` | Under review | Đang xét duyệt |
| `pending_review_pending_subtitle` | We're checking the details for %1$s. This usually takes 1–3 business days. | Chúng tôi đang kiểm tra thông tin của %1$s. Việc này thường mất 1–3 ngày làm việc. |
| `pending_review_rejected_title` | Changes needed | Cần chỉnh sửa |
| `pending_review_rejected_subtitle` | Our team couldn't verify %1$s yet. Fix the details below and send them again. | Đội ngũ của chúng tôi chưa thể xác minh %1$s. Hãy sửa thông tin bên dưới và gửi lại. |

`hint_phone_profile` is also the Profile screen's phone placeholder, which is intended. The two
subtitles gain a `%1$s`, so update every call site.

Remove the keys that become unused. Check each with a search first, because some may be shared:

- `onboarding_welcome`, `join_clearchain`
- `onboarding_submitted_step_*`, `onboarding_remove_icon`
- `submit`, `onboarding_step_label`

`onboarding_hours_label` stays, because `AccountDetailScreen` still uses it.

### Findings map

| Finding | Fixed by |
|---|---|
| 1 Placeholder launcher icon | Adaptive leaf icon, monochrome layer, Android 12+ splash attributes, the teal window on Android 8–11, robot webps deleted |
| 12 Every clear button says "Clear search" | `clearLabel` and the label chain in three components, `FieldCard`'s `LocalFieldLabel`, the Register email clear, `clearLabel` on paired time fields |
| 13 Mixed-country examples | Seattle examples in both languages, Vietnamese state and ZIP labels in US terms |
| 14 Late suggestions over City, ZIP envelope | Inline list, searching and no-match states, focus-bound lookup, US-only results, no ZIP icon |
| 15 Untranslated type on Pending review | Type row uses `role_*_title` and the Register icons |

## Verification

The implementation plan leaves verification out, at the user's request. These are the checks for
whoever verifies afterwards. One question is still open for the user: whether account-bound
screens (Verify email, Onboarding, Pending review pending and rejected) are checked with test
credentials, by the user signing in on the emulator, or by code review only.

- **Build and lint:**
  - `./gradlew assembleDebug spotlessCheck lint` from `Mobile/`, with no new lint warnings.
  - A search of the touched screens and the new components finds no `.dp` literals,
    `Color(`/`Color.White`, or `alpha =` with a number.
- **Visual matrix** on the 720×1280 Android 16 emulator used for the before set. Check every screen
  in dark-en, light-en and light-vi. Also check Register, Onboarding step 2, Verify email and
  Pending review (pending and rejected) at font scales 1.3 and 2.0 in English and Vietnamese.
  Nothing clips, overlaps or truncates a title.
- **Splash.** Cold start on Android 12+ shows teal with the leaf disc, then the gradient splash with
  the disc in the same place. Check once on an Android 8–11 image if one is available.
- **TalkBack:**
  - Every clear button names its field.
  - The code row is one field: "Verification code, 6 digits", then "3 digits entered".
  - The back circle says "Back to sign in".
  - Role cards announce selected or not selected.
  - Remember me is one checkbox.
  - Address rows say "Use this address".
  - The step bars are silent.
- **Finding 14 scenario.** Type an address and move to City before the results arrive: nothing
  appears. Type and wait: "Searching addresses…", then the list inline under Address, with City
  pushed down.
- **Regression spot-checks on shared components.** In Account details, the address field and the
  hours clear labels. In Create listing, Edit listing and Cart pickup, the date clear labels. In
  any search bar, "Clear search" still says that.
- **Flows.** Run each once:
  - a cold start with each kind of session (signed out, profile incomplete, pending, verified)
  - lockout on Login
  - forgot password through to the reset email landing back on Login
- **Live database.** Registering creates a real organization. Reuse the existing Kroger test account
  where possible, and agree with the user before creating new accounts.
- **After set.** Capture `D:\ClearChain-screens\after\1-sign-in` with the same file names as the
  before set.

## Design system update

The theme mirrors the design system, so the artifact
(`https://claude.ai/artifact/W5MSxyo1uFx8jmaEF2iCuS`) gets these changes. They're published only
once the user OKs it.

- `tokens.json`: the tokens in the table above, and the wider `logo-disc` usage note.
- **New component pages:** `BrandScaffold` (replacing the "sign-in header" description),
  `CodeInputField` and `StatusDisc`, in the same format as the existing pages.
- **`TextField` README:** the "Clear search" line under *Watch for* is replaced by the
  `clearLabel` rule and `FieldCard`'s label.
- **`control-height`:** now a minimum. Buttons and fields grow when large text needs room.
- **`README.md`:**
  - Iconography: the launcher icon is now the leaf on the gradient.
  - Colour: the gradient runs behind the status bar on brand screens.
  - Spacing and layout: brand screens use a compact header and a sheet.

## Left to the builder

- `BrandScaffold`'s API and its internal structure.
- The inset mechanism, as long as nothing jumps.
- Focus and IME handling.
- Animation details.
- The shape of the type-label helper.
- Scaling the vector path in the icon and the splash mark.
- Names of any extra spacing tokens.
- Whether `CodeInputField` shows a blinking caret.

## Calls I made

These weren't discussed with the user. Check them while reading.

1. **Subtitles move under the header.** They no longer sit on the gradient, for contrast. The user
   saw and approved this in the mockups.
2. **The onboarding eyebrow is `onSurfaceVariant`, not the teal shown in the mockup.** 12sp teal on
   the background is 3.7:1, which fails AA.
3. **Step 2's subtitle reads "Where you're based, and when you're open".** The mockup said "Where
   NGOs pick up…", which is wrong for an NGO filling it in. Step 1's subtitle says "other
   organizations" for the same reason.
4. **Pending review's steps card keeps today's three strings.** The "In progress" second line from
   the mockup is dropped, so no new strings are needed there.
5. **Verify email keeps an outlined Resend button**, as on Forgot password, instead of the inline
   link shown in the mockup. The two code screens match.
6. **The button on Verify email changes from "Submit" to "Verify".**
7. **The back circle is labelled "Back to sign in"** rather than "Back".
8. **The splash disc stops scaling in**, so it doesn't jump from where the system splash left it.
9. **The address list is US-only**, by country code, and shows "City, State ZIP" with no country.
10. **State and ZIP lose their icons.** City's icon becomes `LocationCity`.
11. **Vietnamese labels for State and ZIP become "Tiểu bang" and "Mã ZIP"**, the US terms.
12. **No auto-submit when the sixth digit is typed.** Behaviour stays as it is today.
13. **Field-label casing stays as it is.** The mockups' sentence case was incidental.

These came from the reviewer's read of the design:

14. **Buttons and fields grow with large text.** They have a 32dp minimum height instead of a fixed
    one, which is the only way to meet the 200% requirement. Nothing changes at normal text size.
15. **State keeps the geocoder's full name ("Washington")**, and the placeholder matches it. The
    mockups showed "WA". A two-letter mapping would change what gets stored.
16. **Unlabelled fields take their `FieldCard`'s label for the clear button.** That finishes finding
    12 across the app without editing the screens outside this section.
17. **Login pins a teal strip behind the status bar** once the hero scrolls away.
18. **The Forgot password code field's keyboard action is Next**, and the spoken name of the code
    field is built from its label.
19. **The Register terms text stays non-link text.** The earlier draft wrongly said the links were
    separately tappable.
