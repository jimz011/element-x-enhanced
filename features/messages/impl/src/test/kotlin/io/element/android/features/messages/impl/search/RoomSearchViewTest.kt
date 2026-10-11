/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.messages.impl.search

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.tests.testutils.EventsRecorder
import io.element.android.tests.testutils.pressBack
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class RoomSearchViewTest : RobolectricTest() {
    @Test
    fun `selecting a search result returns the original message ID`() = runAndroidComposeUiTest<ComponentActivity> {
        var selected: EventId? = null
        val state = RoomSearchStatePreviewParam().values.elementAt(5)
        setContent {
            ElementPreview {
                RoomSearchView(state, onBackClick = {}, onSelectResult = { selected = it })
            }
        }
        onNodeWithText("Hallo alles goed").performClick()
        assertThat(selected).isEqualTo(EventId("\$message"))
    }

    @Test
    fun `back closes chat search`() = runAndroidComposeUiTest<ComponentActivity> {
        var closed = false
        val state = RoomSearchStatePreviewParam().values.first()
        setContent {
            ElementPreview {
                RoomSearchView(state, onBackClick = { closed = true }, onSelectResult = {})
            }
        }
        pressBack()
        assertThat(closed).isTrue()
    }

    @Test
    fun `load more requests another page`() = runAndroidComposeUiTest<ComponentActivity> {
        val events = EventsRecorder<RoomSearchEvent>()
        val state = RoomSearchStatePreviewParam().values.elementAt(5).copy(eventSink = events)
        setContent {
            ElementPreview { RoomSearchView(state, onBackClick = {}, onSelectResult = {}) }
        }
        onNodeWithText("Load more results").performClick()
        events.assertSingle(RoomSearchEvent.LoadMore)
    }
}
