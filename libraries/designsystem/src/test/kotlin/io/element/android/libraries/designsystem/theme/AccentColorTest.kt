/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.tokens.generated.compoundColorsDark
import io.element.android.compound.tokens.generated.compoundColorsLight
import org.junit.Test

class AccentColorTest {
    private val blue = Color(0xFF3B82F6)

    @Test
    fun `solid fills use the chosen accent as is`() {
        val colors = compoundColorsLight.withAccent(blue)
        assertThat(colors.bgAccentRest).isEqualTo(blue)
        assertThat(colors.bgActionPrimaryRest).isEqualTo(blue)
    }

    @Test
    fun `the header and send button gradients follow the accent and keep their opacity`() {
        val colors = compoundColorsLight.withAccent(blue)
        val original = compoundColorsLight
        assertThat(colors.gradientSubtleStop1.copy(alpha = 1f)).isEqualTo(blue)
        assertThat(colors.gradientSubtleStop1.alpha).isEqualTo(original.gradientSubtleStop1.alpha)
        assertThat(colors.gradientSubtleStop6.alpha).isEqualTo(0f)
        assertThat(colors.gradientActionStop2).isEqualTo(blue)
    }

    @Test
    fun `a lower intensity fades the tinted gradients and washes the color out`() {
        val full = compoundColorsLight.withAccent(blue, intensity = 1f)
        val soft = compoundColorsLight.withAccent(blue, intensity = 0.4f)
        assertThat(soft.gradientSubtleStop1.alpha).isLessThan(full.gradientSubtleStop1.alpha)
        // Less blue-ish: the gap between the blue and red channels shrinks
        val fullGap = full.bgActionPrimaryRest.blue - full.bgActionPrimaryRest.red
        val softGap = soft.bgActionPrimaryRest.blue - soft.bgActionPrimaryRest.red
        assertThat(softGap).isLessThan(fullGap)
    }

    @Test
    fun `intensity is clamped so the accent never turns fully grey`() {
        val zero = compoundColorsLight.withAccent(blue, intensity = 0f)
        val min = compoundColorsLight.withAccent(blue, intensity = MIN_ACCENT_INTENSITY)
        assertThat(zero.bgActionPrimaryRest).isEqualTo(min.bgActionPrimaryRest)
    }

    @Test
    fun `only the accent tokens change`() {
        val colors = compoundColorsDark.withAccent(blue)
        assertThat(colors.bgCanvasDefault).isEqualTo(compoundColorsDark.bgCanvasDefault)
        assertThat(colors.textPrimary).isEqualTo(compoundColorsDark.textPrimary)
        assertThat(colors.bgActionPrimaryDisabled).isEqualTo(compoundColorsDark.bgActionPrimaryDisabled)
        assertThat(colors.isLight).isFalse()
    }

    @Test
    fun `text on a solid accent is dark for light accents and light for dark accents`() {
        assertThat(compoundColorsLight.withAccent(Color(0xFFEAB308)).textOnSolidPrimary).isEqualTo(Color.Black)
        assertThat(compoundColorsLight.withAccent(Color(0xFF1E3A8A)).textOnSolidPrimary).isEqualTo(Color.White)
    }

    @Test
    fun `accent text stays readable on the canvas whatever the accent`() {
        val extremes = listOf(Color.Black, Color.White, Color(0xFF111111), Color(0xFFEEEEEE), Color(0xFFEAB308))
        for (accent in extremes) {
            for (base in listOf(compoundColorsLight, compoundColorsDark)) {
                val colors = base.withAccent(accent)
                assertThat(contrastRatio(colors.textActionAccent, colors.bgCanvasDefault)).isAtLeast(4.5f)
                assertThat(contrastRatio(colors.iconAccentPrimary, colors.bgCanvasDefault)).isAtLeast(3f)
            }
        }
    }
}
