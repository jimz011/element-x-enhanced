/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.search

import androidx.compose.foundation.text.input.TextFieldState
import io.element.android.libraries.matrix.api.core.EventId
import kotlinx.collections.immutable.ImmutableList

data class RoomSearchState(
    val query: TextFieldState,
    val results: ImmutableList<RoomSearchResult>,
    val isAvailable: Boolean,
    val isLoading: Boolean,
    val hasError: Boolean,
    val canLoadMore: Boolean,
    val eventSink: (RoomSearchEvent) -> Unit,
)

data class RoomSearchResult(val eventId: EventId, val sender: String, val body: String, val timestamp: String)
