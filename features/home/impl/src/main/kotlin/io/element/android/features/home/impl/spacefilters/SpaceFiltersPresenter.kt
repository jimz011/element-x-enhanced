/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spacefilters

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.spaces.SpaceServiceFilter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

@Inject
class SpaceFiltersPresenter(
    private val matrixClient: MatrixClient,
) : Presenter<SpaceFiltersState> {
    @Composable
    override fun present(): SpaceFiltersState {
        val availableFilters by remember {
            matrixClient.spaceService.spaceFiltersFlow.map { it.toImmutableList() }
        }.collectAsState(initial = persistentListOf())

        if (availableFilters.isEmpty()) {
            return SpaceFiltersState.Disabled
        }

        var selectionMode by remember { mutableStateOf<SelectionMode>(SelectionMode.Unselected) }

        fun handleUnselectedEvent(event: SpaceFiltersEvent.Unselected) {
            when (event) {
                SpaceFiltersEvent.Unselected.ShowFilters -> {
                    selectionMode = SelectionMode.Selecting(previous = null)
                }
                is SpaceFiltersEvent.Unselected.SelectFilter -> {
                    selectionMode = SelectionMode.Selected(event.spaceFilter)
                }
            }
        }

        fun handleSelectingEvent(event: SpaceFiltersEvent.Selecting) {
            val previous = (selectionMode as? SelectionMode.Selecting)?.previous
            when (event) {
                SpaceFiltersEvent.Selecting.Cancel -> {
                    // Dismissing the sheet must not silently drop the space the user was already in.
                    selectionMode = previous?.let { SelectionMode.Selected(it) } ?: SelectionMode.Unselected
                }
                SpaceFiltersEvent.Selecting.ClearSelection -> {
                    selectionMode = SelectionMode.Unselected
                }
                is SpaceFiltersEvent.Selecting.SelectFilter -> {
                    selectionMode = SelectionMode.Selected(event.spaceFilter)
                }
            }
        }

        fun handleSelectedEvent(event: SpaceFiltersEvent.Selected) {
            when (event) {
                SpaceFiltersEvent.Selected.ShowFilters -> {
                    selectionMode = SelectionMode.Selecting(previous = (selectionMode as? SelectionMode.Selected)?.filter)
                }
                SpaceFiltersEvent.Selected.ClearSelection -> {
                    selectionMode = SelectionMode.Unselected
                }
                is SpaceFiltersEvent.Selected.SelectFilter -> {
                    selectionMode = SelectionMode.Selected(event.spaceFilter)
                }
            }
        }

        return when (val mode = selectionMode) {
            SelectionMode.Unselected -> SpaceFiltersState.Unselected(
                availableFilters = availableFilters,
                eventSink = ::handleUnselectedEvent,
            )
            is SelectionMode.Selecting -> {
                val searchQuery = rememberTextFieldState()
                SpaceFiltersState.Selecting(
                    availableFilters = availableFilters,
                    searchQuery = searchQuery,
                    selectedFilter = mode.previous,
                    eventSink = ::handleSelectingEvent,
                )
            }
            is SelectionMode.Selected -> {
                // Re-derive from the latest availableFilters every time, rather than caching a
                // snapshot: the raw filter can go stale (the space is renamed, or leaves/rejoins),
                // and the aggregation below depends on the current sibling list too.
                val upToDateFilter = availableFilters.firstOrNull { it.spaceRoom.roomId == mode.filter.spaceRoom.roomId }
                if (upToDateFilter == null) {
                    // The selected space is no longer available (left, or filters still loading).
                    LaunchedEffect(Unit) { selectionMode = SelectionMode.Unselected }
                    SpaceFiltersState.Unselected(
                        availableFilters = availableFilters,
                        eventSink = ::handleUnselectedEvent,
                    )
                } else {
                    SpaceFiltersState.Selected(
                        availableFilters = availableFilters,
                        selectedFilter = upToDateFilter.withRecursiveDescendants(availableFilters),
                        eventSink = ::handleSelectedEvent,
                    )
                }
            }
        }
    }
}

private sealed interface SelectionMode {
    data object Unselected : SelectionMode

    /** @param previous the space that was selected when the sheet was opened, restored on cancel. */
    data class Selecting(val previous: SpaceServiceFilter?) : SelectionMode
    data class Selected(val filter: SpaceServiceFilter) : SelectionMode
}
