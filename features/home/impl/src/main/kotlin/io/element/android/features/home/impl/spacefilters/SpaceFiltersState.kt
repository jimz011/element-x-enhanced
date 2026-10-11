/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spacefilters

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import io.element.android.libraries.matrix.api.spaces.SpaceServiceFilter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
sealed interface SpaceFiltersState {
    data object Disabled : SpaceFiltersState

    data class Unselected(
        val availableFilters: ImmutableList<SpaceServiceFilter>,
        val eventSink: (SpaceFiltersEvent.Unselected) -> Unit,
    ) : SpaceFiltersState

    data class Selecting(
        val availableFilters: ImmutableList<SpaceServiceFilter>,
        val searchQuery: TextFieldState,
        val selectedFilter: SpaceServiceFilter?,
        val eventSink: (SpaceFiltersEvent.Selecting) -> Unit,
    ) : SpaceFiltersState {
        val visibleFilters: ImmutableList<SpaceServiceFilter>
            get() {
                val query = searchQuery.text.toString()
                if (query.isBlank()) return availableFilters
                return availableFilters.filter { filter ->
                    filter.spaceRoom.displayName.contains(query, ignoreCase = true) ||
                        (filter.spaceRoom.canonicalAlias?.value ?: "").contains(query, ignoreCase = true)
                }.toImmutableList()
            }
    }

    data class Selected(
        val availableFilters: ImmutableList<SpaceServiceFilter>,
        val selectedFilter: SpaceServiceFilter,
        val eventSink: (SpaceFiltersEvent.Selected) -> Unit,
    ) : SpaceFiltersState
}

fun SpaceFiltersState.availableFilters(): ImmutableList<SpaceServiceFilter> {
    return when (this) {
        SpaceFiltersState.Disabled -> emptyList<SpaceServiceFilter>().toImmutableList()
        is SpaceFiltersState.Unselected -> availableFilters
        is SpaceFiltersState.Selecting -> availableFilters
        is SpaceFiltersState.Selected -> availableFilters
    }
}

fun SpaceFiltersState.selectedFilter(): SpaceServiceFilter? {
    return when (this) {
        is SpaceFiltersState.Selected -> this.selectedFilter
        is SpaceFiltersState.Selecting -> this.selectedFilter
        else -> null
    }
}

/**
 * Open the space sheet, whatever state we are currently in. Unlike the upstream top bar button this
 * never toggles the selection off, because in classic the button only ever opens the space list.
 */
fun SpaceFiltersState.showFilters() {
    when (this) {
        SpaceFiltersState.Disabled,
        is SpaceFiltersState.Selecting -> Unit
        is SpaceFiltersState.Unselected -> eventSink(SpaceFiltersEvent.Unselected.ShowFilters)
        is SpaceFiltersState.Selected -> eventSink(SpaceFiltersEvent.Selected.ShowFilters)
    }
}

fun SpaceServiceFilter?.into(): RoomListFilter? {
    return this?.let { RoomListFilter.Identifiers(descendants) }
}
