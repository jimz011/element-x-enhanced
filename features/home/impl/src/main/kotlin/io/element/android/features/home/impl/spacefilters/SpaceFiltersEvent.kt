/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spacefilters

import io.element.android.libraries.matrix.api.spaces.SpaceServiceFilter

sealed interface SpaceFiltersEvent {
    // Only valid in Unselected state
    sealed interface Unselected : SpaceFiltersEvent {
        data object ShowFilters : Unselected
        data class SelectFilter(val spaceFilter: SpaceServiceFilter) : Unselected
    }

    // Only valid in Selecting state
    sealed interface Selecting : SpaceFiltersEvent {
        /** Dismiss the sheet, restoring whatever space was selected before it was opened. */
        data object Cancel : Selecting
        /** Leave the sheet showing all chats, i.e. no space filter at all. */
        data object ClearSelection : Selecting
        data class SelectFilter(val spaceFilter: SpaceServiceFilter) : Selecting
    }

    // Only valid in Selected state
    sealed interface Selected : SpaceFiltersEvent {
        data object ShowFilters : Selected
        data object ClearSelection : Selected
        data class SelectFilter(val spaceFilter: SpaceServiceFilter) : Selected
    }
}
