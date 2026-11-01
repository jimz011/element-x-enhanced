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

/**
 * Returns a copy of these colors where the accent and primary action tokens are derived from [accent].
 *
 * Solid fills (primary buttons, the floating action button, selected states) use [accent] as is, while the
 * tokens used for text, icons and borders are nudged towards black or white until they have enough contrast
 * with the canvas, so a very dark or very light accent does not make them disappear.
 */
fun SemanticColors.withAccent(accent: Color): SemanticColors {
    val canvas = bgCanvasDefault
    val towards = if (isLight) Color.Black else Color.White
    val solid = accent.copy(alpha = 1f)
    val solidHovered = lerp(solid, towards, 0.1f)
    val solidPressed = lerp(solid, towards, 0.2f)
    val onSolid = if (solid.luminance() > 0.4f) Color.Black else Color.White
    val subtle = solid.copy(alpha = if (isLight) 0.12f else 0.2f).compositeOver(canvas)
    val subtleBorder = solid.copy(alpha = 0.4f).compositeOver(canvas)
    val forIcons = solid.withContrastOn(canvas, MIN_ICON_CONTRAST)
    val forText = solid.withContrastOn(canvas, MIN_TEXT_CONTRAST)
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
    )
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
