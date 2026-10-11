/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spacefilters

import com.google.common.truth.Truth.assertThat
import io.element.android.features.home.impl.model.aRoomListRoomSummary
import io.element.android.libraries.matrix.api.core.RoomId
import org.junit.Test

class SpaceUnreadCountsTest {
    @Test
    fun `totalUnreadRoomCount counts every unread room account-wide`() {
        val unreadRoom = aRoomListRoomSummary(id = "!unread:example.org", numberOfUnreadMessages = 1)
        val readRoom = aRoomListRoomSummary(id = "!read:example.org")

        val result = buildSpaceUnreadCounts(summaries = listOf(unreadRoom, readRoom), availableFilters = emptyList())

        assertThat(result.totalUnreadRoomCount).isEqualTo(1)
    }

    @Test
    fun `a space's count is the number of its unread descendants`() {
        val unreadRoomId = RoomId("!unread:example.org")
        val readRoomId = RoomId("!read:example.org")
        val space = aSpaceServiceFilter(
            roomId = RoomId("!space:example.org"),
            descendants = listOf(unreadRoomId, readRoomId),
        )
        val unreadRoom = aRoomListRoomSummary(id = unreadRoomId.value, numberOfUnreadMessages = 1)
        val readRoom = aRoomListRoomSummary(id = readRoomId.value)

        val result = buildSpaceUnreadCounts(
            summaries = listOf(unreadRoom, readRoom),
            availableFilters = listOf(space),
        )

        assertThat(result.countFor(space)).isEqualTo(1)
    }

    @Test
    fun `a top level space's count includes unread rooms that only live in a child space`() {
        val childSpaceId = RoomId("!childSpace:example.org")
        val unreadNestedRoomId = RoomId("!nested:example.org")
        val topLevel = aSpaceServiceFilter(
            roomId = RoomId("!top:example.org"),
            level = 0,
            descendants = listOf(childSpaceId),
        )
        val childSpace = aSpaceServiceFilter(
            roomId = childSpaceId,
            level = 1,
            descendants = listOf(unreadNestedRoomId),
        )
        val unreadNestedRoom = aRoomListRoomSummary(id = unreadNestedRoomId.value, numberOfUnreadMessages = 1)

        val result = buildSpaceUnreadCounts(
            summaries = listOf(unreadNestedRoom),
            availableFilters = listOf(topLevel, childSpace),
        )

        assertThat(result.countFor(topLevel)).isEqualTo(1)
        assertThat(result.countFor(childSpace)).isEqualTo(1)
    }

    @Test
    fun `a space with nothing unread has a count of zero`() {
        val space = aSpaceServiceFilter(roomId = RoomId("!space:example.org"), descendants = emptyList())

        val result = buildSpaceUnreadCounts(summaries = emptyList(), availableFilters = listOf(space))

        assertThat(result.countFor(space)).isEqualTo(0)
    }

    @Test
    fun `countFor a filter that was never part of the counted set is zero, not a crash`() {
        val uncountedFilter = aSpaceServiceFilter(roomId = RoomId("!unknown:example.org"))

        assertThat(emptySpaceUnreadCounts().countFor(uncountedFilter)).isEqualTo(0)
    }
}
