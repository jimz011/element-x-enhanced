/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme

/** A visible grip with a generous touch target; deltas always follow physical left/right movement. */
@Composable
internal fun ChatPaneDivider(
    fraction: Float,
    fractionRange: ClosedFloatingPointRange<Float>,
    onFractionChange: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.a11y_resize_chat_panes)
    val canResize = fractionRange.endInclusive > fractionRange.start
    Box(modifier.width(48.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(ElementTheme.colors.borderDisabled))
        if (canResize) {
            Box(
                modifier = Modifier.size(48.dp, 72.dp)
                    .semantics {
                        contentDescription = label
                        progressBarRangeInfo = ProgressBarRangeInfo(fraction, fractionRange)
                        setProgress { value ->
                            if (!value.isFinite()) return@setProgress false
                            onFractionChange(value.coerceIn(fractionRange))
                            true
                        }
                    }
                    .draggable(
                        state = rememberDraggableState(onDrag),
                        orientation = Orientation.Horizontal,
                        reverseDirection = false,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(6.dp, 48.dp).background(ElementTheme.colors.textSecondary, RoundedCornerShape(3.dp)))
            }
        }
    }
}
