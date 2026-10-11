/*
 * Copyright (c) 2026
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.roomlist

import com.google.common.truth.Truth.assertThat
import io.element.android.features.home.impl.model.RoomSummaryDisplayType
import io.element.android.features.home.impl.model.aRoomListRoomSummary
import io.element.android.libraries.matrix.api.core.RoomId
import org.junit.Test

class ClassicQuickAccessTest {
    @Test
    fun `rooms are kept in the order the room list gives them, i e newest activity first`() {
        // allSummaries is already recency-ordered by the SDK; the quick access row must not reorder it,
        // so a message from someone else bumping a room to the top of the room list also bumps it here.
        val newest = aRoomListRoomSummary(id = "!newest:example.org", latestEventTimestampMillis = 2_000)
        val older = aRoomListRoomSummary(id = "!older:example.org", latestEventTimestampMillis = 1_000)

        val result = buildClassicQuickAccessSummaries(listOf(newest, older), recentlyOpenedTimestamps = emptyMap())

        assertThat(result.map { it.roomId }).containsExactly(newest.roomId, older.roomId).inOrder()
    }

    @Test
    fun `opening a room bumps it to the front, ahead of chats with an older message`() {
        val staleButOpened = aRoomListRoomSummary(id = "!opened:example.org", latestEventTimestampMillis = 1_000)
        val untouched = aRoomListRoomSummary(id = "!untouched:example.org", latestEventTimestampMillis = 2_000)

        val result = buildClassicQuickAccessSummaries(
            summaries = listOf(untouched, staleButOpened),
            recentlyOpenedTimestamps = mapOf(staleButOpened.roomId to 5_000L),
        )

        assertThat(result.map { it.roomId }).containsExactly(staleButOpened.roomId, untouched.roomId).inOrder()
    }

    @Test
    fun `a genuinely newer message elsewhere still outranks a room opened earlier`() {
        val openedEarlier = aRoomListRoomSummary(id = "!opened:example.org", latestEventTimestampMillis = 1_000)
        val freshMessage = aRoomListRoomSummary(id = "!fresh:example.org", latestEventTimestampMillis = 9_000)

        val result = buildClassicQuickAccessSummaries(
            summaries = listOf(openedEarlier, freshMessage),
            recentlyOpenedTimestamps = mapOf(openedEarlier.roomId to 2_000L),
        )

        assertThat(result.map { it.roomId }).containsExactly(freshMessage.roomId, openedEarlier.roomId).inOrder()
    }

    @Test
    fun `spaces are excluded`() {
        val space = aRoomListRoomSummary(id = "!space:example.org", isSpace = true)
        val room = aRoomListRoomSummary(id = "!room:example.org")

        val result = buildClassicQuickAccessSummaries(listOf(space, room), recentlyOpenedTimestamps = emptyMap())

        assertThat(result.map { it.roomId }).containsExactly(room.roomId)
    }

    @Test
    fun `invites and knocked rooms are excluded`() {
        val invite = aRoomListRoomSummary(id = "!invite:example.org", displayType = RoomSummaryDisplayType.INVITE)
        val knocked = aRoomListRoomSummary(id = "!knocked:example.org", displayType = RoomSummaryDisplayType.KNOCKED)
        val room = aRoomListRoomSummary(id = "!room:example.org")

        val result = buildClassicQuickAccessSummaries(listOf(invite, knocked, room), recentlyOpenedTimestamps = emptyMap())

        assertThat(result.map { it.roomId }).containsExactly(room.roomId)
    }

    @Test
    fun `loading placeholders are excluded`() {
        val placeholder = aRoomListRoomSummary(id = "!placeholder:example.org", displayType = RoomSummaryDisplayType.PLACEHOLDER)
        val room = aRoomListRoomSummary(id = "!room:example.org")

        val result = buildClassicQuickAccessSummaries(listOf(placeholder, room), recentlyOpenedTimestamps = emptyMap())

        assertThat(result.map { it.roomId }).containsExactly(room.roomId)
    }

    @Test
    fun `a room opened but no longer present in the room list is simply not there to bump`() {
        val room = aRoomListRoomSummary(id = "!room:example.org")

        val result = buildClassicQuickAccessSummaries(
            summaries = listOf(room),
            recentlyOpenedTimestamps = mapOf(RoomId("!gone:example.org") to 5_000L),
        )

        assertThat(result.map { it.roomId }).containsExactly(room.roomId)
    }

    @Test
    fun `the row is capped at 12 rooms`() {
        val summaries = (0 until 20).map { index -> aRoomListRoomSummary(id = "!room$index:example.org") }

        val result = buildClassicQuickAccessSummaries(summaries, recentlyOpenedTimestamps = emptyMap())

        assertThat(result).hasSize(12)
        assertThat(result.map { it.roomId }).isEqualTo(summaries.take(12).map { it.roomId })
    }

    @Test
    fun `the row is empty when there are no rooms`() {
        assertThat(buildClassicQuickAccessSummaries(emptyList(), recentlyOpenedTimestamps = emptyMap())).isEmpty()
    }
}
