/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.matrix.api.core.EventId
import kotlinx.collections.immutable.persistentListOf

class RoomSearchStatePreviewParam : PreviewParameterProvider<RoomSearchState> {
    override val values: Sequence<RoomSearchState>
        get() {
            val initial = RoomSearchState(TextFieldState(), persistentListOf(), true, false, false, false, {})
            val searched = initial.copy(query = TextFieldState("goed"))
            val results = searched.copy(results = persistentListOf(RoomSearchResult(EventId("\$message"), "Alice", "Hallo alles goed", "12:00")))
            return sequenceOf(
                initial,
                searched.copy(isLoading = true),
                searched,
                searched.copy(hasError = true),
                initial.copy(isAvailable = false),
                results.copy(canLoadMore = true),
                results.copy(isLoading = true),
            )
        }
}
