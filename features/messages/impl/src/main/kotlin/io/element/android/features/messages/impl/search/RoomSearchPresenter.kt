/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.search

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.dateformatter.api.DateFormatter
import io.element.android.libraries.dateformatter.api.DateFormatterMode
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.matrix.api.search.MessageSearchPaginationState
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.getDisambiguatedDisplayName
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Inject
class RoomSearchPresenter(
    private val room: JoinedRoom,
    private val client: MatrixClient,
    private val dateFormatter: DateFormatter,
) : Presenter<RoomSearchState> {
    @Composable
    override fun present(): RoomSearchState {
        val query = rememberTextFieldState()
        val scope = rememberCoroutineScope()
        // This composable only lives while the chat search is open. Its cursor is independent of global search.
        val search = remember {
            if (client.isMessageSearchAvailable) client.messageSearchService.createMessageSearch(scope, room.roomId) else null
        }
        var results by remember { mutableStateOf<ImmutableList<RoomSearchResult>>(persistentListOf()) }
        var isLoading by remember { mutableStateOf(false) }
        var hasError by remember { mutableStateOf(false) }
        var canLoadMore by remember { mutableStateOf(false) }
        var retry by remember { mutableIntStateOf(0) }
        val term = query.text.toString().trim()
        val paginationScope = key(term) { rememberCoroutineScope() }

        LaunchedEffect(term, retry) {
            results = persistentListOf()
            hasError = false
            canLoadMore = false
            isLoading = term.isNotEmpty() && search != null
            if (term.isEmpty() || search == null) return@LaunchedEffect
            delay(300)
            if (search.setQuery(term).isFailure) {
                hasError = true
                isLoading = false
                return@LaunchedEffect
            }
            combine(search.results, search.paginationState) { matches, pagination -> matches to pagination }
                .collect { (matches, pagination) ->
                    results = matches.filter { it.roomId == room.roomId }.mapNotNull { match ->
                        val content = match.content as? MessageContent ?: return@mapNotNull null
                        RoomSearchResult(
                            eventId = match.eventId,
                            sender = match.senderProfile.getDisambiguatedDisplayName(match.senderId),
                            body = content.body,
                            timestamp = dateFormatter.format(match.timestamp, DateFormatterMode.TimeOrDate, useRelative = true),
                        )
                    }.toImmutableList()
                    canLoadMore = pagination is MessageSearchPaginationState.Idle && !pagination.endReached
                    isLoading = pagination is MessageSearchPaginationState.Loading
                    // SDK ranking spans every room. Keep paging if this room has too few matches to scroll.
                    if (results.size < 20 && canLoadMore && !hasError) {
                        isLoading = true
                        canLoadMore = false
                        if (search.paginate().isFailure) {
                            hasError = true
                            isLoading = false
                        }
                    }
                }
        }

        return RoomSearchState(query, results, search != null, isLoading, hasError, canLoadMore) { event ->
            when (event) {
                RoomSearchEvent.Retry -> retry++
                RoomSearchEvent.LoadMore -> if (canLoadMore && !hasError) {
                    canLoadMore = false
                    isLoading = true
                    paginationScope.launch {
                        if (search?.paginate()?.isFailure == true) {
                            hasError = true
                            isLoading = false
                        }
                    }
                }
            }
        }
    }
}
