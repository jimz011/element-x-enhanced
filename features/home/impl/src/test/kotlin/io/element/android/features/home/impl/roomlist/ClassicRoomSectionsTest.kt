/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.roomlist

import com.google.common.truth.Truth.assertThat
import io.element.android.features.home.impl.model.RoomSummaryDisplayType
import io.element.android.features.home.impl.model.aRoomListRoomSummary
import org.junit.Test

class ClassicRoomSectionsTest {
    @Test
    fun `sections are ordered as in classic and empty ones are dropped`() {
        val invite = aRoomListRoomSummary(id = "!invite:example.org", displayType = RoomSummaryDisplayType.INVITE)
        val dm = aRoomListRoomSummary(id = "!dm:example.org", isDm = true)
        val room = aRoomListRoomSummary(id = "!room:example.org")

        // Deliberately out of order, to check the grouping and not the input order.
        val result = buildClassicRoomGroups(listOf(room, dm, invite))

        // DMs and ordinary rooms are not split apart -- that mirrors classic's own "All chats" list,
        // where People/Rooms is a filter chip choice, not automatic grouping.
        assertThat(result.map { it.section }).containsExactly(
            ClassicRoomSection.Invites,
            ClassicRoomSection.Rooms,
        ).inOrder()
        assertThat(result.last().rooms.map { it.roomId }).containsExactly(dm.roomId, room.roomId)
    }

    @Test
    fun `low priority sinks below everything else even when favourited`() {
        val room = aRoomListRoomSummary(id = "!room:example.org")
        val lowPriorityFavourite = aRoomListRoomSummary(
            id = "!low:example.org",
            isFavorite = true,
            isLowPriority = true,
        )

        val result = buildClassicRoomGroups(listOf(lowPriorityFavourite, room))

        assertThat(result.map { it.section }).containsExactly(
            ClassicRoomSection.Rooms,
            ClassicRoomSection.LowPriority,
        ).inOrder()
        assertThat(result.last().rooms.map { it.roomId }).containsExactly(lowPriorityFavourite.roomId)
    }

    @Test
    fun `only Invites and Low priority carry a header -- the default Rooms bucket never does`() {
        assertThat(ClassicRoomSection.Invites.titleRes).isNotNull()
        assertThat(ClassicRoomSection.LowPriority.titleRes).isNotNull()
        assertThat(ClassicRoomSection.Rooms.titleRes).isNull()
    }

    @Test
    fun `order within a section follows the room list order`() {
        val first = aRoomListRoomSummary(id = "!first:example.org")
        val second = aRoomListRoomSummary(id = "!second:example.org")

        val result = buildClassicRoomGroups(listOf(first, second))

        assertThat(result.single().rooms.map { it.roomId })
            .containsExactly(first.roomId, second.roomId)
            .inOrder()
    }

    @Test
    fun `placeholders are kept so the loading skeletons still render`() {
        val placeholder = aRoomListRoomSummary(id = "!placeholder:example.org", displayType = RoomSummaryDisplayType.PLACEHOLDER)

        val result = buildClassicRoomGroups(listOf(placeholder))

        assertThat(result.single().section).isEqualTo(ClassicRoomSection.Rooms)
        assertThat(result.single().rooms).hasSize(1)
    }

    @Test
    fun `an empty room list produces no groups`() {
        assertThat(buildClassicRoomGroups(emptyList())).isEmpty()
    }
}
