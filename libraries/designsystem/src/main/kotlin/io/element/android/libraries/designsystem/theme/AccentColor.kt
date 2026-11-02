/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import io.element.android.compound.tokens.generated.SemanticColors

/** Contrast needed for an icon or border to stay visible against its background (WCAG non-text contrast). */
private const val MIN_ICON_CONTRAST = 3f

/** Contrast needed for accent coloured text to stay readable against its background (WCAG AA). */
private const val MIN_TEXT_CONTRAST = 4.5f

/** The lowest intensity the user can pick; below this the accent is barely distinguishable from grey. */
const val MIN_ACCENT_INTENSITY = 0.2f

/**
 * Returns a copy of these colors where the accent and primary action tokens are derived from [accent].
 *
 * Solid fills (primary buttons, the floating action button, selected states) use [accent] as is, while the
 * tokens used for text, icons and borders are nudged towards black or white until they have enough contrast
 * with the canvas, so a very dark or very light accent does not make them disappear.
 *
 * The tinted gradients (the home header, the send button) are rebuilt from [accent] too, keeping the opacity
 * each stop already had.
 *
 * @param intensity how strongly the accent is applied, from [MIN_ACCENT_INTENSITY] to 1. Lower values wash the
 * color out towards grey and make the tinted backgrounds fainter. 1 keeps [accent] exactly as picked.
 */
fun SemanticColors.withAccent(accent: Color, intensity: Float = 1f): SemanticColors {
    val strength = intensity.coerceIn(MIN_ACCENT_INTENSITY, 1f)
    val canvas = bgCanvasDefault
    val towards = if (isLight) Color.Black else Color.White
    val solid = accent.copy(alpha = 1f).desaturated(strength)
    val solidHovered = lerp(solid, towards, 0.1f)
    val solidPressed = lerp(solid, towards, 0.2f)
    val onSolid = if (solid.luminance() > 0.4f) Color.Black else Color.White
    val subtle = solid.copy(alpha = (if (isLight) 0.12f else 0.2f) * strength).compositeOver(canvas)
    val subtleBorder = solid.copy(alpha = 0.4f * strength).compositeOver(canvas)
    val forIcons = solid.withContrastOn(canvas, MIN_ICON_CONTRAST)
    val forText = solid.withContrastOn(canvas, MIN_TEXT_CONTRAST)
    fun tint(original: Color) = solid.copy(alpha = original.alpha * strength)
    return copy(
        bgAccentRest = solid,
        bgAccentHovered = solidHovered,
        bgAccentPressed = solidPressed,
        bgAccentSelected = subtle,
        bgAccentSubtle = subtle,
        bgActionPrimaryRest = solid,
        bgActionPrimaryHovered = solidHovered,
        bgActionPrimaryPressed = solidPressed,
        bgBadgeAccent = subtle,
        textBadgeAccent = forText,
        borderAccentPrimary = forIcons,
        borderAccentSubtle = subtleBorder,
        borderFocused = forIcons,
        iconAccentPrimary = forIcons,
        iconAccentTertiary = forIcons,
        iconOnSolidPrimary = onSolid,
        textOnSolidPrimary = onSolid,
        textActionAccent = forText,
        textLinkExternal = forText,
        gradientSubtleStop1 = tint(gradientSubtleStop1),
        gradientSubtleStop2 = tint(gradientSubtleStop2),
        gradientSubtleStop3 = tint(gradientSubtleStop3),
        gradientSubtleStop4 = tint(gradientSubtleStop4),
        gradientSubtleStop5 = tint(gradientSubtleStop5),
        gradientSubtleStop6 = tint(gradientSubtleStop6),
        gradientActionStop1 = lerp(solid, Color.White, 0.15f),
        gradientActionStop2 = solid,
        gradientActionStop3 = lerp(solid, Color.Black, 0.15f),
        gradientActionStop4 = lerp(solid, Color.Black, 0.3f),
    )
}

/** Blends this color with its own grey equivalent: [strength] 1 keeps it as is, 0 gives plain grey. */
fun Color.desaturated(strength: Float): Color {
    if (strength >= 1f) return this
    val grey = red * 0.299f + green * 0.587f + blue * 0.114f
    return lerp(Color(grey, grey, grey, alpha), this, strength)
}

/**
 * Moves this color towards black (on a light background) or white (on a dark one) until its contrast ratio
 * with [background] reaches [minRatio], or until it can't get any closer.
 */
internal fun Color.withContrastOn(background: Color, minRatio: Float): Color {
    val towards = if (background.luminance() > 0.5f) Color.Black else Color.White
    var result = this
    for (step in 1..STEPS) {
        if (contrastRatio(result, background) >= minRatio) return result
        result = lerp(this, towards, step.toFloat() / STEPS)
    }
    return result
}

private const val STEPS = 10

internal fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}
