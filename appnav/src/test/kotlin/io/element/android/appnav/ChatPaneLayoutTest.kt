/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChatPaneLayoutTest {
    @Test
    fun `folded phone and short landscape windows use one pane`() {
        assertThat(calculateChatPaneLayout(599.dp, 800.dp)).isNull()
        assertThat(calculateChatPaneLayout(840.dp, 399.dp)).isNull()
        assertThat(calculateChatPaneLayout(840.dp, 800.dp, tabletop = true)).isNull()
    }

    @Test
    fun `unfolded window splits and gives the conversation enough space`() {
        assertThat(calculateChatPaneLayout(600.dp, 400.dp)).isEqualTo(ChatPaneLayout(280.dp, 280.dp))
        assertThat(calculateChatPaneLayout(673.dp, 841.dp)).isEqualTo(ChatPaneLayout(280.dp, 280.dp))
        assertThat(calculateChatPaneLayout(1200.dp, 800.dp)).isEqualTo(ChatPaneLayout(360.dp, 360.dp))
    }

    @Test
    fun `separating hinge leaves the entire hinge clear`() {
        assertThat(calculateChatPaneLayout(900.dp, 700.dp, hingeLeft = 440.dp, hingeRight = 460.dp))
            .isEqualTo(ChatPaneLayout(440.dp, 460.dp))
        assertThat(calculateChatPaneLayout(840.dp, 700.dp, hingeLeft = 420.dp, hingeRight = 420.dp))
            .isEqualTo(ChatPaneLayout(420.dp, 420.dp))
    }

    @Test
    fun `hinge with insufficient room on either side falls back to one pane`() {
        assertThat(calculateChatPaneLayout(900.dp, 700.dp, hingeLeft = 250.dp, hingeRight = 270.dp)).isNull()
        assertThat(calculateChatPaneLayout(900.dp, 700.dp, hingeLeft = 600.dp, hingeRight = 620.dp)).isNull()
    }

    @Test
    fun `hinge outside a resized window does not create an extra pane`() {
        assertThat(calculateChatPaneLayout(900.dp, 700.dp, hingeLeft = 950.dp, hingeRight = 970.dp))
            .isEqualTo(ChatPaneLayout(360.dp, 360.dp))
    }
    @Test
    fun `dragging can enlarge the list beyond its default maximum`() {
        assertThat(calculateChatPaneLayout(900.dp, 700.dp, preferredListFraction = 0.6f))
            .isEqualTo(ChatPaneLayout(540.dp, 540.dp))
    }

    @Test
    fun `resizing protects both pane minimum widths and clamps after window resizing`() {
        assertThat(calculateChatPaneLayout(900.dp, 700.dp, preferredListFraction = -1f))
            .isEqualTo(ChatPaneLayout(280.dp, 280.dp))
        assertThat(calculateChatPaneLayout(900.dp, 700.dp, preferredListFraction = 2f))
            .isEqualTo(ChatPaneLayout(580.dp, 580.dp))
        assertThat(calculateChatPaneLayout(673.dp, 841.dp, preferredListFraction = 0.6f))
            .isEqualTo(ChatPaneLayout(353.dp, 353.dp))
        assertThat(calculateChatPaneLayout(360.dp, 841.dp, preferredListFraction = 0.6f)).isNull()
    }

    @Test
    fun `physical hinge stays clear even with a saved custom split`() {
        assertThat(calculateChatPaneLayout(900.dp, 700.dp, hingeLeft = 440.dp, hingeRight = 460.dp, preferredListFraction = 0.6f))
            .isEqualTo(ChatPaneLayout(440.dp, 460.dp))
        assertThat(
            calculateChatPaneLayout(
                width = 900.dp,
                height = 700.dp,
                hingeLeft = 450.dp,
                hingeRight = 450.dp,
                preferredListFraction = 0.6f,
                hingeOccluding = false,
            )
        ).isEqualTo(ChatPaneLayout(540.dp, 540.dp))
    }

    @Test
    fun `drag starts at the displayed divider after shrinking the window`() {
        val fraction = resizedChatListFraction(673.dp, 538.dp, (-2).dp)
        assertThat((673.dp * fraction).value).isWithin(0.001f).of(351f)
        assertThat(resizedChatListFraction(900.dp, 360.dp, 1000.dp)).isEqualTo(580f / 900f)
        assertThat(resizedChatListFraction(900.dp, 360.dp, (-1000).dp)).isEqualTo(280f / 900f)
    }
}
