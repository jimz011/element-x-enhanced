/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.home.impl.spacefilters

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import io.element.android.features.home.impl.R
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.test.A_ROOM_ALIAS
import io.element.android.tests.testutils.EventsRecorder
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Test

class SpaceFiltersViewTest : RobolectricTest() {
    @Test
    fun `clicking on a filter with alias shows display name and alias`() = runAndroidComposeUiTest {
        val filter = aSpaceServiceFilter(
            displayName = "Test Space",
            canonicalAlias = A_ROOM_ALIAS,
        )
        val eventsRecorder = EventsRecorder<SpaceFiltersEvent.Selecting>()
        setSpaceFiltersView(
            state = aSelectingSpaceFiltersState(
                availableFilters = listOf(filter),
                eventSink = eventsRecorder,
            )
        )

        // Both display name and alias should be visible
        onNodeWithText(filter.spaceRoom.displayName).assertExists()
        onNodeWithText(A_ROOM_ALIAS.value).assertExists()

        onNodeWithText(filter.spaceRoom.displayName).performClick()

        eventsRecorder.assertSingle(SpaceFiltersEvent.Selecting.SelectFilter(filter))
    }

    @Test
    fun `multiple filters are displayed and clickable`() = runAndroidComposeUiTest {
        val filter1 = aSpaceServiceFilter(displayName = "Space One")
        val filter2 = aSpaceServiceFilter(displayName = "Space Two")
        val eventsRecorder = EventsRecorder<SpaceFiltersEvent.Selecting>()
        setSpaceFiltersView(
            state = aSelectingSpaceFiltersState(
                availableFilters = listOf(filter1, filter2),
                eventSink = eventsRecorder,
            )
        )

        // Both filters should be visible
        onNodeWithText(filter1.spaceRoom.displayName).assertExists()
        onNodeWithText(filter2.spaceRoom.displayName).assertExists()

        // Click on second filter
        onNodeWithText(filter2.spaceRoom.displayName).performClick()

        eventsRecorder.assertSingle(SpaceFiltersEvent.Selecting.SelectFilter(filter2))
    }

    @Test
    fun `a top level space's children are collapsed by default`() = runAndroidComposeUiTest {
        val parent = aSpaceServiceFilter(displayName = "Work", roomId = RoomId("!work:example.com"))
        val child = aSpaceServiceFilter(displayName = "Projects", roomId = RoomId("!projects:example.com"), level = 1)
        setSpaceFiltersView(
            state = aSelectingSpaceFiltersState(availableFilters = listOf(parent, child)),
        )

        onNodeWithText(parent.spaceRoom.displayName).assertExists()
        onNodeWithText(child.spaceRoom.displayName).assertDoesNotExist()
    }

    @Test
    fun `expanding a top level space reveals its children and selecting one selects the child`() = runAndroidComposeUiTest {
        val parent = aSpaceServiceFilter(displayName = "Work", roomId = RoomId("!work:example.com"))
        val child = aSpaceServiceFilter(displayName = "Projects", roomId = RoomId("!projects:example.com"), level = 1)
        val eventsRecorder = EventsRecorder<SpaceFiltersEvent.Selecting>()
        setSpaceFiltersView(
            state = aSelectingSpaceFiltersState(
                availableFilters = listOf(parent, child),
                eventSink = eventsRecorder,
            ),
        )

        onNodeWithContentDescription(activity!!.getString(R.string.a11y_expand_space)).performClick()
        onNodeWithText(child.spaceRoom.displayName).assertExists()

        onNodeWithText(child.spaceRoom.displayName).performClick()

        eventsRecorder.assertSingle(SpaceFiltersEvent.Selecting.SelectFilter(child))
    }

    @Test
    fun `reopening with a child selected auto-expands its parent`() = runAndroidComposeUiTest {
        val parent = aSpaceServiceFilter(displayName = "Work", roomId = RoomId("!work:example.com"))
        val child = aSpaceServiceFilter(displayName = "Projects", roomId = RoomId("!projects:example.com"), level = 1)
        setSpaceFiltersView(
            state = aSelectingSpaceFiltersState(
                availableFilters = listOf(parent, child),
                selectedFilter = child,
            ),
        )

        onNodeWithText(child.spaceRoom.displayName).assertExists()
    }

    @Test
    fun `a space with unread rooms shows its count as a badge`() = runAndroidComposeUiTest {
        val space = aSpaceServiceFilter(displayName = "Work", roomId = RoomId("!work:example.com"))
        setSpaceFiltersView(
            state = aSelectingSpaceFiltersState(availableFilters = listOf(space)),
            spaceUnreadCounts = SpaceUnreadCounts(
                totalUnreadRoomCount = 3,
                countsBySpaceRoomId = persistentMapOf(space.spaceRoom.roomId to 3),
            ),
        )

        onNodeWithText("3").assertExists()
    }

    @Test
    fun `a space with nothing unread shows no badge`() = runAndroidComposeUiTest {
        val space = aSpaceServiceFilter(displayName = "Work", roomId = RoomId("!work:example.com"))
        setSpaceFiltersView(
            state = aSelectingSpaceFiltersState(availableFilters = listOf(space)),
            spaceUnreadCounts = emptySpaceUnreadCounts(),
        )

        onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun `all chats shows the total unread room count`() = runAndroidComposeUiTest {
        setSpaceFiltersView(
            state = aSelectingSpaceFiltersState(availableFilters = listOf(aSpaceServiceFilter())),
            spaceUnreadCounts = SpaceUnreadCounts(
                totalUnreadRoomCount = 7,
                countsBySpaceRoomId = persistentMapOf(),
            ),
        )

        onNodeWithText("7").assertExists()
    }
}

private fun AndroidComposeUiTest<ComponentActivity>.setSpaceFiltersView(
    state: SpaceFiltersState,
    spaceUnreadCounts: SpaceUnreadCounts = emptySpaceUnreadCounts(),
    onDirectMessagesClick: () -> Unit = {},
) {
    setContent {
        SpaceFiltersView(
            state = state,
            spaceUnreadCounts = spaceUnreadCounts,
            onDirectMessagesClick = onDirectMessagesClick,
        )
    }
}
