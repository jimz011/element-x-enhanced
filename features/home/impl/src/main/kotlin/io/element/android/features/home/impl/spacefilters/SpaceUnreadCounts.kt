/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spacefilters

import androidx.compose.runtime.Immutable
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.spaces.SpaceServiceFilter
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap

/**
 * Classic shows an unread-room-count badge on the space switcher button, and one per space inside
 * the sheet. [totalUnreadRoomCount] is account-wide (what the switcher button and the "All chats"
 * row show); [countsBySpaceRoomId] is per space, keyed by [SpaceServiceFilter.spaceRoom]'s room id.
 */
@Immutable
data class SpaceUnreadCounts(
    val totalUnreadRoomCount: Int,
    val countsBySpaceRoomId: ImmutableMap<RoomId, Int>,
)

fun emptySpaceUnreadCounts() = SpaceUnreadCounts(
    totalUnreadRoomCount = 0,
    countsBySpaceRoomId = persistentMapOf(),
)

fun SpaceUnreadCounts.countFor(filter: SpaceServiceFilter): Int = countsBySpaceRoomId[filter.spaceRoom.roomId] ?: 0

/**
 * A space's count is the number of unread rooms among its descendants, aggregated the same way
 * [withRecursiveDescendants] aggregates the room list filter itself -- otherwise a parent space's
 * badge would miss every unread room that only lives inside one of its children.
 */
internal fun buildSpaceUnreadCounts(
    summaries: List<RoomListRoomSummary>,
    availableFilters: List<SpaceServiceFilter>,
): SpaceUnreadCounts {
    val unreadRoomIds = summaries
        .asSequence()
        .filter { it.hasNewContent }
        .mapTo(mutableSetOf()) { it.roomId }
    val countsBySpaceRoomId = availableFilters.associate { filter ->
        val aggregated = filter.withRecursiveDescendants(availableFilters)
        filter.spaceRoom.roomId to aggregated.descendants.count { it in unreadRoomIds }
    }
    return SpaceUnreadCounts(
        totalUnreadRoomCount = unreadRoomIds.size,
        countsBySpaceRoomId = countsBySpaceRoomId.toImmutableMap(),
    )
}
