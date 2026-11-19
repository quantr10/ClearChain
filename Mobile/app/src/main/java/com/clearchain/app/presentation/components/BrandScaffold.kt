package com.clearchain.app.presentation.components

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import com.clearchain.app.R
import com.clearchain.app.ui.theme.*

/** Design system brand gradient: `brand-teal` at the top into `brand-green`. */
val BrandGradient: Brush = Brush.verticalGradient(listOf(BrandTeal, BrandGreen))

/** How many composed screens currently want light status-bar icons. Main thread only. */
private var lightStatusBarRequests = 0

/** How long a step bar takes to fill. */
private const val STEP_FILL_MS = 500

/** Below this background luminance the theme is treated as dark. */
private const val DARK_LUMINANCE = 0.5f

/**
 * Keeps the status-bar icons light while composed. Two brand screens overlap during a navigation
 * crossfade, so this counts its callers and restores the theme's choice only when the last one
 * leaves. It reapplies after every composition, because `ClearChainTheme` sets the icons too.
 */
@Composable
fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    val isDark by rememberUpdatedState(MaterialTheme.colorScheme.background.luminance() < DARK_LUMINANCE)
    val window = (view.context as Activity).window

    DisposableEffect(view) {
        lightStatusBarRequests++
        onDispose {
            lightStatusBarRequests--
            if (lightStatusBarRequests == 0) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }
    SideEffect {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }
}

/** What the gradient header of a [BrandScaffold] shows. */
sealed interface BrandHeader {
    /** Login's logo hero, which scrolls away with the content. */
    data object Hero : BrandHeader

    /**
     * A fixed header with a [title]. [onBack] adds a back circle announced as [backLabel], and
     * [step] of [totalSteps] adds the onboarding step bars.
     */
    data class Compact(
        val title: String,
        val onBack: (() -> Unit)? = null,
        val backLabel: String? = null,
        val backEnabled: Boolean = true,
        val step: Int? = null,
        val totalSteps: Int? = null
    ) : BrandHeader
}

/**
 * The frame of the sign-in and first-run screens: a brand-gradient header that runs behind the
 * status bar, and a sheet with rounded top corners that rises over it and holds [content]. A
 * [BrandHeader.Compact] header stays put while the sheet scrolls; the [BrandHeader.Hero] scrolls
 * away with the content. [bottomBar] sits fixed at the bottom of the sheet, above the keyboard.
 */
@Composable
fun BrandScaffold(
    header: BrandHeader,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    LightStatusBarIcons()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (header) {
            BrandHeader.Hero -> HeroLayout(snackbarHostState, bottomBar, content)
            is BrandHeader.Compact -> CompactLayout(header, snackbarHostState, bottomBar, content)
        }
    }
}

// ── Compact header ───────────────────────────────────────────────────────────

@Composable
private fun CompactLayout(
    header: BrandHeader.Compact,
    snackbarHostState: SnackbarHostState?,
    bottomBar: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandGradient)
                .statusBarsPadding()
                .padding(horizontal = ScreenHPadding)
                .padding(top = ScreenVPadding, bottom = ScreenHPadding + SheetOverlap),
            verticalArrangement = Arrangement.spacedBy(CardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CardPadding)
            ) {
                header.onBack?.let { onBack ->
                    IconButton(
                        onClick = onBack,
                        enabled = header.backEnabled,
                        modifier = Modifier.size(BackButtonSize),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = OnBrandGradient.copy(alpha = Alpha.LogoDisc),
                            contentColor = OnBrandGradient,
                            disabledContainerColor = OnBrandGradient.copy(alpha = Alpha.LogoDisc),
                            disabledContentColor = OnBrandGradient.copy(alpha = Alpha.Disabled)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = header.backLabel
                        )
                    }
                }
                Text(
                    text = header.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnBrandGradient,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                )
            }
            if (header.step != null && header.totalSteps != null) {
                StepBars(step = header.step, totalSteps = header.totalSteps)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .overlapAbove(SheetOverlap)
                .clip(sheetShape())
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
        ) {
            Box(Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ScreenHPadding)
                        .padding(top = ScreenHPadding, bottom = ScreenVPadding),
                    verticalArrangement = Arrangement.spacedBy(ListGap),
                    content = content
                )
                snackbarHostState?.let {
                    SnackbarHost(it, Modifier.align(Alignment.BottomCenter))
                }
            }
            bottomBar?.let {
                Box(Modifier.padding(ScreenPadding)) { it() }
            }
        }
    }
}

/** One bar per onboarding step; bars up to [step] are filled. Silent to accessibility. */
@Composable
private fun StepBars(step: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(IconGap)
    ) {
        repeat(totalSteps) { index ->
            val fraction by animateFloatAsState(
                targetValue = if (index < step) 1f else 0f,
                animationSpec = tween(STEP_FILL_MS, easing = FastOutSlowInEasing),
                label = "step_bar_$index"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(StepBarHeight)
                    .clip(CircleShape)
                    .background(OnBrandGradient.copy(alpha = Alpha.LogoDisc))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(OnBrandGradient)
                )
            }
        }
    }
}

// ── Hero header ──────────────────────────────────────────────────────────────

@Composable
private fun HeroLayout(
    snackbarHostState: SnackbarHostState?,
    bottomBar: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    var heroHeight by remember { mutableStateOf(Dp.Hairline) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewport = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { heroHeight = with(density) { it.height.toDp() } }
                    .background(BrandGradient)
                    .statusBarsPadding()
                    .padding(top = HeaderPadding, bottom = HeaderPadding + SheetOverlap),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ListGap)
            ) {
                Box(
                    modifier = Modifier
                        .size(LogoDiscHeroSize)
                        .clip(CircleShape)
                        .background(OnBrandGradient.copy(alpha = Alpha.LogoDisc)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        modifier = Modifier.size(LogoMarkHeroSize),
                        tint = OnBrandGradient
                    )
                }
                Text(
                    text = stringResource(R.string.app_name),
                    style = WordmarkHeaderStyle,
                    color = OnBrandGradient
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // At least the space under the hero, so short content never shows gradient below.
                    .heightIn(min = (viewport - heroHeight).coerceAtLeast(Dp.Hairline))
                    .overlapAbove(SheetOverlap)
                    .clip(sheetShape())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = ScreenHPadding)
                    .padding(top = ScreenHPadding, bottom = ScreenVPadding),
                verticalArrangement = Arrangement.spacedBy(ListGap)
            ) {
                content()
                bottomBar?.invoke()
            }
        }
        // Keeps the white status-bar icons readable once the hero has scrolled away.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(BrandTeal)
        )
        snackbarHostState?.let {
            SnackbarHost(
                hostState = it,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
            )
        }
    }
}

// ── Sheet ────────────────────────────────────────────────────────────────────

/** How far the sheet rises over the gradient, so the gradient shows in its rounded corners. */
private val SheetOverlap = ScreenHPadding

/** `shape-extra-large` on the top corners only. */
@Composable
private fun sheetShape() = MaterialTheme.shapes.extraLarge.copy(
    bottomStart = CornerSize(0),
    bottomEnd = CornerSize(0)
)

/** Pulls the element up by [overlap] so it covers the bottom of the one above it. */
private fun Modifier.overlapAbove(overlap: Dp): Modifier = layout { measurable, constraints ->
    val overlapPx = overlap.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minHeight = constraints.minHeight + overlapPx,
            maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight + overlapPx else constraints.maxHeight
        )
    )
    layout(placeable.width, (placeable.height - overlapPx).coerceAtLeast(0)) {
        placeable.place(0, -overlapPx)
    }
}
