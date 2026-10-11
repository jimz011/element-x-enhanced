/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text

internal data class ChatPanePreviewState(
    val width: Dp,
    val roomSelected: Boolean,
    val hingeLeft: Dp? = null,
    val hingeRight: Dp? = null,
    val height: Dp = 600.dp,
    val tabletop: Boolean = false,
    val listFraction: Float? = null,
)

internal class ChatPaneStatePreviewParam : PreviewParameterProvider<ChatPanePreviewState> {
    override val values = sequenceOf(
        ChatPanePreviewState(360.dp, false),
        ChatPanePreviewState(360.dp, true),
        ChatPanePreviewState(673.dp, false),
        ChatPanePreviewState(673.dp, true),
        ChatPanePreviewState(900.dp, true, 440.dp, 460.dp),
        ChatPanePreviewState(840.dp, true, height = 360.dp),
        ChatPanePreviewState(840.dp, true, tabletop = true),
        ChatPanePreviewState(900.dp, true, listFraction = 0.6f),
        ChatPanePreviewState(900.dp, false, listFraction = 0.32f),
    )
}

@PreviewsDayNight
@Composable
internal fun ChatPaneLayoutPreview(@PreviewParameter(ChatPaneStatePreviewParam::class) state: ChatPanePreviewState) = ElementPreview {
    val panes = calculateChatPaneLayout(state.width, state.height, state.tabletop, state.hingeLeft, state.hingeRight, state.listFraction)
    Box(Modifier.requiredSize(state.width, state.height).background(ElementTheme.colors.borderDisabled)) {
        if (panes != null || !state.roomSelected) {
            Box(
                modifier = Modifier.align(AbsoluteAlignment.TopLeft).width(panes?.listWidth ?: state.width).fillMaxHeight()
                    .background(ElementTheme.colors.bgCanvasDefault).padding(24.dp),
            ) {
                Text(text = "Chats", style = ElementTheme.typography.fontHeadingLgBold)
            }
        }
        if (panes != null || state.roomSelected) {
            val detailStart = panes?.detailStart ?: 0.dp
            val detailModifier = Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset(x = detailStart).width(state.width - detailStart).fillMaxHeight()
            if (state.roomSelected) {
                Box(detailModifier.background(ElementTheme.colors.bgCanvasDefault).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = "Conversation", style = ElementTheme.typography.fontBodyLgRegular)
                }
            } else {
                ChatDetailPlaceholder(detailModifier)
            }
        }
        if (panes != null && panes.listWidth == panes.detailStart) {
            ChatPaneDivider(
                fraction = panes.listWidth / state.width,
                fractionRange = (280.dp / state.width)..((state.width - 320.dp) / state.width),
                onFractionChange = {},
                onDrag = {},
                modifier = Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset(x = panes.listWidth - 24.dp),
            )
        }
    }
}
