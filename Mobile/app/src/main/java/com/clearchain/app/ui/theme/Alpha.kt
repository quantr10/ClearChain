package com.clearchain.app.ui.theme

/** The design system's opacity tokens: alphas the app applies over its colours. */
object Alpha {
    /** `disabled`: disabled controls, including a disabled primary button's fill. */
    val Disabled = 0.38f

    /** `placeholder`: placeholder text, as on-surface-variant at this alpha. */
    val Placeholder = 0.5f

    /** `tile-tint`: the stat-tile fill, and the selected tab's indicator, as primary at this alpha. */
    val TileTint = 0.10f

    /** `category-tint`: a category badge's fill, as the category colour at this alpha. */
    val CategoryTint = 0.12f

    /**
     * `logo-disc`: white at this alpha on the brand gradient: the logo disc, the header back
     * circle and the step-bar track.
     */
    val LogoDisc = 0.2f

    /** `eyebrow-alpha`: the role eyebrow, white at this alpha on the brand gradient. */
    val Eyebrow = 0.75f

    /** `subtitle-alpha`: header subtitles, white at this alpha on the brand gradient. */
    val Subtitle = 0.8f

    /** `ring-outer`: the status picture's outer ring, as primary at this alpha. */
    val RingOuter = 0.08f

    /** `ring-inner`: the status picture's inner ring, as primary at this alpha. */
    val RingInner = 0.12f
}
