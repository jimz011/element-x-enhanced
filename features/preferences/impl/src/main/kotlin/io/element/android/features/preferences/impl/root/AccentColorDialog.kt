/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.root

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.components.dialogs.ListDialog
import io.element.android.libraries.designsystem.components.list.TextFieldListItem
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.ui.strings.CommonStrings

/** The colors offered as one-tap choices, as ARGB ints. */
internal val accentColorPresets: List<Pair<Int, Int>> = listOf(
    CommonStrings.common_accent_color_green to 0xFF0DBD8B.toInt(),
    CommonStrings.common_accent_color_blue to 0xFF3B82F6.toInt(),
    CommonStrings.common_accent_color_indigo to 0xFF6366F1.toInt(),
    CommonStrings.common_accent_color_purple to 0xFF9333EA.toInt(),
    CommonStrings.common_accent_color_pink to 0xFFEC4899.toInt(),
    CommonStrings.common_accent_color_red to 0xFFEF4444.toInt(),
    CommonStrings.common_accent_color_orange to 0xFFF97316.toInt(),
    CommonStrings.common_accent_color_yellow to 0xFFEAB308.toInt(),
    CommonStrings.common_accent_color_teal to 0xFF14B8A6.toInt(),
    CommonStrings.common_accent_color_gray to 0xFF6B7280.toInt(),
)

/** Parses `#RRGGBB` or `RRGGBB` into an opaque ARGB int, or `null` if [input] is not a valid color. */
internal fun parseHexColor(input: String): Int? {
    val digits = input.trim().removePrefix("#")
    if (digits.length != HEX_DIGITS || !digits.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
    return (OPAQUE_ALPHA or digits.toLong(radix = 16)).toInt()
}

/** Formats an ARGB int as `#RRGGBB`, dropping the alpha channel. */
internal fun Int.toHexColorString(): String = "#%06X".format(this and RGB_MASK)

private const val HEX_DIGITS = 6
private const val OPAQUE_ALPHA = 0xFF000000L
private const val RGB_MASK = 0xFFFFFF

/**
 * Lets the user pick the accent color of the app from a few presets, or type any hex color.
 * Nothing is saved until the user confirms; an empty field means the default accent.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccentColorDialog(
    currentColor: Int?,
    onSubmit: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(currentColor) }
    var hex by remember { mutableStateOf(currentColor?.toHexColorString().orEmpty()) }
    val isHexValid = hex.isBlank() || parseHexColor(hex) != null
    ListDialog(
        title = stringResource(CommonStrings.common_accent_color),
        enabled = isHexValid,
        onSubmit = {
            onSubmit(selected)
            onDismiss()
        },
        onDismissRequest = onDismiss,
    ) {
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ColorSwatch(
                    color = null,
                    label = stringResource(CommonStrings.common_accent_color_default),
                    isSelected = selected == null,
                    onClick = {
                        selected = null
                        hex = ""
                    },
                )
                accentColorPresets.forEach { (nameRes, argb) ->
                    ColorSwatch(
                        color = argb,
                        label = stringResource(nameRes),
                        isSelected = selected == argb,
                        onClick = {
                            selected = argb
                            hex = argb.toHexColorString()
                        },
                    )
                }
            }
        }
        item {
            TextFieldListItem(
                placeholder = stringResource(CommonStrings.common_accent_color_custom),
                text = hex,
                onTextChange = { value ->
                    hex = value.take(MAX_HEX_INPUT_LENGTH)
                    selected = if (hex.isBlank()) null else parseHexColor(hex) ?: selected
                },
                label = stringResource(CommonStrings.common_accent_color_custom),
                error = if (isHexValid) null else stringResource(CommonStrings.common_accent_color_invalid),
            )
        }
    }
}

private const val MAX_HEX_INPUT_LENGTH = 7

/** A round color chip. A `null` [color] stands for "use the default accent". */
@Composable
private fun ColorSwatch(
    color: Int?,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val fill = color?.let { Color(it) } ?: ElementTheme.colors.bgSubtleSecondary
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(fill)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) ElementTheme.colors.textPrimary else ElementTheme.colors.borderDisabled,
                shape = CircleShape,
            )
            .clickable(onClick = onClick, onClickLabel = label)
            .semantics {
                contentDescription = label
                selected = isSelected
                role = Role.RadioButton
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            color == null -> Icon(
                imageVector = CompoundIcons.Restart(),
                contentDescription = null,
                tint = ElementTheme.colors.iconSecondary,
            )
            isSelected -> Icon(
                imageVector = CompoundIcons.Check(),
                contentDescription = null,
                tint = if (Color(color).luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }
}
