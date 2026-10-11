/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.theme.components.Text

internal data class ChatPaneLayout(val listWidth: Dp, val detailStart: Dp)

/** Uses the actual window bounds, including multi-window and both halves of a separating hinge. */
internal fun calculateChatPaneLayout(
    width: Dp,
    height: Dp,
    tabletop: Boolean = false,
    hingeLeft: Dp? = null,
    hingeRight: Dp? = null,
    preferredListFraction: Float? = null,
    hingeOccluding: Boolean = true,
): ChatPaneLayout? {
    if (width < 600.dp || height < 400.dp || tabletop) return null
    if (hingeLeft != null && hingeRight != null && hingeRight >= 0.dp && hingeLeft <= width) {
        if (hingeLeft < 280.dp || width - hingeRight < 320.dp || hingeRight < hingeLeft) return null
        if (hingeOccluding || preferredListFraction == null) {
            return ChatPaneLayout(listWidth = hingeLeft, detailStart = hingeRight)
        }
    }
    val preferred = preferredListFraction?.takeIf { it.isFinite() }
    val listWidth = if (preferred == null) {
        (width * 0.4f).coerceIn(280.dp, 360.dp).coerceAtMost(width - 320.dp)
    } else {
        (width * preferred).coerceIn(280.dp, width - 320.dp)
    }
    return ChatPaneLayout(listWidth, listWidth)
}

internal fun resizedChatListFraction(width: Dp, currentWidth: Dp, delta: Dp): Float {
    val visibleWidth = currentWidth.coerceIn(280.dp, width - 320.dp)
    return (visibleWidth + delta).coerceIn(280.dp, width - 320.dp) / width
}

@Composable
internal fun ChatDetailPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(ElementTheme.colors.bgCanvasDefault).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.screen_home_select_chat),
            color = ElementTheme.colors.textSecondary,
            style = ElementTheme.typography.fontBodyLgRegular,
        )
    }
}
