/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spacefilters

import io.element.android.libraries.matrix.api.spaces.SpaceServiceFilter

/**
 * The Rust SDK only flattens a space filter's [SpaceServiceFilter.descendants] recursively for
 * level-1 (child) spaces. A level-0 (top level) space's own descendants list is its *direct*
 * children only -- which, for a child that is itself a subspace, is that subspace's own room id,
 * not the rooms inside it. See matrix-sdk-ui's `SpaceService::build_space_filters`: "the first level
 * space filters only include direct descendants while second level ones contain all descendants".
 *
 * So selecting a parent space and filtering the room list by its raw descendants alone hides every
 * room that only lives inside one of its child spaces -- which is why picking a parent showed no
 * chats. To show "all chats from this space and its children", as classic does, union the parent's
 * own descendants with the already-fully-flattened descendants of each of its level-1 children.
 *
 * [availableFilters] must be the flat list from [io.element.android.libraries.matrix.api.spaces.SpaceService.spaceFiltersFlow],
 * which orders every level-1 filter directly after its level-0 parent.
 */
internal fun SpaceServiceFilter.withRecursiveDescendants(
    availableFilters: List<SpaceServiceFilter>,
): SpaceServiceFilter {
    if (level != 0) return this
    val index = availableFilters.indexOfFirst { it.level == 0 && it.spaceRoom.roomId == spaceRoom.roomId }
    if (index == -1) return this
    val children = availableFilters.subList(index + 1, availableFilters.size).takeWhile { it.level != 0 }
    if (children.isEmpty()) return this
    val aggregatedDescendants = (descendants + children.flatMap { it.descendants }).distinct()
    return copy(descendants = aggregatedDescendants)
}
