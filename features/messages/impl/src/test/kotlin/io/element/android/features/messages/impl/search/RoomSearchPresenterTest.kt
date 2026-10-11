/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.search

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.dateformatter.test.FakeDateFormatter
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.search.MessageSearchPaginationState
import io.element.android.libraries.matrix.api.search.MessageSearchResult
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.room.FakeJoinedRoom
import io.element.android.libraries.matrix.test.search.FakeMessageSearch
import io.element.android.libraries.matrix.test.search.FakeMessageSearchService
import io.element.android.libraries.matrix.test.timeline.aProfileDetails
import io.element.android.tests.testutils.consumeItemsUntilPredicate
import io.element.android.tests.testutils.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomSearchPresenterTest {
    @Test
    fun `search is scoped to the current chat and does not start with a blank query`() = runTest {
        val search = FakeMessageSearch()
        val service = FakeMessageSearchService(search)
        presenter(service).test {
            val state = awaitItem()
            assertThat(state.isAvailable).isTrue()
            assertThat(service.lastRoomId).isEqualTo(A_ROOM_ID)
            advanceUntilIdle()
            assertThat(search.setQueryCallCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keyword results exclude other rooms and preserve the message ID for jumping`() = runTest {
        val search = FakeMessageSearch()
        presenter(FakeMessageSearchService(search)).test {
            awaitItem().query.edit { append("goed") }
            advanceUntilIdle()
            assertThat(search.lastQuery).isEqualTo("goed")
            search.emitResults(persistentListOf(message(), message(RoomId("!another:example.org"), EventId("\$other"))))
            search.emitPaginationState(MessageSearchPaginationState.Idle(endReached = true))
            advanceUntilIdle()
            val state = consumeItemsUntilPredicate { it.results.isNotEmpty() && !it.isLoading }.last()
            assertThat(state.results).containsExactly(RoomSearchResult(EventId("\$message"), "Alice", "Hallo alles goed", "12:00"))
            assertThat(state.canLoadMore).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `typing a new query cancels the previous debounce and clearing removes results`() = runTest {
        val search = FakeMessageSearch()
        presenter(FakeMessageSearchService(search)).test {
            val initial = awaitItem()
            initial.query.edit { append("go") }
            runCurrent()
            advanceTimeBy(150)
            initial.query.edit { replace(0, length, "goed") }
            advanceUntilIdle()
            assertThat(search.setQueryCallCount).isEqualTo(1)
            assertThat(search.lastQuery).isEqualTo("goed")
            search.emitResults(persistentListOf(message()))
            advanceUntilIdle()
            consumeItemsUntilPredicate { it.results.isNotEmpty() }
            initial.query.edit { replace(0, length, "") }
            advanceUntilIdle()
            val cleared = consumeItemsUntilPredicate { it.query.text.isEmpty() && it.results.isEmpty() }.last()
            assertThat(cleared.isLoading).isFalse()
            assertThat(cleared.canLoadMore).isFalse()
            assertThat(search.setQueryCallCount).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search keeps paging when initial pages contain no matches in this chat`() = runTest {
        val search = FakeMessageSearch(controllablePagination = true)
        presenter(FakeMessageSearchService(search)).test {
            awaitItem().query.edit { append("goed") }
            advanceUntilIdle()
            assertThat(search.paginateCallCount).isEqualTo(1)
            search.completePagination(endReached = false)
            advanceUntilIdle()
            assertThat(search.paginateCallCount).isEqualTo(2)
            search.emitResults(persistentListOf(message()))
            search.completePagination(endReached = true)
            advanceUntilIdle()
            val found = consumeItemsUntilPredicate { it.results.isNotEmpty() && !it.isLoading }.last()
            assertThat(found.results.single().body).isEqualTo("Hallo alles goed")
            assertThat(search.cancelledPaginateCallCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `query failures can be retried`() = runTest {
        var shouldFail = true
        val search = FakeMessageSearch(setQueryResult = { if (shouldFail) Result.failure(IllegalStateException()) else Result.success(Unit) })
        presenter(FakeMessageSearchService(search)).test {
            awaitItem().query.edit { append("goed") }
            advanceUntilIdle()
            val failed = consumeItemsUntilPredicate { it.hasError && !it.isLoading }.last()
            assertThat(failed.isLoading).isFalse()
            shouldFail = false
            failed.eventSink(RoomSearchEvent.Retry)
            advanceUntilIdle()
            search.emitResults(persistentListOf(message()))
            search.emitPaginationState(MessageSearchPaginationState.Idle(endReached = true))
            advanceUntilIdle()
            val recovered = consumeItemsUntilPredicate { it.results.isNotEmpty() && !it.isLoading }.last()
            assertThat(recovered.hasError).isFalse()
            assertThat(search.setQueryCallCount).isEqualTo(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unavailable indexing does not create a cursor or call search`() = runTest {
        val service = FakeMessageSearchService()
        presenter(service, available = false).test {
            val state = awaitItem()
            assertThat(state.isAvailable).isFalse()
            state.query.edit { append("goed") }
            advanceUntilIdle()
            assertThat(service.createMessageSearchCallCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun presenter(service: FakeMessageSearchService, available: Boolean = true) = RoomSearchPresenter(
        room = FakeJoinedRoom(),
        client = FakeMatrixClient(isMessageSearchAvailable = available, messageSearchService = service),
        dateFormatter = FakeDateFormatter { _, _, _ -> "12:00" },
    )

    private fun message(roomId: RoomId = A_ROOM_ID, eventId: EventId = EventId("\$message")) = MessageSearchResult(
        roomId = roomId,
        eventId = eventId,
        senderId = A_USER_ID,
        senderProfile = aProfileDetails(displayName = "Alice"),
        content = MessageContent("Hallo alles goed", null, false, null, TextMessageType("Hallo alles goed", null)),
        timestamp = 0,
    )
}
